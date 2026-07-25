package com.makeforge.jjk.ce;

import com.makeforge.jjk.technique.Technique;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session-scoped cursed energy + technique selection.
 *
 * v1 is intentionally NOT persisted to NBT — CE and technique reset on relog.
 * That removes a whole class of serialization bugs and keeps the first build
 * clean. Persistence is a fast follow once the combat layer is confirmed green.
 *
 * The SERVER map is authoritative (gates real ability execution / damage).
 * The CLIENT map is a mirror used only to draw the HUD and give instant
 * feedback; it regens with the same numbers so it stays visually in sync.
 */
public final class CursedEnergy {
    private CursedEnergy() {}

    public static final class Data {
        public Technique technique = Technique.NONE;
        public float ce = Technique.NONE.maxCE;
        // used by Yuji's Black Flash timing window (ticks remaining)
        public int comboWindow = 0;
    }

    private static final Map<UUID, Data> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, Data> CLIENT = new ConcurrentHashMap<>();

    public static Data server(UUID id) { return SERVER.computeIfAbsent(id, k -> new Data()); }
    public static Data client(UUID id) { return CLIENT.computeIfAbsent(id, k -> new Data()); }

    public static void setTechnique(Data d, Technique t) {
        d.technique = t;
        d.ce = t.maxCE;
        d.comboWindow = 0;
    }

    /** Regen tick, shared by both sides. Returns nothing; mutates in place. */
    public static void tick(Data d) {
        Technique t = d.technique;
        if (d.ce < t.maxCE) {
            d.ce = Math.min(t.maxCE, d.ce + t.regenPerTick);
        }
        if (d.comboWindow > 0) d.comboWindow--;
    }

    /** Try to spend cost. True if paid (or free). */
    public static boolean spend(Data d, float cost) {
        if (cost <= 0f) return true;           // Toji / free abilities
        if (d.ce < cost) return false;
        d.ce -= cost;
        return true;
    }

    public static void clearClient() { CLIENT.clear(); }
}
