package dev.riftverse.energy;

import dev.riftverse.Config;
import dev.riftverse.cable.CableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Ein zusammenhängendes Teilnetz: Quellen, Leitungen, Verbraucher.
 * <p>
 * Für jede Quelle sind die Pfade zu allen erreichbaren Verbrauchern mit ihrem Verlust
 * vorberechnet (Dijkstra über die Leitungen, Maschinen leiten nicht weiter).
 */
public final class EnergyNetwork {

    /**
     * Ein Pfad von einer Quelle zu einem Verbraucher.
     *
     * @param loss   Verlust in FE pro Paket (Summe der Leitungen)
     * @param burnAt burnAt[n] = erste Leitung ab der Quelle, die Stufe n nicht verträgt (oder null)
     */
    record Path(BlockPos sink, double loss, int cables, BlockPos[] burnAt) {
        @Nullable
        BlockPos burnFor(VoltageTier tier) {
            return burnAt[tier.level()];
        }
    }

    public record Info(int sources, int sinks, int cables, int lastTransferred, long lastActive) {}

    private record Entry(BlockPos pos, double dist) {}

    private final List<BlockPos> sources = new ArrayList<>();
    private final List<BlockPos> sinks = new ArrayList<>();
    private final Map<BlockPos, List<Path>> paths = new HashMap<>();
    private int cables;
    private long lastActive = Long.MIN_VALUE / 2;
    private int lastTransferred;
    private int rotation;

    EnergyNetwork(ServerLevel level, Map<BlockPos, EnergyNet.Kind> kinds, Map<BlockPos, BlockState> states,
                  Map<BlockPos, List<BlockPos>> adjacency) {
        Set<BlockPos> sinkSet = new HashSet<>();
        for (Map.Entry<BlockPos, EnergyNet.Kind> e : kinds.entrySet()) {
            if (e.getValue() == EnergyNet.Kind.CABLE) {
                cables++;
                continue;
            }
            BlockEntity be = level.getBlockEntity(e.getKey());
            if (be instanceof RiftEnergySource) sources.add(e.getKey());
            if (be instanceof RiftEnergySink) {
                sinks.add(e.getKey());
                sinkSet.add(e.getKey());
            }
        }
        // Feste Reihenfolge, damit sich das Netz nach jedem Neuaufbau gleich verhält
        Comparator<BlockPos> order = Comparator.comparingLong(BlockPos::asLong);
        sources.sort(order);
        sinks.sort(order);

        for (BlockPos source : sources) {
            paths.put(source, findPaths(source, kinds, states, adjacency, sinkSet));
        }
    }

    private static List<Path> findPaths(BlockPos source, Map<BlockPos, EnergyNet.Kind> kinds, Map<BlockPos, BlockState> states,
                                        Map<BlockPos, List<BlockPos>> adjacency, Set<BlockPos> sinkSet) {
        Map<BlockPos, Double> dist = new HashMap<>();
        Map<BlockPos, BlockPos> prev = new HashMap<>();
        PriorityQueue<Entry> queue = new PriorityQueue<>(Comparator.comparingDouble(Entry::dist));
        List<Path> result = new ArrayList<>();

        dist.put(source, 0.0);
        queue.add(new Entry(source, 0.0));
        while (!queue.isEmpty()) {
            Entry e = queue.poll();
            BlockPos u = e.pos();
            if (e.dist() > dist.getOrDefault(u, Double.MAX_VALUE)) continue;

            // Andere Maschinen sind Endpunkte: Strom fließt nicht durch sie hindurch.
            if (!u.equals(source) && kinds.get(u) == EnergyNet.Kind.NODE) {
                if (sinkSet.contains(u)) result.add(makePath(source, u, e.dist(), prev, kinds, states));
                continue;
            }
            for (BlockPos v : adjacency.getOrDefault(u, List.of())) {
                double cost = kinds.get(v) == EnergyNet.Kind.CABLE
                        ? ((CableBlock) states.get(v).getBlock()).lossPerBlock() : 0.0;
                double nd = e.dist() + cost;
                if (nd < dist.getOrDefault(v, Double.MAX_VALUE)) {
                    dist.put(v, nd);
                    prev.put(v, u);
                    queue.add(new Entry(v, nd));
                }
            }
        }
        result.sort(Comparator.comparingDouble(Path::loss));
        return result;
    }

