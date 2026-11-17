package br.ucsal.cade.shared.security.authorization;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Liga o {@link RoleInterceptor} a todas as rotas dos controllers. */
@Configuration(proxyBeanMethods = false)
public class AuthorizationConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RoleInterceptor());
    }
}
