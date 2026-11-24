# Problema e validação

Baseado nos Anexos A e B do manual do POOALAB. A fonte completa é o documento de requisitos v3 da equipe.

## Ficha do problema (Anexo A)

| Campo | Preenchimento |
|---|---|
| Público | Estudantes da UCSal que perdem objetos no campus; funcionários da sala de achados e perdidos |
| Situação | Quando alguém procura um objeto sem lembrar onde o perdeu, ou quando a sala precisa conferir se o objeto é de quem o pede |
| Problema | Os objetos ficam só numa etiqueta, em caixas por mês, sem lista pesquisável; muitos estudantes não sabem que a sala existe |
| Impacto | Objetos acumulados por anos sem dono, busca que depende de revirar caixas, entrega à pessoa errada |
| Alternativa atual | Etiqueta no objeto, perguntas de dia, local e características, formulário de papel, avisos em grupos de WhatsApp |
| Hipótese crítica | Com uma lista pesquisável na sala e o registro da perda a distância, o cruzamento leva o objeto ao dono certo sem expor nada ao público, desde que o funcionário consiga cadastrar cada objeto em menos de 1 minuto (S7) e a conferência no balcão barre quem descreve o objeto de forma genérica (E4) |
| Evidência | Conversas com a sala e o administrador (29/10) e com estudantes (04/11) |
| Recorte | Cadastro rápido de objetos, registro de perdas, cruzamento e retirada registrada |

## Registro de validação (Anexo B)

| Data e perfil | Hipótese | Evidência observada | Decisão |
|---|---|---|---|
| 29/10 — funcionários da sala | A sala tem dificuldade para achar os objetos guardados e para conferir se quem pede é o dono | Não há lista: só uma etiqueta com local e data, em caixas por mês (S1). A maior dificuldade é saber onde o objeto foi perdido ou achado (S4). Já houve entrega à pessoa errada (S3). São contra mostrar foto e características ao público: "chega um esperto e diz que é dele" (S5). Pouco tempo para cadastrar (S7). Muitos não sabem que a sala existe (S6) | **Manter** a lista pesquisável como centro do MVP, com cadastro de 4 campos obrigatórios (RF04). **Decidir:** nada do objeto vai ao público, só contagens por categoria e mês (PR4, RN03); o local exato fica só com a sala. **Acrescentar:** conferência por risco no balcão e página pública para divulgar a sala (S6) |
| 29/10 — administrador | Falta controle sobre quanto tempo os objetos ficam guardados e sobre quem mexeu em cada um | Não há prazo: há objetos de 2024 (A2). Quer rastreabilidade de quem trouxe, quem recebeu e quem retirou (A1). Aceita que o aluno mande características e prova a distância, mas celular sempre presencial para desbloquear (A3) | **Acrescentar** prazo de guarda e destino, validados com ele: 90 dias, ajustável, destino padrão bazar (RN16, RF37 a RF39). **Acrescentar** histórico de cada ação (RF43) e quem trouxe o objeto (RF07). **Manter** a entrega presencial (PR3), com prova anexada pelo estudante (RF15) |
| 04/11 — estudantes | O estudante não sabe onde procurar o que perdeu e usaria um registro online, se a entrega for segura | Um guarda-chuva nunca foi recuperado, e outro objeto foi devolvido só pela descrição, o que o próprio aluno achou "suspeito" (E1). Sugeriram "abrir um chamado" com objeto, características, sala e horário (E3). Medo de fraude ("tem um iPhone aí pra mim?"); pedem login institucional e retirada com documento e matrícula (E4). Visitantes ficam de fora (E5). Um fone ficou na biblioteca (E2) | **Adotar** o registro de perda no lugar do "chamado" (E3) e o cruzamento com a sala. **Acrescentar** login só com e-mail @ucsal.edu.br, limite de registros abertos e descrição travada depois da convocação (E4). **Fora do escopo:** visitantes sem conta, atendidos no balcão (E5), e outros pontos de guarda, como a biblioteca (E2, PR6) |

**Síntese dos aprendizados:** o problema existe nos dois lados. A sala não tem lista nem prazo e já entregou objeto à pessoa errada; o estudante não sabe que a sala existe ou não consegue consultá-la. As três conversas concordaram que mostrar o objeto ao público facilita a fraude (S5, E4). O valor está em cruzar a perda registrada pelo estudante com o que a sala cadastrou e decidir no balcão, com conferência proporcional ao risco do objeto. Do lado da sala, a restrição mais forte é o tempo: o cadastro precisa ser curto, ou não acontece.

**Mudanças realizadas no problema ou na solução:** o rascunho de 15/10 foi reescrito como requisitos v2 (05/11) a partir das três conversas, e cada requisito passou a citar a evidência que o justifica. Por causa de S5 e E4, nada do objeto aparece ao público: o estudante descreve a perda, o sistema cruza com o que a sala cadastrou, e a página pública mostra só contagens por categoria e mês. Entraram a conferência por risco no balcão (S2, S3, A3), o prazo de guarda com destino (A2), o histórico de todas as ações (A1) e o limite e a trava contra descrições genéricas (E4). Visitantes e outros pontos de guarda saíram do escopo (E5, E2). A v3 (10/11) mudou só a stack; o problema, as evidências e as regras continuaram os mesmos.
