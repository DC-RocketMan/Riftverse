package dev.riftverse.tech.multiblock;

import dev.riftverse.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Bauplan eines Multiblocks relativ zu seinem Steuerblock. Der Steuerblock sitzt unten in der Mitte der Vorderseite;
 * {@code back} zählt von der Front nach hinten, {@code right} quer dazu, {@code up} nach oben.
 */
public final class Multiblock {

    /** Ein Teil des Bauplans; {@code hintKey} ist der Übersetzungsschlüssel des erwarteten Blocks. */
    public record Part(int right, int up, int back, Predicate<BlockState> test, String hintKey) {}

    private final List<Part> parts;

    private Multiblock(List<Part> parts) {
        this.parts = List.copyOf(parts);
    }

    public List<Part> parts() {
        return parts;
    }

    public static BlockPos at(BlockPos controller, Direction facing, Part part) {
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        return controller.relative(right, part.right()).relative(Direction.UP, part.up()).relative(back, part.back());
    }

    /** Erstes Teil, das nicht stimmt, oder null, wenn der Multiblock vollständig ist. */
    @Nullable
    public Part firstMissing(Level level, BlockPos controller, Direction facing) {
        for (Part p : parts) {
            BlockPos pos = at(controller, facing, p);
            if (!level.isLoaded(pos) || !p.test().test(level.getBlockState(pos))) return p;
        }
        return null;
    }

    /** Meldung „Es fehlt: Labor-Hülle bei 12 64 -30“, oder null. */
    @Nullable
    public Component missingMessage(Level level, BlockPos controller, Direction facing) {
        Part p = firstMissing(level, controller, facing);
        if (p == null) return null;
        BlockPos pos = at(controller, facing, p);
        return Component.translatable("message.riftverse.multiblock.fehlt", Component.translatable(p.hintKey()),
                pos.getX(), pos.getY(), pos.getZ());
    }

    // ------------------------------------------------------------------ Baupläne

    private static boolean huelle(BlockState s) {
        return s.is(ModBlocks.LABOR_HUELLE.get());
    }

    private static boolean glas(BlockState s) {
        return s.is(Tags.Blocks.GLASS_BLOCKS);
    }

    private static final String HUELLE = "block.riftverse.labor_huelle";
    private static final String GLAS = "block.minecraft.glass";

    /**
     * Portalfluid-Raffinerie, 3 × 3 × 4: Boden und Deckel aus Labor-Hülle, dazwischen zwei Lagen mit
     * Hülle an den Ecken und Glas an den Seiten. Innen bleibt Platz für die Raffineriesäule.
     */
    public static final Multiblock RAFFINERIE = build(4, (right, up, back, list) -> {
        boolean corner = right != 0 && back != 1;
        boolean center = right == 0 && back == 1;
        if (up == 0 || up == 3) list.add(new Part(right, up, back, Multiblock::huelle, HUELLE));
        else if (corner) list.add(new Part(right, up, back, Multiblock::huelle, HUELLE));
        else if (!center) list.add(new Part(right, up, back, Multiblock::glas, GLAS));
    });

    /**
     * Gravitations-Pendel, 3 × 5 × 3: Boden und Deckel aus Labor-Hülle, vier Eckpfosten, in der Mitte hängt der
     * Schwebstein-Kern.
     */
    public static final Multiblock PENDEL = build(5, (right, up, back, list) -> {
        boolean corner = right != 0 && back != 1;
        if (up == 0 || up == 4) list.add(new Part(right, up, back, Multiblock::huelle, HUELLE));
        else if (corner) list.add(new Part(right, up, back, Multiblock::huelle, HUELLE));
        else if (up == 2 && right == 0 && back == 1) {
            list.add(new Part(0, 2, 1, s -> s.is(ModBlocks.SCHWEBSTEIN_KERN.get()), "block.riftverse.schwebstein_kern"));
        }
    });

    private interface Layer {
        void add(int right, int up, int back, List<Part> list);
    }

    private static Multiblock build(int height, Layer layer) {
        List<Part> list = new ArrayList<>();
        for (int up = 0; up < height; up++) {
            for (int back = 0; back <= 2; back++) {
                for (int right = -1; right <= 1; right++) {
                    if (up == 0 && back == 0 && right == 0) continue;   // der Steuerblock selbst
                    layer.add(right, up, back, list);
                }
            }
        }
        return new Multiblock(list);
    }
}
