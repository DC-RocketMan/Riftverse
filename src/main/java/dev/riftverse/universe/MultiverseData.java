package dev.riftverse.universe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Weltweiter Speicher des Multiversums. Pro Universum steht hier nur, was sich nicht aus dem Seed ergibt:
 * Story-Daten, wann es zuletzt besucht wurde, was dort gebaut wurde und ob ein Rift-Anker es hält.
 * Typ, Seed und Modifikatoren stecken bereits im Schlüssel ({@link UniverseId}).
 */
public class MultiverseData extends SavedData {
    public static final int STATE_NONE = 0, STATE_ACTIVE = 1, STATE_DEFEATED = 2, STATE_SPARED = 3;
    public static final String NAME = "riftverse_multiverse";

    public static final class Record {
        // --- Story (Phase 3) ---
        public int state = STATE_NONE;
        public String bossName = "";
        public String epithet = "";
        @Nullable public UUID bossId;
        @Nullable public BlockPos arena;
        public int visits;

        // --- Aufräumen ---
        /** Spielzeit (Overworld) des letzten Besuchs, -1 = nie. */
        public long lastVisit = -1;
        /** Dauerhaft platzierte Blöcke (ohne Fackeln und Gerüste). */
        public final Set<Long> placed = new HashSet<>();
        /** Einmal mehr als die Schwelle verbaut: bleibt dauerhaft „bebaut“. */
        public boolean built;
        /** Platzierte Block-Entities (Truhe, Maschine, Bett). */
        public final Set<Long> blockEntities = new HashSet<>();
        /** Rift-Anker (später auch Rift-Kabelknoten). */
        public final Set<Long> anchors = new HashSet<>();
        /** Gelände wurde zurückgesetzt und seitdem nicht wieder betreten. */
        public boolean cleared;
        public int resets;

        public boolean isAnchored() {
            return !anchors.isEmpty();
        }

        public boolean isBuilt(int threshold) {
            return built || !blockEntities.isEmpty() || placed.size() > threshold;
        }

        boolean worthSaving() {
            return state != STATE_NONE || visits > 0 || lastVisit >= 0 || !anchors.isEmpty() || resets > 0;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("State", state);
            t.putString("Boss", bossName);
            t.putString("Epithet", epithet);
            t.putInt("Visits", visits);
            if (bossId != null) t.putUUID("BossId", bossId);
            if (arena != null) t.putLong("Arena", arena.asLong());
            t.putLong("LastVisit", lastVisit);
            t.put("Placed", longs(placed));
            t.putBoolean("Built", built);
            t.put("BlockEntities", longs(blockEntities));
            t.put("Anchors", longs(anchors));
            t.putBoolean("Cleared", cleared);
            t.putInt("Resets", resets);
            return t;
        }

        static Record load(CompoundTag t) {
            Record r = new Record();
            r.state = t.getInt("State");
            r.bossName = t.getString("Boss");
            r.epithet = t.getString("Epithet");
            r.visits = t.getInt("Visits");
            if (t.hasUUID("BossId")) r.bossId = t.getUUID("BossId");
            if (t.contains("Arena")) r.arena = BlockPos.of(t.getLong("Arena"));
            r.lastVisit = t.contains("LastVisit") ? t.getLong("LastVisit") : -1;
            readLongs(t, "Placed", r.placed);
            r.built = t.getBoolean("Built");
            readLongs(t, "BlockEntities", r.blockEntities);
            readLongs(t, "Anchors", r.anchors);
            r.cleared = t.getBoolean("Cleared");
            r.resets = t.getInt("Resets");
            return r;
        }

        /** Nach dem Löschen des Geländes: Bauzähler leeren, Story und Besuche bleiben. */
        void terrainReset() {
            placed.clear();
            blockEntities.clear();
            built = false;
            cleared = true;
            resets++;
        }

        private static LongArrayTag longs(Set<Long> set) {
            return new LongArrayTag(set.stream().mapToLong(Long::longValue).toArray());
        }

        private static void readLongs(CompoundTag t, String key, Set<Long> into) {
            if (t.contains(key, Tag.TAG_LONG_ARRAY)) {
                for (long l : t.getLongArray(key)) into.add(l);
            }
        }
    }

