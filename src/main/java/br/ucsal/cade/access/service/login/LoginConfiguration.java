package br.ucsal.cade.access.service.login;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LoginCodeProperties.class)
class LoginConfiguration {

    /**
     * Calcula o hash dos códigos de login antes de guardá-los (D03). O BCrypt é lento de propósito: assim, um dump
     * vazado do banco não é revertido testando o milhão de códigos em poucos milissegundos.
     */
    @Bean
    PasswordEncoder loginCodeEncoder() {
        return new BCryptPasswordEncoder();
    }
}
