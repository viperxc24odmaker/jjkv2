package com.makeforge.jjk.technique;

/**
 * Each cursed technique / character. maxCE and regenPerTick tune how the
 * character *feels* to play (Divine wanted the feel to vary per technique).
 */
public enum Technique {
    NONE   ("None",    100f, 0.15f),
    // Gojo: big pool, moderate regen, ults cost a fortune.
    GOJO   ("Gojo",    140f, 0.20f),
    // Sukuna: aggressive, mid pool, fast enough to keep pressing.
    SUKUNA ("Sukuna",  120f, 0.30f),
    // Yuji: physical, tiny CE cost, snappy regen.
    YUJI   ("Yuji",     80f, 0.45f),
    // Megumi: sustained summoner, big pool, slow regen.
    MEGUMI ("Megumi",  150f, 0.18f),
    // Nobara: cheap ranged spam.
    NOBARA ("Nobara",  100f, 0.40f),
    // Toji: Heavenly Restriction — zero CE, pure body.
    TOJI   ("Toji",      0f, 0f);

    public final String display;
    public final float maxCE;
    public final float regenPerTick;

    Technique(String display, float maxCE, float regenPerTick) {
        this.display = display;
        this.maxCE = maxCE;
        this.regenPerTick = regenPerTick;
    }

    public static Technique byName(String s) {
        for (Technique t : values()) {
            if (t.name().equalsIgnoreCase(s) || t.display.equalsIgnoreCase(s)) return t;
        }
        return null;
    }
}
