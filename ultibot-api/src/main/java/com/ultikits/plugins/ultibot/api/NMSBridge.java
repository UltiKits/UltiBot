package com.ultikits.plugins.ultibot.api;

import org.bukkit.Location;
import java.util.UUID;

public interface NMSBridge {
    boolean isSupported();
    BotPlayer createBot(String name, UUID uuid, Location spawnLocation);
}
