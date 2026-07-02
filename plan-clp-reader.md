# Plano — Migração `clpAntigo` → serviços CLP modulares por estação

## Contexto

As classes em `service/clpAntigo/` (`EstoqueService`, `ProcessoService`, `MontagemService`,
`ExpedicaoService`, `ClpController`, `PlcReaderDB`) vêm de outro sistema. Elas declaram o package
antigo `com.tecdes.sistema_bancada.*` e dependem de classes que **não existem** neste módulo
(`PlcReaderMultDB`, `ApiIntegrationService`, e campos estáticos do `SmartService` antigo). **Não
compilam aqui** — são código morto colado pra referência.

Objetivo: portar a lógica de handshake/leitura de cada estação para **serviços modulares
compatíveis com o sistema novo**, separando claramente:
- **De onde os dados vêm** (transporte): `PlcConnectionService.getConnection(ip).readBlock(...)`.
- **Onde os dados estão** (snapshot lido do PLC): os beans `model/clp/*CLP`
  (`EstoqueCLP`/`ProcessoCLP`/`MontagemCLP`/`ExpedicaoCLP`) — os services **não** guardam variáveis
  próprias, trabalham sobre esses models.
- **Estado da produção** (orquestração entre estações): `EstadoProducaoService`.
- **Persistência**: `EstoqueService`/`ExpedicaoService`/repos (JPA).

Cada estação tem **seu próprio IP** (passado por chamada) e roda **só quando o método é chamado** —
sem polling agendado, sem `ScheduledExecutorService`, sem endpoint novo nesta leva.

## Decisões (confirmadas com o usuário)

- **Disparo:** só método de serviço `lerEProcessar(String ip)` por estação. Sem controller/REST agora.
- **Dados lidos:** reusar os models `model/clp/*CLP` já existentes; os services parseiam os bytes
  nesses beans em vez de manter campos próprios.
- **Estado compartilhado:** novo `@Component EstadoProducaoService` (substitui os statics do
  `SmartService` antigo) — só os flags de orquestração que **não** têm model.
- **Persistência:** serviços/repos novos direto (sem `ApiIntegrationService`/HTTP).
- **Escopo:** as quatro estações (estoque, processo, montagem, expedicao).

## Estrutura alvo

Novo package `com.tecdes.smart.app_smart_40.service.clp.estacao` (evita colisão com os
`EstoqueService`/`ExpedicaoService` já existentes em `service/`):

```
model/clp/                                 [JÁ EXISTE — reusado como holder do snapshot do PLC]
├── EstacaoCLP.java                        [base: recebidoOp/numeroOP/startOP/finishOP/cancelOP/ocupado/...]
├── EstoqueCLP.java                        [extends EstacaoCLP + campos do magazine]
├── ProcessoCLP.java                       [extends EstacaoCLP]
├── MontagemCLP.java                       [extends EstacaoCLP]
└── ExpedicaoCLP.java                      [extends EstacaoCLP + campos da expedição]

service/
├── estado/
│   └── EstadoProducaoService.java        [NOVO @Component — estado de produção/orquestração]
└── clp/
    └── estacao/
        ├── EstoqueClpService.java        [NOVO — port de clpAntigo/EstoqueService]
        ├── ProcessoClpService.java       [NOVO — port de clpAntigo/ProcessoService]
        ├── MontagemClpService.java       [NOVO — port de clpAntigo/MontagemService]
        └── ExpedicaoClpService.java      [NOVO — port de clpAntigo/ExpedicaoService]
```

> Ajuste no model: `ExpedicaoCLP` passou a `extends EstacaoCLP` (precisava dos campos de OP/status
> que a expedição lê — `recebidoOp`/`startOP`/`finishOP`/`cancelOP`/`ocupado`...).

### `EstadoProducaoService` (novo `@Component`, `@Getter @Setter`)
Campos (de instância, substituem os `SmartService.xxx` estáticos do antigo):
```
int statusEstoque, statusProcesso, statusMontagem, statusExpedicao;
int statusProducao;
boolean pedidoEmCurso, readOnly, blockFinished, auxExpedicao;
int posicaoExpedicaoSolicitada;
```
Todas as estações injetam este bean e leem/escrevem essas flags (era o que os `SmartService.static`
faziam). Substitui `SmartService.readOnly` etc. nas referências portadas.

### Padrão de cada `*ClpService` (singleton `@Service`, `@RequiredArgsConstructor`)
Injeta o bean do model da estação (`EstoqueCLP`/`ProcessoCLP`/`MontagemCLP`/`ExpedicaoCLP`) +
`PlcConnectionService` + `EstadoProducaoService` (+ persistência onde aplica). **Sem campos de dado
próprios** — todo o snapshot lido fica no model. Dois métodos, separando fonte de dado:
1. `public void lerEProcessar(String ip)` — pega o connector via `plcConnectionService.getConnection(ip)`
   (retorna cedo se `null`), lê o bloco DB próprio da estação com `connector.readBlock(db, offset, size)`,
   chama `processData(ip, bytes)`.
2. `void processData(String ip, byte[] dados)` — parseia os bytes nos setters do model
   (`estoqueCLP.setStartOP(...)` etc.) + handshake de escrita no PLC (`writeBit`/`writeByte`/`writeInt`)
   lendo os getters do model + atualização do `EstadoProducaoService` + persistência.

