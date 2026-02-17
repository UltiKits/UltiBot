package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.ActionType;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.model.MacroEntry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("MacroServiceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MacroServiceImplTest {

    @Mock
    private BotPlayer bot;

    private MacroServiceImpl macroService;

    @BeforeEach
    void setUp() {
        when(bot.getUUID()).thenReturn(UUID.randomUUID());
        macroService = new MacroServiceImpl();
    }

    @Nested
    @DisplayName("recording")
    class Recording {

        @Test
        @DisplayName("should start recording")
        void shouldStartRecording() {
            boolean started = macroService.startRecording(bot, "test_macro");
            assertThat(started).isTrue();
            assertThat(macroService.isRecording(bot)).isTrue();
        }

        @Test
        @DisplayName("should reject second recording on same bot")
        void shouldRejectSecondRecording() {
            macroService.startRecording(bot, "macro1");
            boolean second = macroService.startRecording(bot, "macro2");
            assertThat(second).isFalse();
        }

        @Test
        @DisplayName("should record actions with delays")
        void shouldRecordActionsWithDelays() {
            macroService.startRecording(bot, "test_macro");
            macroService.recordAction(bot, ActionType.JUMP, 0);
            macroService.recordAction(bot, ActionType.SNEAK, 20);
            macroService.recordAction(bot, ActionType.SPRINT, 40);
            List<MacroEntry> entries = macroService.stopRecording(bot);

            assertThat(entries).hasSize(3);
            assertThat(entries.get(0).getActionType()).isEqualTo(ActionType.JUMP);
            assertThat(entries.get(0).getDelayTicks()).isEqualTo(0);
            assertThat(entries.get(1).getActionType()).isEqualTo(ActionType.SNEAK);
            assertThat(entries.get(1).getDelayTicks()).isEqualTo(20);
            assertThat(entries.get(2).getActionType()).isEqualTo(ActionType.SPRINT);
            assertThat(entries.get(2).getDelayTicks()).isEqualTo(40);
        }

        @Test
        @DisplayName("should stop recording and save macro")
        void shouldStopRecordingAndSave() {
            macroService.startRecording(bot, "my_macro");
            macroService.recordAction(bot, ActionType.JUMP, 0);
            macroService.stopRecording(bot);

            assertThat(macroService.isRecording(bot)).isFalse();
            assertThat(macroService.listMacros()).contains("my_macro");
        }

        @Test
        @DisplayName("should return null when stopping without recording")
        void shouldReturnNullWhenNotRecording() {
            List<MacroEntry> result = macroService.stopRecording(bot);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should not record action when not recording")
        void shouldNotRecordWhenNotRecording() {
            // Should not throw
            macroService.recordAction(bot, ActionType.JUMP, 0);
            assertThat(macroService.listMacros()).isEmpty();
        }
    }

    @Nested
    @DisplayName("playback")
    class Playback {

        @Test
        @DisplayName("should retrieve saved macro entries")
        void shouldRetrieveSavedMacro() {
            macroService.startRecording(bot, "replay_test");
            macroService.recordAction(bot, ActionType.ATTACK, 0);
            macroService.recordAction(bot, ActionType.USE, 10);
            macroService.stopRecording(bot);

            List<MacroEntry> entries = macroService.getMacro("replay_test");
            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).getActionType()).isEqualTo(ActionType.ATTACK);
            assertThat(entries.get(1).getActionType()).isEqualTo(ActionType.USE);
        }

        @Test
        @DisplayName("should return null for unknown macro")
        void shouldReturnNullForUnknown() {
            assertThat(macroService.getMacro("nonexistent")).isNull();
        }
    }

    @Nested
    @DisplayName("listing")
    class Listing {

        @Test
        @DisplayName("should list all saved macro names")
        void shouldListAllMacros() {
            macroService.startRecording(bot, "m1");
            macroService.recordAction(bot, ActionType.JUMP, 0);
            macroService.stopRecording(bot);

            // Need a new recording session for the same bot
            macroService.startRecording(bot, "m2");
            macroService.recordAction(bot, ActionType.SNEAK, 0);
            macroService.stopRecording(bot);

            assertThat(macroService.listMacros()).containsExactlyInAnyOrder("m1", "m2");
        }

        @Test
        @DisplayName("should return empty set when no macros saved")
        void shouldReturnEmptyWhenNone() {
            assertThat(macroService.listMacros()).isEmpty();
        }
    }

    @Nested
    @DisplayName("deletion")
    class Deletion {

        @Test
        @DisplayName("should delete a saved macro")
        void shouldDeleteMacro() {
            macroService.startRecording(bot, "to_delete");
            macroService.recordAction(bot, ActionType.JUMP, 0);
            macroService.stopRecording(bot);

            boolean deleted = macroService.deleteMacro("to_delete");
            assertThat(deleted).isTrue();
            assertThat(macroService.getMacro("to_delete")).isNull();
            assertThat(macroService.listMacros()).doesNotContain("to_delete");
        }

        @Test
        @DisplayName("should return false when deleting nonexistent macro")
        void shouldReturnFalseForNonexistent() {
            assertThat(macroService.deleteMacro("ghost")).isFalse();
        }
    }
}
