# Refactor Review — Controladora Bancada SMART 4.0

Análise de lógica, performance, boas práticas e dependências.
Severidade: 🔴 bug/risco real · 🟡 problema de design · 🔵 limpeza/consistência

---

## 1. 🔴 `Pedido.java` — imports Jackson 2 em projeto Jackson 3

**Arquivo:** `model/Pedido.java` linhas 8-9

```java
// ERRADO — Jackson 2 (não existe no classpath do SB 4 / Jackson 3)
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

// CORRETO
import tools.jackson.annotation.JsonIgnore;
import tools.jackson.annotation.JsonManagedReference;
```

`application.properties` usa `spring-boot-starter-webmvc` + Jackson 3 (`tools.jackson.*`).
Com o import antigo a anotação é ignorada silenciosamente; `@JsonIgnore` em `expedicao` não
funciona e a entidade vaza no JSON em certas serializações.

Mesmos imports incorretos podem estar espalhados — vale um `grep -r "com.fasterxml.jackson"` no `src/main`.

**Também no mesmo arquivo:** `Instant` importado mas não usado (linha 6); `@PostPersist` importado e não usado (linha 23). Limpeza trivial.

---

## 2. 🔴 `SmartService.enviarParaProducao` — I/O de rede + `Thread.sleep` dentro de `@Transactional`

**Arquivo:** `service/SmartService.java` linha 51

```java
@Transactional
public void enviarParaProducao(Long idPedido) {
    ...
    pedidoRepository.save(pedido);   // commit ainda NÃO aconteceu
    enviarParaClp(pedido);           // ← abre socket TCP + Thread.sleep(800ms) DENTRO da transação
}
```

`enviarParaClp` → `connector.writeBlock` + `iniciarExecucaoPedido` → `Thread.sleep(800)`.
Durante todo esse tempo a conexão JDBC está segura pelo pool de transações.
Sob carga (múltiplos pedidos em paralelo), o pool esgota e o app trava.

**Correção mínima:** quebrar em dois métodos — um `@Transactional` só para o persist, outro
fora da transação para a comunicação CLP:

```java
@Transactional
public void persistirParaProducao(Long id) { reservar + consumir + save }

public void enviarParaProducao(Long id) {
    persistirParaProducao(id);
    enviarParaClp(pedidoRepository.findById(id).orElseThrow());
}
```

---

## 3. 🔴 `ExpedicaoClpService.processarHandshake` — bug de lógica booleana (linha 185)

**Arquivo:** `service/clp/estacao/ExpedicaoClpService.java` linha 185

```java
// ATUAL — De Morgan: !(adicionar && remover) → quase sempre true
if (!estado.isReadOnly() && (!expedicaoCLP.isAdicionarExpedicao() || !expedicaoCLP.isRemoverExpedicao())) {
    connector.writeBit(9, 2, 0, false); // RecebidoExpedicao = FALSE
}
```

A intenção é resetar `RecebidoExpedicao` quando **nenhuma** flag está ativa.
A condição atual dispara quando **pelo menos uma** é falsa — ou seja, quase sempre,
inclusive quando `adicionarExpedicao == true` (e deveria estar em TRUE).

```java
// CORRETO
if (!estado.isReadOnly() && !expedicaoCLP.isAdicionarExpedicao() && !expedicaoCLP.isRemoverExpedicao()) {
```

---

## 4. 🔴 `PlcConnectionService.closeAll` — race condition (não é `synchronized`)

**Arquivo:** `service/clp/connection/PlcConnectionService.java`

`getConnection` e `disconnect` são `synchronized`, mas `closeAll` não é.
Chamado no shutdown (ApplicationContext close) enquanto o scheduler ainda pode estar
executando `getConnection`. A iteração sobre `conexoes.entrySet()` + `conexoes.clear()` pode
sobrepor com um `put` concurrent → `ConcurrentModificationException` ou conexão perdida.

Adicionar `synchronized` em `closeAll`.

Adicionalmente: `conexoes` é `static` mas a classe é um singleton `@Service`. O `static` é
desnecessário e engana — se houver dois contextos no mesmo processo (testes de integração),
compartilham estado.

---

## 5. 🟡 `ExpedicaoGridProducer` — classe morta / duplicata

**Arquivo:** `service/sse/producer/dashboard/ExpedicaoGridProducer.java`

