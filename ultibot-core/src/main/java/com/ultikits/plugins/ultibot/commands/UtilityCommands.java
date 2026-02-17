package com.ultikits.plugins.ultibot.commands;

import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.model.MacroEntry;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import com.ultikits.plugins.ultibot.service.MacroServiceImpl;
import com.ultikits.plugins.ultibot.service.SkinService;
import com.ultikits.ultitools.abstracts.AbstractCommandExecutor;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.command.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

@CmdExecutor(
        permission = "ultibot.use",
        description = "Bot utility commands",
        alias = {"bot"}
)
@CmdTarget(CmdTarget.CmdTargetType.BOTH)
public class UtilityCommands extends AbstractCommandExecutor {

    private final UltiToolsPlugin plugin;
    private final BotManagerImpl botManager;
    private final MacroServiceImpl macroService;
    private final SkinService skinService;

    public UtilityCommands(UltiToolsPlugin plugin, BotManagerImpl botManager,
                           MacroServiceImpl macroService, SkinService skinService) {
        this.plugin = plugin;
        this.botManager = botManager;
        this.macroService = macroService;
        this.skinService = skinService;
    }

    @CmdMapping(format = "chat <name> <message>")
    public void onChat(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName,
            @CmdParam("message") String message
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        bot.chat(message);
        sender.sendMessage(plugin.i18n("bot_chat_sent").replace("{0}", botName));
    }

    @CmdMapping(format = "cmd <name> <command>")
    public void onCmd(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName,
            @CmdParam("command") String command
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        bot.performCommand(command);
        sender.sendMessage(plugin.i18n("bot_cmd_executed").replace("{0}", botName));
    }

    @CmdMapping(format = "skin <name> <skinName>")
    public void onSkin(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName,
            @CmdParam("skinName") String skinName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        SkinService.SkinData skin = skinService.fetchSkin(skinName, null);
        if (skin == null) {
            sender.sendMessage(plugin.i18n("bot_skin_failed").replace("{0}", skinName));
            return;
        }

        sender.sendMessage(plugin.i18n("bot_skin_changed")
                .replace("{0}", botName)
                .replace("{1}", skinName));
    }

    @CmdMapping(format = "macro record <name> <macroName>")
    public void onMacroRecord(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName,
            @CmdParam("macroName") String macroName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        boolean started = macroService.startRecording(bot, macroName);
        if (!started) {
            sender.sendMessage(plugin.i18n("bot_macro_recording")
                    .replace("{0}", botName).replace("{1}", "already recording"));
            return;
        }

        sender.sendMessage(plugin.i18n("bot_macro_recording")
                .replace("{0}", botName)
                .replace("{1}", macroName));
    }

    @CmdMapping(format = "macro stop <name>")
    public void onMacroStop(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        macroService.stopRecording(bot);
        sender.sendMessage(plugin.i18n("bot_macro_stopped"));
    }

    @CmdMapping(format = "macro play <name> <macroName>")
    public void onMacroPlay(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName,
            @CmdParam("macroName") String macroName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        List<MacroEntry> entries = macroService.getMacro(macroName);
        if (entries == null) {
            sender.sendMessage(plugin.i18n("bot_macro_not_found").replace("{0}", macroName));
            return;
        }

        sender.sendMessage(plugin.i18n("bot_macro_playing").replace("{0}", macroName));
    }

    @CmdMapping(format = "macro list")
    public void onMacroList(@CmdSender CommandSender sender) {
        Set<String> macros = macroService.listMacros();
        if (macros.isEmpty()) {
            sender.sendMessage(plugin.i18n("bot_macro_list_empty"));
            return;
        }

        sender.sendMessage(plugin.i18n("bot_macro_list_header"));
        for (String name : macros) {
            sender.sendMessage("  - " + name);
        }
    }

    @Override
    protected void handleHelp(CommandSender sender) {
        sender.sendMessage(plugin.i18n("usage_chat"));
        sender.sendMessage(plugin.i18n("usage_cmd"));
    }
}
