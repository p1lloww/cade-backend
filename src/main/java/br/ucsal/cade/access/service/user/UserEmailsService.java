package br.ucsal.cade.access.service.user;

import br.ucsal.cade.access.api.UserEmails;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
class UserEmailsService implements UserEmails {

    private final UserRepository users;

    UserEmailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> emailOf(UUID userId) {
        return users.findById(userId).map(User::getEmail);
    }
}
