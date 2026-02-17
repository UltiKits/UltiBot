package com.ultikits.plugins.ultibot.commands;

import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import com.ultikits.ultitools.abstracts.AbstractCommandExecutor;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.command.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

@CmdExecutor(
        permission = "ultibot.use",
        description = "Bot management commands",
        alias = {"bot"}
)
@CmdTarget(CmdTarget.CmdTargetType.BOTH)
public class BotCommands extends AbstractCommandExecutor {

    private final UltiToolsPlugin plugin;
    private final BotManagerImpl botManager;

    public BotCommands(UltiToolsPlugin plugin, BotManagerImpl botManager) {
        this.plugin = plugin;
        this.botManager = botManager;
    }

    @CmdMapping(format = "spawn <name>")
    @CmdTarget(CmdTarget.CmdTargetType.PLAYER)
    public void onSpawn(
            @CmdSender Player player,
            @CmdParam("name") String name
    ) {
        BotPlayer existing = botManager.getBot(name);
        if (existing != null) {
            player.sendMessage(plugin.i18n("bot_name_taken").replace("{0}", name));
            return;
        }

        BotPlayer bot = botManager.spawnBot(name, player.getLocation(), player);
        if (bot == null) {
            player.sendMessage(plugin.i18n("bot_limit_player")
                    .replace("{0}", String.valueOf(botManager.getConfig().getMaxBotsPerPlayer())));
            return;
        }

        player.sendMessage(plugin.i18n("bot_spawned").replace("{0}", name));
    }

    @CmdMapping(format = "remove <name>")
    public void onRemove(
            @CmdSender CommandSender sender,
            @CmdParam("name") String name
    ) {
        if ("all".equalsIgnoreCase(name)) {
            botManager.removeAllBots();
            sender.sendMessage(plugin.i18n("bot_removed_all"));
            return;
        }

        BotPlayer bot = botManager.getBot(name);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", name));
            return;
        }

        botManager.removeBot(name);
        sender.sendMessage(plugin.i18n("bot_removed").replace("{0}", name));
    }

    @CmdMapping(format = "list")
    public void onList(@CmdSender CommandSender sender) {
        Collection<BotPlayer> bots = botManager.getAllBots();
        if (bots.isEmpty()) {
            sender.sendMessage(plugin.i18n("bot_list_empty"));
            return;
        }

        sender.sendMessage(plugin.i18n("bot_list_header"));
        for (BotPlayer bot : bots) {
            UUID owner = botManager.getOwnerOf(bot.getName());
            String ownerName = owner != null ? owner.toString().substring(0, 8) : "unknown";
            sender.sendMessage(plugin.i18n("bot_list_entry")
                    .replace("{0}", bot.getName())
                    .replace("{1}", ownerName));
        }
    }

    @CmdMapping(format = "tp <name>")
    @CmdTarget(CmdTarget.CmdTargetType.PLAYER)
    public void onTeleport(
            @CmdSender Player player,
            @CmdParam("name") String name
    ) {
        BotPlayer bot = botManager.getBot(name);
        if (bot == null) {
            player.sendMessage(plugin.i18n("bot_not_found").replace("{0}", name));
            return;
        }

        bot.moveTo(player.getLocation());
        player.sendMessage(plugin.i18n("bot_teleported").replace("{0}", name));
    }

    @CmdMapping(format = "reload")
    public void onReload(@CmdSender CommandSender sender) {
        if (!sender.hasPermission("ultibot.admin")) {
            sender.sendMessage(plugin.i18n("no_permission"));
            return;
        }
        sender.sendMessage(plugin.i18n("reload_success"));
    }

    @Override
    protected void handleHelp(CommandSender sender) {
        sender.sendMessage(plugin.i18n("usage_spawn"));
        sender.sendMessage(plugin.i18n("usage_remove"));
        sender.sendMessage(plugin.i18n("usage_action"));
        sender.sendMessage(plugin.i18n("usage_cmd"));
        sender.sendMessage(plugin.i18n("usage_chat"));
    }
}
