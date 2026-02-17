package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.*;
import com.ultikits.plugins.ultibot.config.BotConfig;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BotManagerImpl")
class BotManagerImplTest {

    @Mock private NMSBridge nmsBridge;
    @Mock private UltiToolsPlugin plugin;
    @Mock private World world;
    @Mock private Player ownerPlayer;

    private BotConfig config;
    private BotManagerImpl manager;
    private Location spawnLocation;

    @BeforeEach
    void setUp() {
        config = new BotConfig();
        lenient().when(world.getName()).thenReturn("world");
        spawnLocation = new Location(world, 0, 64, 0);
        lenient().when(ownerPlayer.getUniqueId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        lenient().when(ownerPlayer.getName()).thenReturn("TestOwner");
        manager = new BotManagerImpl(nmsBridge, config, plugin);
    }

    @Nested
    @DisplayName("spawnBot")
    class SpawnBot {

        @Test
        @DisplayName("should create and register bot")
        void shouldCreateBot() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(mockBot.getName()).thenReturn("Alice");
            when(mockBot.getUUID()).thenReturn(UUID.randomUUID());
            when(nmsBridge.createBot(eq("Alice"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(mockBot);

            BotPlayer result = manager.spawnBot("Alice", spawnLocation, ownerPlayer);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Alice");
            verify(mockBot).join();
        }

        @Test
        @DisplayName("should reject duplicate name")
        void shouldRejectDuplicateName() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(mockBot.getName()).thenReturn("Alice");
            when(mockBot.getUUID()).thenReturn(UUID.randomUUID());
            when(nmsBridge.createBot(eq("Alice"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(mockBot);

            manager.spawnBot("Alice", spawnLocation, ownerPlayer);
            BotPlayer duplicate = manager.spawnBot("Alice", spawnLocation, ownerPlayer);

            assertThat(duplicate).isNull();
        }

        @Test
        @DisplayName("should enforce per-player limit")
        void shouldEnforcePlayerLimit() {
            config.setMaxBotsPerPlayer(2);
            for (int i = 0; i < 2; i++) {
                BotPlayer bot = mock(BotPlayer.class);
                String name = "Bot" + i;
                when(bot.getName()).thenReturn(name);
                when(bot.getUUID()).thenReturn(UUID.randomUUID());
                when(nmsBridge.createBot(eq(name), any(UUID.class), eq(spawnLocation)))
                    .thenReturn(bot);
                manager.spawnBot(name, spawnLocation, ownerPlayer);
            }

            BotPlayer result = manager.spawnBot("Bot2", spawnLocation, ownerPlayer);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should enforce server-wide limit")
        void shouldEnforceServerLimit() {
            config.setMaxTotalBots(1);
            BotPlayer bot1 = mock(BotPlayer.class);
            when(bot1.getName()).thenReturn("Bot0");
            when(bot1.getUUID()).thenReturn(UUID.randomUUID());
            when(nmsBridge.createBot(eq("Bot0"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(bot1);
            manager.spawnBot("Bot0", spawnLocation, ownerPlayer);

            Player owner2 = mock(Player.class);
            when(owner2.getUniqueId()).thenReturn(UUID.randomUUID());
            BotPlayer result = manager.spawnBot("Bot1", spawnLocation, owner2);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when NMS bridge returns null")
        void shouldReturnNullWhenBridgeFails() {
            when(nmsBridge.createBot(eq("Broken"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(null);

            BotPlayer result = manager.spawnBot("Broken", spawnLocation, ownerPlayer);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("removeBot")
    class RemoveBot {

        @Test
        @DisplayName("should remove by name")
        void shouldRemoveByName() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(mockBot.getName()).thenReturn("Alice");
            UUID botUuid = UUID.randomUUID();
            when(mockBot.getUUID()).thenReturn(botUuid);
            when(nmsBridge.createBot(eq("Alice"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(mockBot);

            manager.spawnBot("Alice", spawnLocation, ownerPlayer);
            manager.removeBot("Alice");

            assertThat(manager.getBot("Alice")).isNull();
            verify(mockBot).disconnect(anyString());
        }

        @Test
        @DisplayName("should remove by UUID")
        void shouldRemoveByUuid() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(mockBot.getName()).thenReturn("Alice");
            UUID botUuid = UUID.randomUUID();
            when(mockBot.getUUID()).thenReturn(botUuid);
            when(nmsBridge.createBot(eq("Alice"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(mockBot);

            manager.spawnBot("Alice", spawnLocation, ownerPlayer);
            manager.removeBot(botUuid);

            assertThat(manager.getBot("Alice")).isNull();
            verify(mockBot).disconnect(anyString());
        }

        @Test
        @DisplayName("should remove all bots")
        void shouldRemoveAll() {
            for (int i = 0; i < 3; i++) {
                BotPlayer bot = mock(BotPlayer.class);
                String name = "Bot" + i;
                when(bot.getName()).thenReturn(name);
                when(bot.getUUID()).thenReturn(UUID.randomUUID());
                when(nmsBridge.createBot(eq(name), any(UUID.class), eq(spawnLocation)))
                    .thenReturn(bot);
                manager.spawnBot(name, spawnLocation, ownerPlayer);
            }

            manager.removeAllBots();
            assertThat(manager.getAllBots()).isEmpty();
        }

        @Test
        @DisplayName("should handle removing non-existent bot gracefully")
        void shouldHandleNonExistent() {
            manager.removeBot("Nonexistent");
            manager.removeBot(UUID.randomUUID());
            // no exception
        }
    }

    @Nested
    @DisplayName("query")
    class Query {

        @Test
        @DisplayName("isBot should return true for registered bot")
        void isBotShouldReturnTrue() {
            BotPlayer mockBot = mock(BotPlayer.class);
            UUID botUuid = UUID.randomUUID();
            when(mockBot.getName()).thenReturn("Alice");
            when(mockBot.getUUID()).thenReturn(botUuid);
            when(nmsBridge.createBot(eq("Alice"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(mockBot);

            manager.spawnBot("Alice", spawnLocation, ownerPlayer);
            assertThat(manager.isBot(botUuid)).isTrue();
        }

        @Test
        @DisplayName("isBot should return false for unknown UUID")
        void isBotShouldReturnFalse() {
            assertThat(manager.isBot(UUID.randomUUID())).isFalse();
        }

        @Test
        @DisplayName("isBot with Player should delegate to UUID check")
        void isBotWithPlayerShouldDelegate() {
            Player fakePlayer = mock(Player.class);
            when(fakePlayer.getUniqueId()).thenReturn(UUID.randomUUID());
            assertThat(manager.isBot(fakePlayer)).isFalse();
        }

        @Test
        @DisplayName("getAllBots should return all")
        void getAllShouldReturnAll() {
            BotPlayer bot1 = mock(BotPlayer.class);
            when(bot1.getName()).thenReturn("A");
            when(bot1.getUUID()).thenReturn(UUID.randomUUID());
            when(nmsBridge.createBot(eq("A"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(bot1);
            manager.spawnBot("A", spawnLocation, ownerPlayer);

            assertThat(manager.getAllBots()).hasSize(1);
        }

        @Test
        @DisplayName("getBot by UUID should return matching bot")
        void getBotByUuidShouldReturn() {
            BotPlayer mockBot = mock(BotPlayer.class);
            UUID botUuid = UUID.randomUUID();
            when(mockBot.getName()).thenReturn("Alice");
            when(mockBot.getUUID()).thenReturn(botUuid);
            when(nmsBridge.createBot(eq("Alice"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(mockBot);

            manager.spawnBot("Alice", spawnLocation, ownerPlayer);
            assertThat(manager.getBot(botUuid)).isSameAs(mockBot);
        }
    }

    @Nested
    @DisplayName("owner management")
    class OwnerManagement {

        @Test
        @DisplayName("should remove only that owner's bots")
        void shouldRemoveOnlyOwnerBots() {
            BotPlayer bot1 = mock(BotPlayer.class);
            when(bot1.getName()).thenReturn("A");
            when(bot1.getUUID()).thenReturn(UUID.randomUUID());
            when(nmsBridge.createBot(eq("A"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(bot1);
            manager.spawnBot("A", spawnLocation, ownerPlayer);

            Player owner2 = mock(Player.class);
            UUID owner2Uuid = UUID.randomUUID();
            when(owner2.getUniqueId()).thenReturn(owner2Uuid);
            BotPlayer bot2 = mock(BotPlayer.class);
            when(bot2.getName()).thenReturn("B");
            when(bot2.getUUID()).thenReturn(UUID.randomUUID());
            when(nmsBridge.createBot(eq("B"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(bot2);
            manager.spawnBot("B", spawnLocation, owner2);

            manager.removeBotsForOwner(ownerPlayer.getUniqueId());

            assertThat(manager.getBot("A")).isNull();
            assertThat(manager.getBot("B")).isNotNull();
        }

        @Test
        @DisplayName("should track owner of bot")
        void shouldTrackOwner() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(mockBot.getName()).thenReturn("Alice");
            when(mockBot.getUUID()).thenReturn(UUID.randomUUID());
            when(nmsBridge.createBot(eq("Alice"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(mockBot);

            manager.spawnBot("Alice", spawnLocation, ownerPlayer);
            assertThat(manager.getOwnerOf("Alice")).isEqualTo(ownerPlayer.getUniqueId());
        }

        @Test
        @DisplayName("should return null owner for unknown bot")
        void shouldReturnNullOwnerForUnknown() {
            assertThat(manager.getOwnerOf("Nobody")).isNull();
        }

        @Test
        @DisplayName("should count bots for owner")
        void shouldCountBotsForOwner() {
            BotPlayer bot = mock(BotPlayer.class);
            when(bot.getName()).thenReturn("A");
            when(bot.getUUID()).thenReturn(UUID.randomUUID());
            when(nmsBridge.createBot(eq("A"), any(UUID.class), eq(spawnLocation)))
                .thenReturn(bot);
            manager.spawnBot("A", spawnLocation, ownerPlayer);

            assertThat(manager.getBotCountForOwner(ownerPlayer.getUniqueId())).isEqualTo(1);
            assertThat(manager.getBotCountForOwner(UUID.randomUUID())).isEqualTo(0);
        }
    }
}
