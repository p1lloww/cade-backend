package br.ucsal.cade.access.persistence.logincode;

/** Outcome of checking a typed code against a {@link LoginCode}. */
public enum LoginCodeVerification {

    /** Right code, within its validity; the code is now used. */
    VALID,

    /** Wrong code; counts as an attempt. */
    WRONG_CODE,

    /** The validity period is over. */
    EXPIRED,

    /** The code was already exchanged for a token. */
    ALREADY_USED,

    /** The maximum number of wrong attempts was reached; a new code must be requested. */
    TOO_MANY_ATTEMPTS
}
