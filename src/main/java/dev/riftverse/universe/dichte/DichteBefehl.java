package dev.riftverse.universe.dichte;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.riftverse.Riftverse;
import dev.riftverse.portal.Heimanker;
import dev.riftverse.portal.RiftPortalEntity;
import dev.riftverse.registry.ModConditions;
import dev.riftverse.universe.Einzigartig;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Befehle für die Dichte-Testwelt (Meilenstein 11, Berechtigungsstufe 2, nur im Entwicklungsmodus):
 * <ul>
 *     <li>{@code /riftverse dichtetest} – Rift in die Dichteprobe</li>
 *     <li>{@code /riftverse dichtetest messen [anzahl]} – misst, wie lange das Füllen eines Chunks in der Dichteprobe
 *     und in der Vanilla-Oberwelt dauert (Entscheidung M11-06)</li>
 * </ul>
 * Gemessen wird nur {@code fillFromNoise} auf frischen Chunks, die nie in die Welt kommen. Bei Riftverse ist darin
 * schon die Oberfläche und die Deko enthalten, bei Vanilla nur das Gelände; der Vergleich ist also eher streng.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class DichteBefehl {
    private DichteBefehl() {
    }

    /** Ergebnis einer Messung in Millisekunden pro Chunk; {@code vanillaMs} ist NaN ohne Vanilla-Oberwelt. */
    public record Messung(int chunks, double dichteMs, double vanillaMs) {
        public boolean schneller() {
            return Double.isNaN(vanillaMs) || dichteMs <= vanillaMs;
        }
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("riftverse")
                .then(Commands.literal("dichtetest")
                        .requires(src -> src.hasPermission(2))
                        .executes(DichteBefehl::oeffnen)
                        .then(Commands.literal("messen")
                                .executes(ctx -> messen(ctx, 64))
                                .then(Commands.argument("anzahl", IntegerArgumentType.integer(8, 512))
                                        .executes(ctx -> messen(ctx, IntegerArgumentType.getInteger(ctx, "anzahl")))))));
    }

    @Nullable
    private static ServerLevel testwelt(MinecraftServer server) {
        if (!ModConditions.entwicklung()) return null;
        return server.getLevel(UniverseType.TEST_DICHTE.dimension());
    }

    private static int oeffnen(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (testwelt(player.server) == null) {
            ctx.getSource().sendFailure(Component.translatable("message.riftverse.testwelt.aus"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        if (Multiverse.at(level, player.blockPosition()) == null) Heimanker.set(player, level.dimension(), player.position());
        Vec3 spot = player.position().add(Vec3.directionFromRotation(0f, player.getYRot()).scale(2.5));
        RiftPortalEntity portal = RiftPortalEntity.create(level, spot, player.getYRot() + 180f, RiftPortalEntity.STYLE_RANDOM, 600);
        portal.setUniverseTarget(UniverseType.TEST_DICHTE.dimension(), Einzigartig.id(UniverseType.TEST_DICHTE));
        portal.setSpawnReturn(false);
        level.addFreshEntity(portal);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.riftverse.dichtetest.offen").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int messen(CommandContext<CommandSourceStack> ctx, int anzahl) {
        MinecraftServer server = ctx.getSource().getServer();
        if (testwelt(server) == null) {
            ctx.getSource().sendFailure(Component.translatable("message.riftverse.testwelt.aus"));
            return 0;
        }
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.translatable("message.riftverse.dichtetest.messen", anzahl), false);
        messenImHintergrund(server, anzahl).thenAccept(m -> {
            if (m != null) server.execute(() -> melden(src, m));
        });
        return 1;
    }

    private static void melden(CommandSourceStack src, Messung m) {
        String dichte = String.format(Locale.GERMAN, "%.2f", m.dichteMs());
        if (Double.isNaN(m.vanillaMs())) {
            src.sendSuccess(() -> Component.translatable("message.riftverse.dichtetest.kein_vanilla", dichte), false);
            return;
        }
        String vanilla = String.format(Locale.GERMAN, "%.2f", m.vanillaMs());
        Component urteil = Component.translatable(m.schneller() ? "message.riftverse.dichtetest.schneller" : "message.riftverse.dichtetest.langsamer");
        src.sendSuccess(() -> Component.translatable("message.riftverse.dichtetest.ergebnis", dichte, vanilla, urteil)
                .withStyle(m.schneller() ? ChatFormatting.GREEN : ChatFormatting.GOLD), false);
    }

    /**
     * Misst in einem eigenen Thread. Der Server-Thread darf dabei nicht warten: Vanilla füllt Chunks über den
     * Hintergrund-Executor, und der braucht einen tickenden Server.
     */
    public static CompletableFuture<Messung> messenImHintergrund(MinecraftServer server, int anzahl) {
        CompletableFuture<Messung> f = new CompletableFuture<>();
        Thread t = new Thread(() -> {
            try {
                f.complete(messen(server, anzahl));
            } catch (Throwable e) {
                Riftverse.LOGGER.error("Dichtemessung fehlgeschlagen", e);
                f.completeExceptionally(e);
            }
        }, "Riftverse-Dichtemessung");
        t.setDaemon(true);
        t.start();
        return f;
    }

    /** Misst {@code anzahl} Chunks in der Dichteprobe und in der Oberwelt; null ohne Testwelt. Nie auf dem Server-Thread. */
    @Nullable
    private static Messung messen(MinecraftServer server, int anzahl) {
        ServerLevel test = testwelt(server);
        if (test == null) return null;
        int c = Multiverse.cellCenter(0) >> 4;
        // Rund um den schwebenden Testberg: Boden, Überhänge und Schwebendes in jedem Chunk
        double dichte = zeit(test, anzahl, c - 4, c - 4);
        ServerLevel welt = server.overworld();
        double vanilla = welt.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator
                ? zeit(welt, anzahl, 4000, 4000) : Double.NaN;
        return new Messung(anzahl, dichte, vanilla);
    }

    private static double zeit(ServerLevel level, int anzahl, int cx0, int cz0) {
        ChunkGenerator gen = level.getChunkSource().getGenerator();
        RandomState rs = level.getChunkSource().randomState();
        StructureManager sm = level.structureManager();
        Registry<Biome> biome = level.registryAccess().registryOrThrow(Registries.BIOME);
        // Aufwärmen, damit der JIT beide Generatoren gleich behandelt
        for (int i = 0; i < 8; i++) fuellen(level, gen, rs, sm, biome, new ChunkPos(cx0 - 20 + i, cz0 - 20));
        long summe = 0;
        for (int i = 0; i < anzahl; i++) {
            summe += fuellen(level, gen, rs, sm, biome, new ChunkPos(cx0 + i % 8, cz0 + i / 8));
        }
        return summe / 1e6 / anzahl;
    }

    /** Füllt einen frischen Chunk, der nie in die Welt kommt; gibt die Dauer von fillFromNoise in ns zurück. */
    private static long fuellen(ServerLevel level, ChunkGenerator gen, RandomState rs, StructureManager sm,
                                Registry<Biome> biome, ChunkPos pos) {
        ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level, biome, null);
        chunk.fillBiomesFromNoise(gen.getBiomeSource(), rs.sampler());
        long t0 = System.nanoTime();
        gen.fillFromNoise(Blender.empty(), rs, sm, chunk).join();
        return System.nanoTime() - t0;
    }
}
