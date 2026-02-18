package com.ultikits.plugins.ultibot.api;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.util.List;
import java.util.UUID;

public interface BotPlayer {
    // Identity
    Player getBukkitPlayer();
    UUID getUUID();
    String getName();

    // Lifecycle
    void join();
    void disconnect(String reason);
    void tick();
    void respawn();

    // Movement
    void moveTo(Location location);
    void setMovement(float forward, float strafe);
    void jump();
    void setSneaking(boolean sneaking);
    void setSprinting(boolean sprinting);
    void lookAt(Location target);
    void lookAt(Entity entity);

    // Actions
    void attack(Entity target);
    void mine(Block block);
    void useItem();
    void interactEntity(Entity target);
    void swapHands();

    // Communication
    void chat(String message);
    void performCommand(String command);

    // Inventory
    void dropItem(boolean entireStack);
    void dropInventory();
    void selectSlot(int slot);

    // State
    boolean isOnGround();
    boolean isUsingItem();
    int getTickCount();

    // --- Testing support ---

    // Message capture: intercept all messages sent to this bot
    List<String> getReceivedMessages();
    void clearMessages();

    // OP management
    void setOp(boolean op);
    boolean isOp();

    // GUI interaction
    void clickSlot(int slot);
    InventoryView getOpenInventoryView();
    void closeInventory();
}
