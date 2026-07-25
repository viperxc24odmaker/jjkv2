package com.makeforge.jjk.client;

import com.makeforge.jjk.net.AbilityPayload;
import com.makeforge.jjk.net.SyncPayload;
import com.makeforge.jjk.technique.Technique;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class JJKClient implements ClientModInitializer {

    // client-side mirror of the server state, fed by SyncPayload
    public static volatile Technique technique = Technique.NONE;
    public static volatile float ce = 0f;
    public static volatile float maxCe = 100f;

    private static KeyMapping keyPrimary;
    private static KeyMapping keySecondary;
    private static KeyMapping keyUltimate;

    @Override
    public void onInitializeClient() {
        // 1.21.11: key categories are typed objects, not strings.
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("jjk", "main"));

        keyPrimary   = reg("key.jjk.primary",   GLFW.GLFW_KEY_G, category);
        keySecondary = reg("key.jjk.secondary", GLFW.GLFW_KEY_H, category);
        keyUltimate  = reg("key.jjk.ultimate",  GLFW.GLFW_KEY_J, category);

        // receive HUD sync from server
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) -> {
            technique = Technique.values()[Math.floorMod(payload.techniqueOrdinal(), Technique.values().length)];
            ce = payload.ce();
            maxCe = payload.maxCe();
        });

        // key handling -> send ability packets
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (keyPrimary.consumeClick())   ClientPlayNetworking.send(new AbilityPayload(0));
            while (keySecondary.consumeClick()) ClientPlayNetworking.send(new AbilityPayload(1));
            while (keyUltimate.consumeClick())  ClientPlayNetworking.send(new AbilityPayload(2));
        });

        HudRenderCallback.EVENT.register(this::renderHud);
    }

    private static KeyMapping reg(String id, int code, KeyMapping.Category cat) {
        KeyMapping k = new KeyMapping(id, InputConstants.Type.KEYSYM, code, cat);
        KeyBindingHelper.registerKeyBinding(k);
        return k;
    }

    private void renderHud(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (technique == Technique.NONE) return;

        int x = 8;
        int y = g.guiHeight() - 46;
        int w = 120;
        int h = 8;

        // label
        g.drawString(mc.font, "§b" + technique.display, x, y - 11, 0xFFFFFF, true);

        // bar background + fill
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xAA101018);
        g.fill(x, y, x + w, y + h, 0xFF23252E);
        float pct = maxCe <= 0 ? 0f : Math.max(0f, Math.min(1f, ce / maxCe));
        int fill = (int) (w * pct);
        // Toji (no CE) shows a red "restriction" bar instead of blue
        int color = technique == Technique.TOJI ? 0xFFE04545 : 0xFF3AA0FF;
        if (fill > 0) g.fill(x, y, x + fill, y + h, color);

        // numeric readout
        String txt = technique == Technique.TOJI
                ? "HEAVENLY RESTRICTION"
                : String.format("%.0f / %.0f", ce, maxCe);
        g.drawString(mc.font, txt, x + w + 6, y, 0xC8D0FF, true);
    }
}
