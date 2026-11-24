# Definição do MVP

Baseado no Anexo C do manual do POOALAB.

- **Usuário principal:** funcionário da sala de achados e perdidos; o estudante é quem ganha a distância.
- **Resultado principal:** o estudante registra a perda de casa, o sistema sugere o objeto certo, o funcionário convoca e registra a retirada no balcão.
- **Fluxo de ponta a ponta:** cadastro do objeto → registro da perda → cruzamento → convocação → conferência e retirada no balcão.
- **Critérios de pronto:**
  1. Um funcionário cadastra um objeto pelo celular em menos de 1 minuto.
  2. O fluxo de ponta a ponta acima funciona.
  3. O funcionário acha um objeto pela busca (categoria + mês) sem abrir caixas.
  4. Um registro genérico ("iPhone") recebe nota baixa no cruzamento e, se convocado, é barrado no balcão, com a recusa registrada.
  5. Um visitante vê a página pública sem nenhuma foto, descrição ou local de objeto.
- **Fora do escopo:** publicação de achados pela comunidade, outros pontos de guarda (biblioteca), contas para quem não tem e-mail institucional, retirada por terceiros, reconhecimento de objetos por imagem, integração com sistemas da UCSal.
- **Maior risco:** de uso, o funcionário não cadastrar os objetos por falta de tempo (S7): sem cadastro não há o que cruzar, e o sistema perde o valor para os dois lados. Mitigação: só 4 campos obrigatórios e caixa do mês sugerida (RF04, RF05). De segurança, alguém levar um objeto com uma descrição genérica (E4). Mitigação: nada do objeto é exposto, limite de registros abertos, descrição travada depois da convocação e conferência por risco no balcão (seção 9.4 dos requisitos).
- **Teste principal:** o fluxo de ponta a ponta com o modo demonstração ([`demo/README.md`](../../demo/README.md)): o estudante registra a perda, o funcionário abre a sugestão de nota alta, convoca, o e-mail chega ao Mailpit e a retirada é registrada no balcão. No mesmo roteiro, o registro genérico "iPhone" recebe nota baixa e, se convocado, é recusado no balcão, com a recusa no histórico. Os critérios de pronto são cobertos por testes automatizados (`./mvnw verify`, rodado no CI a cada PR), entre eles o cruzamento (`CompatibilityScorerTest`), as regras por risco (`VerificationRulesTest`), a privacidade da página pública (`PublicCountsControllerTest`) e os dados de demonstração (`DemoModeTest`). O tempo de cadastro (critério 1) é medido no teste de uso, com o cronômetro.
- **Limitações conhecidas:** ver o README.
