package com.makeforge.jjk.technique;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * A clickable "choose your character" menu, rendered in chat (no GUI Screen).
 * Shown a moment after the player spawns in. Each entry runs /technique on click.
 *
 * Deliberately not a custom Screen: the 1.21.11 render pipeline rewrite makes a
 * bespoke Screen the highest-risk thing to add, and a chat menu can't crash the
 * client. A full GUI can come once the base build is confirmed green.
 */
public final class CharacterMenu {
    private CharacterMenu() {}

    public static void send(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("  §5§l✦ CHOOSE YOUR CURSED TECHNIQUE ✦"));
        player.sendSystemMessage(Component.literal("  §7Click a name to bind it. Keys: §fG §7/ §fH §7/ §fJ"));
        player.sendSystemMessage(Component.literal(""));

        for (Technique t : Technique.values()) {
            if (t == Technique.NONE) continue;
            player.sendSystemMessage(row(t));
        }
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("  §8(you can re-pick anytime with §7/technique <name>§8)"));
    }

    private static MutableComponent row(Technique t) {
        ChatFormatting color = color(t);
        MutableComponent button = Component.literal("  ▶ " + t.display)
                .withStyle(style -> style
                        .withColor(color)
                        .withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/technique " + t.name().toLowerCase()))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("§fPlay as " + t.display + "\n§7" + blurb(t)
                                        + "\n§8Click to select"))));
        return button.append(Component.literal("  §8— " + blurb(t)).withStyle(s -> s.withBold(false)));
    }

    private static ChatFormatting color(Technique t) {
        return switch (t) {
            case GOJO   -> ChatFormatting.AQUA;
            case SUKUNA -> ChatFormatting.DARK_RED;
            case YUJI   -> ChatFormatting.GOLD;
            case MEGUMI -> ChatFormatting.DARK_GRAY;
            case NOBARA -> ChatFormatting.YELLOW;
            case TOJI   -> ChatFormatting.WHITE;
            default     -> ChatFormatting.GRAY;
        };
    }

    private static String blurb(Technique t) {
        return switch (t) {
            case GOJO   -> "Blue / Red / Hollow Purple";
            case SUKUNA -> "Dismantle / Cleave / Malevolent Shrine";
            case YUJI   -> "Divergent Fist / Black Flash — cheap, physical";
            case MEGUMI -> "Divine Dogs / Nue / Shadow Garden";
            case NOBARA -> "Hairpin / Resonance / Straw Doll";
            case TOJI   -> "Heavenly Restriction — zero CE, pure body";
            default     -> "";
        };
    }
}
