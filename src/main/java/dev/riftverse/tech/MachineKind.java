package dev.riftverse.tech;

import dev.riftverse.energy.VoltageTier;
import dev.riftverse.recipe.MachineRecipe;
import dev.riftverse.registry.ModRecipes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/**
 * Die verarbeitenden Maschinen (T0 bis T2). Jede hat einen eigenen Rezepttyp im Datapack
 * ({@code data/riftverse/recipe/<maschine>/...}).
 * <p>
 * Slots: Eingänge 0..n-1, Ausgänge n..n+o-1, Akku-Slot n+o (nur elektrische Maschinen).
 */
public enum MachineKind {
    /** 1 Erz → 2 Staub (Eisen, Kupfer, Zinn, Gold, Xenit) */
    ERZMUEHLE("erzmuehle", 1, 1, 30, List.of()),
    /** Walzen: Platte → 3 Draht · Schneiden: Platte → 2 Hülsen · Ziehen: Barren → 2 Stangen */
    WALZBANK("walzbank", 1, 1, 30, List.of("walzen", "schneiden", "ziehen")),
    /** 3 Kupfer + 1 Zinn → 4 Bronze · Eisen + Kohlestaub → Stahl */
    LEGIERUNGSOFEN("legierungsofen", 2, 1, 40, List.of()),
    /** T0, Handbetrieb: Barren → Platte, Schrott → Schrottplatte, Polymerfetzen → Isolierfolie */
    GARAGENPRESSE("garagenpresse", 3, 1, 0, List.of()),
    /** Xenit-Staub → Xenit-Barren (Xenit-Staub schmilzt nur hier) */
    XENIT_SCHMELZE("xenit_schmelze", 1, 1, 40, List.of()),
    /** Schrottplatte + Redstone + 3 Kupferdraht → Basis-Schaltkreis */
    PLATINEN_AETZER("platinen_aetzer", 3, 1, 30, List.of()),
    /** Fluxkristall → Fluxlinse + Fluxstaub */
    KRISTALL_SCHLEIFER("kristall_schleifer", 1, 2, 50, List.of()),
    /** Alien-Schrott → Polymerfetzen und zufällig Drähte, Federn, Mini-Motoren */
    RECYCLER("recycler", 1, 4, 20, List.of()),

    // --- T2 Rift-Labor (Stufe II) ---------------------------------------------------
    /** Sequenzielle Montage in 3–6 Schritten: Zutat i gehört in Slot i (Rift-Schaltkreis, Kanonenkern, Kanone Mk II). */
    PRAEZISIONS_MONTIERER("praezisions_montierer", 6, 1, 200, List.of(), VoltageTier.II, true),
    /** Säuregel, Biomasse → 2–3 Nebenprodukte (Ätzlösung, Toxinrest, Nährserum) */
    QUANTENZENTRIFUGE("quantenzentrifuge", 2, 3, 300, List.of(), VoltageTier.II, false),
    /** Schwebstein → Antigrav-Platte (ab T3 auch Gravitit → Rahmen) */
    GRAVITATIONSPRESSE("gravitationspresse", 1, 1, 350, List.of(), VoltageTier.II, false),
    /** Universumsprobe + Papier → Koordinatenkarte mit Typ und Modifikatoren */
    DIMENSIONS_SCANNER("dimensions_scanner", 2, 1, 150, List.of(), VoltageTier.II, false);

    private final String id;
    private final int inputs;
    private final int outputs;
    private final int energyPerTick;
    private final List<String> modes;
    private final VoltageTier tier;
    private final boolean ordered;

    MachineKind(String id, int inputs, int outputs, int energyPerTick, List<String> modes) {
        this(id, inputs, outputs, energyPerTick, modes, VoltageTier.I, false);
    }

    MachineKind(String id, int inputs, int outputs, int energyPerTick, List<String> modes, VoltageTier tier, boolean ordered) {
        this.id = id;
        this.inputs = inputs;
        this.outputs = outputs;
        this.energyPerTick = energyPerTick;
        this.modes = modes;
        this.tier = tier;
        this.ordered = ordered;
    }

    public String id() {
        return id;
    }

    public int inputs() {
        return inputs;
    }

    /** Ausgangsslots: 1, beim Kristall-Schleifer 2 (Linse + Staub), beim Recycler 4. */
    public int outputs() {
        return outputs;
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

    /** Höchste Spannungsstufe, die die Maschine verträgt. */
    public VoltageTier tier() {
        return tier;
    }

    /** Montierer: Die Zutaten müssen der Reihe nach in den Slots liegen (Schritt 1 in Slot 1 …). */
    public boolean ordered() {
        return ordered;
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
            case XENIT_SCHMELZE -> ModRecipes.XENIT_SCHMELZE_TYPE.get();
            case PLATINEN_AETZER -> ModRecipes.PLATINEN_AETZER_TYPE.get();
            case KRISTALL_SCHLEIFER -> ModRecipes.KRISTALL_SCHLEIFER_TYPE.get();
            case RECYCLER -> ModRecipes.RECYCLER_TYPE.get();
            case PRAEZISIONS_MONTIERER -> ModRecipes.PRAEZISIONS_MONTIERER_TYPE.get();
            case QUANTENZENTRIFUGE -> ModRecipes.QUANTENZENTRIFUGE_TYPE.get();
            case GRAVITATIONSPRESSE -> ModRecipes.GRAVITATIONSPRESSE_TYPE.get();
            case DIMENSIONS_SCANNER -> ModRecipes.DIMENSIONS_SCANNER_TYPE.get();
        };
    }

    public RecipeSerializer<MachineRecipe> serializer() {
        return switch (this) {
            case ERZMUEHLE -> ModRecipes.ERZMUEHLE.get();
            case WALZBANK -> ModRecipes.WALZBANK.get();
            case LEGIERUNGSOFEN -> ModRecipes.LEGIERUNGSOFEN.get();
            case GARAGENPRESSE -> ModRecipes.GARAGENPRESSE.get();
            case XENIT_SCHMELZE -> ModRecipes.XENIT_SCHMELZE.get();
            case PLATINEN_AETZER -> ModRecipes.PLATINEN_AETZER.get();
            case KRISTALL_SCHLEIFER -> ModRecipes.KRISTALL_SCHLEIFER.get();
            case RECYCLER -> ModRecipes.RECYCLER.get();
            case PRAEZISIONS_MONTIERER -> ModRecipes.PRAEZISIONS_MONTIERER.get();
            case QUANTENZENTRIFUGE -> ModRecipes.QUANTENZENTRIFUGE.get();
            case GRAVITATIONSPRESSE -> ModRecipes.GRAVITATIONSPRESSE.get();
            case DIMENSIONS_SCANNER -> ModRecipes.DIMENSIONS_SCANNER.get();
        };
    }

    public String translationKey() {
        return "block.riftverse." + id;
    }
}
