package br.ucsal.cade.access.service.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Only institutional e-mails may have an account (requirements, section 2; evidence E4). The domain comes from
 * {@code cade.allowed-email-domain} and is always checked on the back end, never only on the front end.
 */
@Component
public class InstitutionalEmailPolicy {

    private final String domain;

    public InstitutionalEmailPolicy(@Value("${cade.allowed-email-domain}") String domain) {
        this.domain = Objects.requireNonNull(domain);
    }

    /**
     * Whether a normalized e-mail ({@link br.ucsal.cade.access.persistence.user.User#normalize(String)}) belongs to
     * the institutional domain. Requires exactly one {@code @} so that {@code a@b@ucsal.edu.br} is refused.
     */
    public boolean accepts(String normalizedEmail) {
        int at = normalizedEmail.indexOf('@');
        return at > 0
                && at == normalizedEmail.lastIndexOf('@')
                && normalizedEmail.substring(at + 1).equals(domain);
    }

    public String domain() {
        return domain;
    }
}