Toda a lógica desta classe foi migrada para `ClpEventoCoordinator.onExpedicaoMudou`
(event-driven, sem polling). O `ExpedicaoGridProducer` ainda existe e ainda está anotado com
`@Scheduled(fixedDelayString = "${clp.grid.interval:2000}")`, publicando `ExpedicaoGridEvent`
em paralelo com o coordinator.

Resultado: duplo disparo do grid de expedição a cada mutação — o event-driven + o scheduled
2s depois. Pode causar oscilações no front (grid exibido correto → atualizado novamente sem
mudança real).

**Ação:** deletar `ExpedicaoGridProducer.java` inteiro.

---

## 6. 🟡 `SmartService.enviarTampa` — dois POSTs HTTP para a mesma URL

**Arquivo:** `service/SmartService.java` linhas 221-244

```java
ResponseEntity<String> rawResponse = apiSeletorTampa.postForEntity(url, request, String.class); // POST 1
...
ResponseEntity<Map> response = apiSeletorTampa.postForEntity(url, request, Map.class);           // POST 2
```

Dois POSTs consecutivos com o mesmo corpo — o ESP32 executa o movimento **duas vezes**.
A primeira chamada (String) parece ser debug que nunca foi removido.

Remover a primeira chamada; ler diretamente como `Map`.

---

## 7. 🟡 `SmartService.enviarTampa` — `RestTemplate` instanciado a cada chamada

```java
RestTemplate apiSeletorTampa = new RestTemplate(); // novo objeto por chamada
```

`RestTemplate` cria um pool de conexões HTTP por instância. Instanciar a cada chamada
descarta o pool e abre nova conexão TCP para cada envio. Injetar como `@Bean` ou guardar
num campo `final`.

---

## 8. 🟡 `SmartService.iniciarExecucaoPedido` — `Boolean.parseBoolean("FALSE")` e catch vazio

**Arquivo:** `service/SmartService.java` linhas 192-210

```java
plcConnector.writeBit(9, 0, 0, Boolean.parseBoolean("FALSE")); // só escreve `false`
plcConnector.writeBit(9, 62, 0, Boolean.parseBoolean("TRUE")); // só escreve `true`
```

`Boolean.parseBoolean("FALSE")` é apenas `false` em tempo de compilação. Não há nenhum valor
dinâmico aqui; só confunde o leitor.

O `catch (Exception ex) {}` no fim do método (linha 207) engole silenciosamente qualquer falha
de comunicação CLP sem log. Um `log.error(...)` mínimo é necessário para diagnóstico.

---

## 9. 🟡 `PedidoRepository.findByOrdemProducao` — deve retornar `Optional`, não `List`

**Arquivo:** `repository/PedidoRepository.java` linha 21

`nr_ordem_producao` tem `@UniqueConstraint` — no máximo 1 resultado.
Retornar `List<Pedido>` força os callers a fazer `.stream().findFirst()` (veja
`ExpedicaoService.guardarNaPosicao` linha 65).

```java
// Mais expressivo, menos código nos callers
Optional<Pedido> findByOrdemProducao(Integer ordemProducao);
```

---

## 10. 🟡 `EstadoProducaoService` — bean Spring em pacote `model/`

**Arquivo:** `model/clp/EstadoProducaoService.java`

É um `@Component` (bean gerenciado pelo Spring, com estado compartilhado entre `*ClpService`s),
não uma entidade JPA nem um VO. Pertence ao pacote `service/clp/` ou `service/clp/state/`.
A atual localização quebra a convenção do projeto e pode confundir quem navega pelas entidades.

---

## 11. 🟡 `ExpedicaoService.primeiraExpedicaoLivre` — `.get()` sem `orElseThrow`

**Arquivo:** `service/ExpedicaoService.java` linha 53

```java
return ExpedicaoResponseDTO.fromEntity(expedicaoRepository.findFirstByPedidoAtualIsNull().get());
```

Se chamado em race condition (outra thread consumiu a última posição livre entre o
`existePosicaoLivre()` em `PedidoService` e esta chamada), lança `NoSuchElementException`
— exceção genérica sem mensagem útil. Substituir por `.orElseThrow(...)` com mensagem clara.

---

## 12. 🟡 `CLAUDE.md` vs código — `recomporFila` inconsistente

