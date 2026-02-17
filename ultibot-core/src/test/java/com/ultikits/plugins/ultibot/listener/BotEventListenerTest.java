package com.ultikits.plugins.ultibot.listener;

import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.config.BotConfig;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;

@DisplayName("BotEventListener")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BotEventListenerTest {

    @Mock private BotManagerImpl botManager;
    @Mock private BotConfig config;

    private BotEventListener listener;

    @BeforeEach
    void setUp() {
        when(botManager.getConfig()).thenReturn(config);
        listener = new BotEventListener(botManager);
    }

    @Nested
    @DisplayName("onPlayerQuit")
    class OnPlayerQuit {

        @Test
        @DisplayName("should remove bots when owner quits and autoRemoveOnQuit is true")
        void shouldRemoveBotsOnOwnerQuit() {
            when(config.isAutoRemoveOnQuit()).thenReturn(true);
            Player quitter = mock(Player.class);
            UUID quitterUuid = UUID.randomUUID();
            when(quitter.getUniqueId()).thenReturn(quitterUuid);
            when(botManager.isBot(quitterUuid)).thenReturn(false);

            PlayerQuitEvent event = new PlayerQuitEvent(quitter, "left");
            listener.onPlayerQuit(event);

            verify(botManager).removeBotsForOwner(quitterUuid);
        }

        @Test
        @DisplayName("should not remove bots when autoRemoveOnQuit is false")
        void shouldNotRemoveWhenDisabled() {
            when(config.isAutoRemoveOnQuit()).thenReturn(false);
            Player quitter = mock(Player.class);
            UUID quitterUuid = UUID.randomUUID();
            when(quitter.getUniqueId()).thenReturn(quitterUuid);
            when(botManager.isBot(quitterUuid)).thenReturn(false);

            PlayerQuitEvent event = new PlayerQuitEvent(quitter, "left");
            listener.onPlayerQuit(event);

            verify(botManager, never()).removeBotsForOwner(any());
        }

        @Test
        @DisplayName("should ignore quit events from bots themselves")
        void shouldIgnoreBotQuit() {
            when(config.isAutoRemoveOnQuit()).thenReturn(true);
            Player bot = mock(Player.class);
            UUID botUuid = UUID.randomUUID();
            when(bot.getUniqueId()).thenReturn(botUuid);
            when(botManager.isBot(botUuid)).thenReturn(true);

            PlayerQuitEvent event = new PlayerQuitEvent(bot, "left");
            listener.onPlayerQuit(event);

            verify(botManager, never()).removeBotsForOwner(any());
        }
    }

    @Nested
    @DisplayName("onPlayerDeath")
    class OnPlayerDeath {

        @Test
        @DisplayName("should respawn bot when it dies and autoRespawn is true")
        void shouldRespawnBot() {
            when(config.isAutoRespawn()).thenReturn(true);
            Player deadPlayer = mock(Player.class);
            UUID deadUuid = UUID.randomUUID();
            when(deadPlayer.getUniqueId()).thenReturn(deadUuid);

            BotPlayer botPlayer = mock(BotPlayer.class);
            when(botManager.getBot(deadUuid)).thenReturn(botPlayer);

            PlayerDeathEvent event = new PlayerDeathEvent(deadPlayer, Collections.emptyList(), 0, "died");
            listener.onPlayerDeath(event);

            verify(botPlayer).respawn();
        }

        @Test
        @DisplayName("should not respawn when autoRespawn is false")
        void shouldNotRespawnWhenDisabled() {
            when(config.isAutoRespawn()).thenReturn(false);
            Player deadPlayer = mock(Player.class);
            UUID deadUuid = UUID.randomUUID();
            when(deadPlayer.getUniqueId()).thenReturn(deadUuid);

            BotPlayer botPlayer = mock(BotPlayer.class);
            when(botManager.getBot(deadUuid)).thenReturn(botPlayer);

            PlayerDeathEvent event = new PlayerDeathEvent(deadPlayer, Collections.emptyList(), 0, "died");
            listener.onPlayerDeath(event);

            verify(botPlayer, never()).respawn();
        }

        @Test
        @DisplayName("should ignore death of non-bot players")
        void shouldIgnoreNonBotDeath() {
            when(config.isAutoRespawn()).thenReturn(true);
            Player deadPlayer = mock(Player.class);
            UUID deadUuid = UUID.randomUUID();
            when(deadPlayer.getUniqueId()).thenReturn(deadUuid);
            when(botManager.getBot(deadUuid)).thenReturn(null);

            PlayerDeathEvent event = new PlayerDeathEvent(deadPlayer, Collections.emptyList(), 0, "died");
            listener.onPlayerDeath(event);

            // No bot to respawn — no exceptions
        }
    }
}
