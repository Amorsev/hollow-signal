package com.hollowsignal.registry;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.director.PlayerHorrorData;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HollowSignal.MODID);

    public static final Supplier<AttachmentType<PlayerHorrorData>> PLAYER_HORROR = ATTACHMENTS.register(
            "player_horror", () -> AttachmentType.serializable(PlayerHorrorData::new).copyOnDeath().build());

    public static PlayerHorrorData of(Player player) {
        return player.getData(PLAYER_HORROR);
    }
}