`CLAUDE.md` descreve: *"o eventual `PRODUCAO` (órfão) na cabeça + os `PENDENTE` por `dataCriacao`"*.
O código atual e o comentário dentro de `PedidoConsumerList.recomporFila` dizem: *"PENDENTEs
NÃO voltam à fila"*.

Se PENDENTEs devem voltar após restart (comportamento descrito no CLAUDE.md), o código está
incompleto. Se a decisão foi não reconsertar PENDENTEs, o CLAUDE.md está desatualizado.
**Alinhar a documentação ao comportamento real.**

---

## 13. 🔵 Logs via `System.out.println` — múltiplos arquivos

O projeto usa `@Slf4j` (Lombok) em vários serviços, mas muitos métodos ainda usam
`System.out.println` / `System.err.println` diretamente:

| Arquivo | Ocorrências |
|---|---|
| `SmartService.java` | `printHex`, `iniciarExecucaoPedido`, `enviarTampa` |
| `PedidoService.java` | `criar()` linha 88 |
| `PlcConnectionService.java` | `getConnection`, `closeAll` |
| `SseEmitterRegistry.java` | `broadcast` (hot-path, sem cliente) |

Todos devem usar `log.debug(...)` ou `log.info(...)`. O `System.out` não respeita nível de log
configurável e não aparece em ferramentas de observabilidade.

---

## 14. 🔵 `@AllArgsConstructor` em `PedidoService` — contra convenção do projeto

`CLAUDE.md` instrui: *"prefer `@RequiredArgsConstructor` for constructor injection"*.
`PedidoService` usa `@AllArgsConstructor`. Trocar para `@RequiredArgsConstructor` +
campos `final` (já estão `final`).

---

## 15. 🔵 `Collectors.toList()` vs `.toList()` — inconsistente

`EstoqueService.getDisponivel/getTodos` e `ExpedicaoService.listarTodos` usam
`.collect(Collectors.toList())` (mutável, Java 8).
`PedidoService.listarTodos`, `historicoDaPosicao` usam `.toList()` (imutável, Java 16+).

Padronizar em `.toList()` (já que o projeto usa Java 17+).

---

## 16. 🔵 `EstoqueService.primeiraPosicaoLivre` — lambda desnecessária

```java
// ATUAL
.map(e -> e.getPosicao())

// MÍNIMO
.map(Estoque::getPosicao)
```

---

## 17. 🔵 Imports `jakarta.transaction.Transactional` vs `org.springframework.transaction.annotation.Transactional` — mistura

`SmartService` usa `jakarta.transaction.Transactional`; `ExpedicaoService` e `EstoqueService`
usam `org.springframework.transaction.annotation.Transactional`.
Ambos funcionam com Spring Boot, mas o `spring.transaction` tem mais opções (readOnly, rollbackFor).
Padronizar em `org.springframework.transaction.annotation.Transactional`.

---

## Resumo por prioridade

| # | Severidade | Local | Impacto |
|---|---|---|---|
| 1 | 🔴 bug | `Pedido.java` imports Jackson | anotações ignoradas em runtime |
| 3 | 🔴 bug | `ExpedicaoClpService` linha 185 | handshake de expedição errado |
| 4 | 🔴 race | `PlcConnectionService.closeAll` | NPE / CME no shutdown |
| 2 | 🔴 perf | `SmartService @Transactional` | pool JDBC esgotado sob carga |
| 5 | 🟡 dead | `ExpedicaoGridProducer` | duplo disparo no SSE |
| 6 | 🟡 bug | `SmartService.enviarTampa` | ESP32 move tampa duas vezes |
| 7 | 🟡 perf | `SmartService` RestTemplate | nova conexão TCP por envio |
| 8 | 🟡 maint | `iniciarExecucaoPedido` | catch vazio oculta falhas CLP |
| 9 | 🟡 design | `PedidoRepository` | caller desnecessariamente verboso |
| 11 | 🟡 robustez | `primeiraExpedicaoLivre` | erro genérico em race condition |
| 12 | 🟡 doc | `recomporFila` vs CLAUDE.md | comportamento documentado ≠ código |
| 10 | 🔵 arch | `EstadoProducaoService` em model/ | pacote errado para um bean |
| 13 | 🔵 ops | `System.out.println` espalhado | logs invisíveis em produção |
| 14–17 | 🔵 estilo | vários | consistência |
