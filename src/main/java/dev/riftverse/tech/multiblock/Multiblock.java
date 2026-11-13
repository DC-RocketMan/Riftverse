package dev.riftverse.tech.multiblock;

import dev.riftverse.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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

    private static boolean graphitHuelle(BlockState s) {
        return s.is(ModBlocks.GRAPHIT_HUELLE.get());
    }

    private static final String GRAPHIT = "block.riftverse.graphit_huelle";

    /**
     * Parallelzapfer, 3 × 3 × 3: Boden aus Graphit-Hülle, darüber vier Eckpfosten aus Graphit-Hülle und darauf vier
     * Blitzableiter als Antennen. In der Mitte über dem Steuerblock bleibt Luft – dort öffnet sich der Riss.
     */
    public static final Multiblock PARALLELZAPFER = build(3, (right, up, back, list) -> {
        boolean corner = right != 0 && back != 1;
        if (up == 0) list.add(new Part(right, up, back, Multiblock::graphitHuelle, GRAPHIT));
        else if (up == 1 && corner) list.add(new Part(right, up, back, Multiblock::graphitHuelle, GRAPHIT));
        else if (up == 1 && right == 0 && back == 1) list.add(new Part(0, 1, 1, BlockState::isAir, "gui.riftverse.zapfer.luft"));
        else if (up == 2 && corner) {
            list.add(new Part(right, up, back, s -> s.is(Blocks.LIGHTNING_ROD), "block.minecraft.lightning_rod"));
        }
    });

    // ------------------------------------------------------------------ Meilenstein 5: T4

    private static final String NULL_HUELLE = "block.riftverse.null_huelle";
    private static final String LUFT = "gui.riftverse.multiblock.luft";

    private static boolean nullHuelle(BlockState s) {
        return s.is(ModBlocks.NULL_HUELLE.get());
    }

    /**
     * Rift-Schmiede, 5 × 5 × 5: Boden aus Graphit-Hülle, vier Eckpfosten und ein Deckelrahmen aus Graphit-Hülle.
     * In der Mitte steht ein Amboss, darüber bleibt Luft – dort hält die Schmiede ein Mini-Portal dauerhaft offen.
     * Der Steuerblock sitzt in der Mitte der vorderen Bodenkante.
     */
    public static final Multiblock RIFT_SCHMIEDE = build(2, 5, 5, (right, up, back, list) -> {
        boolean edgeX = Math.abs(right) == 2, edgeZ = back == 0 || back == 4;
        if (up == 0) list.add(new Part(right, up, back, Multiblock::graphitHuelle, GRAPHIT));
        else if (up < 4 && edgeX && edgeZ) list.add(new Part(right, up, back, Multiblock::graphitHuelle, GRAPHIT));
        else if (up == 4 && (edgeX || edgeZ)) list.add(new Part(right, up, back, Multiblock::graphitHuelle, GRAPHIT));
        else if (up == 1 && right == 0 && back == 2) {
            list.add(new Part(0, 1, 2, st -> st.is(BlockTags.ANVIL), "block.minecraft.anvil"));
        } else if (up == 2 && right == 0 && back == 2) {
            list.add(new Part(0, 2, 2, BlockState::isAir, LUFT));
        }
    });

    /** Position des Mini-Portals der Rift-Schmiede. */
    public static final Part SCHMIEDE_PORTAL = new Part(0, 2, 2, s -> true, "");

    /**
     * Weltenformer, Ring 7 × 7 × 3: unten und oben ein Ring aus Null-Hülle, dazwischen Null-Hülle an den Ecken und
     * Glas an den Seiten. Innen (5 × 5 × 3) muss alles frei bleiben – dort schwebt der Hologramm-Planet.
     * Der Steuerblock sitzt in der Mitte der vorderen Unterkante.
     */
    public static final Multiblock WELTENFORMER = build(3, 7, 3, (right, up, back, list) -> {
        boolean edgeX = Math.abs(right) == 3, edgeZ = back == 0 || back == 6;
        if (!edgeX && !edgeZ) list.add(new Part(right, up, back, BlockState::isAir, LUFT));
        else if (up != 1 || (edgeX && edgeZ)) list.add(new Part(right, up, back, Multiblock::nullHuelle, NULL_HUELLE));
        else list.add(new Part(right, up, back, Multiblock::glas, GLAS));
    });

    /** Mitte des Weltenformer-Rings (dort schwebt der Planet). */
    public static final Part WELTENFORMER_MITTE = new Part(0, 1, 3, s -> true, "");

    /**
     * Eindämmungsring des Singularitätskerns: drei Ringe (je der Rand eines 5 × 5-Quadrats) durch den Kern, wie ein
     * Kreiselkompass. Der Kern sitzt genau in der Mitte; die Ausrichtung spielt keine Rolle.
     */
    public static List<BlockPos> eindaemmungsring(BlockPos core) {
        java.util.Set<BlockPos> set = new java.util.LinkedHashSet<>();
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                if (Math.abs(a) != 2 && Math.abs(b) != 2) continue;
                set.add(core.offset(a, 0, b));   // waagerecht
                set.add(core.offset(a, b, 0));   // senkrecht, Nord-Süd-Ebene
                set.add(core.offset(0, a, b));   // senkrecht, Ost-West-Ebene
            }
        }
        return List.copyOf(set);
    }

    /** Erstes fehlendes Teil des Eindämmungsrings, oder null. */
    @Nullable
    public static BlockPos ringMissing(Level level, BlockPos core) {
        for (BlockPos p : eindaemmungsring(core)) {
            if (!level.isLoaded(p) || !level.getBlockState(p).is(ModBlocks.EINDAEMMUNGSRING.get())) return p;
        }
        return null;
    }

    private interface Layer {
        void add(int right, int up, int back, List<Part> list);
    }

    private static Multiblock build(int height, Layer layer) {
        return build(1, 3, height, layer);
    }

    /** Bauplan mit {@code right} von −halfWidth bis halfWidth und {@code back} von 0 bis depth − 1. */
    private static Multiblock build(int halfWidth, int depth, int height, Layer layer) {
        List<Part> list = new ArrayList<>();
        for (int up = 0; up < height; up++) {
            for (int back = 0; back < depth; back++) {
                for (int right = -halfWidth; right <= halfWidth; right++) {
                    if (up == 0 && back == 0 && right == 0) continue;   // der Steuerblock selbst
                    layer.add(right, up, back, list);
                }
            }
        }
        return new Multiblock(list);
    }
}
