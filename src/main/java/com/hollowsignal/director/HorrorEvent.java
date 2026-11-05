package com.hollowsignal.director;

/**
 * One thing the director can make happen to a player.
 * Implementations must be cheap in {@link #canRun} and must never touch client code.
 */
public interface HorrorEvent {
    String id();

    int minStage();

    /** Relative weight for the random pick; 0 or less = skip. */
    int weight(EventContext ctx);

    /** Ticks before this event may run again for the same player. */
    long cooldownTicks();

    /** Personal dread added to the victim when the event succeeds. */
    int dreadGain();

    /** Try to perform the event. Return false if no valid spot/target was found. */
    boolean run(EventContext ctx);

    /** Jumpscare-type events: rarer when the victim is with friends (traces become more common instead). */
    default boolean scare() { return false; }
}
