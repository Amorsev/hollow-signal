package com.hollowsignal.director.events;

import com.hollowsignal.chat.ContactManager;
import com.hollowsignal.director.EventContext;
import com.hollowsignal.director.HorrorEvent;
import com.hollowsignal.director.Stage;

/** Fake grey "[Server] ..." lines that shouldn't exist in a singleplayer world. */
public final class SystemMessageEvent implements HorrorEvent {
    @Override public String id() { return "system_message"; }
    @Override public int minStage() { return Stage.HOSTILE; }
    @Override public long cooldownTicks() { return 10 * 60 * 20; }
    @Override public int dreadGain() { return 15; }
    @Override public int weight(EventContext ctx) { return 4; }

    @Override
    public boolean run(EventContext ctx) {
        return ContactManager.say(ctx.player(), "system", 0);
    }
}
