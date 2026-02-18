package com.ultikits.plugins.ultibot.nms.v1_21_R1;

import com.mojang.authlib.GameProfile;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.InventoryView;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class BotPlayerV1_21_R1 implements BotPlayer {

    private final ServerPlayer nmsPlayer;
    private final MinecraftServer server;
    private final FakeConnection fakeConnection;
    private int tickCount;

    public BotPlayerV1_21_R1(ServerPlayer nmsPlayer, MinecraftServer server, FakeConnection fakeConnection) {
        this.nmsPlayer = nmsPlayer;
        this.server = server;
        this.fakeConnection = fakeConnection;
        this.tickCount = 0;
    }

    @Override
    public Player getBukkitPlayer() {
        // Use Bukkit API instead of nmsPlayer.getBukkitEntity() to avoid
        // CraftBukkit versioned package mismatch (Paper 1.21+ removed version suffix)
        return Bukkit.getPlayer(nmsPlayer.getUUID());
    }

    @Override
    public UUID getUUID() {
        return nmsPlayer.getUUID();
    }

    @Override
    public String getName() {
        return nmsPlayer.getGameProfile().getName();
    }

    @Override
    public void join() {
        GameProfile profile = nmsPlayer.getGameProfile();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        server.getPlayerList().placeNewPlayer(fakeConnection, nmsPlayer, cookie);
    }

    @Override
    public void disconnect(String reason) {
        // Use Bukkit API to avoid versioned NMS method signature issues
        Player player = Bukkit.getPlayer(nmsPlayer.getUUID());
        if (player != null) {
            player.kickPlayer(reason);
        }
    }

    @Override
    public void tick() {
        nmsPlayer.doTick();
        tickCount++;
    }

    @Override
    public void respawn() {
        server.getPlayerList().respawn(nmsPlayer, false, Entity.RemovalReason.KILLED,
                PlayerRespawnEvent.RespawnReason.PLUGIN);
    }

    @Override
    public void moveTo(Location location) {
        nmsPlayer.absMoveTo(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

    @Override
    public void setMovement(float forward, float strafe) {
        nmsPlayer.zza = forward;
        nmsPlayer.xxa = strafe;
    }

    @Override
    public void jump() {
        nmsPlayer.jumpFromGround();
    }

    @Override
    public void setSneaking(boolean sneaking) {
        nmsPlayer.setShiftKeyDown(sneaking);
    }

    @Override
    public void setSprinting(boolean sprinting) {
        nmsPlayer.setSprinting(sprinting);
    }

    @Override
    public void lookAt(Location target) {
        nmsPlayer.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                new Vec3(target.getX(), target.getY(), target.getZ()));
    }

    @Override
    public void lookAt(org.bukkit.entity.Entity entity) {
        Entity nmsEntity = getNmsEntity(entity);
        nmsPlayer.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                nmsEntity.position());
    }

    @Override
    public void attack(org.bukkit.entity.Entity target) {
        Entity nmsEntity = getNmsEntity(target);
        nmsPlayer.attack(nmsEntity);
    }

    @Override
    public void mine(Block block) {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                block.getX(), block.getY(), block.getZ());
        nmsPlayer.gameMode.handleBlockBreakAction(
                pos,
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                net.minecraft.core.Direction.UP,
                nmsPlayer.level().getMaxBuildHeight(),
                0
        );
    }

    @Override
    public void useItem() {
        nmsPlayer.gameMode.useItem(nmsPlayer, nmsPlayer.level(), nmsPlayer.getMainHandItem(),
                InteractionHand.MAIN_HAND);
    }

    @Override
    public void interactEntity(org.bukkit.entity.Entity target) {
        Entity nmsEntity = getNmsEntity(target);
        nmsPlayer.interactOn(nmsEntity, InteractionHand.MAIN_HAND);
    }

    @Override
    public void swapHands() {
        ItemStack mainItem = nmsPlayer.getItemInHand(InteractionHand.MAIN_HAND).copy();
        ItemStack offItem = nmsPlayer.getItemInHand(InteractionHand.OFF_HAND).copy();
        nmsPlayer.setItemInHand(InteractionHand.MAIN_HAND, offItem);
        nmsPlayer.setItemInHand(InteractionHand.OFF_HAND, mainItem);
    }

    @Override
    public void chat(String message) {
        getBukkitPlayer().chat(message);
    }

    @Override
    public void performCommand(String command) {
        getBukkitPlayer().performCommand(command);
    }

    @Override
    public void dropItem(boolean entireStack) {
        nmsPlayer.drop(entireStack);
    }

    @Override
    public void dropInventory() {
        nmsPlayer.getInventory().dropAll();
    }

    @Override
    public void selectSlot(int slot) {
        if (slot >= 0 && slot <= 8) {
            nmsPlayer.getInventory().selected = slot;
        }
    }

    @Override
    public boolean isOnGround() {
        return nmsPlayer.onGround();
    }

    @Override
    public boolean isUsingItem() {
        return nmsPlayer.isUsingItem();
    }

    @Override
    public int getTickCount() {
        return tickCount;
    }

    // --- Testing support ---

    @Override
    public List<String> getReceivedMessages() {
        return fakeConnection.getCapturedMessages();
    }

    @Override
    public void clearMessages() {
        fakeConnection.clearCapturedMessages();
    }

    @Override
    public void setOp(boolean op) {
        getBukkitPlayer().setOp(op);
    }

    @Override
    public boolean isOp() {
        return getBukkitPlayer().isOp();
    }

    @Override
    public void clickSlot(int slot) {
        Player player = getBukkitPlayer();
        // Use reflection — Paper 1.21+ changed InventoryView from class to interface
        try {
            Object view = player.getClass().getMethod("getOpenInventory").invoke(player);
            if (view == null) {
                return;
            }
            InventoryClickEvent event = new InventoryClickEvent(
                    (InventoryView) view, InventoryType.SlotType.CONTAINER, slot,
                    ClickType.LEFT, InventoryAction.PICKUP_ONE
            );
            Bukkit.getPluginManager().callEvent(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to click slot", e);
        }
    }

    @Override
    public InventoryView getOpenInventoryView() {
        try {
            return (InventoryView) getBukkitPlayer().getClass()
                    .getMethod("getOpenInventory").invoke(getBukkitPlayer());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get open inventory view", e);
        }
    }

    @Override
    public void closeInventory() {
        getBukkitPlayer().closeInventory();
    }

    /**
     * Get NMS Entity from Bukkit Entity via reflection (avoids CraftBukkit versioned package).
     */
    private Entity getNmsEntity(org.bukkit.entity.Entity entity) {
        try {
            Method getHandle = entity.getClass().getMethod("getHandle");
            return (Entity) getHandle.invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get NMS entity handle", e);
        }
    }
}
