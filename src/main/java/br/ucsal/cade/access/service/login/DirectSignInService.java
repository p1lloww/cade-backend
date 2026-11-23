package br.ucsal.cade.access.service.login;

import br.ucsal.cade.access.api.DirectSignIn;
import br.ucsal.cade.access.api.SignedIn;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.access.service.token.AccessToken;
import br.ucsal.cade.access.service.token.AccessTokenIssuer;
import br.ucsal.cade.shared.security.Role;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** A entrada direta do modo demonstração (RF47): existe só no profile {@code demo}. */
@Service
@Profile("demo")
class DirectSignInService implements DirectSignIn {

    private final UserRepository users;
    private final AccessTokenIssuer tokenIssuer;
    private final Clock clock;

    DirectSignInService(UserRepository users, AccessTokenIssuer tokenIssuer, Clock clock) {
        this.users = users;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SignedIn signIn(String email, Role role) {
        String normalized = User.normalize(email);
        User user = users.findByEmail(normalized).orElseGet(() -> users.save(create(normalized, role)));
        AccessToken token = tokenIssuer.issue(user);
        return new SignedIn(user.getId(), user.getEmail(), user.getRole(), token.value(), token.expiresIn());
    }

    private User create(String email, Role role) {
        return switch (role) {
            case STUDENT -> User.newStudent(email, clock.instant());
            case STAFF -> User.newStaff(email, clock.instant());
            case ADMIN -> User.newAdmin(email, clock.instant());
        };
    }
}