    private final Map<String, Record> records = new HashMap<>();
    /** Weltenformer: wie oft jede Bauart schon gezüchtet wurde (siehe {@link Zucht#key()}). */
    private final Map<String, Integer> zuchten = new HashMap<>();
    private int liberated;
    private long gameTime;

    public static MultiverseData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MultiverseData::new, MultiverseData::load, null), NAME);
    }

    public Record record(UniverseId id) {
        return records.computeIfAbsent(id.key(), k -> new Record());
    }

    @Nullable
    public Record existing(UniverseId id) {
        return records.get(id.key());
    }

    public void forEach(BiConsumer<UniverseId, Record> action) {
        records.forEach((key, rec) -> {
            UniverseId id = UniverseId.fromKey(key);
            if (id != null) action.accept(id, rec);
        });
    }

    /** Nächste laufende Nummer für ein Universum dieser Bauart. */
    public int nextZucht(Zucht zucht) {
        int n = zuchten.getOrDefault(zucht.key(), 0);
        zuchten.put(zucht.key(), n + 1);
        setDirty();
        return n;
    }

    /** Anzahl befreiter Universen – Bosse werden mit der Zeit stärker. */
    public int liberated() {
        return liberated;
    }

    public void markLiberated() {
        liberated++;
        setDirty();
    }

    /** Spielzeit beim letzten Speichern; das Aufräumen beim Serverstart rechnet damit. */
    public long gameTime() {
        return gameTime;
    }

    public void setGameTime(long time) {
        if (time != gameTime) {
            gameTime = time;
            setDirty();
        }
    }

    // ------------------------------------------------------------------ Besuche und Bauten

    public void touch(UniverseId id, long now) {
        Record r = record(id);
        r.lastVisit = now;
        r.cleared = false;
        gameTime = Math.max(gameTime, now);
        setDirty();
    }

    public void blockPlaced(UniverseId id, BlockPos pos, boolean blockEntity, boolean counts, int threshold) {
        Record r = record(id);
        long key = pos.asLong();
        if (blockEntity) r.blockEntities.add(key);
        if (counts && !r.built) {
            r.placed.add(key);
            if (r.placed.size() > threshold) {
                r.built = true;
                r.placed.clear();
            }
        }
        setDirty();
    }

    public void blockRemoved(UniverseId id, BlockPos pos) {
        Record r = existing(id);
        if (r == null) return;
        long key = pos.asLong();
        if (r.blockEntities.remove(key) | r.placed.remove(key)) setDirty();
    }

    public void anchorChanged(UniverseId id, BlockPos pos, boolean present) {
        Record r = record(id);
        boolean changed = present ? r.anchors.add(pos.asLong()) : r.anchors.remove(pos.asLong());
        if (changed) setDirty();
    }

    // ------------------------------------------------------------------ Speichern

    /** Auch ohne laufende Welt lesbar (Aufräumen beim Serverstart). */
    public static MultiverseData fromTag(CompoundTag tag) {
        MultiverseData data = new MultiverseData();
        data.liberated = tag.getInt("Liberated");
        data.gameTime = tag.getLong("GameTime");
        CompoundTag z = tag.getCompound("Zuchten");
        for (String key : z.getAllKeys()) data.zuchten.put(key, z.getInt(key));
        CompoundTag recs = tag.getCompound("Universes");
        for (String key : recs.getAllKeys()) {
            if (UniverseId.fromKey(key) == null) continue;   // Einträge aus dem Prototyp
            data.records.put(key, Record.load(recs.getCompound(key)));
        }
        return data;
    }

    private static MultiverseData load(CompoundTag tag, HolderLookup.Provider registries) {
        return fromTag(tag);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Liberated", liberated);
        tag.putLong("GameTime", gameTime);
        CompoundTag recs = new CompoundTag();
        records.forEach((k, v) -> {
            if (v.worthSaving()) recs.put(k, v.save());
        });
        tag.put("Universes", recs);
        CompoundTag z = new CompoundTag();
        zuchten.forEach(z::putInt);
        tag.put("Zuchten", z);
        return tag;
    }
}
