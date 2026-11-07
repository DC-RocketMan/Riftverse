package dev.riftverse.devtest;

import dev.riftverse.Riftverse;
import dev.riftverse.cable.CableBlock;
import dev.riftverse.energy.EnergyNet;
import dev.riftverse.energy.EnergyNetwork;
import dev.riftverse.energy.KreativQuelleBlockEntity;
import dev.riftverse.recipe.MachineRecipeInput;
import dev.riftverse.recipe.WerkbankRecipe;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModRecipes;
import dev.riftverse.tech.ProcessingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rauchtest für die CI ({@code ./gradlew runRauchtest}): startet einen Server, prüft Rezepte,
 * baut ein kleines Netz auf, lässt es laufen und beendet den Server wieder.
 * Nur aktiv mit {@code -Driftverse.rauchtest=true}; im normalen Spiel passiert hier nichts.
 */
public final class RauchTest {
    private static final int RUN_TICKS = 260;

    private static final List<String> failures = new ArrayList<>();
    private static int ticks = -1;
    private static BlockPos source, mill, overvoltageMachine;

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean("riftverse.rauchtest")) return;
        NeoForge.EVENT_BUS.addListener(RauchTest::onStarted);
        NeoForge.EVENT_BUS.addListener(RauchTest::onTick);
        Riftverse.LOGGER.info("RIFTVERSE-RAUCHTEST: aktiv");
    }

    private static void check(boolean ok, String what) {
        Riftverse.LOGGER.info("RIFTVERSE-RAUCHTEST: {} {}", ok ? "ok    " : "FEHLER", what);
        if (!ok) failures.add(what);
    }

    private static void onStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        RecipeManager recipes = server.getRecipeManager();

        // --- Rezepte ------------------------------------------------------------
        check(recipes.getAllRecipesFor(ModRecipes.ERZMUEHLE_TYPE.get()).size() >= 15, "Erzmühle-Rezepte geladen");
        check(recipes.getAllRecipesFor(ModRecipes.WALZBANK_TYPE.get()).size() >= 6, "Walzbank-Rezepte geladen");
        check(recipes.getAllRecipesFor(ModRecipes.LEGIERUNGSOFEN_TYPE.get()).size() >= 4, "Legierungsofen-Rezepte geladen");
        check(recipes.getAllRecipesFor(ModRecipes.GARAGENPRESSE_TYPE.get()).size() >= 9, "Garagenpresse-Rezepte geladen");
        check(recipes.getAllRecipesFor(ModRecipes.WERKBANK_TYPE.get()).size() >= 25, "Werkbank-Rezepte geladen");

        Optional<RecipeHolder<WerkbankRecipe>> plate = recipes.getRecipeFor(ModRecipes.WERKBANK_TYPE.get(),
                CraftingInput.of(2, 1, List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT))), level);
        check(plate.isPresent() && plate.get().value().result().is(ModItems.EISEN_PLATTE.get())
                && plate.get().value().hammer() == 1, "Werkbank: 2 Eisenbarren + Hammer -> Eisenplatte");

        check(recipes.getRecipeFor(ModRecipes.LEGIERUNGSOFEN_TYPE.get(), new MachineRecipeInput(List.of(
                        new ItemStack(ModItems.TIN_INGOT.get()), new ItemStack(Items.COPPER_INGOT, 3)), ""), level)
                .map(h -> h.value().result().is(ModItems.BRONZE_INGOT.get()) && h.value().result().getCount() == 4)
                .orElse(false), "Legierungsofen: 3 Kupfer + 1 Zinn -> 4 Bronze (Slots vertauscht)");

        check(recipes.getRecipeFor(ModRecipes.WALZBANK_TYPE.get(),
                        new MachineRecipeInput(List.of(new ItemStack(ModItems.ZINN_PLATTE.get())), "schneiden"), level)
                .map(h -> h.value().result().is(ModItems.ZINN_HUELSE.get())).orElse(false), "Walzbank: Schneiden -> Zinnhülse");

        // --- Netz: Kreativ-Quelle -> 3 Kupferkabel -> Erzmühle ---------------------
        BlockPos base = new BlockPos(8, 220, 8);
        level.setChunkForced(0, 0, true);
        source = base;
        mill = base.east(4);
        level.setBlock(source, ModBlocks.KREATIV_QUELLE.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 1; i <= 3; i++) level.setBlock(base.east(i), ModBlocks.KUPFERKABEL.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(mill, ModBlocks.ERZMUEHLE.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 1; i <= 3; i++) {
            BlockPos p = base.east(i);
            level.setBlock(p, Block.updateFromNeighbourShapes(level.getBlockState(p), level, p), Block.UPDATE_ALL);
        }
        if (level.getBlockEntity(mill) instanceof ProcessingBlockEntity machine) {
            machine.items().setStackInSlot(0, new ItemStack(Items.RAW_IRON, 8));
        } else {
            check(false, "Erzmühle hat eine Block-Entity");
        }

        BlockState cable = level.getBlockState(base.east(1));
        check(cable.getValue(CableBlock.PROPERTY_BY_DIRECTION.get(Direction.WEST))
                && cable.getValue(CableBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST)), "Kabel verbindet sich mit Quelle und Kabel");

        // --- Überspannung: Quelle Stufe II direkt an einer Stufe-I-Maschine -----------
        BlockPos hot = base.south(12);
        overvoltageMachine = hot.east();
        level.setBlock(hot, ModBlocks.KREATIV_QUELLE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(hot) instanceof KreativQuelleBlockEntity q) q.cycleTier();   // I -> II
        level.setBlock(overvoltageMachine, ModBlocks.WALZBANK.get().defaultBlockState(), Block.UPDATE_ALL);

        ticks = 0;
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (ticks < 0 || ++ticks < RUN_TICKS) return;
        ticks = -1;
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();

        EnergyNetwork.Info info = EnergyNet.info(level, source.east(2));
        check(info != null && info.sources() == 1 && info.sinks() == 1 && info.cables() == 3,
                "Netz erkannt: 1 Quelle, 1 Verbraucher, 3 Leitungen" + (info == null ? " (kein Netz)" : " " + info));
        if (level.getBlockEntity(mill) instanceof ProcessingBlockEntity machine) {
            ItemStack out = machine.items().getStackInSlot(machine.outputSlot());
            check(out.is(ModItems.EISEN_STAUB.get()) && out.getCount() >= 2, "Erzmühle mahlt mit Netzstrom: " + out);
            check(machine.energy().stored() > 0, "Erzmühle hat Strom im Puffer: " + machine.energy().stored() + " FE");
        } else {
            check(false, "Erzmühle steht noch");
        }
        check(level.getBlockState(overvoltageMachine).isAir(), "Überspannung: Walzbank an Stufe II ist explodiert");

        if (failures.isEmpty()) {
            Riftverse.LOGGER.info("RIFTVERSE-RAUCHTEST: BESTANDEN");
        } else {
            Riftverse.LOGGER.error("RIFTVERSE-RAUCHTEST: NICHT BESTANDEN ({} Fehler): {}", failures.size(), failures);
        }
        server.halt(false);
    }

    private RauchTest() {}
}
