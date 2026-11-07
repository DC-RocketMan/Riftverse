package dev.riftverse.universe;

import dev.riftverse.Config;
import dev.riftverse.Riftverse;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Aufräumen der Universen: Universen ohne Bauten und ohne Rift-Anker, die {@code tage} Ingame-Tage nicht
 * besucht wurden, verlieren beim nächsten Serverstart ihr Gelände. Weil das Gelände aus dem Seed entsteht,
 * sieht ein zurückgesetztes Universum beim nächsten Besuch gleich aus, nur Erze und abgebaute Blöcke sind
 * wieder da.
 * <p>
 * Gelöscht wird nur beim Serverstart, weil Region-Dateien im laufenden Betrieb offen sind.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class UniverseCleanup {
    /** Blöcke, die nicht als Bau zählen (Fackeln, Gerüste, Leitern …). */
    public static final TagKey<Block> ZAEHLT_NICHT = TagKey.create(Registries.BLOCK, Riftverse.id("zaehlt_nicht_als_bau"));

    public static long maxAgeTicks() {
        return Config.CLEANUP_DAYS.get() * 24000L;
    }

    /** Soll dieses Universum zurückgesetzt werden? */
    public static boolean shouldReset(MultiverseData.Record r, long now, long maxAge, int threshold) {
        return r.lastVisit >= 0 && !r.cleared && !r.isAnchored() && !r.isBuilt(threshold) && now - r.lastVisit >= maxAge;
    }

    /** Tage bis zum Zurücksetzen, gezählt ab dem letzten Besuch; -1 = bleibt (bebaut oder verankert). */
    public static int daysLeft(MultiverseData.Record r, long now) {
        if (r.isAnchored() || r.isBuilt(Config.BUILD_THRESHOLD.get())) return -1;
        long left = Math.max(0, r.lastVisit + maxAgeTicks() - now);
        return (int) Math.ceil(left / 24000.0);
    }

    // ------------------------------------------------------------------ Serverstart

    @SubscribeEvent
    public static void onAboutToStart(ServerAboutToStartEvent event) {
        if (!Config.CLEANUP.get()) return;
        Path root = event.getServer().getWorldPath(LevelResource.ROOT);
        Path file = root.resolve("data").resolve(MultiverseData.NAME + ".dat");
        if (!Files.isRegularFile(file)) return;
        try {
            CompoundTag full = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            MultiverseData data = MultiverseData.fromTag(full.getCompound("data"));
            long now = data.gameTime();
            long maxAge = maxAgeTicks();
            int threshold = Config.BUILD_THRESHOLD.get();
            List<String> done = new ArrayList<>();
            data.forEach((id, rec) -> {
                if (!shouldReset(rec, now, maxAge, threshold)) return;
                int files = deleteTerrain(root, id);
                rec.terrainReset();
                done.add(id.key() + " (" + files + " Dateien)");
            });
            if (done.isEmpty()) return;
            full.put("data", data.save(new CompoundTag(), null));
            NbtIo.writeCompressed(full, file);
            Riftverse.LOGGER.info("Riftverse: {} Universen zurückgesetzt: {}", done.size(), done);
        } catch (IOException | RuntimeException e) {
            Riftverse.LOGGER.warn("Riftverse: Aufräumen der Universen übersprungen", e);
        }
    }

    /** Löscht die 8 × 8 Region-Dateien (Blöcke, Entities, POI) der Zelle. */
    static int deleteTerrain(Path root, UniverseId id) {
        Path dim = DimensionType.getStorageFolder(id.type().dimension(), root);
        int count = 0;
        for (String folder : new String[]{"region", "entities", "poi"}) {
            Path dir = dim.resolve(folder);
            if (!Files.isDirectory(dir)) continue;
            for (int i = 0; i < Multiverse.REGIONS_PER_CELL; i++) {
                for (int j = 0; j < Multiverse.REGIONS_PER_CELL; j++) {
                    int rx = id.cellX() * Multiverse.REGIONS_PER_CELL + i;
                    int rz = id.cellZ() * Multiverse.REGIONS_PER_CELL + j;
                    try {
                        if (Files.deleteIfExists(dir.resolve("r." + rx + "." + rz + ".mca"))) count++;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }
        return count;
    }

    // ------------------------------------------------------------------ Besuche und Bauten zählen

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0) return;
        UniverseId id = Multiverse.at(player.level(), player.blockPosition());
        if (id == null || player.isSpectator()) return;
        MultiverseData.get(player.server).touch(id, player.server.overworld().getGameTime());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 200 != 0) return;
        MultiverseData.get(event.getServer()).setGameTime(event.getServer().overworld().getGameTime());
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        UniverseId id = Multiverse.at(level, event.getPos());
        if (id == null) return;
        boolean be = event.getPlacedBlock().hasBlockEntity();
        boolean counts = !event.getPlacedBlock().is(ZAEHLT_NICHT);
        MultiverseData.get(level.getServer()).blockPlaced(id, event.getPos(), be, counts, Config.BUILD_THRESHOLD.get());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        UniverseId id = Multiverse.at(level, event.getPos());
        if (id != null) MultiverseData.get(level.getServer()).blockRemoved(id, event.getPos());
    }

    /** Für Rift-Anker und später Rift-Kabelknoten. */
    public static void anchorChanged(Level level, BlockPos pos, boolean present) {
        if (!(level instanceof ServerLevel sl)) return;
        UniverseId id = Multiverse.at(level, pos);
        if (id != null) MultiverseData.get(sl.getServer()).anchorChanged(id, pos, present);
    }

    private UniverseCleanup() {}
}
