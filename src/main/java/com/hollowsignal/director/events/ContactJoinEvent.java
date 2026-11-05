package com.hollowsignal.director.events;

import com.hollowsignal.chat.ContactManager;
import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;

/** "<name> joined the game" — only for this player — and it starts talking. */
public final class ContactJoinEvent implements HorrorEvent {
    @Override public String id() { return "contact_join"; }
    @Override public int minStage() { return Stage.CONTACT; }
    @Override public long cooldownTicks() { return 20 * 60 * 20; }
    @Override public int dreadGain() { return 30; }

    @Override
    public int weight(EventContext ctx) {
        return ContactManager.isOnline(ctx.player()) ? 0 : 7;
    }

    @Override
    public boolean run(EventContext ctx) {
        if (ContactManager.isOnline(ctx.player())) return false;
        ContactManager.join(ctx.player(), (4 + ctx.random().nextInt(5)) * 60 * 20);
        return true;
    }
}
