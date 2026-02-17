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
