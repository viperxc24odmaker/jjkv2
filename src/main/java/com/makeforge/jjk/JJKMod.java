package com.makeforge.jjk;

import com.makeforge.jjk.ce.CursedEnergy;
import com.makeforge.jjk.net.AbilityPayload;
import com.makeforge.jjk.net.SyncPayload;
import com.makeforge.jjk.technique.Technique;
import com.makeforge.jjk.technique.TechniqueAbilities;
import com.makeforge.jjk.technique.CharacterMenu;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JJKMod implements ModInitializer {

    // --- tiny server-thread scheduler for delayed / repeated effects ---
    private record Task(Runnable run, int[] delay) {}
    private static final List<Task> TASKS = new ArrayList<>();

    /** Schedule work to run in {delayTicks} server ticks (server thread). */
    public static void schedule(int delayTicks, Runnable run) {
        synchronized (TASKS) { TASKS.add(new Task(run, new int[]{ delayTicks })); }
    }

    @Override
    public void onInitialize() {
        // register packets: C2S ability trigger, S2C HUD sync
        PayloadTypeRegistry.playC2S().register(AbilityPayload.TYPE, AbilityPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPayload.TYPE, SyncPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(AbilityPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            ((ServerLevel) player.level()).getServer().execute(() -> TechniqueAbilities.execute(player, payload.slot()));
        });

        // CE regen + run scheduled tasks each server tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean sync = server.getTickCount() % 5 == 0;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                CursedEnergy.Data d = CursedEnergy.server(p.getUUID());
                CursedEnergy.tick(d);
                if (sync) {
                    ServerPlayNetworking.send(p, new SyncPayload(
                            d.technique.ordinal(), d.ce, d.technique.maxCE));
                }
            }
            synchronized (TASKS) {
                Iterator<Task> it = TASKS.iterator();
                while (it.hasNext()) {
                    Task t = it.next();
                    if (t.delay()[0] <= 0) {
                        try { t.run().run(); } catch (Exception ignored) {}
                        it.remove();
                    } else {
                        t.delay()[0]--;
                    }
                }
            }
        });

        // show the character picker a moment after the player spawns in
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            schedule(20, () -> CharacterMenu.send(player));
        });

        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            dispatcher.register(Commands.literal("technique")
                    .executes(ctx -> {
                        ServerPlayer p = ctx.getSource().getPlayerOrException();
                        CharacterMenu.send(p);
                        return 1;
                    })
                    .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer p = ctx.getSource().getPlayerOrException();
                                String arg = StringArgumentType.getString(ctx, "name");
                                Technique t = Technique.byName(arg);
                                if (t == null) {
                                    p.displayClientMessage(Component.literal(
                                            "§cUnknown. Try: gojo, sukuna, yuji, megumi, nobara, toji"), false);
                                    return 0;
                                }
                                CursedEnergy.setTechnique(CursedEnergy.server(p.getUUID()), t);
                                p.displayClientMessage(Component.literal(
                                        "§bTechnique set: §f" + t.display), false);
                                return 1;
                            })));

            dispatcher.register(Commands.literal("ce").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                CursedEnergy.Data d = CursedEnergy.server(p.getUUID());
                p.displayClientMessage(Component.literal(String.format(
                        "§9CE: §f%.0f/%.0f  §7(%s)", d.ce, d.technique.maxCE, d.technique.display)), false);
                return 1;
            }));
        });
    }
}
