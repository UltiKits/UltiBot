package com.ultikits.plugins.ultibot.commands;

import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.model.MacroEntry;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import com.ultikits.plugins.ultibot.service.MacroServiceImpl;
import com.ultikits.plugins.ultibot.service.SkinService;
import com.ultikits.ultitools.abstracts.AbstractCommandExecutor;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.command.*;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

@CmdExecutor(
        permission = "ultibot.use",
        description = "Bot utility commands",
        alias = {"bot"},
        manualRegister = true
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

    // --- Testing support commands ---

    @CmdMapping(format = "messages <name>")
    public void onMessages(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        List<String> messages = bot.getReceivedMessages();
        if (messages.isEmpty()) {
            sender.sendMessage(plugin.i18n("bot_messages_empty").replace("{0}", botName));
            return;
        }

        sender.sendMessage(plugin.i18n("bot_messages_header")
                .replace("{0}", botName)
                .replace("{1}", String.valueOf(messages.size())));
        for (int i = 0; i < messages.size(); i++) {
            sender.sendMessage("[" + i + "] " + messages.get(i));
        }
    }

    @CmdMapping(format = "clearmsg <name>")
    public void onClearMessages(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        bot.clearMessages();
        sender.sendMessage(plugin.i18n("bot_messages_cleared").replace("{0}", botName));
    }

    @CmdMapping(format = "op <name>")
    public void onOp(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        bot.setOp(true);
        sender.sendMessage(plugin.i18n("bot_op_set").replace("{0}", botName));
    }

    @CmdMapping(format = "deop <name>")
    public void onDeop(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        bot.setOp(false);
        sender.sendMessage(plugin.i18n("bot_op_removed").replace("{0}", botName));
    }

    @CmdMapping(format = "click <name> <slot>")
    public void onClick(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName,
            @CmdParam("slot") String slotStr
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        int slot;
        try {
            slot = Integer.parseInt(slotStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.i18n("bot_click_invalid_slot").replace("{0}", slotStr));
            return;
        }

        InventoryView view = bot.getOpenInventoryView();
        if (view == null) {
            sender.sendMessage(plugin.i18n("bot_no_inventory").replace("{0}", botName));
            return;
        }

        bot.clickSlot(slot);
        sender.sendMessage(plugin.i18n("bot_click_success")
                .replace("{0}", botName)
                .replace("{1}", String.valueOf(slot)));
    }

    @CmdMapping(format = "closeinv <name>")
    public void onCloseInventory(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        bot.closeInventory();
        sender.sendMessage(plugin.i18n("bot_inv_closed").replace("{0}", botName));
    }

    @CmdMapping(format = "inv <name>")
    public void onInventory(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        InventoryView view = bot.getOpenInventoryView();
        if (view == null) {
            sender.sendMessage(plugin.i18n("bot_no_inventory").replace("{0}", botName));
            return;
        }

        Inventory topInv = view.getTopInventory();
        sender.sendMessage(plugin.i18n("bot_inv_header")
                .replace("{0}", botName)
                .replace("{1}", view.getTitle()));

        boolean hasItems = false;
        for (int i = 0; i < topInv.getSize(); i++) {
            ItemStack item = topInv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                hasItems = true;
                String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().getDisplayName()
                        : item.getType().name();
                sender.sendMessage("  [" + i + "] " + displayName + " x" + item.getAmount());
            }
        }

        if (!hasItems) {
            sender.sendMessage(plugin.i18n("bot_inv_empty"));
        }
    }

    // --- Macro commands ---

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
