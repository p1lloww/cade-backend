package br.ucsal.cade.shared.audit;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEventTest {

    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID ENTITY_ID = UUID.randomUUID();
    private static final String ACTION = "FOUND_ITEM_REGISTERED";
    private static final String ENTITY_TYPE = "FoundItem";
    private static final String REASON = "Objeto cadastrado pelo funcionário";

    @Test
    void createsAValidEvent() {
        AuditEvent event = new AuditEvent(ACTOR_ID, ACTION, ENTITY_TYPE, ENTITY_ID, REASON);

        assertThat(event.actorId()).isEqualTo(ACTOR_ID);
        assertThat(event.action()).isEqualTo(ACTION);
        assertThat(event.entityType()).isEqualTo(ENTITY_TYPE);
        assertThat(event.entityId()).isEqualTo(ENTITY_ID);
        assertThat(event.reason()).isEqualTo(REASON);
    }

    @Test
    void acceptsTheSystemActor() {
        AuditEvent event = new AuditEvent(AuditEvent.SYSTEM_ACTOR, ACTION, ENTITY_TYPE, ENTITY_ID, REASON);

        assertThat(event.actorId()).isEqualTo(AuditEvent.SYSTEM_ACTOR);
    }

    @Test
    void rejectsANullActor() {
        assertThatThrownBy(() -> new AuditEvent(null, ACTION, ENTITY_TYPE, ENTITY_ID, REASON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullAction() {
        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, null, ENTITY_TYPE, ENTITY_ID, REASON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullEntityType() {
        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, ACTION, null, ENTITY_ID, REASON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullEntityId() {
        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, ACTION, ENTITY_TYPE, null, REASON))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsANullReason() {
        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, ACTION, ENTITY_TYPE, ENTITY_ID, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsAnEmptyAction() {
        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, "", ENTITY_TYPE, ENTITY_ID, REASON))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, "   ", ENTITY_TYPE, ENTITY_ID, REASON))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnEmptyEntityType() {
        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, ACTION, "", ENTITY_ID, REASON))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, ACTION, "   ", ENTITY_ID, REASON))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnEmptyReason() {
        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, ACTION, ENTITY_TYPE, ENTITY_ID, ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, ACTION, ENTITY_TYPE, ENTITY_ID, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsValuesWithExactlyTheMaximumLength() {
        String name = "A".repeat(AuditEvent.MAX_NAME_LENGTH);
        String reason = "R".repeat(AuditEvent.MAX_REASON_LENGTH);

        assertThatCode(() -> new AuditEvent(ACTOR_ID, name, name, ENTITY_ID, reason))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnActionLongerThanTheLimit() {
        String tooLong = "A".repeat(AuditEvent.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, tooLong, ENTITY_TYPE, ENTITY_ID, REASON))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnEntityTypeLongerThanTheLimit() {
        String tooLong = "E".repeat(AuditEvent.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, ACTION, tooLong, ENTITY_ID, REASON))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAReasonLongerThanTheLimit() {
        String tooLong = "R".repeat(AuditEvent.MAX_REASON_LENGTH + 1);

        assertThatThrownBy(() -> new AuditEvent(ACTOR_ID, ACTION, ENTITY_TYPE, ENTITY_ID, tooLong))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
