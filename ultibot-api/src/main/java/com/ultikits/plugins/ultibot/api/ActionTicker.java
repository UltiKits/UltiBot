package com.ultikits.plugins.ultibot.api;

public interface ActionTicker {
    void start();
    void stop();
    boolean isRunning();
    ActionType getActionType();
}
