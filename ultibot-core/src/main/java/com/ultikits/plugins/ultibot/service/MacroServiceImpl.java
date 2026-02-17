package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.ActionType;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.model.MacroEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MacroServiceImpl {

    // macroName -> list of entries
    private final Map<String, List<MacroEntry>> savedMacros = new ConcurrentHashMap<>();

    // bot UUID -> recording session
    private final Map<UUID, RecordingSession> activeRecordings = new ConcurrentHashMap<>();

    private static class RecordingSession {
        final String macroName;
        final List<MacroEntry> entries = new ArrayList<>();

        RecordingSession(String macroName) {
            this.macroName = macroName;
        }
    }

    public boolean startRecording(BotPlayer bot, String macroName) {
        UUID botUuid = bot.getUUID();
        if (activeRecordings.containsKey(botUuid)) {
            return false; // already recording
        }
        activeRecordings.put(botUuid, new RecordingSession(macroName));
        return true;
    }

    public void recordAction(BotPlayer bot, ActionType actionType, long delayTicks) {
        RecordingSession session = activeRecordings.get(bot.getUUID());
        if (session == null) {
            return; // not recording
        }
        session.entries.add(new MacroEntry(actionType, delayTicks));
    }

    public List<MacroEntry> stopRecording(BotPlayer bot) {
        RecordingSession session = activeRecordings.remove(bot.getUUID());
        if (session == null) {
            return null;
        }
        List<MacroEntry> entries = Collections.unmodifiableList(session.entries);
        savedMacros.put(session.macroName, entries);
        return entries;
    }

    public boolean isRecording(BotPlayer bot) {
        return activeRecordings.containsKey(bot.getUUID());
    }

    public List<MacroEntry> getMacro(String macroName) {
        return savedMacros.get(macroName);
    }

    public Set<String> listMacros() {
        return Collections.unmodifiableSet(savedMacros.keySet());
    }

    public boolean deleteMacro(String macroName) {
        return savedMacros.remove(macroName) != null;
    }
}
