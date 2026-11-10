# Cadê? — Achados e Perdidos UCSal

> **Status:** em desenvolvimento (POOALAB 2026.2). As seções marcadas com _A preencher_ são completadas ao longo do projeto.

O Cadê? liga o estudante que perdeu um objeto à sala de achados e perdidos da UCSal. O estudante registra a perda online, a sala cadastra o que chega e o sistema cruza os dois lados, sugerindo ao funcionário só os pares prováveis. A conferência e a entrega continuam presenciais, no balcão.

Este é o repositório do **back-end**. O front-end está em [cade-frontend](https://github.com/p1lloww/cade-frontend).

## Problema, público e proposta de valor

- **Público:** estudantes da UCSal que perdem objetos no campus e funcionários da sala de achados e perdidos.
- **Problema:** os objetos ficam registrados só numa etiqueta presa a eles, em caixas por mês, sem lista pesquisável. Muitos estudantes não sabem que a sala existe.
- **Proposta:** a sala ganha uma lista pesquisável, e o estudante resolve a distância: registra a perda e só vai à sala quando é convocado.

Detalhes e evidências em [docs/problema-validacao.md](docs/problema-validacao.md) e [docs/definicao-mvp.md](docs/definicao-mvp.md).

## Integrantes e turma

_A preencher:_ nome completo e turma de cada integrante.

## Tecnologias

| Camada | Tecnologia | Justificativa |
|---|---|---|
| Back-end | Java 25 + Spring Boot 4.1 | Linguagem da disciplina; padrões de POOA visíveis no código |
| Banco | PostgreSQL 17 | Relacional, transações para estados e histórico |
| Esquema do banco | JPA/Hibernate (`ddl-auto: update`) | Rápido para um MVP; migrations com Flyway ficam como próximo passo |
| Login | Código de uso único por e-mail @ucsal.edu.br + JWT | Sem senha e sem depender de provedor externo |
| E-mail | Gmail SMTP (Mailpit em desenvolvimento) | Grátis e simples; Mailpit captura os e-mails localmente |
| Assíncrono | Eventos do Spring + Virtual Threads | O cadastro responde na hora; cruzamento e e-mails rodam em paralelo |
| Infra | Docker Compose | Banco, Mailpit e back-end com um comando |

## Pré-requisitos

- Docker 24+ com Docker Compose
- Para desenvolver: JDK 25 (o Maven vem pelo `./mvnw`)

## Como executar

### Tudo pelo Docker

```bash
cp .env.example .env    # opcional: ajuste os valores
docker compose up -d --build
```

- API: http://localhost:8080 (saúde em `/actuator/health`)
- E-mails capturados (Mailpit): http://localhost:8025

### Desenvolvimento (back-end pela IDE ou pelo Maven)

```bash
docker compose up -d postgres mailpit
./mvnw spring-boot:run
```

Para zerar o banco local: `docker compose down -v`.

## Estrutura do código

Monólito modular (D10). Cada módulo dos requisitos é um pacote em `br.ucsal.cade`, com as camadas `api` (o único pacote visível para outros módulos), `controller`, `service` e `persistence`. O código é em inglês (D11); a correspondência com os termos dos requisitos está no [glossário](docs/GLOSSARIO.md).

| Pacote | Módulo | Pode depender de |
|---|---|---|
| `access` | M1 Acesso | — |
| `founditems` | M2 Achados | — |
| `lossreports` | M3 Perdas | — |
| `matching` | M4 Cruzamento | founditems, lossreports |
| `pickup` | M5 Convocação e retirada | founditems, lossreports, matching |
| `notifications` | M6 Avisos | access, lossreports, pickup |
| `retention` | M7 Prazos e destino | founditems |
| `publicpage` | M8 Página pública | founditems |
| `administration` | M9 Administração | access |
| `demo` | M10 Modo demonstração | access, founditems, lossreports |
| `shared` | Núcleo compartilhado (histórico, eventos, erros, e-mail, segurança, relógio, web) | nenhum módulo |

O teste `ModuleBoundariesTest` (ArchUnit) garante essas regras: acesso entre módulos só pela `api` e nas direções da tabela, sem ciclos, e camadas na ordem `controller → service → persistence`.

Documentos de apoio em `docs/`: [requisitos](docs/requisitos.md), [decisões](docs/DECISOES.md) e [glossário](docs/GLOSSARIO.md).

## Como contribuir

A `main` é protegida. Crie uma branch (`feat/...`, `fix/...`, `docs/...`), abra um Pull Request citando o card (`Closes #N`) e peça a revisão de outro integrante. O merge só acontece com 1 aprovação.

## Testes

```bash
./mvnw verify
```

Os testes de integração sobem um PostgreSQL temporário com Testcontainers, então o Docker precisa estar rodando.

## Dados de teste e demonstração

_A preencher:_ usuários fictícios do perfil `demo` e roteiro em [demo/](demo/).

## Roteiro do fluxo principal

_A preencher._

## Limitações conhecidas e próximos passos

- O esquema do banco é gerado pelo Hibernate (`ddl-auto: update`), sem migrations versionadas.
- Os eventos ficam na memória da aplicação: se o back-end cair entre o commit e o processamento, aquele evento se perde.

## Fontes, licenças, componentes de terceiros e uso de IA

Ver [docs/declaracao-ia-terceiros.md](docs/declaracao-ia-terceiros.md).
