package com.ultikits.plugins.ultibot.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UltiBotAPI {
    // Create & manage
    BotPlayer spawnBot(String name, Location location);
    BotPlayer spawnBot(String name, Location location, String skinOwner);
    void removeBot(String name);
    void removeBot(UUID uuid);
    void removeAllBots();

    // Query
    BotPlayer getBot(String name);
    BotPlayer getBot(UUID uuid);
    Collection<BotPlayer> getAllBots();
    boolean isBot(Player player);
    boolean isBot(UUID uuid);

    // Actions
    void performAction(BotPlayer bot, ActionType action);
    ActionTicker startRepeatingAction(BotPlayer bot, ActionType action, int intervalTicks);
    void stopAllActions(BotPlayer bot);

    // Macros
    void startRecording(BotPlayer bot, String macroName);
    void stopRecording(BotPlayer bot);
    void playMacro(BotPlayer bot, String macroName);
    List<String> listMacros();
}
