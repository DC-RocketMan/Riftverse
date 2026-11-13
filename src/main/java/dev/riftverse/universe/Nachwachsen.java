package dev.riftverse.universe;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Organische Welt: Abgebaute Blöcke des Geländes (Tag {@code riftverse:waechst_nach}) wachsen nach ein bis zwei
 * Minuten wieder nach, sofern die Stelle frei ist. Die Liste überlebt einen Neustart (SavedData pro Dimension).
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class Nachwachsen extends SavedData {
    public static final String NAME = "riftverse_nachwachsen";
    public static final int MIN_DELAY = 1200, RANDOM_DELAY = 1200;

    private record Entry(BlockPos pos, BlockState state, long due) {}

    private final List<Entry> entries = new ArrayList<>();

    public static Nachwachsen get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Nachwachsen::new, Nachwachsen::load, null), NAME);
    }

    public int size() {
        return entries.size();
    }

    public void schedule(BlockPos pos, BlockState state, long due) {
        if (entries.size() >= 20_000) return;
        entries.add(new Entry(pos.immutable(), state, due));
        setDirty();
    }

    // ------------------------------------------------------------------ Events

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        UniverseProfile profile = Multiverse.profileAt(level, event.getPos());
        if (profile == null || profile.look != UniverseType.ORGANISCH) return;
        BlockState state = event.getState();
        if (!state.is(ModTags.Blocks.WAECHST_NACH)) return;
        long due = level.getGameTime() + MIN_DELAY + level.random.nextInt(RANDOM_DELAY);
        get(level).schedule(event.getPos(), state, due);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
        UniverseType type = UniverseType.of(level.dimension());
        if (type != UniverseType.ORGANISCH && type != UniverseType.GEZUECHTET) return;
        Nachwachsen data = get(level);
        if (data.entries.isEmpty()) return;
        long now = level.getGameTime();
        boolean changed = false;
        Iterator<Entry> it = data.entries.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.due() > now || !level.isLoaded(e.pos())) continue;
            it.remove();
            changed = true;
            if (!level.getBlockState(e.pos()).isAir() || !level.getEntities(null, new net.minecraft.world.phys.AABB(e.pos())).isEmpty()) continue;
            level.setBlock(e.pos(), e.state(), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, e.pos().getX() + 0.5, e.pos().getY() + 0.5, e.pos().getZ() + 0.5,
                    6, 0.3, 0.3, 0.3, 0.0);
            level.playSound(null, e.pos(), SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.5f, 0.6f);
        }
        if (changed) data.setDirty();
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : entries) {
            CompoundTag t = new CompoundTag();
            t.putLong("Pos", e.pos().asLong());
            t.put("State", NbtUtils.writeBlockState(e.state()));
            t.putLong("Due", e.due());
            list.add(t);
        }
        tag.put("Entries", list);
        return tag;
    }

    public static Nachwachsen load(CompoundTag tag, HolderLookup.Provider registries) {
        Nachwachsen data = new Nachwachsen();
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            BlockState state = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), t.getCompound("State"));
            if (state.isAir()) continue;
            data.entries.add(new Entry(BlockPos.of(t.getLong("Pos")), state, t.getLong("Due")));
        }
        return data;
    }
}
