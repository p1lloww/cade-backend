# Documentação do Cadê? (back-end)

## Fonte da verdade

Consulte antes de modelar, codar ou criar tarefas. Ficam na raiz de `docs/` porque as issues do backlog apontam para
eles.

| Documento | Conteúdo |
|---|---|
| [requisitos.md](requisitos.md) | RF, RN e RNF (v3), prioridades, estados e plano de corte |
| [DECISOES.md](DECISOES.md) | decisões D01 em diante sobre stack, arquitetura e processo; em conflito com os requisitos, vale a decisão |
| [GLOSSARIO.md](GLOSSARIO.md) | termos dos requisitos (português) e nomes do código (inglês, D11) |
| [REGRAS-DO-PROJETO.md](REGRAS-DO-PROJETO.md) | regras para quem escreve código aqui: escopo, arquitetura, privacidade, testes obrigatórios, definição de pronto e git |

## `tecnico/`: como partes importantes funcionam por dentro

| Documento | Conteúdo |
|---|---|
| [autorizacao.md](tecnico/autorizacao.md) | CORS, token e `@RequiresRole`, do navegador ao controller, e o checklist de rota nova (D09, D20) |
| [registro-perda.md](tecnico/registro-perda.md) | o registro de perda em UML: classes, sequência com os eventos, situações e foto (PER-01) |
| [limite-registros-abertos.md](tecnico/limite-registros-abertos.md) | como o lock otimista garante no máximo 5 registros de perda em andamento por estudante, mesmo com registros simultâneos (RN05) |
| [modulo-cruzamento.md](tecnico/modulo-cruzamento.md) | guia de leitura do módulo de cruzamento: mapa das pastas, como a nota é calculada, a vida de uma sugestão e o que o funcionário decide, com diagramas (CRU-01 a CRU-03) |
| [convocacao.md](tecnico/convocacao.md) | a convocação, que muda objeto, perda e sugestão na mesma transação, e o lock otimista que garante uma convocação ativa por objeto (RET-01, D25) |
| [retirada.md](tecnico/retirada.md) | o balcão: regras de conferência por risco (Strategy), retirada, recusa e bloqueio após 3 recusas, com diagramas UML (RET-03, D26) |
| [cruzamento.md](tecnico/cruzamento.md) | o cruzamento disparado pelos eventos de cadastro, depois do commit e numa Virtual Thread, com os casos de falha (CRU-02, D23) |
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
