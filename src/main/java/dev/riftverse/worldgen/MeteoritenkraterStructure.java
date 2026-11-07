package dev.riftverse.worldgen;

import com.mojang.serialization.MapCodec;
import dev.riftverse.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Meteoritenkrater in der Overworld: alle 900–1.100 Blöcke einer, in allen Biomen außer Flüssen.
 * Im Ozean liegt eine versunkene Variante am Meeresgrund. Gebaut wird er von {@link KraterPiece}.
 */
public class MeteoritenkraterStructure extends Structure {
    public static final MapCodec<MeteoritenkraterStructure> CODEC = simpleCodec(MeteoritenkraterStructure::new);

    public MeteoritenkraterStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        if (KraterOrte.isRiver(KraterOrte.centerBiome(context.biomeSource(), context.randomState().sampler(), x, z))) {
            return Optional.empty();
        }
        int floor = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState());
        int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        boolean sunken = surface > floor + 1;
        WorldgenRandom random = context.random();
        BlockPos center = new BlockPos(x, floor, z);
        KraterPiece piece = new KraterPiece(center, 6 + random.nextInt(3), random.nextLong(), sunken,
                context.chunkGenerator().getSeaLevel());
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(piece)));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.METEORITENKRATER.get();
    }
}
