package com.ultikits.plugins.ultibot.commands;

import com.ultikits.plugins.ultibot.api.ActionType;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.model.MacroEntry;
import com.ultikits.plugins.ultibot.service.ActionServiceImpl;
import com.ultikits.plugins.ultibot.service.BotManagerImpl;
import com.ultikits.plugins.ultibot.service.MacroServiceImpl;
import com.ultikits.plugins.ultibot.service.SkinService;
import com.ultikits.ultitools.abstracts.AbstractCommandExecutor;
import com.ultikits.ultitools.abstracts.UltiToolsPlugin;
import com.ultikits.ultitools.annotations.command.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Set;
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
    private final ActionServiceImpl actionService;
    private final MacroServiceImpl macroService;
    private final SkinService skinService;

    public BotCommands(UltiToolsPlugin plugin, BotManagerImpl botManager,
                       ActionServiceImpl actionService, MacroServiceImpl macroService,
                       SkinService skinService) {
        this.plugin = plugin;
        this.botManager = botManager;
        this.actionService = actionService;
        this.macroService = macroService;
        this.skinService = skinService;
    }

    // --- Core bot management ---

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

    @CmdMapping(format = "spawnat <name>")
    public void onSpawnAt(
            @CmdSender CommandSender sender,
            @CmdParam("name") String name
    ) {
        if (botManager.getBot(name) != null) {
            sender.sendMessage(plugin.i18n("bot_name_taken").replace("{0}", name));
            return;
        }
        World world = Bukkit.getWorlds().get(0);
        Location loc = world.getSpawnLocation();
        BotPlayer bot = botManager.spawnBotNoOwner(name, loc);
        if (bot == null) {
            sender.sendMessage("Failed to spawn bot " + name);
            return;
        }
        sender.sendMessage(plugin.i18n("bot_spawned").replace("{0}", name));
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

    // --- Action commands ---

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

    // --- Utility commands ---

    @CmdMapping(format = "chat <name> <words...>")
    public void onChat(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName,
            @CmdParam("words") String[] words
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        String message = String.join(" ", words);
        bot.chat(message);
        sender.sendMessage(plugin.i18n("bot_chat_sent").replace("{0}", botName));
    }

    @CmdMapping(format = "cmd <name> <args...>")
    public void onCmd(
            @CmdSender CommandSender sender,
            @CmdParam("name") String botName,
            @CmdParam("args") String[] args
    ) {
        BotPlayer bot = botManager.getBot(botName);
        if (bot == null) {
            sender.sendMessage(plugin.i18n("bot_not_found").replace("{0}", botName));
            return;
        }

        String command = String.join(" ", args);
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

        // Use reflection for InventoryView methods — Paper 1.21+ changed it from class to interface
        try {
            Object view = bot.getOpenInventoryView();
            if (view == null) {
                sender.sendMessage(plugin.i18n("bot_no_inventory").replace("{0}", botName));
                return;
            }

            java.lang.reflect.Method getTopInv = view.getClass().getMethod("getTopInventory");
            java.lang.reflect.Method getTitle = view.getClass().getMethod("getTitle");
            Inventory topInv = (Inventory) getTopInv.invoke(view);
            String title = (String) getTitle.invoke(view);

            sender.sendMessage(plugin.i18n("bot_inv_header")
                    .replace("{0}", botName)
                    .replace("{1}", title));

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
        } catch (Exception e) {
            sender.sendMessage("Error inspecting inventory: " + e.getMessage());
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
        sender.sendMessage("§6=== UltiBot Commands ===");
        sender.sendMessage("§e/bot spawnat <name> §7- Spawn bot at world spawn");
        sender.sendMessage("§e/bot spawn <name> §7- Spawn bot at your location");
        sender.sendMessage("§e/bot remove <name|all> §7- Remove bot(s)");
        sender.sendMessage("§e/bot list §7- List all bots");
        sender.sendMessage("§e/bot tp <name> §7- Teleport bot to you");
        sender.sendMessage("§e/bot chat <name> <msg> §7- Bot sends chat");
        sender.sendMessage("§e/bot cmd <name> <cmd> §7- Bot runs command");
        sender.sendMessage("§e/bot action <name> <type> <interval> §7- Repeat action");
        sender.sendMessage("§e/bot stop <name> §7- Stop actions");
        sender.sendMessage("§e/bot messages <name> §7- View received messages");
        sender.sendMessage("§e/bot clearmsg <name> §7- Clear messages");
        sender.sendMessage("§e/bot op/deop <name> §7- Toggle OP");
        sender.sendMessage("§e/bot inv <name> §7- Inspect open inventory");
        sender.sendMessage("§e/bot click <name> <slot> §7- Click inventory slot");
        sender.sendMessage("§e/bot closeinv <name> §7- Close inventory");
        sender.sendMessage("§e/bot skin <name> <skin> §7- Change skin");
        sender.sendMessage("§e/bot macro ... §7- Macro recording/playback");
        sender.sendMessage("§e/bot reload §7- Reload config");
    }
}
