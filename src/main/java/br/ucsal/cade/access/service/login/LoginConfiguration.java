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
     * Hashes login codes before they are stored (D03). BCrypt is slow on purpose, so a leaked table cannot be
     * reversed by trying all one million codes in a few milliseconds.
     */
    @Bean
    PasswordEncoder loginCodeEncoder() {
        return new BCryptPasswordEncoder();
    }
}
