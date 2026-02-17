package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.*;
import com.ultikits.plugins.ultibot.config.BotConfig;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BotManagerImpl {

    private final NMSBridge nmsBridge;
    @Getter
    private final BotConfig config;
    private final UltiToolsPlugin plugin;

    // name -> BotPlayer
    private final Map<String, BotPlayer> botsByName = new ConcurrentHashMap<>();
    // uuid -> BotPlayer
    private final Map<UUID, BotPlayer> botsByUuid = new ConcurrentHashMap<>();
    // owner uuid -> set of bot names
    private final Map<UUID, Set<String>> ownerBots = new ConcurrentHashMap<>();

    public BotManagerImpl(NMSBridge nmsBridge, BotConfig config, UltiToolsPlugin plugin) {
        this.nmsBridge = nmsBridge;
        this.config = config;
        this.plugin = plugin;
    }

    public BotPlayer spawnBot(String name, Location location, Player owner) {
        if (botsByName.containsKey(name)) {
            return null;
        }
        UUID ownerUuid = owner.getUniqueId();
        Set<String> owned = ownerBots.computeIfAbsent(ownerUuid, k -> new HashSet<>());
        if (owned.size() >= config.getMaxBotsPerPlayer()) {
            return null;
        }
        if (botsByName.size() >= config.getMaxTotalBots()) {
            return null;
        }

        UUID botUuid = UUID.randomUUID();
        BotPlayer bot = nmsBridge.createBot(name, botUuid, location);
        if (bot == null) {
            return null;
        }

        botsByName.put(name, bot);
        botsByUuid.put(bot.getUUID(), bot);
        owned.add(name);
        bot.join();
        return bot;
    }

    public void removeBot(String name) {
        BotPlayer bot = botsByName.remove(name);
        if (bot != null) {
            botsByUuid.remove(bot.getUUID());
            removeFromOwnerMap(name);
            bot.disconnect("Removed");
        }
    }

    public void removeBot(UUID uuid) {
        BotPlayer bot = botsByUuid.remove(uuid);
        if (bot != null) {
            botsByName.remove(bot.getName());
            removeFromOwnerMap(bot.getName());
            bot.disconnect("Removed");
        }
    }

    public void removeAllBots() {
        for (BotPlayer bot : new ArrayList<>(botsByName.values())) {
            bot.disconnect("Server cleanup");
        }
        botsByName.clear();
        botsByUuid.clear();
        ownerBots.clear();
    }

    public void removeBotsForOwner(UUID ownerUuid) {
        Set<String> owned = ownerBots.remove(ownerUuid);
        if (owned != null) {
            for (String name : new ArrayList<>(owned)) {
                BotPlayer bot = botsByName.remove(name);
                if (bot != null) {
                    botsByUuid.remove(bot.getUUID());
                    bot.disconnect("Owner disconnected");
                }
            }
        }
    }

    public BotPlayer getBot(String name) {
        return botsByName.get(name);
    }

    public BotPlayer getBot(UUID uuid) {
        return botsByUuid.get(uuid);
    }

    public Collection<BotPlayer> getAllBots() {
        return Collections.unmodifiableCollection(botsByName.values());
    }

    public boolean isBot(Player player) {
        return isBot(player.getUniqueId());
    }

    public boolean isBot(UUID uuid) {
        return botsByUuid.containsKey(uuid);
    }

    public UUID getOwnerOf(String botName) {
        for (Map.Entry<UUID, Set<String>> entry : ownerBots.entrySet()) {
            if (entry.getValue().contains(botName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public int getBotCountForOwner(UUID ownerUuid) {
        Set<String> owned = ownerBots.get(ownerUuid);
        return owned == null ? 0 : owned.size();
    }

    private void removeFromOwnerMap(String botName) {
        for (Set<String> owned : ownerBots.values()) {
            owned.remove(botName);
        }
    }
}
