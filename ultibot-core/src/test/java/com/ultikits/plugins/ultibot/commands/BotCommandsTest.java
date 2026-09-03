package com.ultikits.plugins.ultibot.commands;

import com.ultikits.plugins.ultibot.api.ActionTicker;
import com.ultikits.plugins.ultibot.api.ActionType;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.config.BotConfig;
import com.ultikits.plugins.ultibot.model.MacroEntry;
import com.ultikits.plugins.ultibot.service.ActionServiceImpl;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import com.ultikits.plugins.ultibot.service.MacroServiceImpl;
import com.ultikits.plugins.ultibot.service.SkinService;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    @Mock private ActionServiceImpl actionService;
    @Mock private MacroServiceImpl macroService;
    @Mock private SkinService skinService;
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
        commands = new BotCommands(plugin, botManager, actionService, macroService, skinService);
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

        @Test
        @DisplayName("should send error when bot name is already taken")
        void shouldSendErrorWhenNameTaken() {
            BotPlayer existing = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(existing);

            commands.onSpawn(player, "Alice");

            verify(botManager, never()).spawnBot(anyString(), any(Location.class), any(Player.class));
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_name_taken");
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

        @Test
        @DisplayName("should send no_permission message when sender lacks admin permission")
        void shouldDenyWithoutPermission() {
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("ultibot.admin")).thenReturn(false);

            commands.onReload(sender);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("no_permission");
        }
    }

    @Nested
    @DisplayName("spawnat")
    class SpawnAt {

        @Test
        @DisplayName("should spawn bot at world spawn location")
        void shouldSpawnAtWorldSpawn() {
            CommandSender sender = mock(CommandSender.class);
            Location spawnLoc = new Location(world, 1, 65, 1);
            when(world.getSpawnLocation()).thenReturn(spawnLoc);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.spawnBotNoOwner(eq("Alice"), eq(spawnLoc))).thenReturn(mockBot);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getWorlds).thenReturn(Collections.singletonList(world));

                commands.onSpawnAt(sender, "Alice");
            }

            verify(botManager).spawnBotNoOwner("Alice", spawnLoc);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_spawned");
        }

        @Test
        @DisplayName("should send error when name already taken")
        void shouldSendErrorWhenNameTaken() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Alice")).thenReturn(mock(BotPlayer.class));

            commands.onSpawnAt(sender, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_name_taken");
        }

        @Test
        @DisplayName("should send failure message when NMS bridge cannot spawn the bot")
        void shouldSendFailureMessageWhenSpawnFails() {
            CommandSender sender = mock(CommandSender.class);
            Location spawnLoc = new Location(world, 1, 65, 1);
            when(world.getSpawnLocation()).thenReturn(spawnLoc);
            when(botManager.spawnBotNoOwner(eq("Alice"), eq(spawnLoc))).thenReturn(null);

            try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
                bukkit.when(Bukkit::getWorlds).thenReturn(Collections.singletonList(world));

                commands.onSpawnAt(sender, "Alice");
            }

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("Failed to spawn bot Alice");
        }
    }

    @Nested
    @DisplayName("action")
    class Action {

        @Test
        @DisplayName("should start repeating action on bot")
        void shouldStartAction() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            ActionTicker ticker = mock(ActionTicker.class);
            when(actionService.startRepeatingAction(mockBot, ActionType.JUMP, 20)).thenReturn(ticker);

            commands.onAction(sender, "Alice", "jump", 20);

            verify(actionService).startRepeatingAction(mockBot, ActionType.JUMP, 20);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_action_started");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onAction(sender, "Ghost", "jump", 20);

            verify(actionService, never()).startRepeatingAction(any(), any(), anyInt());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }

        @Test
        @DisplayName("should send error for invalid action type")
        void shouldSendErrorForInvalidAction() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onAction(sender, "Alice", "NOT_A_REAL_ACTION", 20);

            verify(actionService, never()).startRepeatingAction(any(), any(), anyInt());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_action_invalid");
        }
    }

    @Nested
    @DisplayName("stop")
    class Stop {

        @Test
        @DisplayName("should stop all actions on bot")
        void shouldStopAllActions() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onStop(sender, "Alice");

            verify(actionService).stopAllActions(mockBot);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_action_stopped");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onStop(sender, "Ghost");

            verify(actionService, never()).stopAllActions(any());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("chat")
    class Chat {

        @Test
        @DisplayName("should join words and make bot send chat message")
        void shouldSendChatMessage() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onChat(sender, "Alice", new String[]{"Hello", "world"});

            verify(mockBot).chat("Hello world");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_chat_sent");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onChat(sender, "Ghost", new String[]{"Hello"});

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("cmd")
    class Cmd {

        @Test
        @DisplayName("should join args and make bot execute command")
        void shouldExecuteCommand() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onCmd(sender, "Alice", new String[]{"say", "hello"});

            verify(mockBot).performCommand("say hello");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_cmd_executed");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onCmd(sender, "Ghost", new String[]{"say", "hi"});

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("skin")
    class Skin {

        @Test
        @DisplayName("should change bot skin")
        void shouldChangeSkin() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(skinService.fetchSkin(eq("Notch"), any())).thenReturn(new SkinService.SkinData("val", "sig"));

            commands.onSkin(sender, "Alice", "Notch");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_skin_changed");
        }

        @Test
        @DisplayName("should send error when skin fetch fails")
        void shouldSendErrorWhenFetchFails() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(skinService.fetchSkin(eq("InvalidPlayer"), any())).thenReturn(null);

            commands.onSkin(sender, "Alice", "InvalidPlayer");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_skin_failed");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onSkin(sender, "Ghost", "Notch");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("messages")
    class Messages {

        @Test
        @DisplayName("should show captured messages")
        void shouldShowCapturedMessages() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getReceivedMessages()).thenReturn(Arrays.asList("Hello", "World"));

            commands.onMessages(sender, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender, atLeast(3)).sendMessage(captor.capture());
            List<String> msgs = captor.getAllValues();
            assertThat(msgs.get(0)).contains("bot_messages_header");
            assertThat(msgs.get(1)).contains("[0] Hello");
            assertThat(msgs.get(2)).contains("[1] World");
        }

        @Test
        @DisplayName("should show empty message when no captures")
        void shouldShowEmptyMessages() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getReceivedMessages()).thenReturn(Collections.emptyList());

            commands.onMessages(sender, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_messages_empty");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onMessages(sender, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("clearmsg")
    class ClearMessages {

        @Test
        @DisplayName("should clear bot messages")
        void shouldClearMessages() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onClearMessages(sender, "Alice");

            verify(mockBot).clearMessages();
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_messages_cleared");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onClearMessages(sender, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("op/deop")
    class OpManagement {

        @Test
        @DisplayName("should set bot as OP")
        void shouldSetOp() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onOp(sender, "Alice");

            verify(mockBot).setOp(true);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_op_set");
        }

        @Test
        @DisplayName("should remove bot OP")
        void shouldRemoveOp() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onDeop(sender, "Alice");

            verify(mockBot).setOp(false);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_op_removed");
        }

        @Test
        @DisplayName("should send error for unknown bot on op")
        void shouldSendErrorOnOp() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onOp(sender, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }

        @Test
        @DisplayName("should send error for unknown bot on deop")
        void shouldSendErrorOnDeop() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onDeop(sender, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("click")
    class Click {

        @Test
        @DisplayName("should click slot on bot")
        void shouldClickSlot() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            InventoryView mockView = mock(InventoryView.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getOpenInventoryView()).thenReturn(mockView);

            commands.onClick(sender, "Alice", "3");

            verify(mockBot).clickSlot(3);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_click_success");
        }

        @Test
        @DisplayName("should send error when no inventory open")
        void shouldSendErrorNoInventory() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getOpenInventoryView()).thenReturn(null);

            commands.onClick(sender, "Alice", "0");

            verify(mockBot, never()).clickSlot(anyInt());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_no_inventory");
        }

        @Test
        @DisplayName("should send error for invalid slot number")
        void shouldSendErrorForInvalidSlot() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onClick(sender, "Alice", "abc");

            verify(mockBot, never()).clickSlot(anyInt());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_click_invalid_slot");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onClick(sender, "Ghost", "0");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("closeinv")
    class CloseInventory {

        @Test
        @DisplayName("should close bot inventory")
        void shouldCloseInventory() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onCloseInventory(sender, "Alice");

            verify(mockBot).closeInventory();
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_inv_closed");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onCloseInventory(sender, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("inv")
    class InventoryView_ {

        @Test
        @DisplayName("should show inventory contents via reflective lookup")
        void shouldShowInventoryContents() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            InventoryView mockView = mock(InventoryView.class);
            Inventory mockInv = mock(Inventory.class);
            ItemStack mockItem = mock(ItemStack.class);
            ItemMeta mockMeta = mock(ItemMeta.class);

            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getOpenInventoryView()).thenReturn(mockView);
            when(mockView.getTopInventory()).thenReturn(mockInv);
            when(mockView.getTitle()).thenReturn("Test GUI");
            when(mockInv.getSize()).thenReturn(9);
            when(mockInv.getItem(0)).thenReturn(mockItem);
            when(mockItem.getType()).thenReturn(Material.DIAMOND);
            when(mockItem.getAmount()).thenReturn(5);
            when(mockItem.hasItemMeta()).thenReturn(true);
            when(mockItem.getItemMeta()).thenReturn(mockMeta);
            when(mockMeta.hasDisplayName()).thenReturn(true);
            when(mockMeta.getDisplayName()).thenReturn("Shiny Diamond");
            for (int i = 1; i < 9; i++) {
                when(mockInv.getItem(i)).thenReturn(null);
            }

            commands.onInventory(sender, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender, atLeast(2)).sendMessage(captor.capture());
            List<String> msgs = captor.getAllValues();
            assertThat(msgs.get(0)).contains("bot_inv_header");
            assertThat(msgs.get(1)).contains("[0]").contains("Shiny Diamond").contains("x5");
        }

        @Test
        @DisplayName("should show a display name fallback to material name when meta has no display name")
        void shouldFallBackToMaterialName() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            InventoryView mockView = mock(InventoryView.class);
            Inventory mockInv = mock(Inventory.class);
            ItemStack mockItem = mock(ItemStack.class);

            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getOpenInventoryView()).thenReturn(mockView);
            when(mockView.getTopInventory()).thenReturn(mockInv);
            when(mockView.getTitle()).thenReturn("Test GUI");
            when(mockInv.getSize()).thenReturn(1);
            when(mockInv.getItem(0)).thenReturn(mockItem);
            when(mockItem.getType()).thenReturn(Material.DIAMOND);
            when(mockItem.getAmount()).thenReturn(1);
            when(mockItem.hasItemMeta()).thenReturn(false);

            commands.onInventory(sender, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender, atLeast(2)).sendMessage(captor.capture());
            assertThat(captor.getAllValues().get(1)).contains("DIAMOND");
        }

        @Test
        @DisplayName("should show empty inventory")
        void shouldShowEmptyInventory() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            InventoryView mockView = mock(InventoryView.class);
            Inventory mockInv = mock(Inventory.class);

            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getOpenInventoryView()).thenReturn(mockView);
            when(mockView.getTopInventory()).thenReturn(mockInv);
            when(mockView.getTitle()).thenReturn("Empty GUI");
            when(mockInv.getSize()).thenReturn(9);
            for (int i = 0; i < 9; i++) {
                when(mockInv.getItem(i)).thenReturn(null);
            }

            commands.onInventory(sender, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender, atLeast(2)).sendMessage(captor.capture());
            List<String> msgs = captor.getAllValues();
            assertThat(msgs.get(0)).contains("bot_inv_header");
            assertThat(msgs.get(1)).contains("bot_inv_empty");
        }

        @Test
        @DisplayName("should send error when no inventory open")
        void shouldSendErrorNoInventory() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getOpenInventoryView()).thenReturn(null);

            commands.onInventory(sender, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_no_inventory");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onInventory(sender, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }

        @Test
        @DisplayName("should report an error message when inventory inspection throws")
        void shouldReportErrorWhenReflectionFails() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            InventoryView mockView = mock(InventoryView.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getOpenInventoryView()).thenReturn(mockView);
            when(mockView.getTopInventory()).thenThrow(new RuntimeException("boom"));

            commands.onInventory(sender, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("Error inspecting inventory:");
        }
    }

    @Nested
    @DisplayName("macro")
    class Macro {

        @Test
        @DisplayName("should start recording macro")
        void shouldStartRecording() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(macroService.startRecording(mockBot, "test_macro")).thenReturn(true);

            commands.onMacroRecord(sender, "Alice", "test_macro");

            verify(macroService).startRecording(mockBot, "test_macro");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_recording");
        }

        @Test
        @DisplayName("should show already-recording message when start fails")
        void shouldShowAlreadyRecording() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(macroService.startRecording(mockBot, "test_macro")).thenReturn(false);

            commands.onMacroRecord(sender, "Alice", "test_macro");

            verify(macroService).startRecording(mockBot, "test_macro");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_recording");
        }

        @Test
        @DisplayName("should send error for unknown bot on record")
        void shouldSendErrorForUnknownBotOnRecord() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onMacroRecord(sender, "Ghost", "test_macro");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }

        @Test
        @DisplayName("should stop recording macro")
        void shouldStopRecording() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(macroService.stopRecording(mockBot)).thenReturn(Collections.emptyList());

            commands.onMacroStop(sender, "Alice");

            verify(macroService).stopRecording(mockBot);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_stopped");
        }

        @Test
        @DisplayName("should send error for unknown bot on stop")
        void shouldSendErrorForUnknownBotOnStop() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onMacroStop(sender, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }

        @Test
        @DisplayName("should play saved macro")
        void shouldPlayMacro() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(macroService.getMacro("test")).thenReturn(
                    Collections.singletonList(new MacroEntry(ActionType.JUMP, 5L)));

            commands.onMacroPlay(sender, "Alice", "test");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_playing");
        }

        @Test
        @DisplayName("should send error for unknown macro")
        void shouldSendErrorForUnknownMacro() {
            CommandSender sender = mock(CommandSender.class);
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(macroService.getMacro("ghost")).thenReturn(null);

            commands.onMacroPlay(sender, "Alice", "ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_not_found");
        }

        @Test
        @DisplayName("should send error for unknown bot on play")
        void shouldSendErrorForUnknownBotOnPlay() {
            CommandSender sender = mock(CommandSender.class);
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onMacroPlay(sender, "Ghost", "test");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }

        @Test
        @DisplayName("should list saved macros")
        void shouldListMacros() {
            CommandSender sender = mock(CommandSender.class);
            Set<String> names = new LinkedHashSet<>(Arrays.asList("macro1", "macro2"));
            when(macroService.listMacros()).thenReturn(names);

            commands.onMacroList(sender);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender, atLeast(2)).sendMessage(captor.capture());
            assertThat(captor.getAllValues().get(0)).contains("bot_macro_list_header");
        }

        @Test
        @DisplayName("should show empty message when no macros saved")
        void shouldShowEmptyMacros() {
            CommandSender sender = mock(CommandSender.class);
            when(macroService.listMacros()).thenReturn(Collections.emptySet());

            commands.onMacroList(sender);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_list_empty");
        }
    }

    @Nested
    @DisplayName("help")
    class Help {

        @Test
        @DisplayName("should print the full command usage listing")
        void shouldPrintUsageListing() {
            CommandSender sender = mock(CommandSender.class);

            commands.handleHelp(sender);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(sender, atLeast(10)).sendMessage(captor.capture());
            assertThat(captor.getAllValues().get(0)).contains("UltiBot Commands");
        }
    }
}
