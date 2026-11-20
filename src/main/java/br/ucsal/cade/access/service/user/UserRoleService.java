package br.ucsal.cade.access.service.user;

import br.ucsal.cade.access.dto.request.RoleChangeRequestDTO;
import br.ucsal.cade.access.dto.response.UserResponseDTO;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.exception.RoleChangeNotAllowedException;
import br.ucsal.cade.shared.exception.UserNotFoundException;
import br.ucsal.cade.shared.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Gestão dos funcionários pelo administrador (RF03). O perfil novo só vale a partir do próximo login, porque o token
 * já emitido leva o perfil antigo até vencer.
 */
@Service
public class UserRoleService {

    static final String GRANTED_ACTION = "STAFF_ROLE_GRANTED";
    static final String REVOKED_ACTION = "STAFF_ROLE_REVOKED";

    private static final Logger log = LoggerFactory.getLogger(UserRoleService.class);
    private static final Sort BY_EMAIL = Sort.by("email");

    private final UserRepository users;
    private final ApplicationEventPublisher events;

    UserRoleService(UserRepository users, ApplicationEventPublisher events) {
        this.users = users;
        this.events = events;
    }

    /**
     * Perfil nulo e e-mail nulo ou vazio não filtram. A ordem é sempre pelo e-mail: a ordem pedida é ignorada, para
     * que um campo inexistente não vire erro do banco.
     */
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> search(Role role, String emailPart, Pageable pageable) {
        log.debug("Searching users: role={}, page={}, size={}", role, pageable.getPageNumber(), pageable.getPageSize());
        String normalizedPart = emailPart == null ? "" : User.normalize(emailPart);
        Pageable byEmail = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), BY_EMAIL);
        return users.search(role, normalizedPart, byEmail).map(UserResponseDTO::from);
    }

    /**
     * Dá ou retira o perfil de funcionário e grava o histórico na mesma transação (RF43, D10).
     *
     * @param adminId id da conta do administrador (claim {@code sub} do token)
     */
    @Transactional
    public UserResponseDTO changeRole(UUID userId, RoleChangeRequestDTO request, UUID adminId) {
        log.debug("Changing role of user {} to {} by {}", userId, request.role(), adminId);
        User user = users.findById(userId).orElseThrow(() -> {
            log.info("Role change refused: user {} not found", userId);
            return new UserNotFoundException();
        });

        String action;
        try {
            action = switch (request.role()) {
                case STAFF -> {
                    user.grantStaff();
                    yield GRANTED_ACTION;
                }
                case STUDENT -> {
                    user.revokeStaff();
                    yield REVOKED_ACTION;
                }
                case ADMIN -> throw new IllegalArgumentException("ADMIN is refused by RoleChangeRequestDTO validation");
            };
        } catch (RoleChangeNotAllowedException e) {
            log.info("Role change refused for user {}: current role is {}", userId, user.getRole());
            throw e;
        }

        events.publishEvent(new AuditEvent(adminId, action, User.class.getSimpleName(), user.getId(),
                request.reason()));
        log.info("{} for user {} by {}", action, userId, adminId);
        return UserResponseDTO.from(user);
    }
}