    private static Path makePath(BlockPos source, BlockPos sink, double loss, Map<BlockPos, BlockPos> prev,
                                 Map<BlockPos, EnergyNet.Kind> kinds, Map<BlockPos, BlockState> states) {
        List<BlockPos> cableList = new ArrayList<>();
        BlockPos p = prev.get(sink);
        while (p != null && !p.equals(source)) {
            if (kinds.get(p) == EnergyNet.Kind.CABLE) cableList.add(p);
            p = prev.get(p);
        }
        // cableList läuft vom Verbraucher zur Quelle; die erste schwache Leitung ab der Quelle schmilzt
        BlockPos[] burnAt = new BlockPos[VoltageTier.values().length + 1];
        for (VoltageTier tier : VoltageTier.values()) {
            for (int i = cableList.size() - 1; i >= 0; i--) {
                BlockPos c = cableList.get(i);
                if (tier.exceeds(((CableBlock) states.get(c).getBlock()).tier())) {
                    burnAt[tier.level()] = c;
                    break;
                }
            }
        }
        return new Path(sink.immutable(), loss, cableList.size(), burnAt);
    }

    long lastActive() {
        return lastActive;
    }

    Info info() {
        return new Info(sources.size(), sinks.size(), cables, lastTransferred, lastActive);
    }

    /**
     * Verteilt die Pakete aller Quellen.
     *
     * @return false, wenn das Netz neu aufgebaut werden muss
     */
    boolean tick(ServerLevel level, long time, List<Runnable> afterTick) {
        if (sources.isEmpty() || sinks.isEmpty()) return true;
        Config.OvervoltageMode mode = Config.OVERVOLTAGE.get();
        int transferred = 0;
        rotation++;

        for (BlockPos sourcePos : sources) {
            BlockEntity be = level.getBlockEntity(sourcePos);
            if (!(be instanceof RiftEnergySource source) || be.isRemoved()) return false;

            int offered = source.getOfferedEnergy();
            if (offered <= 0) continue;
            VoltageTier tier = source.getSourceTier();
            List<Path> list = paths.get(sourcePos);
            if (list == null || list.isEmpty()) continue;

            double remaining = offered;
            int n = list.size();
            int start = Math.floorMod(rotation, n);   // reihum, damit alle Verbraucher drankommen
            for (int i = 0; i < n && remaining >= 1.0; i++) {
                Path path = list.get((start + i) % n);
                if (path.loss() >= remaining) continue;

                BlockEntity sinkBe = level.getBlockEntity(path.sink());
                if (!(sinkBe instanceof RiftEnergySink sink) || sinkBe.isRemoved()) return false;
                int demand = sink.getDemandedEnergy();
                if (demand <= 0) continue;
                int amount = (int) Math.min(demand, Math.floor(remaining - path.loss()));
                if (amount <= 0) continue;

                if (mode != Config.OvervoltageMode.OFF) {
                    BlockPos burn = path.burnFor(tier);
                    if (burn != null) {
                        // Paket zu groß für die Leitung: sie schmilzt, das Paket ist verloren
                        afterTick.add(() -> CableBlock.meltdown(level, burn));
                        source.drawEnergy(offered);
                        return false;
                    }
                    if (tier.exceeds(sink.getSinkTier())) {
                        // Paket zu groß für die Maschine: sie explodiert sofort, ohne Vorwarnung
                        afterTick.add(() -> sink.onOvervoltage(tier));
                        source.drawEnergy(offered);
                        return false;
                    }
                }

                int accepted = sink.injectEnergy(amount, tier);
                if (accepted > 0) {
                    remaining -= accepted + path.loss();
                    transferred += accepted;
                }
            }
            int used = (int) Math.ceil(offered - Math.max(0.0, remaining));
            if (used > 0) source.drawEnergy(Math.min(offered, used));
        }

        lastTransferred = transferred;
        if (transferred > 0) lastActive = time;
        return true;
    }
}
