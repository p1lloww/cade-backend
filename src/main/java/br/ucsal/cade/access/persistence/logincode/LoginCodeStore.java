package br.ucsal.cade.access.persistence.logincode;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Login codes in Redis. Each e-mail has at most two keys, both with an expiry, so nothing is left behind:
 *
 * <ul>
 *   <li>{@code cade:login-code:<email>}: hash with the current code; a new code overwrites the previous one, so only
 *       the latest is valid;</li>
 *   <li>{@code cade:login-code-cooldown:<email>}: exists while a new code cannot be requested yet.</li>
 * </ul>
 *
 * <p>Operations that read and change a key run as a single Redis command or Lua script, so two concurrent requests
 * cannot both pass a check.
 */
@Repository
public class LoginCodeStore {

    private static final String CODE_PREFIX = "cade:login-code:";
    private static final String COOLDOWN_PREFIX = "cade:login-code-cooldown:";

    private static final String CODE_HASH = "codeHash";
    private static final String CREATED_AT = "createdAt";
    private static final String EXPIRES_AT = "expiresAt";
    private static final String ATTEMPTS = "attempts";

    /** Replaces the code of the e-mail and sets its expiry in one step. */
    private static final RedisScript<Long> SAVE = RedisScript.of("""
            redis.call('DEL', KEYS[1])
            redis.call('HSET', KEYS[1], 'codeHash', ARGV[1], 'createdAt', ARGV[2], 'expiresAt', ARGV[3], 'attempts', 0)
            return redis.call('PEXPIRE', KEYS[1], ARGV[4])
            """, Long.class);

    /**
     * Counts one attempt and returns the new total, or -1 when there is no code. A plain HINCRBY would create a
     * key with no expiry if the code had just expired.
     */
    private static final RedisScript<Long> REGISTER_ATTEMPT = RedisScript.of("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                return -1
            end
            return redis.call('HINCRBY', KEYS[1], 'attempts', 1)
            """, Long.class);

    private final StringRedisTemplate redis;

    LoginCodeStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Starts the waiting time before the next code can be requested.
     *
     * @return {@code false} if a waiting time is already running for this e-mail
     */
    public boolean startCooldown(String email, Duration interval) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(COOLDOWN_PREFIX + email, "1", interval));
    }

    /** Time left before the e-mail can request another code; zero if it already can. */
    public Duration cooldownRemaining(String email) {
        Long millis = redis.getExpire(COOLDOWN_PREFIX + email, TimeUnit.MILLISECONDS);
        return millis == null || millis < 0 ? Duration.ZERO : Duration.ofMillis(millis);
    }

    /** Stores the code, replacing the previous one of the same e-mail; Redis deletes it when {@code ttl} is over. */
    public void save(LoginCode code, Duration ttl) {
        redis.execute(SAVE, List.of(CODE_PREFIX + code.email()),
                code.codeHash(), code.createdAt().toString(), code.expiresAt().toString(),
                String.valueOf(ttl.toMillis()));
    }

    /** The current code of the e-mail; empty if it was never requested, has expired or was already used. */
    public Optional<LoginCode> find(String email) {
        Map<Object, Object> fields = redis.opsForHash().entries(CODE_PREFIX + email);
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new LoginCode(email,
                (String) fields.get(CODE_HASH),
                Instant.parse((String) fields.get(CREATED_AT)),
                Instant.parse((String) fields.get(EXPIRES_AT)),
                Integer.parseInt((String) fields.get(ATTEMPTS))));
    }

    /**
     * Counts one attempt before the code is compared, so parallel guesses cannot all see the old count.
     *
     * @return the number of attempts including this one, or -1 if the code no longer exists
     */
    public long registerAttempt(String email) {
        Long attempts = redis.execute(REGISTER_ATTEMPT, List.of(CODE_PREFIX + email));
        return attempts == null ? -1 : attempts;
    }

    /**
     * Deletes everything kept for the e-mail after a successful login: the code and the waiting time.
     *
     * @return {@code true} only for the request that actually deleted the code, so a code is used once even when
     *         two requests with the right code arrive together
     */
    public boolean consume(String email) {
        boolean deleted = Boolean.TRUE.equals(redis.delete(CODE_PREFIX + email));
        redis.delete(COOLDOWN_PREFIX + email);
        return deleted;
    }
}
