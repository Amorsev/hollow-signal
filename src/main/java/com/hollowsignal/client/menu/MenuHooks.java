package com.hollowsignal.client.menu;

import com.hollowsignal.HollowSignal;
import com.hollowsignal.config.HSConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.packs.repository.PackRepository;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = HollowSignal.MODID, value = Dist.CLIENT)
public final class MenuHooks {
    private MenuHooks() {}

    private static final String PROGRAMMER_ART = "programmer_art";
    private static boolean packCheckDone;
    private static boolean hardcoreCheckDone;

    /** Swap the vanilla title screen for the old-style one. */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen && HSConfig.CLIENT.oldMainMenu.get()) {
            event.setNewScreen(new OldTitleScreen());
        }
    }

    /**
     * Once the game has fully loaded (no loading overlay) and we're on the title screen,
     * enable or disable the built-in Programmer Art pack according to the config — once.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // HARDCORE set by hand in the config file / generic GUI without the warning? Ask once, on the title screen.
        if (!hardcoreCheckDone && mc.getOverlay() == null && (mc.screen instanceof OldTitleScreen || mc.screen instanceof TitleScreen)) {
            hardcoreCheckDone = true;
            if (HSConfig.CLIENT.safetyMode.get() == com.hollowsignal.config.SafetyMode.HARDCORE && !HSConfig.CLIENT.hardcore()) {
                mc.setScreen(new com.hollowsignal.client.screen.HardcoreWarningScreen(mc.screen));
                return;
            }
        }
        if (packCheckDone) return;
        if (mc.getOverlay() != null || !(mc.screen instanceof OldTitleScreen || mc.screen instanceof TitleScreen)) return;
        packCheckDone = true;

        boolean want = HSConfig.CLIENT.oldTextures.get();
        boolean applied = HSConfig.CLIENT.oldTexturesApplied.get();
        if (want == applied) return; // already in the requested state; respect manual changes since then

        PackRepository repo = mc.getResourcePackRepository();
        if (repo.getPack(PROGRAMMER_ART) == null) return;
        List<String> selected = new ArrayList<>(repo.getSelectedIds());
        boolean changed = false;
        if (want && !selected.contains(PROGRAMMER_ART)) {
            // Directly above vanilla: the player's own resource packs still win.
            int vanilla = selected.indexOf("vanilla");
            selected.add(vanilla >= 0 ? vanilla + 1 : 0, PROGRAMMER_ART);
            changed = true;
        } else if (!want && selected.remove(PROGRAMMER_ART)) {
            changed = true;
        }
        HSConfig.CLIENT.oldTexturesApplied.set(want);
        HSConfig.CLIENT.oldTexturesApplied.save();
        if (changed) {
            repo.setSelected(selected);
            mc.options.updateResourcePacks(repo); // saves options and reloads resources
            HollowSignal.LOGGER.info("Programmer Art {}", want ? "enabled" : "disabled");
        }
    }
}
