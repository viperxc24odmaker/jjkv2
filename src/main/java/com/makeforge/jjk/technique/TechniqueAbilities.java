package com.makeforge.jjk.technique;

import com.makeforge.jjk.JJKMod;
import com.makeforge.jjk.ce.CursedEnergy;
import com.makeforge.jjk.util.CombatUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Server-side execution of every technique. slot: 0=G primary, 1=H secondary,
 * 2=J ultimate/domain.
 *
 * Effect Holder constants are grouped here so if Mojmap renamed any
 * (SPEED / STRENGTH / RESISTANCE / HASTE etc.) it's a one-line fix.
 */
public final class TechniqueAbilities {
    private TechniqueAbilities() {}

    // --- effect holders (rename here if CI complains) ---
    private static final Holder<MobEffect> SPEED      = MobEffects.SPEED;
    private static final Holder<MobEffect> STRENGTH   = MobEffects.STRENGTH;
    private static final Holder<MobEffect> RESISTANCE = MobEffects.RESISTANCE;
    private static final Holder<MobEffect> HASTE      = MobEffects.HASTE;
    private static final Holder<MobEffect> LEVITATION = MobEffects.LEVITATION;
    private static final Holder<MobEffect> DARKNESS   = MobEffects.DARKNESS;
    private static final Holder<MobEffect> WEAKNESS   = MobEffects.WEAKNESS;
    private static final Holder<MobEffect> GLOWING    = MobEffects.GLOWING;
    private static final Holder<MobEffect> SLOWNESS    = MobEffects.SLOWNESS;

    public static void execute(ServerPlayer p, int slot) {
        ServerLevel level = (ServerLevel) p.level();
        CursedEnergy.Data d = CursedEnergy.server(p.getUUID());
        switch (d.technique) {
            case GOJO   -> gojo(p, level, d, slot);
            case SUKUNA -> sukuna(p, level, d, slot);
            case YUJI   -> yuji(p, level, d, slot);
            case MEGUMI -> megumi(p, level, d, slot);
            case NOBARA -> nobara(p, level, d, slot);
            case TOJI   -> toji(p, level, d, slot);
            default     -> msg(p, "§7No technique. Use §f/technique <name>");
        }
    }

