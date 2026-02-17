package com.ultikits.plugins.ultibot.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BotConfig")
class BotConfigTest {

    @Test
    @DisplayName("should have sensible defaults")
    void shouldHaveSensibleDefaults() {
        BotConfig config = new BotConfig();
        assertThat(config.getMaxBotsPerPlayer()).isEqualTo(5);
        assertThat(config.getMaxTotalBots()).isEqualTo(20);
        assertThat(config.getDefaultSkin()).isEqualTo("Steve");
        assertThat(config.isTickBots()).isTrue();
        assertThat(config.isAllowChunkLoading()).isFalse();
        assertThat(config.getBotPrefix()).isEqualTo("[Bot] ");
        assertThat(config.isAutoRemoveOnQuit()).isTrue();
        assertThat(config.isAutoRespawn()).isTrue();
    }
}
