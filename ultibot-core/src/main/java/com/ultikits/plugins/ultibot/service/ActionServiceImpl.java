package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.*;
import com.ultikits.ultitools.annotations.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActionServiceImpl {

    // bot UUID -> (actionType -> ticker)
    private final Map<UUID, Map<ActionType, SimpleActionTicker>> activeTickers = new ConcurrentHashMap<>();

    public void performAction(BotPlayer bot, ActionType action) {
        switch (action) {
            case JUMP:
                bot.jump();
                break;
            case SNEAK:
                bot.setSneaking(true);
                break;
            case SPRINT:
                bot.setSprinting(true);
                break;
            case USE:
                bot.useItem();
                break;
            case DROP_ITEM:
                bot.dropItem(false);
                break;
            case DROP_STACK:
                bot.dropItem(true);
                break;
            case DROP_INVENTORY:
                bot.dropInventory();
                break;
            case ATTACK:
                // Attack without a target — no-op swing
                break;
            case MINE:
                // Mine without a target block — no-op
                break;
            case LOOK_AT_NEAREST:
                // Requires entity scan — handled at higher level
                break;
            default:
                break;
        }
    }

    public ActionTicker startRepeatingAction(BotPlayer bot, ActionType action, int intervalTicks) {
        Map<ActionType, SimpleActionTicker> botTickers =
                activeTickers.computeIfAbsent(bot.getUUID(), k -> new ConcurrentHashMap<>());

        // Stop existing ticker for this action type if present
        SimpleActionTicker existing = botTickers.get(action);
        if (existing != null) {
            existing.stop();
        }

        SimpleActionTicker ticker = new SimpleActionTicker(action, bot, intervalTicks, this);
        ticker.start();
        botTickers.put(action, ticker);
        return ticker;
    }

    public void stopAllActions(BotPlayer bot) {
        Map<ActionType, SimpleActionTicker> botTickers = activeTickers.remove(bot.getUUID());
        if (botTickers != null) {
            for (SimpleActionTicker ticker : botTickers.values()) {
                ticker.stop();
            }
        }
    }

    public void stopAction(BotPlayer bot, ActionType action) {
        Map<ActionType, SimpleActionTicker> botTickers = activeTickers.get(bot.getUUID());
        if (botTickers != null) {
            SimpleActionTicker ticker = botTickers.remove(action);
            if (ticker != null) {
                ticker.stop();
            }
        }
    }

    public void tickAll() {
        for (Map<ActionType, SimpleActionTicker> botTickers : activeTickers.values()) {
            for (SimpleActionTicker ticker : botTickers.values()) {
                ticker.tick();
            }
        }
    }
}
