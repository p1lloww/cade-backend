package br.ucsal.cade.access.persistence.user;

import br.ucsal.cade.shared.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Conta de usuário (RF02). Identificada pelo e-mail institucional, sem senha (D02), e com um único perfil.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    private User(String email, Role role, Instant createdAt) {
        this.email = normalize(email);
        this.role = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Conta criada no primeiro login válido: todo usuário novo começa como estudante. */
    public static User newStudent(String email, Instant now) {
        return new User(email, Role.STUDENT, now);
    }

    /** Conta de administrador, criada na instalação. */
    public static User newAdmin(String email, Instant now) {
        return new User(email, Role.ADMIN, now);
    }

    /** E-mails são comparados sem espaços nas pontas e sem diferenciar maiúsculas e minúsculas. */
    public static String normalize(String email) {
        return Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT);
    }

    /** Torna esta conta administradora; usado na criação do administrador inicial. */
    public void promoteToAdmin() {
        this.role = Role.ADMIN;
    }

    /** Dá o perfil de funcionário a um estudante (RF03). */
    public void grantStaff() {
        requireNotAdmin();
        if (role == Role.STAFF) {
            throw RoleChangeNotAllowedException.alreadyStaff();
        }
        this.role = Role.STAFF;
    }

    /** Retira o perfil de funcionário: a conta volta a ser de estudante (RF03). */
    public void revokeStaff() {
        requireNotAdmin();
        if (role != Role.STAFF) {
            throw RoleChangeNotAllowedException.notStaff();
        }
        this.role = Role.STUDENT;
    }

    private void requireNotAdmin() {
        if (role == Role.ADMIN) {
            throw RoleChangeNotAllowedException.adminAccount();
        }
    }

    public boolean hasRole(Role role) {
        return this.role == role;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
