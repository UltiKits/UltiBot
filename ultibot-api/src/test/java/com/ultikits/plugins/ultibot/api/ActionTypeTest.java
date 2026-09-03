package com.ultikits.plugins.ultibot.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ActionType")
class ActionTypeTest {

    @Test
    @DisplayName("should have all 10 action types")
    void shouldHaveAllActionTypes() {
        assertThat(ActionType.values()).hasSize(10);
    }

    @Test
    @DisplayName("should resolve from name")
    void shouldResolveFromName() {
        assertThat(ActionType.valueOf("ATTACK")).isEqualTo(ActionType.ATTACK);
        assertThat(ActionType.valueOf("MINE")).isEqualTo(ActionType.MINE);
        assertThat(ActionType.valueOf("LOOK_AT_NEAREST")).isEqualTo(ActionType.LOOK_AT_NEAREST);
    }

    @Test
    @DisplayName("should contain exactly the ten documented constants, by name")
    void shouldExposeExactConstantSet() {
        assertThat(ActionType.values())
                .extracting(Enum::name)
                .containsExactlyInAnyOrder(
                        "ATTACK", "MINE", "USE", "JUMP", "DROP_ITEM", "DROP_STACK",
                        "DROP_INVENTORY", "LOOK_AT_NEAREST", "SNEAK", "SPRINT");
    }

    @Test
    @DisplayName("valueOf should reject a name that is not a declared constant")
    void valueOfShouldRejectUnknownName() {
        assertThatThrownBy(() -> ActionType.valueOf("TELEPORT"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("valueOf should reject a null name")
    void valueOfShouldRejectNullName() {
        assertThatThrownBy(() -> ActionType.valueOf(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("valueOf should be case-sensitive and reject a lowercase match")
    void valueOfShouldBeCaseSensitive() {
        assertThatThrownBy(() -> ActionType.valueOf("attack"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
