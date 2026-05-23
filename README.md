<div align="center">

<!-- HERO -->
<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0066ff,100:00ffff&height=160&section=header&text=Controladora%20Bancada%20SMART%204.0&fontSize=28&fontColor=ffffff&fontAlignY=45&desc=Back-end%20REST%20%7C%20Indústria%204.0%20%7C%20Java%2017&descAlignY=68&descSize=14" width="100%"/>

![Java](https://img.shields.io/badge/Java_17-0066ff?style=for-the-badge&logo=openjdk&logoColor=00ffff)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.0.6-0a0a0f?style=for-the-badge&logo=springboot&logoColor=00ffff)
![MySQL](https://img.shields.io/badge/MySQL_8-0a0a0f?style=for-the-badge&logo=mysql&logoColor=0066ff)
![WebSocket](https://img.shields.io/badge/WebSocket-0a0a0f?style=for-the-badge&logo=socket.io&logoColor=00ffff)
![Maven](https://img.shields.io/badge/Maven-0a0a0f?style=for-the-badge&logo=apachemaven&logoColor=0066ff)
![License](https://img.shields.io/badge/MIT_License-0066ff?style=for-the-badge&logoColor=white)

> **Situação de Aprendizagem — 3º Semestre · Técnico em Desenvolvimento de Sistemas · SENAI**

</div>

---

## `[01]` Sobre o Projeto

Este repositório contém a aplicação **back-end** desenvolvida como Situação de Aprendizagem do 3º semestre do Técnico em Desenvolvimento de Sistemas. O sistema simula a controladora de uma **bancada de manufatura inteligente** compatível com os conceitos da **Indústria 4.0**.

A aplicação expõe uma **API REST** que gerencia o fluxo completo de produção da bancada: recebimento de pedidos, controle de estoque de blocos, montagem de lâminas e expedição dos produtos finalizados — com comunicação em tempo real via **WebSocket**.

---

## `[02]` Funcionalidades

| Módulo | Descrição |
|---|---|
| 📦 **Gestão de Pedidos** | Cria, lista e conclui pedidos com controle de tipo, cor de tampa e status (`PENDENTE → EM ANDAMENTO → CONCLUÍDO`) |
| 🔲 **Controle de Estoque** | Gerencia posições de blocos no estoque físico — adiciona e remove por posição com validação de disponibilidade |
| 🧱 **Montagem de Blocos** | Cada bloco é composto por lâminas com cor, padrão e posição definidos, refletindo a estrutura física da bancada |
| 🚚 **Expedição** | Registra a saída dos pedidos concluídos com rastreabilidade por data de entrada na expedição |
| 📡 **Tempo Real (WebSocket)** | Comunicação assíncrona com a bancada física ou interface de monitoramento via Spring WebSocket |
| 🗄️ **Persistência JPA/MySQL** | Entidades mapeadas com JPA + Hibernate, constraints de integridade e DDL auto-gerenciado em ambiente dev |

---

## `[03]` Stack Tecnológica

```
Java 17  ·  Spring Boot 4.0.6  ·  Spring Web MVC  ·  Spring Data JPA
Spring WebSocket  ·  Hibernate  ·  MySQL 8  ·  Lombok  ·  Maven
```

---

## `[04]` Arquitetura do Projeto

```
app_smart_4.0/
├── src/main/java/com/tecdes/smart/app_smart_40/
│   ├── controller/              ← Endpoints REST (HTTP layer)
│   │   ├── PedidoController.java
│   │   └── EstoqueController.java
│   ├── service/                 ← Regras de negócio
│   │   ├── PedidoService.java
│   │   ├── EstoqueService.java
│   │   ├── BlocoService.java
│   │   ├── LaminaService.java
│   │   └── ExpedicaoService.java
│   ├── model/                   ← Entidades JPA
│   │   ├── Pedido.java
│   │   ├── Bloco.java
│   │   ├── Lamina.java
│   │   ├── Estoque.java
│   │   ├── Expedicao.java
│   │   └── enums/               ← CorBloco, CorLamina, StatusPedido...
│   ├── repository/              ← Acesso a dados (Spring Data)
│   ├── dto/                     ← Data Transfer Objects
│   ├── config/                  ← Configurações (WebSocket, etc.)
│   ├── util/
│   └── Application.java
└── src/main/resources/
    └── application.properties
```

---

## `[05]` Modelo de Dados

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    Pedido    │──────▶│    Bloco     │──────▶│    Lamina    │
│──────────────│  1:N  │──────────────│  1:N  │──────────────│
│ id           │       │ id           │       │ id           │
│ ordemProd.   │       │ cor (enum)   │       │ cor (enum)   │
│ status       │       │ pedido →     │       │ padrao       │
│ tipoPedido   │       │ estoque →    │       │ posicaoBloco │
│ corTampa     │       │ laminas →    │       │ bloco →      │
│ dataCriacao  │       └──────────────┘       └──────────────┘
│ blocos →     │
│ expedicao →  │       ┌──────────────┐       ┌──────────────┐
└──────────────┘       │   Estoque   │       │  Expedicao   │
                       │──────────────│       │──────────────│
                       │ nrPosicao   │       │ id           │
                       │ blocos →    │       │ pedidos →    │
                       └──────────────┘       └──────────────┘
```

**Enums disponíveis:** `CorBloco` · `CorLamina` · `CorTampa` · `StatusPedido` · `TipoPedido` · `PadraoLamina` · `PosicaoLamina`

---

## `[06]` API REST

**Base URL:** `http://localhost:8088`

### Pedidos — `/api/pedidos`

```http
GET    /api/pedidos              → Lista todos os pedidos
POST   /api/pedidos              → Cria um novo pedido
PUT    /api/pedidos/{id}/status  → Conclui um pedido
```

### Estoque — `/api/estoque`

```http
GET    /api/estoque/disponivel         → Retorna posições disponíveis
PUT    /api/estoque/adicionar          → Adiciona um bloco ao estoque
PUT    /api/estoque/remover/{nrPos}    → Remove bloco por posição
```

---

## `[07]` Branches

| Branch | Finalidade |
|---|---|
| `main` | Código estável e revisado — versão de referência do projeto |
| `feature` | Desenvolvimento ativo — novas funcionalidades, controllers, services e entidades |
| `hot-fix` | Correções urgentes aplicadas diretamente sobre a branch estável |

---

## `[08]` Como Executar

### Pré-requisitos

- Java 17+
- Maven 3.8+
- MySQL 8.x rodando na porta `3306`

### Passo a passo

**1. Clone o repositório**
```bash
git clone https://github.com/William-Colasso/controladora-bancada-smart-4.0.git
cd controladora-bancada-smart-4.0/app_smart_4.0
```

**2. Crie o banco de dados**
```sql
CREATE DATABASE DB_SMART_4_0;
```

**3. Configure as credenciais**

Edite `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/DB_SMART_4_0
spring.datasource.username=root
spring.datasource.password=sua_senha
```

**4. Execute a aplicação**
```bash
./mvnw spring-boot:run
```

A API estará disponível em **`http://localhost:8088`**

---

## `[09]` Licença

Distribuído sob a licença **MIT**. Consulte o arquivo [`LICENSE`](LICENSE) para mais informações.

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:00ffff,100:0066ff&height=80&section=footer" width="100%"/>

**Desenvolvido por [William Colasso](https://github.com/William-Colasso)**  
`Técnico em Desenvolvimento de Sistemas · 3º Semestre · SENAI`

</div>