Manter a lógica de bits/offsets **idêntica** ao antigo (já validada contra o CLP). Trocar só:
- package + injeções (`PlcConnectionService`/`PlcConnector` do package novo `service.clp.connection`);
- campos locais antigos → setters/getters do model `*CLP`;
- `SmartService.xxx` → `estadoProducaoService.getXxx()/setXxx()`;
- persistência (ver abaixo).

### Bloco DB lido por estação (em `lerEProcessar`)
Derivado dos offsets que o `processData` antigo consome (só o bloco realmente usado — os blocos extra
do `PlcReaderMultDB` antigo eram só pra display/SSE, descartados):
- **Estoque:** `readBlock(9, 0, 111)` — usa índices até 109.
- **Processo:** `readBlock(2, 0, 9)`.
- **Montagem:** `readBlock(57, 0, 9)`.
- **Expedicao:** `readBlock(9, 0, 48)` — usa índices até 45.

## Mapeamento de persistência (antigo → novo)

### Estoque
- "Buscar posição livre" — antigo `buscarPrimeiraPosicaoPorCor(0, ...)` (cor 0 = VAZIO).
  Novo: `estoqueRepository.findPosicoesVazias()` (já retorna VAZIO ordenado por posição) → pega o
  primeiro `.getPosicao()`. Reusar via `EstoqueService` (adicionar getter se preciso) ou injetar o repo.
- Adicionar bloco detectado pelo PLC: `EstoqueService.adicionarBloco(new EstoqueRequestDTO(posicao, cor))`.
- Remover bloco: `EstoqueService.removerBloco((byte) posicao)`.
- `corGuardarEstoque` é `int` → converter para `CorBloco` (usar o `@JsonCreator`/`fromValue` do enum;
  se não houver helper público int→enum, adicionar `CorBloco.fromValue(int)`).
- Manter também o `writeByte(9, offset, cor)` no CLP como no antigo (espelho da memória do PLC).

### Expedicao (lacuna de modelo — precisa de métodos novos)
O `Expedicao` novo é `posicao` + `pedido` (OneToOne); **não** guarda "OP por posição" como o antigo.
Para persistir o que o PLC reporta:
- **Adicionar** OP `opGuardadoExpedicao` na `posicaoGuardarExp`: achar o `Pedido` por
  `pedidoRepository.findByOrdemProducao(op)`, achar a `Expedicao` da posição, setar `pedido`, salvar.
- **Remover** da `posicaoRemovidoExpedicao`: achar `Expedicao` da posição, `setPedido(null)`, salvar.
- Adições necessárias:
  - `ExpedicaoRepository.findByPosicao(Integer posicao)`.
  - `ExpedicaoService.guardarNaPosicao(int posicao, int ordemProducao)` e
    `ExpedicaoService.removerDaPosicao(int posicao)`.
- `posicaoExpedicaoSolicitada` e `auxExpedicao` saem do `EstadoProducaoService`.

## Integração com `SmartService` (opcional, recomendado)
`SmartService.enviarParaProducao()` continua igual (escrita do pedido). Após enviar, pode chamar
`estoqueClpService.lerEProcessar(ip)` / etc. quando quiser avançar o handshake — mas **sem loop**,
sob demanda. Sem mudança obrigatória no `SmartService` nesta leva.

## O que NÃO é portado
- `PlcReaderDB` / `PlcReaderMultDB` (eram pra polling agendado — não há leitura por tempo).
- `clpAntigo/ClpController` (`/start-readings`, `/stop-readings`, SSE `/smartstream`, `/ping`,
  `/reset-status`, `/readonly`) — sem endpoint novo agora.
- `ApiIntegrationService` (HTTP) — substituído por chamadas diretas aos serviços/repos.

## Limpeza
Após os 4 serviços novos compilarem e os testes passarem, **remover a pasta `service/clpAntigo/`**
inteira (não compila no módulo e foi totalmente substituída). Confirmar que nada mais referencia o
package `com.tecdes.sistema_bancada`.

## Arquivos
**Novos:** `service/estado/EstadoProducaoService.java`, `service/clp/estacao/{Estoque,Processo,Montagem,Expedicao}ClpService.java`.
**Editados:** `ExpedicaoRepository.java` (`findByPosicao`), `ExpedicaoService.java` (guardar/remover por posição), `model/clp/ExpedicaoCLP.java` (`extends EstacaoCLP`).
**Reusados:** `model/clp/{Estacao,Estoque,Processo,Montagem,Expedicao}CLP.java` como holder do snapshot; `CorBloco.fromValue` e `EstoqueRequestDTO` já existiam.
**Removidos:** `service/clpAntigo/*`.

## Verificação
1. Compilar: `cd app_smart_4.0 && ./mvnw clean compile` — garante que o package antigo sumiu e os novos compilam.
2. Testes unitários no padrão Mockito existente (`@ExtendWith(MockitoExtension.class)`, factory `buildX()`):
   - Por estação: dado um `byte[]` de entrada conhecido, mockar `PlcConnectionService`/`PlcConnector`
     e verificar os `writeBit/writeByte/writeInt` esperados + transições no `EstadoProducaoService`.
   - Estoque/Expedicao: verificar chamadas de persistência (`adicionarBloco`/`removerBloco`/guardar/remover).
   - Rodar: `./mvnw test -Dtest=EstoqueClpServiceTest` etc.
3. Sanidade end-to-end (opcional, com CLP/mocks): chamar `lerEProcessar(ip)` e conferir leitura+handshake
   num único disparo.
