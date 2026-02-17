package com.ultikits.plugins.ultibot.commands;

import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.config.BotConfig;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("BotCommands")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BotCommandsTest {

    @Mock private UltiToolsPlugin plugin;
    @Mock private BotManagerImpl botManager;
    @Mock private Player player;
    @Mock private World world;
    @Mock private BotConfig config;

    private BotCommands commands;

    @BeforeEach
    void setUp() {
        when(plugin.i18n(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission(anyString())).thenReturn(true);
        when(botManager.getConfig()).thenReturn(config);
        when(config.getMaxBotsPerPlayer()).thenReturn(5);
        commands = new BotCommands(plugin, botManager);
    }

    @Nested
    @DisplayName("spawn")
    class Spawn {

        @Test
        @DisplayName("should spawn bot and send success message")
        void shouldSpawnBot() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(mockBot.getName()).thenReturn("Alice");
            when(botManager.spawnBot(eq("Alice"), any(Location.class), eq(player)))
                    .thenReturn(mockBot);

            commands.onSpawn(player, "Alice");

            verify(botManager).spawnBot(eq("Alice"), any(Location.class), eq(player));
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_spawned");
        }

        @Test
        @DisplayName("should send error when spawn fails (limit or duplicate)")
        void shouldSendErrorWhenSpawnFails() {
            when(botManager.spawnBot(eq("Alice"), any(Location.class), eq(player)))
                    .thenReturn(null);

            commands.onSpawn(player, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            // Could be limit or name-taken; the command checks botManager.getBot first
            assertThat(captor.getValue()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("remove")
    class Remove {

        @Test
        @DisplayName("should remove bot by name")
        void shouldRemoveByName() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onRemove(player, "Alice");

            verify(botManager).removeBot("Alice");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_removed");
        }

        @Test
        @DisplayName("should remove all bots")
        void shouldRemoveAll() {
            commands.onRemove(player, "all");

            verify(botManager).removeAllBots();
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_removed_all");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknown() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onRemove(player, "Ghost");

            verify(botManager, never()).removeBot("Ghost");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("list")
    class ListCmd {

        @Test
        @DisplayName("should list active bots")
        void shouldListBots() {
            BotPlayer bot1 = mock(BotPlayer.class);
            when(bot1.getName()).thenReturn("Alice");
            BotPlayer bot2 = mock(BotPlayer.class);
            when(bot2.getName()).thenReturn("Bob");
            when(botManager.getAllBots()).thenReturn(Arrays.asList(bot1, bot2));
            when(botManager.getOwnerOf("Alice")).thenReturn(UUID.randomUUID());
            when(botManager.getOwnerOf("Bob")).thenReturn(UUID.randomUUID());

            commands.onList(player);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player, atLeast(2)).sendMessage(captor.capture());
            List<String> messages = captor.getAllValues();
            assertThat(messages.get(0)).contains("bot_list_header");
        }

        @Test
        @DisplayName("should show empty message when no bots")
        void shouldShowEmpty() {
            when(botManager.getAllBots()).thenReturn(Collections.emptyList());

            commands.onList(player);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_list_empty");
        }
    }

    @Nested
    @DisplayName("teleport")
    class Teleport {

        @Test
        @DisplayName("should teleport bot to player location")
        void shouldTeleportBot() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            Location playerLoc = new Location(world, 10, 64, 20);
            when(player.getLocation()).thenReturn(playerLoc);

            commands.onTeleport(player, "Alice");

            verify(mockBot).moveTo(playerLoc);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_teleported");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknown() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onTeleport(player, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("reload")
    class Reload {

        @Test
        @DisplayName("should send reload success message")
        void shouldReload() {
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission(anyString())).thenReturn(true);

            commands.onReload(sender);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("reload_success");
        }
    }
}
