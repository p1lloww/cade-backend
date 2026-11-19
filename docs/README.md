# Documentação do Cadê? (back-end)

## Fonte da verdade

Consulte antes de modelar, codar ou criar tarefas. Ficam na raiz de `docs/` porque as issues do backlog apontam para
eles.

| Documento | Conteúdo |
|---|---|
| [requisitos.md](requisitos.md) | RF, RN e RNF (v3), prioridades, estados e plano de corte |
| [DECISOES.md](DECISOES.md) | decisões D01 em diante sobre stack, arquitetura e processo; em conflito com os requisitos, vale a decisão |
| [GLOSSARIO.md](GLOSSARIO.md) | termos dos requisitos (português) e nomes do código (inglês, D11) |

## `tecnico/`: como partes importantes funcionam por dentro

| Documento | Conteúdo |
|---|---|
| [autorizacao.md](tecnico/autorizacao.md) | CORS, token e `@RequiresRole`, do navegador ao controller, e o checklist de rota nova (D09, D20) |
| [registro-perda.md](tecnico/registro-perda.md) | o registro de perda em UML: classes, sequência com os eventos, situações e foto (PER-01) |
| [limite-registros-abertos.md](tecnico/limite-registros-abertos.md) | como o lock otimista garante no máximo 5 registros de perda em andamento por estudante, mesmo com registros simultâneos (RN05) |
| [login-codigos.md](tecnico/login-codigos.md) | como os códigos de login ficam no PostgreSQL e por que cada regra é um comando SQL só (D18) |

## `entrega/`: anexos do manual do POOALAB

| Documento | Anexo |
|---|---|
| [problema-validacao.md](entrega/problema-validacao.md) | A e B: problema e validação |
| [definicao-mvp.md](entrega/definicao-mvp.md) | C: definição do MVP |
| [declaracao-ia-terceiros.md](entrega/declaracao-ia-terceiros.md) | E: uso de IA e componentes de terceiros |
| [pooa-no-projeto.md](entrega/pooa-no-projeto.md) | Bônus de POOA: onde cada conteúdo da disciplina aparece no código e qual problema resolve |

## `imagens/`

Figuras usadas pelos documentos, como o fluxo principal dos requisitos.
