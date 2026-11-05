package com.hollowsignal.director;

import com.hollowsignal.director.events.ChestGiftEvent;
import com.hollowsignal.director.events.ContactJoinEvent;
import com.hollowsignal.director.events.ContactMessageEvent;
import com.hollowsignal.director.events.SystemMessageEvent;
import com.hollowsignal.director.events.PhantomSoundEvent;
import com.hollowsignal.director.events.ScreenEffectEvents;
import com.hollowsignal.director.events.CreepEvent;
import com.hollowsignal.director.events.GlanceEvent;
import com.hollowsignal.director.events.StalkEvent;
import com.hollowsignal.director.events.SignEvent;
import com.hollowsignal.director.events.TorchTheftEvent;
import com.hollowsignal.director.events.TunnelEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Static event registry. Later stages add their events here. */
public final class HorrorEvents {
    private HorrorEvents() {}

    private static final Map<String, HorrorEvent> EVENTS = new LinkedHashMap<>();

    static {
        add(new TorchTheftEvent());
        add(new SignEvent());
        add(new ChestGiftEvent());
        add(new TunnelEvent());
        add(new StalkEvent());
        add(new GlanceEvent());
        add(new CreepEvent());
        add(new ContactJoinEvent());
        add(new ContactMessageEvent());
        add(new SystemMessageEvent());
        add(new PhantomSoundEvent());
        ScreenEffectEvents.all().forEach(HorrorEvents::add);
        com.hollowsignal.director.events.SignalMobEvents.all().forEach(HorrorEvents::add);
        add(new com.hollowsignal.world.StaticEntry.CallEvent());
        com.hollowsignal.director.events.FourthWallEvents.all().forEach(HorrorEvents::add);
        com.hollowsignal.director.events.MultiplayerEvents.all().forEach(HorrorEvents::add);
    }

    private static void add(HorrorEvent e) {
        EVENTS.put(e.id(), e);
    }

    public static Collection<HorrorEvent> all() {
        return Collections.unmodifiableCollection(EVENTS.values());
    }

    public static HorrorEvent get(String id) {
        return EVENTS.get(id);
    }

    public static Collection<String> ids() {
        return Collections.unmodifiableCollection(EVENTS.keySet());
    }
}
