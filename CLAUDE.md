# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Controladora da Bancada SMART 4.0 — a Spring Boot app that controls/simulates an Industry-4.0 manufacturing workbench (estoque → blocos/lâminas → expedição). Codebase, comments, and domain names are in **Portuguese**; keep that language when adding code.

The Maven project lives in the **`app_smart_4.0/`** subdirectory, not the repo root. Run all build/run/test commands from there.

## Commands

```bash
cd app_smart_4.0

# Run the app (http://localhost:8088). Windows: use mvnw.cmd
./mvnw spring-boot:run

# Build / package
./mvnw clean package
./mvnw clean package -DskipTests

# All tests
./mvnw test

# A single test class or method
./mvnw test -Dtest=PedidoServiceTest
./mvnw test -Dtest=PedidoServiceTest#nomeDoMetodo
```

Requires **Java 17** and a **MySQL 8** instance on `localhost:3306` with database `DB_SMART_4_0` (credentials in `src/main/resources/application.properties`, default `root`/`senai`). `ddl-auto=update` auto-creates tables; `DataInitializer` (CommandLineRunner) seeds **28 estoque positions** and **12 expedição positions** on first run if those tables are empty.

## Stack notes (Spring Boot 4.0.6 — recent, watch for differences)

- Jackson 3: imports are `tools.jackson.*`, **not** `com.fasterxml.jackson.*`.
- Web starter is `spring-boot-starter-webmvc` (not `-web`); test starters follow the same `-test` naming.
- **Lombok** is used throughout: `@RequiredArgsConstructor` for constructor injection (prefer this over `@Autowired`), `@Builder` on entities, `@Slf4j` for logging.

## Architecture

### Backend — layered, package-by-type under `com.tecdes.smart.app_smart_40`
`controller → service → repository → model` (JPA entities), with `dto/` for transfer and `model/enums/` for domain enums.

- **Two controller styles:** REST controllers (`PedidoController`, `EstoqueController`, `ExpedicaoController`) expose `/api/**` returning DTOs as JSON. `PageController` is an MVC `@Controller` that returns **Thymeleaf view names** (`home`, `formulario`, `dashboard/dashboard`, `pedidos/pedidos`, `estacoes/estacoes`) and injects model attributes (none for `estacoes`, which is a pure SSE consumer). `formulario` also accepts an optional `?id=N` → **edit mode**: it loads the pedido and injects it as `pedidoEditJson` (Jackson JSON, enums as ints) for the form to prefill; without `id` it's create mode (`pedidoEditJson` = null).
- **DTOs are Java `record`s** with static `fromEntity(...)` and instance `toEntity()` converters (see `EstoqueResponseDTO`). Services map entities ↔ DTOs at the boundary; controllers never expose entities directly.
- **Domain enums carry an int `value`** (e.g. `CorBloco`: VAZIO=0, PRETO=1, VERMELHO=2, AZUL=3). This integer is what crosses to the frontend and **must stay in sync** with `static/js/core/enums.js` (`COR_INT_TO_NAME`).
- **Data model:** `Pedido 1—N Bloco 1—N Lamina`; `Estoque` holds bloco positions; `Expedicao` holds finished pedidos. Status flow: `PENDENTE → PRODUCAO → CONCLUIDO` — `PENDENTE→PRODUCAO` via `SmartService.enviarParaProducao`; `PRODUCAO→CONCLUIDO` **automatically** when the expedição CLP stores the pedido's OP (see *Editar pedido + sincronização de status*) **or** manually via `PUT /api/pedidos/{id}/status` (`concluir`).
- **Exceptions:** custom exceptions in `exception/` are translated to HTTP responses by `GlobalExceptionHandler` (`@RestControllerAdvice`) using `ErrorResponseDTO`.

