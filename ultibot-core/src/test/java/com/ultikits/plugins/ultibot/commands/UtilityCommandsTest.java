package com.ultikits.plugins.ultibot.commands;

import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.model.MacroEntry;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import com.ultikits.plugins.ultibot.service.MacroServiceImpl;
import com.ultikits.plugins.ultibot.service.SkinService;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UtilityCommands")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UtilityCommandsTest {

    @Mock private UltiToolsPlugin plugin;
    @Mock private BotManagerImpl botManager;
    @Mock private MacroServiceImpl macroService;
    @Mock private SkinService skinService;
    @Mock private Player player;

    private UtilityCommands commands;

    @BeforeEach
    void setUp() {
        when(plugin.i18n(anyString())).thenAnswer(inv -> inv.getArgument(0));
        commands = new UtilityCommands(plugin, botManager, macroService, skinService);
    }

    @Nested
    @DisplayName("chat")
    class Chat {

        @Test
        @DisplayName("should make bot send chat message")
        void shouldSendChatMessage() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onChat(player, "Alice", "Hello world");

            verify(mockBot).chat("Hello world");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_chat_sent");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onChat(player, "Ghost", "Hello");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("cmd")
    class Cmd {

        @Test
        @DisplayName("should make bot execute command")
        void shouldExecuteCommand() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onCmd(player, "Alice", "say hello");

            verify(mockBot).performCommand("say hello");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_cmd_executed");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onCmd(player, "Ghost", "say hi");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("skin")
    class Skin {

        @Test
        @DisplayName("should change bot skin")
        void shouldChangeSkin() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(skinService.fetchSkin(eq("Notch"), any()))
                    .thenReturn(new SkinService.SkinData("val", "sig"));

            commands.onSkin(player, "Alice", "Notch");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_skin_changed");
        }

        @Test
        @DisplayName("should send error when skin fetch fails")
        void shouldSendErrorWhenFetchFails() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(skinService.fetchSkin(eq("InvalidPlayer"), any())).thenReturn(null);

            commands.onSkin(player, "Alice", "InvalidPlayer");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_skin_failed");
        }
    }

    @Nested
    @DisplayName("messages")
    class Messages {

        @Test
        @DisplayName("should show captured messages")
        void shouldShowCapturedMessages() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getReceivedMessages()).thenReturn(Arrays.asList("Hello", "World"));

            commands.onMessages(player, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player, atLeast(3)).sendMessage(captor.capture());
            List<String> msgs = captor.getAllValues();
            assertThat(msgs.get(0)).contains("bot_messages_header");
            assertThat(msgs.get(1)).contains("[0] Hello");
            assertThat(msgs.get(2)).contains("[1] World");
        }

        @Test
        @DisplayName("should show empty message when no captures")
        void shouldShowEmptyMessages() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getReceivedMessages()).thenReturn(Collections.emptyList());

            commands.onMessages(player, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_messages_empty");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onMessages(player, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("clearmsg")
    class ClearMessages {

        @Test
        @DisplayName("should clear bot messages")
        void shouldClearMessages() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onClearMessages(player, "Alice");

            verify(mockBot).clearMessages();
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_messages_cleared");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onClearMessages(player, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("op/deop")
    class OpManagement {

        @Test
        @DisplayName("should set bot as OP")
        void shouldSetOp() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onOp(player, "Alice");

            verify(mockBot).setOp(true);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_op_set");
        }

        @Test
        @DisplayName("should remove bot OP")
        void shouldRemoveOp() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onDeop(player, "Alice");

            verify(mockBot).setOp(false);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_op_removed");
        }

        @Test
        @DisplayName("should send error for unknown bot on op")
        void shouldSendErrorOnOp() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onOp(player, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }

        @Test
        @DisplayName("should send error for unknown bot on deop")
        void shouldSendErrorOnDeop() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onDeop(player, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("click")
    class Click {

        @Test
        @DisplayName("should click slot on bot")
        void shouldClickSlot() {
            BotPlayer mockBot = mock(BotPlayer.class);
            InventoryView mockView = mock(InventoryView.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getOpenInventoryView()).thenReturn(mockView);

            commands.onClick(player, "Alice", "3");

            verify(mockBot).clickSlot(3);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_click_success");
        }

        @Test
        @DisplayName("should send error when no inventory open")
        void shouldSendErrorNoInventory() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getOpenInventoryView()).thenReturn(null);

            commands.onClick(player, "Alice", "0");

            verify(mockBot, never()).clickSlot(anyInt());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_no_inventory");
        }

        @Test
        @DisplayName("should send error for invalid slot number")
        void shouldSendErrorForInvalidSlot() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onClick(player, "Alice", "abc");

            verify(mockBot, never()).clickSlot(anyInt());
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_click_invalid_slot");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onClick(player, "Ghost", "0");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("closeinv")
    class CloseInventory {

        @Test
        @DisplayName("should close bot inventory")
        void shouldCloseInventory() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);

            commands.onCloseInventory(player, "Alice");

            verify(mockBot).closeInventory();
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_inv_closed");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onCloseInventory(player, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("inv")
    class InventoryView_ {

        @Test
        @DisplayName("should show inventory contents")
        void shouldShowInventoryContents() {
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
            // slots 1-8 return null
            for (int i = 1; i < 9; i++) {
                when(mockInv.getItem(i)).thenReturn(null);
            }

            commands.onInventory(player, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player, atLeast(2)).sendMessage(captor.capture());
            List<String> msgs = captor.getAllValues();
            assertThat(msgs.get(0)).contains("bot_inv_header");
            assertThat(msgs.get(1)).contains("[0]").contains("Shiny Diamond").contains("x5");
        }

        @Test
        @DisplayName("should show empty inventory")
        void shouldShowEmptyInventory() {
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

            commands.onInventory(player, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player, atLeast(2)).sendMessage(captor.capture());
            List<String> msgs = captor.getAllValues();
            assertThat(msgs.get(0)).contains("bot_inv_header");
            assertThat(msgs.get(1)).contains("bot_inv_empty");
        }

        @Test
        @DisplayName("should send error when no inventory open")
        void shouldSendErrorNoInventory() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(mockBot.getOpenInventoryView()).thenReturn(null);

            commands.onInventory(player, "Alice");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_no_inventory");
        }

        @Test
        @DisplayName("should send error for unknown bot")
        void shouldSendErrorForUnknownBot() {
            when(botManager.getBot("Ghost")).thenReturn(null);

            commands.onInventory(player, "Ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_not_found");
        }
    }

    @Nested
    @DisplayName("macro")
    class Macro {

        @Test
        @DisplayName("should start recording macro")
        void shouldStartRecording() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(macroService.startRecording(mockBot, "test_macro")).thenReturn(true);

            commands.onMacroRecord(player, "Alice", "test_macro");

            verify(macroService).startRecording(mockBot, "test_macro");
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_recording");
        }

        @Test
        @DisplayName("should stop recording macro")
        void shouldStopRecording() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(macroService.stopRecording(mockBot)).thenReturn(Collections.emptyList());

            commands.onMacroStop(player, "Alice");

            verify(macroService).stopRecording(mockBot);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_stopped");
        }

        @Test
        @DisplayName("should list saved macros")
        void shouldListMacros() {
            Set<String> names = new LinkedHashSet<>(Arrays.asList("macro1", "macro2"));
            when(macroService.listMacros()).thenReturn(names);

            commands.onMacroList(player);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player, atLeast(2)).sendMessage(captor.capture());
            assertThat(captor.getAllValues().get(0)).contains("bot_macro_list_header");
        }

        @Test
        @DisplayName("should show empty message when no macros")
        void shouldShowEmptyMacros() {
            when(macroService.listMacros()).thenReturn(Collections.emptySet());

            commands.onMacroList(player);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_list_empty");
        }

        @Test
        @DisplayName("should play saved macro")
        void shouldPlayMacro() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(macroService.getMacro("test")).thenReturn(Collections.emptyList());

            commands.onMacroPlay(player, "Alice", "test");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_playing");
        }

        @Test
        @DisplayName("should send error for unknown macro")
        void shouldSendErrorForUnknownMacro() {
            BotPlayer mockBot = mock(BotPlayer.class);
            when(botManager.getBot("Alice")).thenReturn(mockBot);
            when(macroService.getMacro("ghost")).thenReturn(null);

            commands.onMacroPlay(player, "Alice", "ghost");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(player).sendMessage(captor.capture());
            assertThat(captor.getValue()).contains("bot_macro_not_found");
        }
    }
}
