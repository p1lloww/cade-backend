package br.ucsal.cade.shared.security;

/**
 * Role of an account (RF02). Each account has exactly one role; visitors have no account.
 *
 * <p>Lives in the shared kernel because role-based authorization ({@code @RequiresRole}) is used by the
 * controllers of every module, and only the modules listed in D10 may depend on {@code access}.
 */
public enum Role {

    /** Every new valid login starts as a student. */
    STUDENT,

    /** Granted by an administrator (RF03). */
    STAFF,

    /** Created at installation; can do everything staff can, and more. */
    ADMIN
}
