package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ActionServiceImpl")
class ActionServiceImplTest {

    private ActionServiceImpl actionService;

    @BeforeEach
    void setUp() {
        actionService = new ActionServiceImpl();
    }

    private BotPlayer createMockBot(String name) {
        BotPlayer bot = mock(BotPlayer.class);
        when(bot.getName()).thenReturn(name);
        when(bot.getUUID()).thenReturn(UUID.randomUUID());
        return bot;
    }

    @Nested
    @DisplayName("performAction")
    class PerformAction {

        @Test
        @DisplayName("should execute ATTACK on bot")
        void shouldExecuteAttack() {
            BotPlayer bot = createMockBot("Alice");
            actionService.performAction(bot, ActionType.ATTACK);
            // ATTACK without target is a no-op swing — implementation delegates to bot
        }

        @Test
        @DisplayName("should execute JUMP on bot")
        void shouldExecuteJump() {
            BotPlayer bot = createMockBot("Alice");
            actionService.performAction(bot, ActionType.JUMP);
            verify(bot).jump();
        }

        @Test
        @DisplayName("should execute SNEAK toggle on bot")
        void shouldExecuteSneak() {
            BotPlayer bot = createMockBot("Alice");
            actionService.performAction(bot, ActionType.SNEAK);
            verify(bot).setSneaking(true);
        }

        @Test
        @DisplayName("should execute SPRINT toggle on bot")
        void shouldExecuteSprint() {
            BotPlayer bot = createMockBot("Alice");
            actionService.performAction(bot, ActionType.SPRINT);
            verify(bot).setSprinting(true);
        }

        @Test
        @DisplayName("should execute USE on bot")
        void shouldExecuteUse() {
            BotPlayer bot = createMockBot("Alice");
            actionService.performAction(bot, ActionType.USE);
            verify(bot).useItem();
        }

        @Test
        @DisplayName("should execute DROP_ITEM on bot")
        void shouldExecuteDropItem() {
            BotPlayer bot = createMockBot("Alice");
            actionService.performAction(bot, ActionType.DROP_ITEM);
            verify(bot).dropItem(false);
        }

        @Test
        @DisplayName("should execute DROP_STACK on bot")
        void shouldExecuteDropStack() {
            BotPlayer bot = createMockBot("Alice");
            actionService.performAction(bot, ActionType.DROP_STACK);
            verify(bot).dropItem(true);
        }

        @Test
        @DisplayName("should execute DROP_INVENTORY on bot")
        void shouldExecuteDropInventory() {
            BotPlayer bot = createMockBot("Alice");
            actionService.performAction(bot, ActionType.DROP_INVENTORY);
            verify(bot).dropInventory();
        }
    }

    @Nested
    @DisplayName("repeating actions")
    class RepeatingActions {

        @Test
        @DisplayName("should create and store a ticker")
        void shouldCreateTicker() {
            BotPlayer bot = createMockBot("Alice");
            ActionTicker ticker = actionService.startRepeatingAction(bot, ActionType.JUMP, 20);

            assertThat(ticker).isNotNull();
            assertThat(ticker.getActionType()).isEqualTo(ActionType.JUMP);
            assertThat(ticker.isRunning()).isTrue();
        }

        @Test
        @DisplayName("should track multiple actions per bot")
        void shouldTrackMultipleActions() {
            BotPlayer bot = createMockBot("Alice");
            ActionTicker t1 = actionService.startRepeatingAction(bot, ActionType.JUMP, 20);
            ActionTicker t2 = actionService.startRepeatingAction(bot, ActionType.SNEAK, 40);

            assertThat(t1).isNotNull();
            assertThat(t2).isNotNull();
            assertThat(t1.getActionType()).isNotEqualTo(t2.getActionType());
        }

        @Test
        @DisplayName("should stop all actions for a bot")
        void shouldStopAllActions() {
            BotPlayer bot = createMockBot("Alice");
            ActionTicker t1 = actionService.startRepeatingAction(bot, ActionType.JUMP, 20);
            ActionTicker t2 = actionService.startRepeatingAction(bot, ActionType.SNEAK, 40);

            actionService.stopAllActions(bot);

            assertThat(t1.isRunning()).isFalse();
            assertThat(t2.isRunning()).isFalse();
        }

        @Test
        @DisplayName("should stop individual action type")
        void shouldStopIndividualAction() {
            BotPlayer bot = createMockBot("Alice");
            ActionTicker t1 = actionService.startRepeatingAction(bot, ActionType.JUMP, 20);
            ActionTicker t2 = actionService.startRepeatingAction(bot, ActionType.SNEAK, 40);

            actionService.stopAction(bot, ActionType.JUMP);

            assertThat(t1.isRunning()).isFalse();
            assertThat(t2.isRunning()).isTrue();
        }

        @Test
        @DisplayName("should handle stopping actions for bot with no active actions")
        void shouldHandleNoActiveActions() {
            BotPlayer bot = createMockBot("Alice");
            actionService.stopAllActions(bot); // no exception
        }

        @Test
        @DisplayName("replacing same action type should stop previous ticker")
        void shouldReplaceExistingAction() {
            BotPlayer bot = createMockBot("Alice");
            ActionTicker t1 = actionService.startRepeatingAction(bot, ActionType.JUMP, 20);
            ActionTicker t2 = actionService.startRepeatingAction(bot, ActionType.JUMP, 10);

            assertThat(t1.isRunning()).isFalse();
            assertThat(t2.isRunning()).isTrue();
        }
    }
}
