package dev.riftverse.devtest;

import dev.riftverse.energy.KreativQuelleBlockEntity;
import dev.riftverse.energy.UmspannerBlock;
import dev.riftverse.item.PortalKanoneItem;
import dev.riftverse.recipe.MachineRecipeInput;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModDataComponents.KanonenTank;
import dev.riftverse.registry.ModFluids;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModRecipes;
import dev.riftverse.tech.FluxZelleBlock;
import dev.riftverse.tech.FluxZelleBlockEntity;
import dev.riftverse.tech.FluxkristallResonatorBlockEntity;
import dev.riftverse.tech.GravitationsPendelBlockEntity;
import dev.riftverse.tech.ProcessingBlockEntity;
import dev.riftverse.tech.RaffinerieBlockEntity;
import dev.riftverse.tech.TechBlock;
import dev.riftverse.tech.multiblock.Multiblock;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import dev.riftverse.universe.UniverseType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

import static dev.riftverse.devtest.RauchTest.check;

/** Rauchtest für Meilenstein 3 (T2 Rift-Labor). */
final class RauchTestM3 {
    private static BlockPos umspannerMill, umspannerCable, fluxZelle, montierer, zentrifuge, scanner, raffinerie, pendel, resonator;

    static void aufbauen(MinecraftServer server, ServerLevel level, RecipeManager recipes, BlockPos base) {
        for (int cx = 0; cx <= 1; cx++) {
            for (int cz = base.getZ() >> 4; cz <= (base.getZ() >> 4) + 3; cz++) level.setChunkForced(cx, cz, true);
        }

        // --- Rezepte ------------------------------------------------------------------
        check(recipes.getAllRecipesFor(ModRecipes.PRAEZISIONS_MONTIERER_TYPE.get()).size() >= 4, "Montierer-Rezepte geladen");
        check(recipes.getAllRecipesFor(ModRecipes.QUANTENZENTRIFUGE_TYPE.get()).size() >= 4, "Quantenzentrifuge-Rezepte geladen");
        check(!recipes.getAllRecipesFor(ModRecipes.GRAVITATIONSPRESSE_TYPE.get()).isEmpty(), "Gravitationspresse-Rezepte geladen");
        check(!recipes.getAllRecipesFor(ModRecipes.DIMENSIONS_SCANNER_TYPE.get()).isEmpty(), "Dimensions-Scanner-Rezepte geladen");
        check(!recipes.getAllRecipesFor(ModRecipes.RAFFINERIE_TYPE.get()).isEmpty(), "Raffinerie-Rezepte geladen");
        List<ItemStack> ordered = riftSchaltkreisZutaten();
        check(recipes.getRecipeFor(ModRecipes.PRAEZISIONS_MONTIERER_TYPE.get(), new MachineRecipeInput(ordered, ""), level)
                .map(h -> h.value().result().is(ModItems.RIFT_SCHALTKREIS.get())).orElse(false), "Montierer: Zutaten in Reihenfolge -> Rift-Schaltkreis");
        List<ItemStack> swapped = riftSchaltkreisZutaten();
        ItemStack first = swapped.get(0);
        swapped.set(0, swapped.get(2));
        swapped.set(2, first);
        check(recipes.getRecipeFor(ModRecipes.PRAEZISIONS_MONTIERER_TYPE.get(), new MachineRecipeInput(swapped, ""), level).isEmpty(),
                "Montierer: vertauschte Reihenfolge passt nicht");
        check(UniverseType.reachableWith(2).size() == 6, "Kanone Mk II erreicht sechs Universumstypen");
        check(Math.abs(UniverseProfile.get(new UniverseId(UniverseType.NIEDRIGGRAVITATION, 0, 0)).gravity - 0.3) < 1e-9,
                "Niedriggravitationswelt hat 0,3 g");

        // --- Umspanner: Quelle II -> Flux-Leitung -> Umspanner -> Kupferkabel -> Erzmühle (Stufe I) ---------
        BlockPos line = base;
        place(level, line, ModBlocks.KREATIV_QUELLE.get().defaultBlockState());
        if (level.getBlockEntity(line) instanceof KreativQuelleBlockEntity q) q.cycleTier();   // I -> II
        place(level, line.east(1), ModBlocks.FLUX_LEITUNG.get().defaultBlockState());
        place(level, line.east(2), ModBlocks.UMSPANNER.get().defaultBlockState().setValue(UmspannerBlock.FACING, Direction.WEST));
        place(level, line.east(3), ModBlocks.KUPFERKABEL.get().defaultBlockState());
        place(level, line.east(4), ModBlocks.ERZMUEHLE.get().defaultBlockState());
        connect(level, line.east(1));
        connect(level, line.east(3));
        umspannerMill = line.east(4);
        umspannerCable = line.east(1);

        // --- Flux-Zelle: Quelle II -> Flux-Leitung -> Flux-Zelle ----------------------------------------
        BlockPos cellLine = base.south(4);
        place(level, cellLine, ModBlocks.KREATIV_QUELLE.get().defaultBlockState());
        if (level.getBlockEntity(cellLine) instanceof KreativQuelleBlockEntity q) q.cycleTier();
        place(level, cellLine.east(1), ModBlocks.FLUX_LEITUNG_ISOLIERT.get().defaultBlockState());
        place(level, cellLine.east(2), ModBlocks.FLUX_ZELLE.get().defaultBlockState());
        connect(level, cellLine.east(1));
        fluxZelle = cellLine.east(2);

        // --- T2-Maschinen mit vollem Puffer -----------------------------------------------------------
        BlockPos row = base.south(8);
        montierer = row;
        zentrifuge = row.east(3);
        scanner = row.east(6);
        place(level, montierer, ModBlocks.PRAEZISIONS_MONTIERER.get().defaultBlockState());
        place(level, zentrifuge, ModBlocks.QUANTENZENTRIFUGE.get().defaultBlockState());
        place(level, scanner, ModBlocks.DIMENSIONS_SCANNER.get().defaultBlockState());
        if (level.getBlockEntity(montierer) instanceof ProcessingBlockEntity m) {
            List<ItemStack> in = riftSchaltkreisZutaten();
            for (int i = 0; i < in.size(); i++) m.items().setStackInSlot(i, in.get(i));
            m.energy().set(m.energy().capacity());
        } else check(false, "Montierer hat eine Block-Entity");
        if (level.getBlockEntity(zentrifuge) instanceof ProcessingBlockEntity m) {
            m.items().setStackInSlot(0, new ItemStack(ModItems.SAEUREGEL.get(), 2));
            m.energy().set(m.energy().capacity());
        } else check(false, "Quantenzentrifuge hat eine Block-Entity");
        if (level.getBlockEntity(scanner) instanceof ProcessingBlockEntity m) {
            ItemStack probe = new ItemStack(ModItems.UNIVERSUMSPROBE.get());
            probe.set(ModDataComponents.ZIEL.get(), new UniverseId(UniverseType.ZEITDILATATION, 3, -7));
            m.items().setStackInSlot(0, probe);
            m.items().setStackInSlot(1, new ItemStack(Items.PAPER));
            m.energy().set(m.energy().capacity());
        } else check(false, "Dimensions-Scanner hat eine Block-Entity");

        // --- Portalfluid-Raffinerie (Multiblock 3 × 3 × 4) ---------------------------------------------
        raffinerie = base.south(16).east(2);
        build(level, raffinerie, ModBlocks.PORTALFLUID_RAFFINERIE.get().defaultBlockState(), Multiblock.RAFFINERIE);
        if (level.getBlockEntity(raffinerie) instanceof RaffinerieBlockEntity r) {
            check(r.checkStructure() == null, "Raffinerie: Multiblock erkannt");
            r.items().setStackInSlot(RaffinerieBlockEntity.SLOT_IN_A, new ItemStack(ModItems.SAEUREGEL.get()));
            r.items().setStackInSlot(RaffinerieBlockEntity.SLOT_IN_B, new ItemStack(ModItems.FLUXSTAUB.get()));
            r.items().setStackInSlot(RaffinerieBlockEntity.SLOT_CART_IN, new ItemStack(ModItems.FLUID_KARTUSCHE.get()));
            int filled = r.fluidHandler().fill(new FluidStack(ModFluids.ROH_PORTALFLUID.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            check(filled == 1000, "Raffinerie nimmt Roh-Portalfluid über Rohre an");
            r.energy().set(r.energy().capacity());
        } else check(false, "Raffinerie hat eine Block-Entity");

        // --- Gravitations-Pendel (Multiblock 3 × 5 × 3) --------------------------------------------------
        pendel = base.south(16).east(8);
        build(level, pendel, ModBlocks.GRAVITATIONS_PENDEL.get().defaultBlockState(), Multiblock.PENDEL);

        // --- Fluxkristall-Resonator mit einer Kammer, 4 Stäbe im Quadrat, 4 Kühlkörper ----------------
        resonator = base.south(24);
        place(level, resonator, ModBlocks.FLUXKRISTALL_RESONATOR.get().defaultBlockState());
        place(level, resonator.above(), ModBlocks.RESONATOR_KAMMER.get().defaultBlockState());
        if (level.getBlockEntity(resonator) instanceof FluxkristallResonatorBlockEntity r) {
            int c = FluxkristallResonatorBlockEntity.MAX_COLUMNS;
            for (int slot : new int[]{0, 1, c, c + 1}) r.items().setStackInSlot(slot, new ItemStack(ModItems.FLUXKRISTALL_STAB.get()));
            for (int slot : new int[]{2, c + 2, 2 * c, 2 * c + 1}) r.items().setStackInSlot(slot, new ItemStack(ModItems.KUEHLKOERPER.get()));
        } else check(false, "Resonator hat eine Block-Entity");
    }

    static void ergebnis(MinecraftServer server, ServerLevel level) {
        check(level.getBlockState(umspannerMill).is(ModBlocks.ERZMUEHLE.get()), "Umspanner schützt die Stufe-I-Erzmühle vor Stufe II");
        check(level.getBlockState(umspannerCable).is(ModBlocks.FLUX_LEITUNG.get()), "Flux-Leitung verträgt Stufe II");
        if (level.getBlockEntity(umspannerMill) instanceof ProcessingBlockEntity m) {
            check(m.energy().stored() > 0, "Erzmühle bekommt heruntergespannten Strom: " + m.energy().stored() + " FE");
        }
        if (level.getBlockEntity(fluxZelle) instanceof FluxZelleBlockEntity z) {
            BlockState st = level.getBlockState(fluxZelle);
            check(z.energy().stored() > 0 && st.getValue(FluxZelleBlock.FUELLUNG) >= 1,
                    "Flux-Zelle lädt mit Stufe II: " + z.energy().stored() + " FE, Band " + st.getValue(FluxZelleBlock.FUELLUNG));
        } else check(false, "Flux-Zelle steht noch");

        if (level.getBlockEntity(montierer) instanceof ProcessingBlockEntity m) {
            ItemStack out = m.items().getStackInSlot(m.outputSlot());
            check(out.is(ModItems.RIFT_SCHALTKREIS.get()), "Präzisions-Montierer baut einen Rift-Schaltkreis (" + out + ")");
        }
        if (level.getBlockEntity(zentrifuge) instanceof ProcessingBlockEntity m) {
            ItemStack out = m.items().getStackInSlot(m.outputSlot());
            check(out.is(ModItems.AETZLOESUNG.get()), "Quantenzentrifuge: Säuregel -> Ätzlösung (" + out + ")");
        }
        if (level.getBlockEntity(scanner) instanceof ProcessingBlockEntity m) {
            ItemStack out = m.items().getStackInSlot(m.outputSlot());
            UniverseId id = out.get(ModDataComponents.ZIEL.get());
            check(out.is(ModItems.KOORDINATENKARTE.get()) && new UniverseId(UniverseType.ZEITDILATATION, 3, -7).equals(id),
                    "Dimensions-Scanner: Probe -> Koordinatenkarte mit Ziel (" + out + ", " + id + ")");
        }
        if (level.getBlockEntity(raffinerie) instanceof RaffinerieBlockEntity r) {
            ItemStack out = r.items().getStackInSlot(RaffinerieBlockEntity.SLOT_CART_OUT);
            check(out.is(ModItems.FLUID_KARTUSCHE_STABIL.get()),
                    "Raffinerie: Roh-Portalfluid + Säuregel + Fluxstaub -> stabile Kartusche (" + out + ", formed=" + r.formed() + ")");
        }
        if (level.getBlockEntity(pendel) instanceof GravitationsPendelBlockEntity p) {
            check(p.formed() && p.energy().stored() > 0, "Gravitations-Pendel: Multiblock erzeugt Strom (" + p.energy().stored() + " FE)");
        } else check(false, "Gravitations-Pendel steht noch");
        if (level.getBlockEntity(resonator) instanceof FluxkristallResonatorBlockEntity r) {
            check(r.columns() == 4, "Resonator: Kammer erweitert das Raster auf 4 Spalten (" + r.columns() + ")");
            check(r.energy().stored() > 0 && r.heat() < FluxkristallResonatorBlockEntity.MAX_HEAT,
                    "Resonator erzeugt Strom (" + r.energy().stored() + " FE, Hitze " + r.heat() + ")");
            check(r.items().getStackInSlot(0).getDamageValue() > 0, "Fluxkristall-Stäbe nutzen sich ab");
        } else check(false, "Resonator steht noch (Überhitzung?)");

        kanoneMk2(level);
    }

    /** Mk II nimmt nur stabiles Portalfluid (100 mB pro Schuss); Mk I schießt mit stabilem Fluid in T2-Welten. */
    private static void kanoneMk2(ServerLevel level) {
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.getInventory().clearContent();
        PortalKanoneItem mk2 = ModItems.PORTAL_KANONE_MK2.get();
        ItemStack gun = new ItemStack(mk2);
        player.getInventory().add(new ItemStack(ModItems.FLUID_KARTUSCHE_VOLL.get()));
        player.getCooldowns().removeCooldown(mk2);
        check(!mk2.fire(player, gun), "Mk II lehnt Roh-Portalfluid ab");
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(ModItems.FLUID_KARTUSCHE_STABIL.get()));
        player.getCooldowns().removeCooldown(mk2);
        KanonenTank shot = mk2.shoot(player, gun);
        check(shot != null && shot.stabil() && PortalKanoneItem.tank(gun).mb() == 900,
                "Mk II lädt eine stabile Kartusche und verbraucht 100 mB (" + PortalKanoneItem.tank(gun).mb() + " übrig)");

        PortalKanoneItem mk1 = ModItems.PORTAL_KANONE_MK1.get();
        ItemStack old = new ItemStack(mk1);
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(ModItems.FLUID_KARTUSCHE_STABIL.get()));
        player.getCooldowns().removeCooldown(mk1);
        KanonenTank mk1Shot = mk1.shoot(player, old);
        check(mk1Shot != null && mk1Shot.stabil(), "Mk I schießt auch mit stabilem Portalfluid (T2-Welten möglich)");
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        player.getInventory().clearContent();
    }

