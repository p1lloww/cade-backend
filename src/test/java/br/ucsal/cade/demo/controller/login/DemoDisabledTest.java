package br.ucsal.cade.demo.controller.login;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.api.DirectSignIn;
import br.ucsal.cade.access.persistence.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sem o profile {@code demo}, o login direto não existe (RF47, D02): as rotas respondem 404, não há bean que emita
 * token sem o código e as contas fictícias não são criadas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DemoDisabledTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private UserRepository users;

    @Test
    void theDemoRoutesDoNotExist() throws Exception {
        mvc.perform(post("/demo/login").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/demo/accounts")).andExpect(status().isNotFound());
    }

    @Test
    void thereIsNoBeanThatIssuesATokenWithoutACode() {
        assertThat(context.getBeanNamesForType(DirectSignIn.class)).isEmpty();
    }

}
