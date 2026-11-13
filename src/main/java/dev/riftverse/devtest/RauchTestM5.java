package dev.riftverse.devtest;

import dev.riftverse.cable.CableBlock;
import dev.riftverse.energy.KreativQuelleBlockEntity;
import dev.riftverse.item.PortalKanoneItem;
import dev.riftverse.recipe.MachineRecipeInput;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModFluids;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModRecipes;
import dev.riftverse.tech.ProcessingBlockEntity;
import dev.riftverse.tech.SingularitaetskernBlockEntity;
import dev.riftverse.tech.TechBlock;
import dev.riftverse.tech.WeltenformerBlockEntity;
import dev.riftverse.tech.multiblock.Multiblock;
import dev.riftverse.universe.LeereWelt;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import dev.riftverse.universe.UniverseType;
import dev.riftverse.universe.Zucht;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

import static dev.riftverse.devtest.RauchTest.check;

/** Rauchtest für Meilenstein 5 (T4 Weltenformer). */
final class RauchTestM5 {
    private static BlockPos schmiede, schmiedeOffen, nullCable, weltenformer, kernSatt, kernWachsend, kernRing, kernNotaus,
            kernMaschine, leereLicht;
    private static ServerLevel leere;
    private static final List<BlockPos> futterErde = new ArrayList<>();

