package br.ucsal.cade.access.persistence;

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
 * User account (RF02). Identified by the institutional e-mail, with no password (D02), and a single role.
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

    /** Account created on the first valid login: every new user starts as a student. */
    public static User newStudent(String email, Instant now) {
        return new User(email, Role.STUDENT, now);
    }

    /** Administrator account, created at installation. */
    public static User newAdmin(String email, Instant now) {
        return new User(email, Role.ADMIN, now);
    }

    /** E-mails are compared trimmed and case-insensitively. */
    public static String normalize(String email) {
        return Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT);
    }

    /** Makes this account an administrator; used when creating the initial administrator. */
    public void promoteToAdmin() {
        this.role = Role.ADMIN;
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
