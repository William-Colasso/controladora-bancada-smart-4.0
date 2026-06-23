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

- **Two controller styles:** REST controllers (`PedidoController`, `EstoqueController`, `ExpedicaoController`) expose `/api/**` returning DTOs as JSON. `PageController` is an MVC `@Controller` that returns **Thymeleaf view names** (`home`, `formulario`, `dashboard/dashboard`, `pedidos/pedidos`) and injects model attributes.
- **DTOs are Java `record`s** with static `fromEntity(...)` and instance `toEntity()` converters (see `EstoqueResponseDTO`). Services map entities ↔ DTOs at the boundary; controllers never expose entities directly.
- **Domain enums carry an int `value`** (e.g. `CorBloco`: VAZIO=0, PRETO=1, VERMELHO=2, AZUL=3). This integer is what crosses to the frontend and **must stay in sync** with `static/js/core/enums.js` (`COR_INT_TO_NAME`).
- **Data model:** `Pedido 1—N Bloco 1—N Lamina`; `Estoque` holds bloco positions; `Expedicao` holds finished pedidos. Status flow: `PENDENTE → PRODUCAO/EM ANDAMENTO → CONCLUIDO`.
- **Exceptions:** custom exceptions in `exception/` are translated to HTTP responses by `GlobalExceptionHandler` (`@RestControllerAdvice`) using `ErrorResponseDTO`.