    /* ============================ GOJO ============================ */
    private static void gojo(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1f).normalize();
        switch (slot) {
            case 0 -> { // BLUE — implode: pull everything toward the aim point
                if (!pay(p, d, 20)) return;
                Vec3 focus = eye.add(look.scale(8));
                CombatUtil.particles(level, ParticleTypes.SONIC_BOOM, focus, 1, 0, 0);
                CombatUtil.particles(level, ParticleTypes.PORTAL, focus, 40, 1.2, 0.2);
                for (LivingEntity e : CombatUtil.around(level, p, focus, 6)) {
                    CombatUtil.pullToward(e, focus, 1.1);
                    CombatUtil.magicDamage(level, p, e, 4f);
                }
            }
            case 1 -> { // RED — repel blast
                if (!pay(p, d, 25)) return;
                Vec3 focus = eye.add(look.scale(4));
                CombatUtil.particles(level, ParticleTypes.EXPLOSION, focus, 3, 0.4, 0);
                CombatUtil.particles(level, ParticleTypes.FLAME, focus, 30, 1.0, 0.15);
                for (LivingEntity e : CombatUtil.around(level, p, focus, 6)) {
                    CombatUtil.knockAway(e, focus, 2.2, 0.6);
                    CombatUtil.magicDamage(level, p, e, 8f);
                }
            }
            case 2 -> { // HOLLOW PURPLE — piercing beam
                if (!pay(p, d, 90)) return;
                domainText(p, "§5§lHOLLOW PURPLE");
                for (int i = 1; i <= 30; i++) {
                    Vec3 point = eye.add(look.scale(i));
                    CombatUtil.particles(level, ParticleTypes.WITCH, point, 6, 0.3, 0.02);
                    CombatUtil.particles(level, ParticleTypes.SOUL_FIRE_FLAME, point, 4, 0.25, 0.02);
                    for (LivingEntity e : CombatUtil.around(level, p, point, 2.2)) {
                        CombatUtil.magicDamage(level, p, e, 14f);
                        CombatUtil.knockAway(e, eye, 1.5, 0.3);
                    }
                }
            }
        }
    }

    /* ============================ SUKUNA ============================ */
    private static void sukuna(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // DISMANTLE — cone of slashes
                if (!pay(p, d, 15)) return;
                slashParticles(level, p, 5);
                for (LivingEntity e : CombatUtil.cone(p, 6, 40)) {
                    CombatUtil.magicDamage(level, p, e, 7f);
                }
            }
            case 1 -> { // CLEAVE — heavy single target
                if (!pay(p, d, 25)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 6);
                if (t != null) {
                    slashParticles(level, p, 8);
                    float bonus = t.getMaxHealth() > 40 ? 6f : 0f; // adapts to tanky foes
                    CombatUtil.magicDamage(level, p, t, 12f + bonus);
                    CombatUtil.knockAway(t, p.position(), 0.8, 0.3);
                } else msg(p, "§cNo target.");
            }
            case 2 -> { // MALEVOLENT SHRINE — repeating AoE slashes
                if (!pay(p, d, 80)) return;
                domainText(p, "§4§lMALEVOLENT SHRINE");
                CombatUtil.effect(p, RESISTANCE, 120, 1);
                Vec3 center = p.position();
                for (int wave = 0; wave < 6; wave++) {
                    final int w = wave;
                    JJKMod.schedule(w * 10, () -> {
                        slashParticles(level, p, 12);
                        for (LivingEntity e : CombatUtil.around(level, p, center, 8)) {
                            CombatUtil.magicDamage(level, p, e, 6f);
                        }
                    });
                }
            }
        }
    }

    /* ============================ YUJI ============================ */
    private static void yuji(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // DIVERGENT FIST — hit now, second hit shortly after
                if (!pay(p, d, 8)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 6f);
                CombatUtil.particles(level, ParticleTypes.CRIT, t.getEyePosition(), 10, 0.3, 0.1);
                d.comboWindow = 30; // opens Black Flash window
                JJKMod.schedule(6, () -> {
                    if (t.isAlive()) {
                        CombatUtil.physicalDamage(level, p, t, 5f);
                        CombatUtil.particles(level, ParticleTypes.ENCHANTED_HIT, t.getEyePosition(), 12, 0.3, 0.1);
                    }
                });
            }
            case 1 -> { // BLACK FLASH — huge burst, bigger if in combo window
                if (!pay(p, d, 12)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "§cNo target."); return; }
                boolean flash = d.comboWindow > 0;
                float dmg = flash ? 20f : 10f;
                if (flash) domainText(p, "§0§lBLACK FLASH!");
                CombatUtil.physicalDamage(level, p, t, dmg);
                CombatUtil.particles(level, ParticleTypes.SQUID_INK, t.getEyePosition(), flash ? 40 : 15, 0.5, 0.05);
                CombatUtil.particles(level, ParticleTypes.CRIT, t.getEyePosition(), 20, 0.4, 0.2);
                d.comboWindow = 0;
            }
            case 2 -> { // MANJI KICK — knockback + self speed
                if (!pay(p, d, 10)) return;
                CombatUtil.effect(p, SPEED, 100, 1);
                for (LivingEntity e : CombatUtil.cone(p, 4, 50)) {
                    CombatUtil.physicalDamage(level, p, e, 6f);
                    CombatUtil.knockAway(e, p.position(), 1.8, 0.5);
                }
            }
        }
    }

    /* ============================ MEGUMI ============================ */
    private static void megumi(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // DIVINE DOGS — summon two wolves at your target
                if (!pay(p, d, 30)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 20);
                Vec3 at = p.position().add(p.getViewVector(1f).scale(2));
                CombatUtil.summonWolf(level, p, at.add(0.6, 0, 0), t);
                CombatUtil.summonWolf(level, p, at.add(-0.6, 0, 0), t);
                CombatUtil.particles(level, ParticleTypes.SMOKE, at, 25, 0.6, 0.05);
            }
            case 1 -> { // NUE — lightning strike + levitate victims
                if (!pay(p, d, 25)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 24);
                Vec3 strike = (t != null) ? t.position()
                        : p.getEyePosition().add(p.getViewVector(1f).scale(16));
                CombatUtil.lightning(level, strike);
                for (LivingEntity e : CombatUtil.around(level, p, strike, 4)) {
                    CombatUtil.effect(e, LEVITATION, 40, 1);
                    CombatUtil.magicDamage(level, p, e, 6f);
                }
            }
            case 2 -> { // CHIMERA SHADOW GARDEN — wolf swarm + darkness
                if (!pay(p, d, 90)) return;
                domainText(p, "§8§lCHIMERA SHADOW GARDEN");
                Vec3 base = p.position();
                for (int i = 0; i < 5; i++) {
                    double a = (Math.PI * 2 / 5) * i;
                    Vec3 at = base.add(Math.cos(a) * 3, 0, Math.sin(a) * 3);
                    CombatUtil.summonWolf(level, p, at, null);
                }
                for (LivingEntity e : CombatUtil.around(level, p, base, 10)) {
                    CombatUtil.effect(e, DARKNESS, 120, 0);
                    CombatUtil.effect(e, SLOWNESS, 120, 1);
                }
            }
        }
    }

    /* ============================ NOBARA ============================ */
    private static void nobara(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        switch (slot) {
            case 0 -> { // HAIRPIN — explosive nail on target
                if (!pay(p, d, 10)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 18);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.particles(level, ParticleTypes.EXPLOSION, t.position(), 2, 0.3, 0);
                CombatUtil.magicDamage(level, p, t, 8f);
                for (LivingEntity e : CombatUtil.around(level, p, t.position(), 2.5)) {
                    if (e != t) CombatUtil.magicDamage(level, p, e, 3f);
                }
            }
            case 1 -> { // RESONANCE — mark a target: echoes damage over time
                if (!pay(p, d, 12)) return;
                LivingEntity t = CombatUtil.raycastLiving(p, 18);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.effect(t, GLOWING, 100, 0);
                CombatUtil.effect(t, WEAKNESS, 100, 1);
                for (int i = 1; i <= 5; i++) {
                    JJKMod.schedule(i * 15, () -> {
                        if (t.isAlive()) {
                            CombatUtil.magicDamage(level, p, t, 3f);
                            CombatUtil.particles(level, ParticleTypes.CRIT, t.getEyePosition(), 6, 0.2, 0.1);
                        }
                    });
                }
            }
            case 2 -> { // STRAW DOLL BURST — AoE resonance detonation
                if (!pay(p, d, 40)) return;
                domainText(p, "§e§lRESONANCE — HAIRPIN");
                Vec3 aim = p.getEyePosition().add(p.getViewVector(1f).scale(6));
                CombatUtil.particles(level, ParticleTypes.EXPLOSION, aim, 5, 0.6, 0);
                for (LivingEntity e : CombatUtil.around(level, p, aim, 6)) {
                    CombatUtil.magicDamage(level, p, e, 12f);
                    CombatUtil.knockAway(e, aim, 1.2, 0.4);
                }
            }
        }
    }

    /* ============================ TOJI (Heavenly Restriction) ============================ */
    private static void toji(ServerPlayer p, ServerLevel level, CursedEnergy.Data d, int slot) {
        // Zero CE. Pure body. Everything is free but physical & short-range.
        switch (slot) {
            case 0 -> { // INVERTED SPEAR — brutal thrust
                LivingEntity t = CombatUtil.raycastLiving(p, 5);
                if (t == null) { msg(p, "§cNo target."); return; }
                CombatUtil.physicalDamage(level, p, t, 16f);
                CombatUtil.knockAway(t, p.position(), 1.0, 0.2);
                CombatUtil.particles(level, ParticleTypes.SWEEP_ATTACK, t.getEyePosition(), 3, 0.2, 0);
            }
            case 1 -> { // PLAYFUL CLOUD — wide sweep
                slashParticles(level, p, 6);
                for (LivingEntity e : CombatUtil.cone(p, 5, 60)) {
                    CombatUtil.physicalDamage(level, p, e, 10f);
                    CombatUtil.knockAway(e, p.position(), 1.4, 0.4);
                }
            }
            case 2 -> { // HEAVENLY RESTRICTION — burst of raw physical power
                CombatUtil.effect(p, SPEED, 200, 2);
                CombatUtil.effect(p, STRENGTH, 200, 1);
                CombatUtil.effect(p, RESISTANCE, 200, 1);
                CombatUtil.effect(p, HASTE, 200, 2);
                domainText(p, "§f§lHEAVENLY RESTRICTION");
            }
        }
    }

    /* ============================ helpers ============================ */
    private static boolean pay(ServerPlayer p, CursedEnergy.Data d, float cost) {
        if (CursedEnergy.spend(d, cost)) return true;
        msg(p, "§9Not enough cursed energy.");
        return false;
    }

    private static void slashParticles(ServerLevel level, ServerPlayer p, int count) {
        Vec3 front = p.getEyePosition().add(p.getViewVector(1f).scale(2));
        CombatUtil.particles(level, ParticleTypes.SWEEP_ATTACK, front, count, 0.6, 0.02);
        CombatUtil.particles(level, ParticleTypes.CRIT, front, count * 2, 0.6, 0.05);
    }

    private static void domainText(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text), true);
    }

    private static void msg(ServerPlayer p, String text) {
        p.displayClientMessage(Component.literal(text), true);
    }
}
