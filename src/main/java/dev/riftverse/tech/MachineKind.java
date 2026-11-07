package dev.riftverse.tech;

import dev.riftverse.energy.VoltageTier;
import dev.riftverse.recipe.MachineRecipe;
import dev.riftverse.registry.ModRecipes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/**
 * Die verarbeitenden Maschinen aus Meilenstein 1. Jede hat einen eigenen Rezepttyp im Datapack
 * ({@code data/riftverse/recipe/<maschine>/...}).
 */
public enum MachineKind {
    /** 1 Erz → 2 Staub (Eisen, Kupfer, Zinn, Gold, Xenit) */
    ERZMUEHLE("erzmuehle", 1, 30, List.of()),
    /** Walzen: Platte → 3 Draht · Schneiden: Platte → 2 Hülsen · Ziehen: Barren → 2 Stangen */
    WALZBANK("walzbank", 1, 30, List.of("walzen", "schneiden", "ziehen")),
    /** 3 Kupfer + 1 Zinn → 4 Bronze · Eisen + Kohlestaub → Stahl */
    LEGIERUNGSOFEN("legierungsofen", 2, 40, List.of()),
    /** T0, Handbetrieb: Barren → Platte, Schrott → Schrottplatte, Polymerfetzen → Isolierfolie */
    GARAGENPRESSE("garagenpresse", 3, 0, List.of());

    private final String id;
    private final int inputs;
    private final int energyPerTick;
    private final List<String> modes;

    MachineKind(String id, int inputs, int energyPerTick, List<String> modes) {
        this.id = id;
        this.inputs = inputs;
        this.energyPerTick = energyPerTick;
        this.modes = modes;
    }

    public String id() {
        return id;
    }

    public int inputs() {
        return inputs;
    }

    /** Verbrauch in FE/t; 0 = Handbetrieb. */
    public int energyPerTick() {
        return energyPerTick;
    }

    public boolean electric() {
        return energyPerTick > 0;
    }

    /** Puffer reicht für 20 Sekunden Arbeit. */
    public int capacity() {
        return Math.max(1, energyPerTick * 400);
    }

    public VoltageTier tier() {
        return VoltageTier.I;
    }

    public List<String> modes() {
        return modes;
    }

    public boolean hasModes() {
        return !modes.isEmpty();
    }

    public Component modeName(int mode) {
        if (!hasModes()) return Component.empty();
        return Component.translatable("gui.riftverse.mode." + modes.get(Math.floorMod(mode, modes.size())));
    }

    public RecipeType<MachineRecipe> recipeType() {
        return switch (this) {
            case ERZMUEHLE -> ModRecipes.ERZMUEHLE_TYPE.get();
            case WALZBANK -> ModRecipes.WALZBANK_TYPE.get();
            case LEGIERUNGSOFEN -> ModRecipes.LEGIERUNGSOFEN_TYPE.get();
            case GARAGENPRESSE -> ModRecipes.GARAGENPRESSE_TYPE.get();
        };
    }

    public RecipeSerializer<MachineRecipe> serializer() {
        return switch (this) {
            case ERZMUEHLE -> ModRecipes.ERZMUEHLE.get();
            case WALZBANK -> ModRecipes.WALZBANK.get();
            case LEGIERUNGSOFEN -> ModRecipes.LEGIERUNGSOFEN.get();
            case GARAGENPRESSE -> ModRecipes.GARAGENPRESSE.get();
        };
    }

    public String translationKey() {
        return "block.riftverse." + id;
    }
}
