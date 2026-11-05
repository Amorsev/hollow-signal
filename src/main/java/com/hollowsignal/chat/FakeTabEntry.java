package com.hollowsignal.chat;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Puts the entity's name into ONE player's Tab list (and removes it again).
 * In singleplayer this also makes the Tab list usable at all: vanilla hides it when you're alone,
 * and now you aren't.
 */
public final class FakeTabEntry {
    private FakeTabEntry() {}

    public static UUID uuidFor(String name) {
        return UUIDUtil.createOfflinePlayerUUID(name);
    }

    public static void show(ServerPlayer viewer, String name) {
        // A NeoForge FakePlayer is never added to the world or the real player list; it only
        // provides the data (profile, latency, game mode) the vanilla packet needs.
        FakePlayer ghost = FakePlayerFactory.get(viewer.serverLevel(), new GameProfile(uuidFor(name), name));
        viewer.connection.send(new ClientboundPlayerInfoUpdatePacket(EnumSet.of(
                ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE), List.of(ghost)));
    }

    public static void hide(ServerPlayer viewer, String name) {
        viewer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(uuidFor(name))));
    }
}
