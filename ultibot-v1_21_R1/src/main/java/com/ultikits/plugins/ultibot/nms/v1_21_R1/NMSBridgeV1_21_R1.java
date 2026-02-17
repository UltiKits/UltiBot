package com.ultikits.plugins.ultibot.nms.v1_21_R1;

import com.mojang.authlib.GameProfile;
import com.ultikits.plugins.ultibot.api.BotPlayer;
import com.ultikits.plugins.ultibot.api.NMSBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.bukkit.craftbukkit.v1_21_R1.CraftWorld;

import java.util.UUID;

public class NMSBridgeV1_21_R1 implements NMSBridge {

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public BotPlayer createBot(String name, UUID uuid, Location spawnLocation) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel level = ((CraftWorld) spawnLocation.getWorld()).getHandle();
        GameProfile profile = new GameProfile(uuid, name);

        ServerPlayer nmsPlayer = new ServerPlayer(server, level, profile,
                ClientInformation.createDefault());
        nmsPlayer.absMoveTo(spawnLocation.getX(), spawnLocation.getY(),
                spawnLocation.getZ(), spawnLocation.getYaw(), spawnLocation.getPitch());

        // Wire up fake connection so server doesn't NPE
        FakeConnection fakeConn = new FakeConnection();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerGamePacketListenerImpl listener = new ServerGamePacketListenerImpl(
                server, fakeConn, nmsPlayer, cookie);
        nmsPlayer.connection = listener;

        return new BotPlayerV1_21_R1(nmsPlayer, server, fakeConn);
    }
}