    static void aufbauen(MinecraftServer server, ServerLevel level, RecipeManager recipes, BlockPos base) {
        for (int cx = 0; cx <= 2; cx++) {
            for (int cz = base.getZ() >> 4; cz <= (base.getZ() >> 4) + 4; cz++) level.setChunkForced(cx, cz, true);
        }

        // --- Rezepte und Grundwerte -----------------------------------------------------------------------
        check(recipes.getAllRecipesFor(ModRecipes.RIFT_SCHMIEDE_TYPE.get()).size() >= 8, "Rift-Schmiede-Rezepte geladen");
        check(recipes.getAllRecipesFor(ModRecipes.RIFT_SCHMIEDE_TYPE.get()).stream()
                .anyMatch(h -> h.value().result().is(ModItems.PORTAL_KANONE_MK4.get())), "Rift-Schmiede kennt die Kanone Mk IV");
        check(recipes.getRecipeFor(ModRecipes.RIFT_SCHMIEDE_TYPE.get(), new MachineRecipeInput(hullZutaten(), ""), level)
                .map(h -> h.value().result().is(ModItems.NULL_HUELLE.get()) && h.value().result().getCount() == 4).orElse(false),
                "Rift-Schmiede: 4 Graphit-Hülle + Gravitit + 2 Obsidian + Xenit- und Stahlplatten -> 4 Null-Hülle");
        check(UniverseType.reachableWith(4).size() == 10, "Kanone Mk IV erreicht zehn Universumstypen");
        check(UniverseType.randomWith(4).contains(UniverseType.LEERE) && !UniverseType.randomWith(4).contains(UniverseType.GEZUECHTET),
                "Zufallssprünge: Leere Welt ja, gezüchtete Universen nie");
        check(UniverseType.randomWith(3).size() == 8, "Mk III springt zufällig in acht Typen");

        PortalKanoneItem mk2 = ModItems.PORTAL_KANONE_MK2.get(), mk3 = ModItems.PORTAL_KANONE_MK3.get(), mk4 = ModItems.PORTAL_KANONE_MK4.get();
        check(mk3.accepts(3) && mk3.accepts(4) && !mk3.accepts(2), "Mk III nimmt Konzentrat und Weltenformer-Essenz");
        check(mk4.accepts(4) && !mk4.accepts(3) && mk4.shotCost() == 25, "Mk IV: nur Essenz, 25 mB pro Schuss");
        check(!mk2.accepts(4), "Mk II verträgt keine Essenz");
        check(PortalKanoneItem.stufe(new ItemStack(ModItems.FLUID_KARTUSCHE_ESSENZ.get())) == 4, "Essenz-Kartusche hat Fluid-Stufe 4");
        check(ModDataComponents.KanonenTank.ofStufe(100, 0, 4).stufe() == 4, "Kanonentank merkt sich Essenz");

        // --- Gezüchtete Universen: Bauplan steckt in der Zelle ------------------------------------------------
        Zucht z = new Zucht(UniverseType.KRISTALL, UniverseType.HOCHGRAVITATION, 2, false);
        boolean roundtrip = true;
        for (int n : new int[]{0, 7, 49}) roundtrip &= Zucht.of(z.id(n)).equals(z);
        for (UniverseType a : Zucht.BASIS) {
            for (UniverseType b : Zucht.BASIS) {
                for (int g = 0; g < 3; g++) {
                    Zucht x = new Zucht(a, b, g, g == 1);
                    UniverseId id = x.id(13);
                    roundtrip &= Zucht.of(id).equals(x) && Math.abs(id.cellX()) <= Multiverse.MAX_CELL && Math.abs(id.cellZ()) <= Multiverse.MAX_CELL;
                }
            }
        }
        check(roundtrip, "Zucht: Bauplan lässt sich aus der Zelle zurücklesen (alle 384 Kombinationen)");
        UniverseProfile zp = UniverseProfile.get(z.id(3));
        check(zp.look == UniverseType.KRISTALL && zp.rohstoff == UniverseType.HOCHGRAVITATION && Math.abs(zp.gravity - 2.5) < 1e-9
                && !zp.gefahren, "Gezüchtetes Universum: Kristall-Landschaft, Gravitit, 2,5 g, ohne Gefahren");
        UniverseId erst = WeltenformerBlockEntity.erschaffen(level, z), zweit = WeltenformerBlockEntity.erschaffen(level, z);
        check(!erst.equals(zweit) && Zucht.of(erst).equals(z) && Zucht.of(zweit).equals(z),
                "Weltenformer: jede Zucht bekommt ein eigenes Universum gleicher Bauart");
        boolean gravititDa = false;
        for (int y = -60; y < 40 && !gravititDa; y++) {
            for (int x = 0; x < 64 && !gravititDa; x++) {
                BlockState ore = zp.oreAt(x, y, y * 3);
                gravititDa = ore != null && ore.is(ModBlocks.GRAVITIT_ORE.get());
            }
        }
        check(gravititDa, "Gezüchtetes Universum: Erze kommen vom gewählten Rohstoff-Typ (Gravitit)");

        // --- Leere Welt: Ankunftskern, Nullkern mit Wächter, Licht wird gefressen -----------------------------
        leere = server.getLevel(UniverseType.LEERE.dimension());
        if (leere != null) {
            check(!leere.dimensionType().bedWorks() && !leere.dimensionType().respawnAnchorWorks(), "Leere Welt: kein Bett, kein Respawn-Anker");
            check(leere.dimensionType().fixedTime().isPresent(), "Leere Welt: ewige Sternennacht");
            int c = Multiverse.cellCenter(0);
            leere.setChunkForced(c >> 4, c >> 4, true);
            int top = leere.getHeight(Heightmap.Types.MOTION_BLOCKING, c, c);
            check(top > UniverseProfile.ANKUNFT_Y - 4 && top < UniverseProfile.ANKUNFT_Y + 8, "Leere Welt: Ankunftskern in der Zellmitte (Y " + top + ")");
            leereLicht = new BlockPos(c + 2, top, c + 2);
            leere.setBlock(leereLicht, Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
            LeereWelt.merken(leere, leereLicht);
            check(leere.getBlockState(leereLicht).is(Blocks.TORCH), "Leere Welt: Fackel gesetzt");
            nullkern(UniverseProfile.get(new UniverseId(UniverseType.LEERE, 0, 0)));
        } else check(false, "Leere Welt geladen");

        // --- Rift-Schmiede: Quelle IV -> 3 Null-Leitungen -> Schmiede (Multiblock 5 × 5 × 5) -----------------
        schmiede = base.east(4);
        buildSchmiede(level, schmiede, true);
        BlockPos q = schmiede.north(4);
        source(level, q, 4);
        for (int i = 1; i <= 3; i++) place(level, q.south(i), ModBlocks.NULL_LEITUNG.get().defaultBlockState());
        for (int i = 1; i <= 3; i++) connect(level, q.south(i));
        nullCable = q.south(2);
        if (level.getBlockEntity(schmiede) instanceof ProcessingBlockEntity m) {
            check(m.checkStructure() == null, "Rift-Schmiede: Multiblock erkannt");
            List<ItemStack> in = hullZutaten();
            for (int i = 0; i < in.size(); i++) m.items().setStackInSlot(i, in.get(i));
        } else check(false, "Rift-Schmiede hat eine Block-Entity");

        // Ohne Aufbau arbeitet die Schmiede nicht
        schmiedeOffen = base.east(20);
        buildSchmiede(level, schmiedeOffen, false);
        BlockPos q2 = schmiedeOffen.north(2);
        source(level, q2, 4);
        place(level, q2.south(), ModBlocks.NULL_LEITUNG.get().defaultBlockState());
        connect(level, q2.south());
        if (level.getBlockEntity(schmiedeOffen) instanceof ProcessingBlockEntity m) {
            check(m.checkStructure() != null, "Rift-Schmiede ohne Aufbau meldet das fehlende Teil");
            List<ItemStack> in = hullZutaten();
            for (int i = 0; i < in.size(); i++) m.items().setStackInSlot(i, in.get(i));
        }

        // --- Weltenformer: Ring 7 × 7 × 3, zwei Quellen IV, Modus Essenz -------------------------------------
        weltenformer = base.south(12).east(4);
        buildWeltenformer(level, weltenformer);
        BlockPos wc = weltenformer.north();
        place(level, wc, ModBlocks.NULL_LEITUNG.get().defaultBlockState());
        source(level, wc.north(), 4);
        source(level, wc.east(), 4);
        connect(level, wc);
        if (level.getBlockEntity(weltenformer) instanceof WeltenformerBlockEntity w) {
            check(w.checkStructure() == null, "Weltenformer: Ring erkannt");
            int filled = w.fluidHandler().fill(new FluidStack(ModFluids.PORTALFLUID_KONZENTRAT.get(), 8_000), IFluidHandler.FluidAction.EXECUTE);
            check(filled == 8_000 && w.konzentrat() == 8_000, "Weltenformer: Konzentrat über Rohre (" + w.konzentrat() + " mB)");
            check(w.fluidHandler().fill(new FluidStack(ModFluids.PORTALFLUID.get(), 1_000), IFluidHandler.FluidAction.SIMULATE) == 0,
                    "Weltenformer nimmt kein einfaches Portalfluid");
            w.setParameter(WeltenformerBlockEntity.MODE_ESSENZ, 1, true);
            w.items().setStackInSlot(WeltenformerBlockEntity.SLOT_ROH, new ItemStack(ModItems.FLUID_KARTUSCHE.get()));
            w.items().setStackInSlot(WeltenformerBlockEntity.SLOT_ZUTAT, new ItemStack(ModItems.INVERSIONSSPLITTER.get()));
            w.energy().set(w.energy().capacity());
            ItemStack probeA = new ItemStack(ModItems.UNIVERSUMSPROBE.get()), probeB = new ItemStack(ModItems.UNIVERSUMSPROBE.get());
            probeA.set(ModDataComponents.ZIEL.get(), new UniverseId(UniverseType.ORGANISCH, 4, 4));
            probeB.set(ModDataComponents.ZIEL.get(), new UniverseId(UniverseType.SPIEGEL, -2, 9));
            w.items().setStackInSlot(WeltenformerBlockEntity.SLOT_PROBE_A, probeA);
            w.items().setStackInSlot(WeltenformerBlockEntity.SLOT_PROBE_B, probeB);
            Zucht wz = w.zucht();
            check(wz != null && wz.landschaft() == UniverseType.ORGANISCH && wz.rohstoff() == UniverseType.SPIEGEL,
                    "Weltenformer liest Landschaft und Rohstoffe aus den Proben");
        } else check(false, "Weltenformer hat eine Block-Entity");

        // --- Singularitätskern -------------------------------------------------------------------------------
        BlockPos kb = base.south(30);
        kernSatt = kb.east(4);
        place(level, kernSatt, ModBlocks.SINGULARITAETSKERN.get().defaultBlockState());
        if (level.getBlockEntity(kernSatt) instanceof SingularitaetskernBlockEntity k) {
            for (int i = 0; i < SingularitaetskernBlockEntity.FUTTER; i++) k.items().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
            k.schalten();
            check(k.gezuendet(), "Singularitätskern lässt sich zünden");
        } else check(false, "Singularitätskern hat eine Block-Entity");

        // Hungernd und ohne Ring: wächst und frisst die Erde um sich herum, aber keine Maschine
        kernWachsend = kb.east(16);
        place(level, kernWachsend, ModBlocks.SINGULARITAETSKERN.get().defaultBlockState());
        futterErde.clear();
        for (BlockPos p : BlockPos.betweenClosed(kernWachsend.offset(-2, -2, -2), kernWachsend.offset(2, 2, 2))) {
            if (p.equals(kernWachsend) || p.distSqr(kernWachsend) > 4) continue;
            place(level, p, Blocks.DIRT.defaultBlockState());
            futterErde.add(p.immutable());
        }
        kernMaschine = kernWachsend.east();
        // Eine Kiste (Block-Entity, aber kein Netzteilnehmer – eine Xenit-Zelle würde am Stufe-IV-Kern explodieren)
        place(level, kernMaschine, Blocks.CHEST.defaultBlockState());
        futterErde.remove(kernMaschine);
        if (level.getBlockEntity(kernWachsend) instanceof SingularitaetskernBlockEntity k) {
            k.setZustand(true, SingularitaetskernBlockEntity.GEDULD - 1, 0);
        }

        // Hungernd mit Eindämmungsring: schaltet sicher ab
        kernRing = kb.east(28);
        place(level, kernRing, ModBlocks.SINGULARITAETSKERN.get().defaultBlockState());
        for (BlockPos p : Multiblock.eindaemmungsring(kernRing)) place(level, p, ModBlocks.EINDAEMMUNGSRING.get().defaultBlockState());
        if (level.getBlockEntity(kernRing) instanceof SingularitaetskernBlockEntity k) {
            check(k.ringFehlt() == null, "Eindämmungsring erkannt (" + Multiblock.eindaemmungsring(kernRing).size() + " Blöcke)");
            k.setZustand(true, SingularitaetskernBlockEntity.GEDULD - 1, 0);
        }

        // Redstone-Signal: Notabschaltung
        kernNotaus = kb.south(10).east(4);
        place(level, kernNotaus, ModBlocks.SINGULARITAETSKERN.get().defaultBlockState());
        if (level.getBlockEntity(kernNotaus) instanceof SingularitaetskernBlockEntity k) {
            for (int i = 0; i < SingularitaetskernBlockEntity.FUTTER; i++) k.items().setStackInSlot(i, new ItemStack(Items.COBBLESTONE, 64));
            k.schalten();
        }
        place(level, kernNotaus.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
    }

    static void ergebnis(MinecraftServer server, ServerLevel level) {
        // --- Rift-Schmiede und Null-Leitung ---
        check(level.getBlockState(nullCable).getBlock() instanceof CableBlock, "Null-Leitung trägt Stufe IV, ohne zu schmelzen");
        if (level.getBlockEntity(schmiede) instanceof ProcessingBlockEntity m) {
            ItemStack out = m.items().getStackInSlot(m.outputSlot());
            check(out.is(ModItems.NULL_HUELLE.get()) && out.getCount() == 4, "Rift-Schmiede schmiedet mit Stufe IV: " + out);
        } else check(false, "Rift-Schmiede steht noch");
        if (level.getBlockEntity(schmiedeOffen) instanceof ProcessingBlockEntity m) {
            check(m.items().getStackInSlot(m.outputSlot()).isEmpty(), "Rift-Schmiede ohne Aufbau bleibt still");
        } else check(false, "Unvollständige Rift-Schmiede steht noch");

        // --- Weltenformer ---
        if (level.getBlockEntity(weltenformer) instanceof WeltenformerBlockEntity w) {
            ItemStack out = w.items().getStackInSlot(WeltenformerBlockEntity.SLOT_OUT);
            check(out.is(ModItems.FLUID_KARTUSCHE_ESSENZ.get()), "Weltenformer gewinnt Weltenformer-Essenz: " + out);
            check(w.konzentrat() == 8_000 - WeltenformerBlockEntity.ESSENZ_KONZENTRAT, "Weltenformer verbraucht 2.000 mB Konzentrat ("
                    + w.konzentrat() + " mB)");
        } else check(false, "Weltenformer steht noch");

        // --- Singularitätskern ---
        if (level.getBlockEntity(kernSatt) instanceof SingularitaetskernBlockEntity k) {
            int left = 0;
            for (int i = 0; i < SingularitaetskernBlockEntity.FUTTER; i++) left += k.items().getStackInSlot(i).getCount();
            check(left == 0, "Singularitätskern frisst 64 Items pro Sekunde (übrig: " + left + ")");
            check(k.energy().stored() > 1_000_000, "Gefütterter Singularitätskern liefert Strom (" + k.energy().stored() + " FE)");
        } else check(false, "Satter Singularitätskern steht noch");
        if (level.getBlockEntity(kernWachsend) instanceof SingularitaetskernBlockEntity k) {
            long eaten = futterErde.stream().filter(p -> level.getBlockState(p).isAir()).count();
            check(k.wachstum() >= 1 && eaten >= 4, "Hungriger Kern ohne Ring wächst (Stufe " + k.wachstum() + ") und frisst Blöcke ("
                    + eaten + ")");
        } else check(false, "Wachsender Singularitätskern steht noch");
        check(level.getBlockState(kernMaschine).is(Blocks.CHEST), "Der Kern frisst keine Block-Entities (Kiste steht noch)");
        if (level.getBlockEntity(kernRing) instanceof SingularitaetskernBlockEntity k) {
            check(!k.gezuendet() && k.wachstum() == 0, "Eindämmungsring schaltet den hungrigen Kern sicher ab");
        } else check(false, "Singularitätskern im Ring steht noch");
        if (level.getBlockEntity(kernNotaus) instanceof SingularitaetskernBlockEntity k) {
            check(!k.gezuendet(), "Redstone-Signal löst die Notabschaltung aus");
        }

        // --- Leere Welt ---
        if (leere != null && leereLicht != null) {
            check(!leere.getBlockState(leereLicht).is(Blocks.TORCH), "Leere Welt: die Fackel wurde gefressen");
        }
    }

    /** Sucht im Universum einen Nullkern und prüft Spawner, Kammer und Erz. */
    private static void nullkern(UniverseProfile p) {
        int c0 = Multiverse.cellCenter(0) - Multiverse.CELL / 2;
        UniverseProfile.Kern found = null;
        for (int gx = c0 / UniverseProfile.KERN_RASTER + 2; gx < (c0 + Multiverse.CELL) / UniverseProfile.KERN_RASTER - 2 && found == null; gx++) {
            for (int gz = c0 / UniverseProfile.KERN_RASTER + 2; gz < (c0 + Multiverse.CELL) / UniverseProfile.KERN_RASTER - 2 && found == null; gz++) {
                UniverseProfile.Kern k = p.kernIn(gx, gz);
                if (k != null && k.nullkern()) found = k;
            }
        }
        check(found != null, "Leere Welt: es gibt Nullkerne");
        if (found == null) return;
        leere.getChunk(found.x() >> 4, found.z() >> 4);
        BlockPos spawner = new BlockPos(found.x(), found.y() - 1, found.z());
        check(leere.getBlockState(spawner).is(Blocks.SPAWNER) && leere.getBlockEntity(spawner) instanceof SpawnerBlockEntity,
                "Nullkern bei " + spawner.toShortString() + ": Wächter-Spawner in der Kammer");
        check(leere.getBlockState(spawner.above()).isAir(), "Nullkern: Kammer ist frei");
        int erz = 0;
        for (BlockPos b : BlockPos.betweenClosed(spawner.offset(-2, -1, -2), spawner.offset(2, 3, 2))) {
            if (leere.getBlockState(b).is(ModBlocks.NULLMATERIE_ERZ.get())) erz++;
        }
        check(erz >= 1 && erz <= 7, "Nullkern: " + erz + " Nullmaterie-Erz (selten)");
    }

    private static List<ItemStack> hullZutaten() {
        return new ArrayList<>(List.of(new ItemStack(ModItems.GRAPHIT_HUELLE.get(), 4), new ItemStack(ModItems.GRAVITIT.get()),
                new ItemStack(Items.OBSIDIAN, 2), new ItemStack(ModItems.XENIT_PLATTE.get(), 2),
                new ItemStack(ModItems.STAHL_PLATTE.get(), 2)));
    }

    private static void buildSchmiede(ServerLevel level, BlockPos controller, boolean komplett) {
        Direction facing = Direction.NORTH;
        place(level, controller, ModBlocks.RIFT_SCHMIEDE.get().defaultBlockState().setValue(TechBlock.FACING, facing));
        if (!komplett) return;
        for (Multiblock.Part part : Multiblock.RIFT_SCHMIEDE.parts()) {
            BlockState s;
            if (part.hintKey().equals("block.riftverse.graphit_huelle")) s = ModBlocks.GRAPHIT_HUELLE.get().defaultBlockState();
            else if (part.hintKey().equals("block.minecraft.anvil")) s = Blocks.ANVIL.defaultBlockState();
            else s = Blocks.AIR.defaultBlockState();
            place(level, Multiblock.at(controller, facing, part), s);
        }
    }

    private static void buildWeltenformer(ServerLevel level, BlockPos controller) {
        Direction facing = Direction.NORTH;
        place(level, controller, ModBlocks.WELTENFORMER.get().defaultBlockState().setValue(TechBlock.FACING, facing));
        for (Multiblock.Part part : Multiblock.WELTENFORMER.parts()) {
            BlockState s;
            if (part.hintKey().equals("block.riftverse.null_huelle")) s = ModBlocks.NULL_HUELLE.get().defaultBlockState();
            else if (part.hintKey().equals("block.minecraft.glass")) s = Blocks.GLASS.defaultBlockState();
            else s = Blocks.AIR.defaultBlockState();
            place(level, Multiblock.at(controller, facing, part), s);
        }
    }

    /** Kreativ-Quelle mit Stufe {@code tier}. */
    private static void source(ServerLevel level, BlockPos pos, int tier) {
        place(level, pos, ModBlocks.KREATIV_QUELLE.get().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof KreativQuelleBlockEntity q) {
            for (int i = 1; i < tier; i++) q.cycleTier();
        }
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    private static void connect(ServerLevel level, BlockPos cable) {
        level.setBlock(cable, Block.updateFromNeighbourShapes(level.getBlockState(cable), level, cable), Block.UPDATE_ALL);
    }

    private RauchTestM5() {}
}
