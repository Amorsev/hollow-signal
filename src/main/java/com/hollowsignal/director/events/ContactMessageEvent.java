package com.hollowsignal.director.events;

import com.hollowsignal.chat.ContactManager;
import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;

/** While it's "online", it writes on its own from time to time. */
public final class ContactMessageEvent implements HorrorEvent {
    @Override public String id() { return "contact_message"; }
    @Override public int minStage() { return Stage.CONTACT; }
    @Override public long cooldownTicks() { return 2 * 60 * 20; }
    @Override public int dreadGain() { return 8; }

    @Override
    public int weight(EventContext ctx) {
        return ContactManager.isOnline(ctx.player()) ? 12 : 0;
    }

    @Override
    public boolean run(EventContext ctx) {
        if (!ContactManager.isOnline(ctx.player())) return false;
        return ContactManager.say(ctx.player(), "idle", 0);
    }
}
