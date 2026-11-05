package com.hollowsignal.client;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.entity.ListenerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Player-shaped (standard 64x64 skin layout), so it can pass for "another player" from a distance.
 * In MIMIC mode it wears the skin of the friend it's impersonating.
 */
public class ListenerRenderer extends HumanoidMobRenderer<ListenerEntity, HumanoidModel<ListenerEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, "textures/entity/listener.png");

    public ListenerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ListenerEntity entity) {
        UUID friend = entity.mimicOf().orElse(null);
        if (friend != null && Minecraft.getInstance().getConnection() != null) {
            PlayerInfo info = Minecraft.getInstance().getConnection().getPlayerInfo(friend);
            if (info != null) return info.getSkin().texture();
        }
        return TEXTURE;
    }
}
