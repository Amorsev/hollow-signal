package com.hollowsignal.test;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.chat.Dialogue;
import com.hollowsignal.registry.ModWorld;
import com.hollowsignal.world.ReceiverBlock;
import com.hollowsignal.world.StaticArena;
import com.hollowsignal.entity.ListenerEntity;
import com.hollowsignal.entity.ListenerSpawner;
import com.hollowsignal.registry.ModAttachments;
import com.hollowsignal.world.Endings;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Automated checks run by `gradlew runGameTestServer` (loads the real datapacks, no EULA needed).
 * Only active when game tests are enabled for this namespace (dev run configs) — no effect in normal play.
 */
@GameTestHolder(HollowSignal.MODID)
@PrefixGameTestTemplate(false)
public final class HSGameTests {
    private HSGameTests() {}

    private static FakePlayer fake(GameTestHelper h, String name) {
        return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name));
    }

    /** Split reality: a scare/mimic Listener is sent only to its victim; a stalker is visible to everyone. */
    @GameTest(template = "empty")
    public static void listenerPrivacy(GameTestHelper h) {
        FakePlayer alice = fake(h, "hs_test_alice"), bob = fake(h, "hs_test_bob");
        BlockPos pos = h.absolutePos(new BlockPos(0, 1, 0));

        ListenerEntity scare = ListenerSpawner.spawn(alice, pos, ListenerEntity.Mode.GLANCE, 100);
        h.assertTrue(scare != null, "spawn failed");
        h.assertTrue(scare.broadcastToPlayer(alice), "victim must see the scare");
        h.assertFalse(scare.broadcastToPlayer(bob), "a friend must NOT see the victim's scare");

        scare.setMimic(bob);
        h.assertTrue(scare.mimicOf().orElse(null) != null && scare.mimicOf().get().equals(bob.getUUID()), "mimic uuid not set");
        h.assertTrue(scare.getCustomName() != null && "hs_test_bob".equals(scare.getCustomName().getString()), "mimic name not set");
        scare.discard();

        ListenerEntity stalker = ListenerSpawner.spawn(alice, pos, ListenerEntity.Mode.STALK, 100);
        h.assertTrue(stalker != null && stalker.broadcastToPlayer(alice) && stalker.broadcastToPlayer(bob), "stalker should be visible to all");
        stalker.discard();
        h.succeed();
    }

    /** The ending is decided by the group's AVERAGE relationship. */
    @GameTest(template = "empty")
    public static void groupEnding(GameTestHelper h) {
        FakePlayer a = fake(h, "hs_test_a"), b = fake(h, "hs_test_b");
        ModAttachments.of(a).setRelationship(80);
        ModAttachments.of(b).setRelationship(-20);
        h.assertTrue(Endings.decide(List.of(a, b)) == Endings.Type.RELEASED, "avg 30 should release");
        ModAttachments.of(a).setRelationship(0);
        ModAttachments.of(b).setRelationship(-80);
        h.assertTrue(Endings.decide(List.of(a, b)) == Endings.Type.ERASED, "avg -40 should erase");
        ModAttachments.of(b).setRelationship(0);
        h.assertTrue(Endings.decide(List.of(a, b)) == Endings.Type.LOOP, "avg 0 should loop");
        h.assertTrue(Dialogue.INSTANCE.topic("mimic") != null, "mimic dialogue missing");
        h.succeed();
    }

    /** Balance knobs: intensity scales cooldowns; disabledEvents switches events off. */
    @GameTest(template = "empty")
    public static void balanceKnobs(GameTestHelper h) {
        var sign = com.hollowsignal.director.HorrorEvents.get("sign");
        var cfg = com.hollowsignal.config.HSConfig.SERVER;
        double oldIntensity = cfg.intensity.get();
        var oldDisabled = List.copyOf(cfg.disabledEvents.get());
        try {
            cfg.intensity.set(2.0);
            h.assertTrue(com.hollowsignal.director.HorrorDirector.cooldown(sign) == sign.cooldownTicks() / 2, "intensity 2 should halve cooldowns");
            cfg.disabledEvents.set(List.of("sign"));
            h.assertTrue(com.hollowsignal.director.HorrorDirector.disabled(sign), "sign should be disabled");
            h.assertFalse(com.hollowsignal.director.HorrorDirector.disabled(com.hollowsignal.director.HorrorEvents.get("tunnel")), "tunnel should stay enabled");
        } finally {
            cfg.intensity.set(oldIntensity);
            cfg.disabledEvents.set(oldDisabled);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void dialogueLoaded(GameTestHelper h) {
        h.assertTrue(Dialogue.INSTANCE.topic("how_are_you") != null, "dialogue topic how_are_you missing");
        h.assertTrue(Dialogue.INSTANCE.topic("unknown") != null, "dialogue topic unknown missing");
        h.assertTrue(Dialogue.INSTANCE.match("Как ТВОИ дела???") == Dialogue.INSTANCE.topic("how_are_you"), "phrase matching broken");
        h.succeed();
    }

    /** The datapack JSON parsed and registered (the game-test server itself doesn't open custom dimensions). */
    @GameTest(template = "empty")
    public static void staticWorldgenRegistered(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        h.assertTrue(access.registryOrThrow(Registries.DIMENSION_TYPE).containsKey(id("the_static")), "dimension_type the_static missing");
        h.assertTrue(access.registryOrThrow(Registries.BIOME).containsKey(id("static_void")), "biome static_void missing");
        h.assertTrue(access.registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(id("static_debris")), "configured_feature missing");
        h.assertTrue(access.registryOrThrow(Registries.PLACED_FEATURE).containsKey(id("static_debris")), "placed_feature missing");
        h.succeed();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(HollowSignal.MODID, path);
    }

    @GameTest(template = "empty")
    public static void staticDimensionAndArena(GameTestHelper h) {
        // In a normal world this is the_static; the game-test server has no custom dimensions,
        // so the arena build is verified in the test level instead.
        ServerLevel stat = StaticArena.level(h.getLevel().getServer());
        if (stat == null) stat = h.getLevel();
        StaticArena.ensureBuilt(stat);
        h.assertTrue(stat.getBlockState(StaticArena.CENTER).is(ModWorld.CORE.get()), "core not placed");
        for (int i = 0; i < StaticArena.RECEIVERS; i++) {
            var s = stat.getBlockState(StaticArena.receiverPos(i));
            h.assertTrue(s.is(ModWorld.RECEIVER.get()) && !s.getValue(ReceiverBlock.ACTIVE), "receiver " + i + " missing");
        }
        h.assertTrue(StaticArena.activeReceivers(stat) == 0, "receivers should start off");
        h.succeed();
    }
}
