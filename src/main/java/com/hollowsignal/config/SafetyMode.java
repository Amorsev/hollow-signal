package com.hollowsignal.config;

/**
 * SAFE     — every "fourth wall" effect is simulated inside the game window.
 * HARDCORE — opt-in: real OS dialogs, text files in the user's folders, real crashes,
 *            and (behind a separate flag) a cancellable PC shutdown.
 */
public enum SafetyMode {
    SAFE,
    HARDCORE
}
