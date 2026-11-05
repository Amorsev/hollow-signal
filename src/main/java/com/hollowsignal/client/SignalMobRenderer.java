package com.hollowsignal.client;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.entity.SignalMob;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Player-shaped renderer with its own texture and size (Watcher is tall, Reporter is small). */
public class SignalMobRenderer<T extends SignalMob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public SignalMobRenderer(EntityRendererProvider.Context ctx, String textureName, float scale) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f * scale);
        this.texture = ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, "textures/entity/" + textureName + ".png");
        this.scale = scale;
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        pose.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