### PLC / CLP integration — `service/clp/`
Talks to a **Siemens S7 PLC** over TCP **port 102** (`S7ProtocolClient`, `PlcConnector`). `PlcConnectionService` pools one connection per IP in a `ConcurrentHashMap` (`getConnection`/`disconnect`/`closeAll`). This — not WebSocket — is the real hardware link (the README's "WebSocket" claim is aspirational).

**Per-station IP at runtime — `ClpIpRegistry`.** Each of the 4 stations (`EstacaoClp`: ESTOQUE, PROCESSO, MONTAGEM, EXPEDICAO) has its own CLP IP. `ClpIpRegistry` (`@Component`, all methods `synchronized`, `EnumMap`) is seeded at boot from `clp.ip.*` properties and **mutated at runtime** via `ClpIpController` (`GET /api/clp/ips`, `PUT /api/clp/ips/{estacao}` body `{"ip":"x.x.x.x"}`). SSE producers read `getIp(estacao)` **every poll cycle**, so an IP change takes effect without restart. On change, `setIp` validates the IP (regex + octet ≤255 → `IllegalArgumentException` → 400) and evicts the old connection (`plcConnectionService.disconnect`) only if no other station still uses it. **`EstacaoClp` name forms:** `getFrontKey()` (frontend/SSE key) and `apiName()` (REST path) — currently **identical** for all 4 (`estoque/processo/montagem/expedicao`); `fromApi()` resolves the REST path or throws 400. ⚠️ The `bancada-status` overlay (`bancadaStatus.js` `ESTACOES`, `components.html` ids) still keys the process station as `"producao"`, so it **doesn't match** the `"processo"` frontKey the backend emits — pre-existing overlay mismatch for that one station, unrelated to the read-path refactor.

**Write/command path — the ONLY S7 socket reader (`ClpComandoService` + `ClpComandoController`).** The handshake (`RecebidoOP`, magazine add/remove) lives in the `*ClpService` (`lerEProcessar`/`processData`), which implement `EstacaoClpHandshake` (`estacao()` + `boolean lerEProcessar(ip)` + `dados()`). `lerEProcessar` returns `true` when the read+process ran (connector ok), `false` when the station is unreachable — the "passada lida" signal. `ClpComandoService` injects `List<EstacaoClpHandshake>` into an `EnumMap` and dispatches per station, resolving the IP from `ClpIpRegistry` **on each call**. `ClpComandoController` exposes `POST /api/clp/{estacao}/processar` (one pass) and `POST /api/clp/processar` (all four). After each pass `ClpComandoService.processar()` publishes an **`EstacaoHeartbeat` event** (only when `lerEProcessar` returned `true`) → broadcast as the **ephemeral** SSE `estacao-heartbeat` (liveness pulse; the full `*CLP` data goes out separately via the read-only `clp_data` producers, **not** from here anymore). **Driven automatically by `ClpProcessamentoScheduler`** (`@Scheduled(fixedDelayString="${clp.processar.interval:300}")`, default **300ms**) which calls `processarTodas()` — **gated by SSE client presence** (`sseRegistry.count() == 0` → skip): opening any SSE page runs the handshake for **all** stations, closing the last tab stops it. There is **no** front-end toggle/poller. The resulting state reaches the browser via the **SSE** channel, not the POST response. Each `lerEProcessar` wraps `readBlock`+`processData` in `synchronized(connector)`; the writes inside `processData` stay gated by `estado.isReadOnly()`. **The `*CLP` beans are filled ONLY here** (top of `processData`), which also stamps `EstadoProducaoService.ultimoLeituraMillis = now` — the **freshness** signal that gates the periodic `clp_data`/`status` producers (below). This is now the **single** S7 reader: the status producers no longer hit the socket (they derive from the bean). With no SSE client (or IP unset / CLP unreachable), nothing fills the beans and no heartbeat is emitted, so the front flips to "sem comunicação".

**Read/leitura gating — by SSE client presence (`SseEmitterRegistry.count()`).** The SSE producers do **not** read at boot, and there is **no per-station opt-in** (the old `ClpLeituraRegistry` + `/conectar`/`/desconectar` endpoints + `ClpHealthService` were removed). Instead, the status + grid producers and the write-path scheduler check `sseRegistry.count() == 0` at the top of `poll()` and return early (clearing the `ultimo` cache so a reconnect re-emits); the `clp_data` producers gate on reading **freshness** instead, which only goes fresh while the count-gated write path runs — so the S7 socket (write path) / MySQL (grids) is only hit while **at least one browser has an open `EventSource`** on `GET /api/stream`. Opening *any* SSE-consuming page (home, `/estacoes`, dashboard) is enough to start reading **all** stations; closing the last tab stops it. This makes the live data work on every screen without a manual "connect" step, while still idling when nobody is watching. IPs are configured **separately** via `ClpIpController` (`PUT /api/clp/ips/{estacao}`) — that is the only job of the home "connection" UI now.

### Real-time SSE module (read-only) — `service/sse/`
Pushes live CLP/grid state to the browser, **replacing the dashboard's 3s polling**. Pattern: **capture → route → deliver**, coupled only by event TYPE (Spring `ApplicationEventPublisher` + `@EventListener`). Servlet-based `SseEmitter` (no WebFlux on classpath). Enabled by `@EnableScheduling` + `@EnableAsync` on `Application.java`.

- **Never write, never read the socket:** producers never `writeBit/writeByte/writeInt`, never `lerEProcessar`/`processData`, never mutate the shared `*CLP` beans or `EstadoProducaoService`. They no longer call `PlcConnector.readBlock` either — the read/processing is centralized in the **write path** (`*ClpService.lerEProcessar`, see above and memory `sse-clp-read-only`); producers only **read the bean** the write path filled.
- **Producers** (`service/sse/producer/`, `@Scheduled(fixedDelayString=...)`, publish **only on change** vs a cached last snapshot, **freshness-gated** via `ultimoLeituraMillis`):
  - 4 station-status producers extend `EstacaoStatusProducerBase` — each subclass injects its concrete `*CLP` bean + fixes its `EstacoesCLP`. **Derives from the bean (no socket):** `derivar()` maps the bean's flags → `estado` (emergencia→off; aguardando/manual→pause; ocupado→on; else off) + `funcionamento` (finish→2; start→1; ocupado→0; else null). **Gated by SSE client presence** (`poll()` returns early when `sseRegistry.count() == 0`) **and freshness:** when `now - ultimoLeituraMillis > 1500ms` (comunicação parada / IP unset) it emits `"off"` instead of deriving — preserving the offline UX. `@Scheduled` `${clp.poll.interval:1000}`.
  - `EstoqueGridProducer` / `ExpedicaoGridProducer` source grids from MySQL (`EstoqueService.getTodos` / `ExpedicaoService.listarTodos`) — same canonical source as the REST endpoints; also gated by `sseRegistry.count()` (no DB query when no client is connected).
  - **`estacao-all` producers** (`service/sse/producer/clp_data/`): abstract `EstacaoCLPProducer` + 4 `@Component` subclasses (`Estoque/Expedicao/Processo/MontagemCLPProducer`), each with its own `@Scheduled` cadence — **estoque/expedicao 300ms, processo/montagem 1s** (`${clp.poll.<estacao>:...}`). They re-publish the in-memory `*CLP` bean (filled by the write path) as `EstacaoAllData`, **on-change**: since the producer holds the **same mutable singleton** the write path fills, change is detected by comparing the bean's **serialized JSON signature** (an `ObjectMapper` is injected; handles array fields like `posicoesOcupadas[]`/`orderExpedicao[]` with no per-bean `equals`). Still **freshness-gated** (`publicar()` skips if `now - ultimoLeituraMillis > 1500ms`). The bean is injected by concrete type (`@Component("estoque")` etc.), the same singleton the matching `*ClpService` fills. This is now the **only** publisher of `estacao-all` (the write path stopped publishing it).
- **Events** (`dto/event/`, records): `EstacaoStatusEvent(estacao, estado, funcionamento)`, `EstoqueGridEvent(posicoes)`, `ExpedicaoGridEvent(posicoes)`, `EstacaoAllData(estacao, dados)` (full `*CLP` bean; published only by the `clp_data` producers, on-change + freshness-gated), `EstacaoHeartbeat(estacao)` (liveness pulse published by the **write path** on each successful pass — ephemeral, not cached).
- **Routing/delivery** (`service/sse/`): `SseNotifier` (one `@Async @EventListener` per event type → `registry.broadcast(...)`); `SseEmitterRegistry` (`CopyOnWriteArrayList<SseEmitter>`, removes dead clients per-send so one failure doesn't break the rest). Because data producers publish **only on change**, the registry caches the **last payload per (event name + key)** (`broadcast` stores it) and **replays each cached snapshot to every newly-connected client** (`add`). `SseController` `GET /api/stream` (`text/event-stream`). SSE event names: `estacao-status`, `estoque`, `expedicao`, `estacao-all`, `estacao-heartbeat`. **`estacao-status` and `estacao-all` are generic across the 4 stations**, so `broadcast` has an overload `broadcast(evento, chave, dado)` — `SseNotifier` passes `evento.estacao()` as the key so the cache holds **one entry per station** and replay-on-connect delivers **all four**. **`estacao-heartbeat` uses `broadcastEfemero(evento, dado)`** — sent to all clients but **NOT cached** (a stale pulse replayed to a late client would falsely read as "leitura viva"; the front's watchdog self-corrects in ≤2.5s regardless).
- **Adding a new source** = new producer + new event record + one `@EventListener` in `SseNotifier`. Nothing existing is touched.
- **Frontend:** `core/sse.js` (`createSse(path)` → `EventSource` wrapper with `on(evento, cb)` + auto-reconnect backoff). `pages/dashboard.js` consumes `estoque`/`expedicao`; `pages/home.js` feeds `estacao-status` into `bancadaStatus.setEstado/setFuncionamento`; `pages/estacoes.js` (the `/estacoes` page) consumes **both** `estacao-status` (card badge/operação **and** the `bancada-status` component shown on top via `bancadaStatus.setEstado/setFuncionamento`) and `estacao-all` (the full `*CLP` bean rendered as a generic key/value grid per card — iterates whatever fields the backend sends, no hardcoded schema). No DB, no `/api/**`; the card `id` is the SSE `frontKey`. Every SSE-consuming page just calls `createSse().connect()`; opening it is what triggers reading (client-presence gating above). `home.js`'s per-station button now only **saves the IP** (`PUT /api/clp/ips/{estacao}`, "IP salvo"/"IP inválido" badge) and prefills the inputs from `GET /api/clp/ips` on load — it no longer enables/disables reading. The row id (`estoque-clp-ip` → `estoque`) is the REST `apiName`, which equals the SSE `frontKey` for every station.
  - **No comunicação toggle:** the write/handshake loop runs **automatically** server-side (`ClpProcessamentoScheduler`, gated by SSE presence) — there is no start/stop button. `home.js` keeps only an **informative** `#comunicacao-status` badge driven by the **`estacao-heartbeat`** pulse: each event stamps `ultimaLeitura` and a 1s `setInterval` shows green "Leitura automática ativa" (event in ≤2500ms) or dim "Sem leitura do CLP" (IP unset / CLP unreachable / no client).
  - **`/estacoes` freshness watchdog:** same heartbeat idea — but now keyed off **`estacao-heartbeat`** (not `estacao-all`, which became on-change and would falsely look idle for a steady station). `estacoes.js` stamps `ultimaLeitura[estacao]` + sets `data-comunicacao="on"` on each `estacao-heartbeat`, and a 1s `setInterval` flips any card with no pulse in >2500ms to `data-comunicacao="off"` — so an idle station that's actually being read is **not** confused with reading that has stopped. `renderDados` (the `estacao-all` handler) renders the key/value grid **and** surfaces the OP em execução (below).
  - **OP em execução + popup 3D:** `renderDados` reads `numeroOP` from the bean → fills a "OP em execução" line per card + shows a **Ver pedido** button (hidden when OP=0, `opAtual[estacao]` map). The button fetches `/api/pedidos` and filters by `ordemProducao` (small list — no dedicated endpoint), then opens a `<dialog id="pedido-modal">` reusing `createPedidoViewer` + `buildDetailHTML` from `components/`. Because the popup reuses the pedido-detail markup, `/estacoes` also links `pedidos.css` + `pedido-viewer.css`.

### Frontend — vanilla ES modules, **no build step**
Served as static assets from `src/main/resources/static/`, rendered by Thymeleaf templates in `templates/`.

- **Three JS layers** under `static/js/`:
  - `pages/*.js` — one entry module per page, loaded via `<script type="module" th:src="@{/js/pages/<page>.js}">`. Owns page state and wiring.
  - `core/*.js` — shared singletons/utilities: `Api` (fetch wrapper, throws on non-2xx), `Toast`, `createPoller` (interval polling with pause/resume/refresh), `createSse` (`EventSource` wrapper, see SSE module), `enums`, `format`, `dom`.
  - `components/*.js` — pure render helpers (`createXCell`/`renderXCell`) that build/update DOM nodes; no fetching.
- **SSR hydration pattern** (see `pages/dashboard.js`): the page controller serializes initial data to JSON into hidden `<input>`s; the JS reads them on load (`carregarDadosIniciais`) and renders immediately. Live updates now come from **SSE** (`createSse`), not polling — the dashboard's old `createPoller` was replaced (see the SSE module section). `createPoller` is still the pattern for any page that polls `/api/**`; after a mutation, do `poller.pause()` → call API → `poller.refresh()` → `poller.resume()`.
- **Thymeleaf fragments** live in `templates/fragments/` (`smart40Fragments.html` for `headDeps`/`navbar`, `components.html` for reusable component markup) and are pulled in via `th:replace="~{fragments/... :: name(args)}"`.
- **CSS** mirrors this split: `static/css/components/*.css` for component styles, page-level CSS at `static/css/<page>.css`, with `vars.css`/`base.css` as the design-system base. ⚠️ `.btn` sets `display:inline-flex`, which **beats the `hidden` attribute** — to toggle a `.btn`/anchor-`.btn` via `hidden` (e.g. `#detailEdit`, `.ver-pedido-btn`) the `.btn[hidden]{display:none}` rule in `components/button.css` is required.

### The `bancada-status` component
`#bancada-status` (fragment in `templates/fragments/components.html`) stacks absolutely-positioned `<img>` overlays on a base image. `static/js/components/bancadaStatus.js` default-exports a controller that builds image filenames from `img/bancada/` and swaps `src` per station: a **station-state** layer (`off/on/pause`, always visible) and an optional **funcionamento/ocupação** layer (`0/1/2`, hidden when no task). Filenames are built by convention — note the two families' different separators and station spellings (`Est`/`Estoque`).

### `SmartService` — orchestrator for production flow
`service/SmartService.java` is the single entry point that transitions a pedido from `PENDENTE` to `PRODUCAO`. `PedidoService.criar()` only validates and persists (optimistic check); **actual resource consumption happens in `SmartService.enviarParaProducao()`**: reserves an expedição slot, deducts estoque, saves via `pedidoRepository` (not via `pedidoService.atualizar()`), then sends the 60-byte S7 payload to the ESTOQUE station's PLC, resolving the IP via `ClpIpRegistry.getIp(ESTOQUE)` (same source as the read side — no longer hardcoded). A secondary REST call to an ESP32 at `10.74.241.245/api/move_pos` selects the physical lid (tampa). CLP failures are logged but do not roll back the already-persisted reservation.

### Editar pedido + sincronização de status pelo CLP
- **Editar (`PUT /api/pedidos/{id}` → `PedidoService.atualizar`):** edita **apenas** pedidos `PENDENTE` (guard → `IllegalStateException` → 400 via `GlobalExceptionHandler`). Re-roda as validações de `criar()` (tipo/estoque/lâminas), substitui os blocos **in place** (`pedido.getBlocos().clear()+addAll`, que exige `orphanRemoval=true` no `@OneToMany Pedido.blocos`) e **preserva** `id/ordemProducao/status/dataCriacao`. (O `atualizar` antigo fazia `dto.toEntity()+setId` — quebrado: zerava campos `NOT NULL` e orfanava blocos.)
- **CONCLUIDO automático (CLP→pedido):** o write path, ao guardar uma peça na expedição (`ExpedicaoClpService.processData` → `ExpedicaoService.guardarNaPosicao(posicao, opGuardadoExpedicao)`), além de vincular `Expedicao.pedidoAtual`, marca o pedido `PRODUCAO → CONCLUIDO` + `dataEntradaExpedicao`. É o **único** ponto de sync CLP→pedido, feito dentro de `ExpedicaoService` (via `pedidoRepository.findByOrdemProducao`) para evitar o ciclo de dependência `ExpedicaoService ↔ PedidoService` (PedidoService já injeta ExpedicaoService).

### 3D Pedido Viewer — `static/js/components/pedidoViewer.js`
Vanilla ES module that renders a Three.js 3D block stack representing a pedido. Zero build step — depends on Three.js 0.176.0 via an **importmap** added to the `headDeps` fragment in `smart40Fragments.html` (must precede any `<script type="module">`).

**API:** `createPedidoViewer(containerEl)` → `{ update(pedido), dispose() }`. Container must have CSS-defined dimensions. `update(null)` shows the empty-state placeholder; `update(pedidoDTO)` builds the 3D scene.

**Geometry** mirrors `3d-em-react/blockModel.ts`: base floor + 4 corner columns (with `COL_OVERSHOOT` above the body) + back wall + colored blades on the 3 open faces (FRENTE=+Z, ESQUERDA=−X, DIREITA=+X) + tampa on top. Each call to `update()` disposes the previous scene's geometries and materials to avoid WebGL context leaks.

**Enum int values handled locally** (not via `core/enums.js`) because `PosicaoLamina` has `@JsonValue` 1/2/3 but `enums.js` maps 0/1/2 — the viewer uses its own correct mapping.

**Where it's used:**
- `pedidos.html` — detail panel is a 2-column grid (300px viewer + info text); viewer is created once on first `openDetail()` and reused across polling cycles. The detail header has an **Editar** anchor (`#detailEdit` → `/formulario?id=`) shown **only** when the pedido is `PENDENTE`. Status filtering uses `normalizeStatus(p.status)` (REST returns the enum **int**, not the name).
- `formulario.html` — 2-column layout with a sticky "STEP 03 Preview 3D" sidebar; updates in real-time via `change` event delegation on `#blocos-container` plus explicit calls on lâmina add/remove. In **edit mode** (`window.PEDIDO_EDIT` set by `/formulario?id=N`) `formulario.js` prefills the selects + lâmina rows from the pedido (values are the enum ints) and switches submit to `PUT /api/pedidos/{id}`.

### `3d-em-react/` — TypeScript/React/Three.js reference source
Located at `static/3d-em-react/`, this is the **original 3D viewer implementation** using react-three-fiber and drei. It is **not built or served** — it exists as a design reference only. The Vanilla JS viewer (`pedidoViewer.js`) was ported from it.

### Tests — unit only, Mockito-based
All service tests (`src/test/…/service/`) use `@ExtendWith(MockitoExtension.class)` with mocked repositories — no Spring context, no real database. `ApplicationTests` is the only Spring context test. When adding new service tests, follow the existing `buildX()` factory-method pattern in each test class.
