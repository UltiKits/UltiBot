package com.ultikits.plugins.ultibot.nms.v1_21_R1;

import com.mojang.authlib.GameProfile;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class BotPlayerV1_21_R1 implements BotPlayer {

    private final ServerPlayer nmsPlayer;
    private final MinecraftServer server;
    private final Connection fakeConnection;
    private int tickCount;

    public BotPlayerV1_21_R1(ServerPlayer nmsPlayer, MinecraftServer server, Connection fakeConnection) {
        this.nmsPlayer = nmsPlayer;
        this.server = server;
        this.fakeConnection = fakeConnection;
        this.tickCount = 0;
    }

    @Override
    public Player getBukkitPlayer() {
        return nmsPlayer.getBukkitEntity();
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
        server.getPlayerList().remove(nmsPlayer);
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
        Entity nmsEntity = ((CraftEntity) entity).getHandle();
        nmsPlayer.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                nmsEntity.position());
    }

    @Override
    public void attack(org.bukkit.entity.Entity target) {
        Entity nmsEntity = ((CraftEntity) target).getHandle();
        nmsPlayer.attack(nmsEntity);
    }

    @Override
    public void mine(Block block) {
        // Use gameMode to initiate block breaking
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
        Entity nmsEntity = ((CraftEntity) target).getHandle();
        nmsPlayer.interactOn(nmsEntity, InteractionHand.MAIN_HAND);
    }

    @Override
    public void swapHands() {
        // Swap main hand and off hand items
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
        // Drop all inventory items
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
}
