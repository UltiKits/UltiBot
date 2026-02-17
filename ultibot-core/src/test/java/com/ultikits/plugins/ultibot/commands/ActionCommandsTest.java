package com.ultikits.plugins.ultibot.commands;

import com.ultikits.plugins.ultibot.api.ActionTicker;
import com.ultikits.plugins.ultibot.api.ActionType;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.service.ActionServiceImpl;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ActionCommands")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActionCommandsTest {

    @Mock private UltiToolsPlugin plugin;
    @Mock private BotManagerImpl botManager;
    @Mock private ActionServiceImpl actionService;
    @Mock private Player player;

    private ActionCommands commands;

    @BeforeEach
    void setUp() {
        when(plugin.i18n(anyString())).thenAnswer(inv -> inv.getArgument(0));
        commands = new ActionCommands(plugin, botManager, actionService);
    }

    @Nested
    @DisplayName("action")
    class Action {

        @Test
        @DisplayName("should start action on bot")
        void shouldStartAction() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            ActionTicker ticker = mock(ActionTicker.class);
            when(actionService.startRepeatingAction(mockBot, ActionType.JUMP, 20))
                    .thenReturn(ticker);

            commands.onAction(player, "Alice", "JUMP", 20);

            verify(actionService).startRepeatingAction(mockBot, ActionType.JUMP, 20);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_action_started");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onAction(player, "Ghost", "JUMP", 20);

            verify(actionService, never()).startRepeatingAction(any(), any(), anyInt());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }

        @Test
        @DisplayName("should send error for invalid action type")
        void shouldSendErrorForInvalidAction() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onAction(player, "Alice", "INVALID_ACTION", 20);

            verify(actionService, never()).startRepeatingAction(any(), any(), anyInt());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_action_invalid");
        }
    }

    @Nested
    @DisplayName("stop")
    class Stop {

        @Test
        @DisplayName("should stop all actions on bot")
        void shouldStopAllActions() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onStop(player, "Alice");

            verify(actionService).stopAllActions(mockBot);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_action_stopped");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onStop(player, "Ghost");

            verify(actionService, never()).stopAllActions(any());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("help")
    class Help {
        @Test
        @DisplayName("should send usage info")
        void shouldSendUsageInfo() {
            commands.handleHelp(player);
            verify(player).sendMessage(anyString());
        }
    }
}
