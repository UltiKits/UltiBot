package com.ultikits.plugins.ultibot.service;

import com.ultikits.plugins.ultibot.api.ActionTicker;
import com.ultikits.plugins.ultibot.api.ActionType;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import lombok.Getter;

public class SimpleActionTicker implements ActionTicker {

    @Getter
    private final ActionType actionType;
    private final BotPlayer bot;
    private final int intervalTicks;
    private final ActionServiceImpl actionService;
    private volatile boolean running;
    private int tickCounter;

    public SimpleActionTicker(ActionType actionType, BotPlayer bot, int intervalTicks,
                              ActionServiceImpl actionService) {
        this.actionType = actionType;
        this.bot = bot;
        this.intervalTicks = intervalTicks;
        this.actionService = actionService;
        this.running = false;
        this.tickCounter = 0;
    }

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void tick() {
        if (!running) {
            return;
        }
        tickCounter++;
        if (tickCounter >= intervalTicks) {
            tickCounter = 0;
            actionService.performAction(bot, actionType);
        }
    }
}