### PLC / CLP integration — `service/clp/`
Talks to a **Siemens S7 PLC** over TCP **port 102** (`S7ProtocolClient`, `PlcConnector`). `PlcConnectionService` pools one connection per IP in a `ConcurrentHashMap` (`getConnection`/`disconnect`/`closeAll`). This — not WebSocket — is the real hardware link (the README's "WebSocket" claim is aspirational).

**Per-station IP at runtime — `ClpIpRegistry`.** Each of the 4 stations (`EstacaoClp`: ESTOQUE, PROCESSO, MONTAGEM, EXPEDICAO) has its own CLP IP. `ClpIpRegistry` (`@Component`, all methods `synchronized`, `EnumMap`) is seeded at boot from `clp.ip.*` properties and **mutated at runtime** via `ClpIpController` (`GET /api/clp/ips`, `PUT /api/clp/ips/{estacao}` body `{"ip":"x.x.x.x"}`). SSE producers read `getIp(estacao)` **every poll cycle**, so an IP change takes effect without restart. On change, `setIp` validates the IP (regex + octet ≤255 → `IllegalArgumentException` → 400) and evicts the old connection (`plcConnectionService.disconnect`) only if no other station still uses it. **`EstacaoClp` has two name forms:** `getFrontKey()` (frontend/SSE key — note PROCESSO → `"producao"`) vs `apiName()` (REST path — `"processo"`); `fromApi()` resolves the REST path or throws 400.

**Write/command path — on-demand REST (`ClpComandoService` + `ClpComandoController`).** The write handshake (`RecebidoOP`, magazine add/remove) lives in the `*ClpService` (`lerEProcessar`/`processData`), which now implement `EstacaoClpHandshake` (`estacao()` + `lerEProcessar(ip)`). `ClpComandoService` injects `List<EstacaoClpHandshake>` into an `EnumMap` and dispatches per station, resolving the IP from `ClpIpRegistry` **on each call** (same source as the read side). `ClpComandoController` exposes `POST /api/clp/{estacao}/processar` (one pass on a station) and `POST /api/clp/processar` (all four). **No scheduler: each call is a single pass — the caller repeats** (frontend drives it with `createPoller`). The resulting state reaches the browser via the **SSE** channel, not the POST response (which only confirms the pass). Each `lerEProcessar` wraps `readBlock`+`processData` in `synchronized(connector)` to serialize with the read-only SSE reads on the same S7 socket; the writes inside `processData` stay gated by `estado.isReadOnly()`.

### Real-time SSE module (read-only) — `service/sse/`
Pushes live CLP/grid state to the browser, **replacing the dashboard's 3s polling**. Pattern: **capture → route → deliver**, coupled only by event TYPE (Spring `ApplicationEventPublisher` + `@EventListener`). Servlet-based `SseEmitter` (no WebFlux on classpath). Enabled by `@EnableScheduling` + `@EnableAsync` on `Application.java`.

- **Strictly read-only:** producers call **only** `PlcConnector.readBlock` — never `writeBit/writeByte/writeInt`, never `lerEProcessar`/`processData`, never mutate the shared `*CLP` beans or `EstadoProducaoService`. The write/handshake path stays in `*ClpService` and is triggered by a **separate REST API** (`POST /api/clp/{estacao}/processar` via `ClpComandoService`/`ClpComandoController` — see the Write/command path above and memory `sse-clp-read-only`).
- **Producers** (`service/sse/producer/`, `@Scheduled(fixedDelayString=...)`, publish **only on change** vs a cached last snapshot):
  - 4 station-status producers extend `EstacaoStatusProducerBase` — each subclass fixes its `EstacaoClp` + `(db, size, opByte, flagsByte)`. `derivar(byte[])` maps status bits → `estado` (emergencia→off; aguardando/manual→pause; ocupado→on; else off) + `funcionamento` (finish→2; start→1; ocupado→0; else null). `synchronized(connector)` guards the S7 socket; read error → `disconnect(ip)` + offline event.
  - `EstoqueGridProducer` / `ExpedicaoGridProducer` source grids from MySQL (`EstoqueService.getTodos` / `ExpedicaoService.listarTodos`) — same canonical source as the REST endpoints.
- **Events** (`dto/event/`, records): `EstacaoStatusEvent(estacao, estado, funcionamento)`, `EstoqueGridEvent(posicoes)`, `ExpedicaoGridEvent(posicoes)`.
- **Routing/delivery** (`service/sse/`): `SseNotifier` (one `@Async @EventListener` per event type → `registry.broadcast(name, dto)`); `SseEmitterRegistry` (`CopyOnWriteArrayList<SseEmitter>`, removes dead clients per-send so one failure doesn't break the rest); `SseController` `GET /api/stream` (`text/event-stream`). SSE event names: `estacao-status`, `estoque`, `expedicao`.
- **Adding a new source** = new producer + new event record + one `@EventListener` in `SseNotifier`. Nothing existing is touched.
- **Frontend:** `core/sse.js` (`createSse(path)` → `EventSource` wrapper with `on(evento, cb)` + auto-reconnect backoff). `pages/dashboard.js` consumes `estoque`/`expedicao`; `pages/home.js` feeds `estacao-status` into `bancadaStatus.setEstado/setFuncionamento`.

### Frontend — vanilla ES modules, **no build step**
Served as static assets from `src/main/resources/static/`, rendered by Thymeleaf templates in `templates/`.

- **Three JS layers** under `static/js/`:
  - `pages/*.js` — one entry module per page, loaded via `<script type="module" th:src="@{/js/pages/<page>.js}">`. Owns page state and wiring.
  - `core/*.js` — shared singletons/utilities: `Api` (fetch wrapper, throws on non-2xx), `Toast`, `createPoller` (interval polling with pause/resume/refresh), `createSse` (`EventSource` wrapper, see SSE module), `enums`, `format`, `dom`.
  - `components/*.js` — pure render helpers (`createXCell`/`renderXCell`) that build/update DOM nodes; no fetching.
- **SSR hydration pattern** (see `pages/dashboard.js`): the page controller serializes initial data to JSON into hidden `<input>`s; the JS reads them on load (`carregarDadosIniciais`) and renders immediately. Live updates now come from **SSE** (`createSse`), not polling — the dashboard's old `createPoller` was replaced (see the SSE module section). `createPoller` is still the pattern for any page that polls `/api/**`; after a mutation, do `poller.pause()` → call API → `poller.refresh()` → `poller.resume()`.
- **Thymeleaf fragments** live in `templates/fragments/` (`smart40Fragments.html` for `headDeps`/`navbar`, `components.html` for reusable component markup) and are pulled in via `th:replace="~{fragments/... :: name(args)}"`.
- **CSS** mirrors this split: `static/css/components/*.css` for component styles, page-level CSS at `static/css/<page>.css`, with `vars.css`/`base.css` as the design-system base.

### The `bancada-status` component
`#bancada-status` (fragment in `templates/fragments/components.html`) stacks absolutely-positioned `<img>` overlays on a base image. `static/js/components/bancadaStatus.js` default-exports a controller that builds image filenames from `img/bancada/` and swaps `src` per station: a **station-state** layer (`off/on/pause`, always visible) and an optional **funcionamento/ocupação** layer (`0/1/2`, hidden when no task). Filenames are built by convention — note the two families' different separators and station spellings (`Est`/`Estoque`).

### `SmartService` — orchestrator for production flow
`service/SmartService.java` is the single entry point that transitions a pedido from `PENDENTE` to `PRODUCAO`. `PedidoService.criar()` only validates and persists (optimistic check); **actual resource consumption happens in `SmartService.enviarParaProducao()`**: reserves an expedição slot, deducts estoque, saves via `pedidoRepository` (not via `pedidoService.atualizar()`), then sends the 60-byte S7 payload to the ESTOQUE station's PLC, resolving the IP via `ClpIpRegistry.getIp(ESTOQUE)` (same source as the read side — no longer hardcoded). A secondary REST call to an ESP32 at `10.74.241.245/api/move_pos` selects the physical lid (tampa). CLP failures are logged but do not roll back the already-persisted reservation.

### 3D Pedido Viewer — `static/js/components/pedidoViewer.js`
Vanilla ES module that renders a Three.js 3D block stack representing a pedido. Zero build step — depends on Three.js 0.176.0 via an **importmap** added to the `headDeps` fragment in `smart40Fragments.html` (must precede any `<script type="module">`).

**API:** `createPedidoViewer(containerEl)` → `{ update(pedido), dispose() }`. Container must have CSS-defined dimensions. `update(null)` shows the empty-state placeholder; `update(pedidoDTO)` builds the 3D scene.

**Geometry** mirrors `3d-em-react/blockModel.ts`: base floor + 4 corner columns (with `COL_OVERSHOOT` above the body) + back wall + colored blades on the 3 open faces (FRENTE=+Z, ESQUERDA=−X, DIREITA=+X) + tampa on top. Each call to `update()` disposes the previous scene's geometries and materials to avoid WebGL context leaks.

**Enum int values handled locally** (not via `core/enums.js`) because `PosicaoLamina` has `@JsonValue` 1/2/3 but `enums.js` maps 0/1/2 — the viewer uses its own correct mapping.

**Where it's used:**
- `pedidos.html` — detail panel is a 2-column grid (300px viewer + info text); viewer is created once on first `openDetail()` and reused across polling cycles.
- `formulario.html` — 2-column layout with a sticky "STEP 03 Preview 3D" sidebar; updates in real-time via `change` event delegation on `#blocos-container` plus explicit calls on lâmina add/remove.

### `3d-em-react/` — TypeScript/React/Three.js reference source
Located at `static/3d-em-react/`, this is the **original 3D viewer implementation** using react-three-fiber and drei. It is **not built or served** — it exists as a design reference only. The Vanilla JS viewer (`pedidoViewer.js`) was ported from it.

### Tests — unit only, Mockito-based
All service tests (`src/test/…/service/`) use `@ExtendWith(MockitoExtension.class)` with mocked repositories — no Spring context, no real database. `ApplicationTests` is the only Spring context test. When adding new service tests, follow the existing `buildX()` factory-method pattern in each test class.
