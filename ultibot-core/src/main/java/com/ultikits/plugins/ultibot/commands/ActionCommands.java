package com.ultikits.plugins.ultibot.commands;

import com.ultikits.plugins.ultibot.api.ActionType;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.service.ActionServiceImpl;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import com.ultikits.ultitools.abstracts.AbstractCommandExecutor;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.command.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CmdExecutor(
        permission = "ultibot.action",
        description = "Bot action commands",
        alias = {"bot"}
)
@CmdTarget(CmdTarget.CmdTargetType.BOTH)
public class ActionCommands extends AbstractCommandExecutor {

    private final UltiToolsPlugin plugin;
    private final BotManagerImpl botManager;
    private final ActionServiceImpl actionService;

    public ActionCommands(UltiToolsPlugin plugin, BotManagerImpl botManager,
                          ActionServiceImpl actionService) {
        this.plugin = plugin;
        this.botManager = botManager;
        this.actionService = actionService;
    }

    @CmdMapping(format = "action <name> <action> <interval>")
    public void onAction(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName,
            @CmdParam("action") String actionName,
            @CmdParam("interval") int interval
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        ActionType actionType;
        try {
            actionType = ActionType.valueOf(actionName.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.i18n("bot_action_invalid").replace("{0}", actionName));
            return;
        }

        actionService.startRepeatingAction(bot, actionType, interval);
        sender.sendMessage(plugin.i18n("bot_action_started")
                .replace("{0}", botName)
                .replace("{1}", actionType.name()));
    }

    @CmdMapping(format = "stop <name>")
    public void onStop(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        actionService.stopAllActions(bot);
        sender.sendMessage(plugin.i18n("bot_action_stopped").replace("{0}", botName));
    }

    @Override
    protected void handleHelp(CommandSender sender) {
        sender.sendMessage(plugin.i18n("usage_action"));
    }
}
