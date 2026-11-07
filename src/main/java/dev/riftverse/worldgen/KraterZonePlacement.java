package dev.riftverse.worldgen;

import com.mojang.serialization.MapCodec;
import dev.riftverse.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

/**
 * Xenit-Zone: Lässt eine Ader nur mit Wahrscheinlichkeit {@code w = max(0, 1 − d / 200)} durch, wobei d der
 * Abstand des Chunks zum nächsten Meteoritenkrater ist. Am Krater ist die Dichte voll, bei 150 Blöcken bleibt
 * ein Viertel, bei 200 Blöcken endet das Xenit.
 * <pre>{@code {"type": "riftverse:krater_zone"}}</pre>
 */
public class KraterZonePlacement extends PlacementModifier {
    public static final KraterZonePlacement INSTANCE = new KraterZonePlacement();
    public static final MapCodec<KraterZonePlacement> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        ServerLevel level = context.getLevel().getLevel();
        double d = KraterOrte.distanceToNearest(level, pos.getX() + 8, pos.getZ() + 8);
        double w = KraterOrte.zoneWeight(d);
        return w > 0 && random.nextDouble() < w ? Stream.of(pos) : Stream.empty();
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.KRATER_ZONE.get();
    }
}
