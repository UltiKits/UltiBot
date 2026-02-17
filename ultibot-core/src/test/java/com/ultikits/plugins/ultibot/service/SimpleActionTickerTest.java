package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.ActionType;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleActionTicker")
class SimpleActionTickerTest {

    @Mock private BotPlayer bot;
    @Mock private ActionServiceImpl actionService;
    private SimpleActionTicker ticker;

    @BeforeEach
    void setUp() {
        ticker = new SimpleActionTicker(ActionType.JUMP, bot, 5, actionService);
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {
        @Test
        @DisplayName("starts in stopped state")
        void startsNotRunning() {
            assertThat(ticker.isRunning()).isFalse();
        }

        @Test
        @DisplayName("start sets running to true")
        void startSetsRunning() {
            ticker.start();
            assertThat(ticker.isRunning()).isTrue();
        }

        @Test
        @DisplayName("stop sets running to false")
        void stopSetsNotRunning() {
            ticker.start();
            ticker.stop();
            assertThat(ticker.isRunning()).isFalse();
        }

        @Test
        @DisplayName("returns correct action type")
        void returnsActionType() {
            assertThat(ticker.getActionType()).isEqualTo(ActionType.JUMP);
        }
    }

    @Nested
    @DisplayName("tick behavior")
    class TickBehavior {
        @Test
        @DisplayName("does nothing when not running")
        void doesNothingWhenStopped() {
            ticker.tick();
            verifyNoInteractions(actionService);
        }

        @Test
        @DisplayName("performs action after interval ticks")
        void performsActionAfterInterval() {
            ticker.start();
            // Tick 4 times (interval is 5)
            for (int i = 0; i < 4; i++) {
                ticker.tick();
            }
            verifyNoInteractions(actionService);
            // 5th tick triggers the action
            ticker.tick();
            verify(actionService).performAction(bot, ActionType.JUMP);
        }

        @Test
        @DisplayName("repeats action every interval")
        void repeatsAction() {
            ticker.start();
            // First cycle: 5 ticks
            for (int i = 0; i < 5; i++) {
                ticker.tick();
            }
            verify(actionService, times(1)).performAction(bot, ActionType.JUMP);

            // Second cycle: 5 more ticks
            for (int i = 0; i < 5; i++) {
                ticker.tick();
            }
            verify(actionService, times(2)).performAction(bot, ActionType.JUMP);
        }
    }
}