    // ------------------------------------------------------------------ Hilfen

    private static List<ItemStack> riftSchaltkreisZutaten() {
        return new java.util.ArrayList<>(List.of(
                new ItemStack(ModItems.BASIS_SCHALTKREIS.get()),
                new ItemStack(ModItems.GOLDDRAHT.get(), 2),
                new ItemStack(ModItems.AETZLOESUNG.get()),
                new ItemStack(ModItems.FLUXSTAUB.get()),
                ItemStack.EMPTY, ItemStack.EMPTY));
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    private static void connect(ServerLevel level, BlockPos cable) {
        level.setBlock(cable, Block.updateFromNeighbourShapes(level.getBlockState(cable), level, cable), Block.UPDATE_ALL);
    }

    /** Setzt Steuerblock (Front nach Norden) und alle Teile des Bauplans. */
    private static void build(ServerLevel level, BlockPos controller, BlockState state, Multiblock plan) {
        Direction facing = Direction.NORTH;
        place(level, controller, state.setValue(TechBlock.FACING, facing));
        for (Multiblock.Part part : plan.parts()) {
            BlockState s;
            if (part.hintKey().equals("block.riftverse.labor_huelle")) s = ModBlocks.LABOR_HUELLE.get().defaultBlockState();
            else if (part.hintKey().equals("block.riftverse.schwebstein_kern")) s = ModBlocks.SCHWEBSTEIN_KERN.get().defaultBlockState();
            else s = Blocks.GLASS.defaultBlockState();
            place(level, Multiblock.at(controller, facing, part), s);
        }
    }

    private RauchTestM3() {}
}
