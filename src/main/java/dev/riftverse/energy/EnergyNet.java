package dev.riftverse.energy;

import dev.riftverse.cable.CableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Das Riftverse-Netz einer Dimension.
 * <p>
 * Intern ein Graph aus Quellen, Leitungen und Verbrauchern. Er wird nach jeder Blockänderung
 * neu aufgebaut; die Verluste pro Pfad werden dabei zwischengespeichert (wie bei IC2).
 * Am Ende jedes Server-Ticks verteilt jedes Teilnetz die Pakete seiner Quellen.
 */
public final class EnergyNet {
    private static final Map<ServerLevel, EnergyNet> NETS = new HashMap<>();

    private final ServerLevel level;
    private final Set<BlockPos> nodes = new HashSet<>();
    private final List<EnergyNetwork> networks = new ArrayList<>();
    private final Map<BlockPos, EnergyNetwork> byPos = new HashMap<>();
    private final List<Runnable> pending = new ArrayList<>();
    /** Chunks, deren Laden/Entladen ein Teilnetz verändern kann. Wird beim Neuaufbau ersetzt. */
    private volatile Set<Long> watchedChunks = Set.of();
    private boolean dirty = true;

    private EnergyNet(ServerLevel level) {
        this.level = level;
    }

    // ------------------------------------------------------------------ Zugriff

    public static synchronized EnergyNet get(ServerLevel level) {
        return NETS.computeIfAbsent(level, EnergyNet::new);
    }

    @Nullable
    private static synchronized EnergyNet existing(Level level) {
        return level instanceof ServerLevel sl ? NETS.get(sl) : null;
    }

    /** Eine Maschine, ein Generator oder ein Ein-/Ausspeiser ist geladen worden. */
    public static void addNode(Level level, BlockPos pos) {
        if (level instanceof ServerLevel sl) {
            EnergyNet net = get(sl);
            net.nodes.add(pos.immutable());
            net.dirty = true;
        }
    }

    public static void removeNode(Level level, BlockPos pos) {
        EnergyNet net = existing(level);
        if (net != null) {
            net.nodes.remove(pos);
            net.dirty = true;
        }
    }

    /** Leitung gesetzt, entfernt oder umgefärbt. */
    public static void markDirty(Level level) {
        if (level instanceof ServerLevel sl) get(sl).dirty = true;
    }

    /** Fließt in der Leitung an {@code pos} gerade Strom? (für den Schlag an blanken Kabeln) */
    public static boolean isLive(Level level, BlockPos pos) {
        EnergyNet net = existing(level);
        if (net == null) return false;
        EnergyNetwork n = net.byPos.get(pos);
        return n != null && level.getGameTime() - n.lastActive() <= 2;
    }

    /** Kennzahlen des Teilnetzes an {@code pos}, oder null. */
    @Nullable
    public static EnergyNetwork.Info info(ServerLevel level, BlockPos pos) {
        EnergyNet net = get(level);
        if (net.dirty) net.rebuild();
        EnergyNetwork n = net.byPos.get(pos);
        return n == null ? null : n.info();
    }

    // ------------------------------------------------------------------ Events (NeoForge.EVENT_BUS)

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            EnergyNet net = existing(sl);
            if (net != null) net.tick();
        }
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        chunkChanged(event);
    }

    public static void onChunkUnload(ChunkEvent.Unload event) {
        chunkChanged(event);
    }

    private static void chunkChanged(ChunkEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        EnergyNet net = existing(sl);
        if (net != null && net.watchedChunks.contains(event.getChunk().getPos().toLong())) {
            net.dirty = true;
        }
    }

    public static synchronized void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel sl) NETS.remove(sl);
    }

    public static synchronized void onServerStopped(ServerStoppedEvent event) {
        NETS.clear();
    }

    // ------------------------------------------------------------------ Tick

    private void tick() {
        if (dirty) rebuild();
        long time = level.getGameTime();
        for (EnergyNetwork network : networks) {
            if (!network.tick(level, time, pending)) dirty = true;
        }
        if (!pending.isEmpty()) {
            List<Runnable> run = new ArrayList<>(pending);
            pending.clear();
            run.forEach(Runnable::run);
        }
    }

    // ------------------------------------------------------------------ Graph aufbauen

    enum Kind { NONE, CABLE, NODE }

    static Kind kindAt(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CableBlock) return Kind.CABLE;
        if (state.getBlock() instanceof RiftEnergyConnector) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null && !be.isRemoved() && (be instanceof RiftEnergySource || be instanceof RiftEnergySink)) {
                return Kind.NODE;
            }
        }
        return Kind.NONE;
    }

    /** Sind u und v (v liegt in Richtung d von u) im Riftverse-Netz verbunden? */
    static boolean linked(BlockState su, Kind ku, BlockState sv, Kind kv, Direction d) {
        if (ku == Kind.CABLE && kv == Kind.CABLE) return CableBlock.compatible(su, sv);
        boolean uOk = ku == Kind.CABLE || ((RiftEnergyConnector) su.getBlock()).canConnectEnergy(su, d);
        boolean vOk = kv == Kind.CABLE || ((RiftEnergyConnector) sv.getBlock()).canConnectEnergy(sv, d.getOpposite());
        return uOk && vOk;
    }

    private void rebuild() {
        dirty = false;
        networks.clear();
        byPos.clear();
        Set<Long> watched = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();

        nodes.removeIf(p -> !level.isLoaded(p) || kindAt(level, p, level.getBlockState(p)) != Kind.NODE);

        for (BlockPos start : new ArrayList<>(nodes)) {
            if (visited.contains(start)) continue;

            Map<BlockPos, Kind> kinds = new HashMap<>();
            Map<BlockPos, BlockState> states = new HashMap<>();
            Map<BlockPos, List<BlockPos>> adjacency = new HashMap<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();

            visited.add(start);
            kinds.put(start, Kind.NODE);
            states.put(start, level.getBlockState(start));
            queue.add(start);

            while (!queue.isEmpty()) {
                BlockPos u = queue.poll();
                Kind ku = kinds.get(u);
                BlockState su = states.get(u);
                watched.add(ChunkPos.asLong(u));

                for (Direction d : Direction.values()) {
                    BlockPos v = u.relative(d);
                    if (!level.isLoaded(v)) {
                        watched.add(ChunkPos.asLong(v));
                        continue;
                    }
                    BlockState sv = states.containsKey(v) ? states.get(v) : level.getBlockState(v);
                    Kind kv = kinds.containsKey(v) ? kinds.get(v) : kindAt(level, v, sv);
                    if (kv == Kind.NONE || !linked(su, ku, sv, kv, d)) continue;

                    adjacency.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
                    if (visited.add(v)) {
                        kinds.put(v, kv);
                        states.put(v, sv);
                        queue.add(v);
                    }
                }
            }

            EnergyNetwork network = new EnergyNetwork(level, kinds, states, adjacency);
            networks.add(network);
            for (BlockPos p : kinds.keySet()) byPos.put(p, network);
        }
        watchedChunks = Set.copyOf(watched);
    }
}
