package dev.riftverse.story;

import dev.riftverse.universe.UniverseId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Weltweiter Speicher: Welche Universen haben welche Geschichte erlebt? */
public class MultiverseData extends SavedData {
    public static final int STATE_NONE = 0, STATE_ACTIVE = 1, STATE_DEFEATED = 2, STATE_SPARED = 3;
    private static final String NAME = "riftverse_multiverse";

    public static final class Record {
        public int state = STATE_NONE;
        public String bossName = "";
        public String epithet = "";
        @Nullable public UUID bossId;
        @Nullable public BlockPos arena;
        public int visits;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("State", state);
            t.putString("Boss", bossName);
            t.putString("Epithet", epithet);
            t.putInt("Visits", visits);
            if (bossId != null) t.putUUID("BossId", bossId);
            if (arena != null) t.putLong("Arena", arena.asLong());
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
            return r;
        }
    }

    private final Map<String, Record> records = new HashMap<>();
    private int liberated;

    public static MultiverseData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MultiverseData::new, MultiverseData::load, null), NAME);
    }

    public Record record(UniverseId id) {
        return records.computeIfAbsent(id.key(), k -> new Record());
    }

    /** Anzahl befreiter Universen – Bosse werden mit der Zeit stärker. */
    public int liberated() {
        return liberated;
    }

    public void markLiberated() {
        liberated++;
        setDirty();
    }

    private static MultiverseData load(CompoundTag tag, HolderLookup.Provider registries) {
        MultiverseData data = new MultiverseData();
        data.liberated = tag.getInt("Liberated");
        CompoundTag recs = tag.getCompound("Universes");
        for (String key : recs.getAllKeys()) {
            data.records.put(key, Record.load(recs.getCompound(key)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Liberated", liberated);
        CompoundTag recs = new CompoundTag();
        records.forEach((k, v) -> {
            if (v.state != STATE_NONE || v.visits > 0) recs.put(k, v.save());
        });
        tag.put("Universes", recs);
        return tag;
    }
}
