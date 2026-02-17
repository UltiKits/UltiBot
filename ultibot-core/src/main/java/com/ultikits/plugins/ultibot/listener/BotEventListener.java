package com.ultikits.plugins.ultibot.listener;

import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.config.BotConfig;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class BotEventListener implements Listener {

    private final BotManagerImpl botManager;

    public BotEventListener(BotManagerImpl botManager) {
        this.botManager = botManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Ignore quit events from bots themselves
        if (botManager.isBot(uuid)) {
            return;
        }

        BotConfig config = botManager.getConfig();
        if (config.isAutoRemoveOnQuit()) {
            botManager.removeBotsForOwner(uuid);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        BotPlayer bot = botManager.getBot(uuid);
        if (bot == null) {
            return; // not a bot
        }

        BotConfig config = botManager.getConfig();
        if (config.isAutoRespawn()) {
            bot.respawn();
        }
    }
}
