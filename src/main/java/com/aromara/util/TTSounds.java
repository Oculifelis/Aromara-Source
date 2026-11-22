package com.aromara.util;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Thaumaturge's own sound events, played by id so Aromara needs no hard reference to its sound registry. */
public final class TTSounds {
    public static final SoundEvent WRITE = of("write");
    public static final SoundEvent LEARN = of("learn");
    public static final SoundEvent CHANT = of("chant");
    public static final SoundEvent JACOBS = of("jacobs");
    public static final SoundEvent EGSCREECH = of("egscreech");
    public static final SoundEvent ZAP = of("zap");

    private TTSounds() {
    }

    private static SoundEvent of(String name) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("thaumaturge", name));
    }
}
