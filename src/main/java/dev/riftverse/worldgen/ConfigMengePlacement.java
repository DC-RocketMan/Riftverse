package dev.riftverse.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.riftverse.Config;
import dev.riftverse.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

/**
 * Skaliert die Zahl der Adern mit einem Faktor aus der Config. Steht vorn in der Platzierung: Bei Faktor f gibt es
 * ⌊f⌋ Kopien der Position und mit Wahrscheinlichkeit f − ⌊f⌋ eine weitere; alles Folgende (count, in_square …)
 * läuft für jede Kopie. Faktor 0 schaltet das Erz ab, 1 lässt alles, wie es ist.
 * <pre>{@code {"type": "riftverse:config_menge", "option": "zinn_dichte"}}</pre>
 */
public class ConfigMengePlacement extends PlacementModifier {
    public static final MapCodec<ConfigMengePlacement> CODEC = Codec.STRING.fieldOf("option")
            .xmap(ConfigMengePlacement::new, p -> p.option);

    private final String option;

    public ConfigMengePlacement(String option) {
        this.option = option;
    }

    public String option() {
        return option;
    }

    /** Wie viele Kopien bei Faktor {@code f} entstehen (für den Rauchtest öffentlich). */
    public static int kopien(double f, RandomSource random) {
        if (f <= 0) return 0;
        int ganz = (int) Math.floor(f);
        return ganz + (random.nextDouble() < f - ganz ? 1 : 0);
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        int n = kopien(Config.density(option), random);
        return n == 1 ? Stream.of(pos) : Stream.generate(() -> pos).limit(n);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.CONFIG_MENGE.get();
    }
}
