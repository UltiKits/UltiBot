package com.ultikits.plugins.ultibot.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

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
}
