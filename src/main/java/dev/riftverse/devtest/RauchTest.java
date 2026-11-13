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
import dev.riftverse.fluid.Zerfall;
import dev.riftverse.item.FluidKartuscheItem;
import dev.riftverse.item.PortalKanoneItem;
import dev.riftverse.portal.RiftPortalEntity;
import dev.riftverse.portal.RiftTravel;
import dev.riftverse.tech.FluidMixerBlockEntity;
import dev.riftverse.tech.ProcessingBlockEntity;
import dev.riftverse.tech.XenitZelleBlockEntity;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.MultiverseData;
import dev.riftverse.universe.UniverseCleanup;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import dev.riftverse.universe.UniverseType;
import dev.riftverse.worldgen.KraterOrte;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rauchtest für die CI ({@code ./gradlew runRauchtest}): startet einen Server, prüft Rezepte,
 * baut ein kleines Netz und die Maschinen aus Meilenstein 2 auf, erzeugt Universen und einen
 * Meteoritenkrater, ab Meilenstein 3 auch Umspanner, Flux-Zelle, T2-Maschinen, Multiblöcke und Kanone Mk II
 * ({@link RauchTestM3}), ab Meilenstein 4 Stufe III, die T3-Maschinen und -Generatoren, Kabelknoten und Kanone Mk III
 * ({@link RauchTestM4}), ab Meilenstein 5 Stufe IV, Rift-Schmiede, Weltenformer, Singularitätskern, Leere Welt und
 * gezüchtete Universen ({@link RauchTestM5}), lässt alles laufen und beendet den Server wieder.
 * Nur aktiv mit {@code -Driftverse.rauchtest=true}; im normalen Spiel passiert hier nichts.
 */
public final class RauchTest {
    private static final int RUN_TICKS = 380;

    private static final List<String> failures = new ArrayList<>();
    private static int ticks = -1;
    private static BlockPos source, mill, overvoltageMachine;
    private static BlockPos grinder, recycler, mixer, cell, cellConsumer;
    private static Pig traveler;

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean("riftverse.rauchtest")) return;
        NeoForge.EVENT_BUS.addListener(RauchTest::onStarted);
        NeoForge.EVENT_BUS.addListener(RauchTest::onTick);
        Riftverse.LOGGER.info("RIFTVERSE-RAUCHTEST: aktiv");
    }

    static void check(boolean ok, String what) {
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
        for (int cz = 0; cz <= 3; cz++) level.setChunkForced(0, cz, true);   // alle Testaufbauten müssen ticken
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

        meilenstein2(server, level, recipes, base);
        RauchTestM3.aufbauen(server, level, recipes, base.south(64));
        RauchTestM4.aufbauen(server, level, recipes, base.south(112));
        RauchTestM5.aufbauen(server, level, recipes, base.south(176));
        RauchTestM6.aufbauen(server, level, recipes, base.south(240));
        ticks = 0;
    }

    // =====================================================================
    // Meilenstein 2
    // =====================================================================
    private static void meilenstein2(MinecraftServer server, ServerLevel level, RecipeManager recipes, BlockPos base) {
        // --- Rezepte -------------------------------------------------------------
        check(recipes.getAllRecipesFor(ModRecipes.XENIT_SCHMELZE_TYPE.get()).size() >= 2, "Xenit-Schmelze-Rezepte geladen");
        check(!recipes.getAllRecipesFor(ModRecipes.PLATINEN_AETZER_TYPE.get()).isEmpty(), "Platinen-Ätzer-Rezepte geladen");
        check(!recipes.getAllRecipesFor(ModRecipes.KRISTALL_SCHLEIFER_TYPE.get()).isEmpty(), "Kristall-Schleifer-Rezepte geladen");
        check(!recipes.getAllRecipesFor(ModRecipes.RECYCLER_TYPE.get()).isEmpty(), "Recycler-Rezepte geladen");
        check(!recipes.getAllRecipesFor(ModRecipes.FLUID_MIXER_TYPE.get()).isEmpty(), "Fluid-Mixer-Rezepte geladen");
        check(recipes.getRecipeFor(ModRecipes.PLATINEN_AETZER_TYPE.get(), new MachineRecipeInput(List.of(
                        new ItemStack(ModItems.KUPFERDRAHT.get(), 3), new ItemStack(Items.REDSTONE), new ItemStack(ModItems.SCHROTTPLATTE.get())), ""), level)
                .map(h -> h.value().result().is(ModItems.BASIS_SCHALTKREIS.get())).orElse(false),
                "Platinen-Ätzer: Schrottplatte + Redstone + 3 Kupferdraht -> Basis-Schaltkreis");

        // --- Maschinen mit vollem Puffer: Schleifer (2 Ausgänge), Recycler (4), Fluid-Mixer ---
        BlockPos row = base.south(24);
        grinder = row;
        recycler = row.east(3);
        mixer = row.east(6);
        level.setBlock(grinder, ModBlocks.KRISTALL_SCHLEIFER.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(recycler, ModBlocks.RECYCLER.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(mixer, ModBlocks.FLUID_MIXER.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(grinder) instanceof ProcessingBlockEntity m) {
            m.items().setStackInSlot(0, new ItemStack(ModItems.FLUXKRISTALL.get(), 2));
            m.energy().set(m.energy().capacity());
        } else check(false, "Kristall-Schleifer hat eine Block-Entity");
        if (level.getBlockEntity(recycler) instanceof ProcessingBlockEntity m) {
            m.items().setStackInSlot(0, new ItemStack(ModItems.ALIEN_SCHROTT.get(), 8));
            m.energy().set(m.energy().capacity());
        } else check(false, "Recycler hat eine Block-Entity");
        if (level.getBlockEntity(mixer) instanceof FluidMixerBlockEntity m) {
            m.items().setStackInSlot(FluidMixerBlockEntity.SLOT_INPUT, new ItemStack(ModItems.FLUXSTAUB.get(), 2));
            m.items().setStackInSlot(FluidMixerBlockEntity.SLOT_BUCKET_IN, new ItemStack(Items.WATER_BUCKET));
            m.items().setStackInSlot(FluidMixerBlockEntity.SLOT_CART_IN, new ItemStack(ModItems.FLUID_KARTUSCHE.get()));
            m.energy().set(m.energy().capacity());
        } else check(false, "Fluid-Mixer hat eine Block-Entity");

        // --- Xenit-Zelle: Quelle -> Kabel -> Zelle -> Kabel -> Walzbank -----------------------------
        BlockPos line = base.south(36);
        cell = line.east(2);
        cellConsumer = line.east(4);
        level.setBlock(line, ModBlocks.KREATIV_QUELLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(line.east(1), ModBlocks.KUPFERKABEL.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(cell, ModBlocks.XENIT_ZELLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(line.east(3), ModBlocks.KUPFERKABEL.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(cellConsumer, ModBlocks.WALZBANK.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i : new int[]{1, 3}) {
            BlockPos p = line.east(i);
            level.setBlock(p, Block.updateFromNeighbourShapes(level.getBlockState(p), level, p), Block.UPDATE_ALL);
        }

        // --- Universen ------------------------------------------------------------------
        for (UniverseType type : UniverseType.values()) {
            ServerLevel u = server.getLevel(type.dimension());
            check(u != null, "Dimension " + type.id() + " geladen");
            if (u == null) continue;
            int c = Multiverse.cellCenter(0);
            u.getChunk(c >> 4, c >> 4);
            int top = u.getHeight(Heightmap.Types.WORLD_SURFACE, c, c);
            UniverseProfile p = UniverseProfile.get(new UniverseId(type, 0, 0));
            check(top > u.getMinBuildHeight() + 8, type.id() + ": Gelände erzeugt (Oberfläche bei Y " + top + ", "
                    + p.terrain.label + ", " + p.modifiers + ")");
            check(!p.modifiers.isEmpty() && p.modifiers.size() <= 3, type.id() + ": 1–3 Modifikatoren");
            check(Multiverse.at(u, new BlockPos(c, 64, c)) != null, type.id() + ": Position liegt in einem Universum");
            BlockPos land = RiftTravel.findSafe(u, new BlockPos(c + 40, 100, c - 40), 32, false);
            check(RiftTravel.isSafe(u, land) && new UniverseId(type, 0, 0).equals(Multiverse.at(u, land)),
                    type.id() + ": sicherer Landeplatz " + land.toShortString());
        }

        // --- Rift: Ein Schwein läuft durch ein Portal in ein Universum ----------------------------
        BlockPos portalAt = base.south(48);
        level.setBlock(portalAt.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        traveler = EntityType.PIG.create(level);
        if (traveler != null) {
            traveler.setNoAi(true);
            traveler.moveTo(portalAt.getX() + 0.5, portalAt.getY(), portalAt.getZ() + 0.5, 0f, 0f);
            level.addFreshEntity(traveler);
            RiftPortalEntity portal = RiftPortalEntity.create(level, Vec3.atBottomCenterOf(portalAt), 0f, RiftPortalEntity.STYLE_RANDOM, 200);
            UniverseId target = new UniverseId(UniverseType.KRISTALL, 1, 0);
            portal.setUniverseTarget(target.type().dimension(), target);
            portal.setSpawnReturn(false);
            level.addFreshEntity(portal);
        }
        check(UniverseType.reachableWith(1).size() == 3, "Kanone Mk I erreicht drei Universumstypen");

        // --- Meteoritenkrater -------------------------------------------------------------
        BlockPos crater = KraterOrte.nearest(level, 0, 0, 3);
        check(crater != null, "Meteoritenkrater in der Nähe des Spawns berechnet: " + crater);
        if (crater != null) {
            ChunkAccess chunk = level.getChunk(crater.getX() >> 4, crater.getZ() >> 4);
            int cores = 0, ores = 0;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                    for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                        BlockState st = chunk.getBlockState(pos.set(x, y, z));
                        if (st.is(ModBlocks.METEORITENKERN.get())) cores++;
                        else if (st.is(ModBlocks.XENIT_ORE.get()) || st.is(ModBlocks.DEEPSLATE_XENIT_ORE.get())) ores++;
                    }
                }
            }
            check(cores == 1, "Krater-Chunk enthält den Meteoritenkern (" + cores + ")");
            check(ores >= 20, "Krater-Chunk enthält Xenit-Erz (" + ores + ")");
            check(KraterOrte.distanceToNearest(level, crater.getX() + 150, crater.getZ()) <= 151, "Xenit-Zone findet den Krater");
        }
        check(Math.abs(KraterOrte.zoneWeight(0) - 1.0) < 1e-9 && Math.abs(KraterOrte.zoneWeight(150) - 0.25) < 1e-9
                && KraterOrte.zoneWeight(200) == 0.0, "Xenit-Zone: 100 % am Krater, 25 % bei 150, 0 bei 200 Blöcken");

        // --- Aufräumen der Universen ------------------------------------------------------
        MultiverseData.Record rec = new MultiverseData.Record();
        rec.lastVisit = 0;
        long tenDays = 10 * 24000L;
        check(UniverseCleanup.shouldReset(rec, tenDays, tenDays, 64), "Unbebautes Universum wird nach 10 Tagen zurückgesetzt");
        check(!UniverseCleanup.shouldReset(rec, tenDays - 1, tenDays, 64), "… aber nicht vorher");
        rec.anchors.add(BlockPos.ZERO.asLong());
        check(!UniverseCleanup.shouldReset(rec, tenDays, tenDays, 64), "Universum mit Rift-Anker bleibt");
        rec.anchors.clear();
        rec.blockEntities.add(BlockPos.ZERO.asLong());
        check(!UniverseCleanup.shouldReset(rec, tenDays, tenDays, 64), "Universum mit Truhe/Maschine bleibt");
        try {
            Path root = Files.createTempDirectory("riftverse-rauchtest");
            UniverseId id = new UniverseId(UniverseType.SCHROTT, -1, 2);
            Path region = DimensionType.getStorageFolder(id.type().dimension(), root).resolve("region");
            Files.createDirectories(region);
            Files.createFile(region.resolve("r.-8.16.mca"));    // gehört zu Zelle (-1, 2)
            Files.createFile(region.resolve("r.-1.23.mca"));    // gehört zu Zelle (-1, 2)
            Files.createFile(region.resolve("r.0.16.mca"));     // Nachbarzelle (0, 2) bleibt
            int deleted = UniverseCleanup.deleteTerrain(root, id);
            check(deleted == 2 && Files.exists(region.resolve("r.0.16.mca")), "Aufräumen löscht genau die 8 × 8 Regionen der Zelle");
        } catch (Exception e) {
            check(false, "Aufräumen-Test: " + e);
        }
    }

    private static void kanonenTest(ServerLevel level) {
        // --- Portal-Kanone: Fluid aus Kartuschen, 250 mB pro Schuss, Zerfall ------------------------------
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.getInventory().clearContent();
        ItemStack gun = new ItemStack(ModItems.PORTAL_KANONE_MK1.get());
        PortalKanoneItem kanone = ModItems.PORTAL_KANONE_MK1.get();
        check(!kanone.fire(player, gun), "Kanone ohne Fluid schießt nicht");
        long now = Zerfall.now(level);   // läuft erst nach einigen hundert Ticks, damit "zerfallen" > 0 ist
        player.getInventory().add(FluidKartuscheItem.gefuellt(Math.max(1, now - 10)));
        player.getCooldowns().removeCooldown(kanone);
        check(!kanone.fire(player, gun), "Zerfallene Kartusche wird nicht geladen");
        player.getInventory().clearContent();
        player.getInventory().add(FluidKartuscheItem.gefuellt(now + Zerfall.DAUER));
        player.getCooldowns().removeCooldown(kanone);
        check(kanone.fire(player, gun) && PortalKanoneItem.tank(gun).mb() == 750,
                "Kanone lädt eine Kartusche und verbraucht 250 mB (" + PortalKanoneItem.tank(gun).mb() + " übrig)");
        check(player.getInventory().countItem(ModItems.FLUID_KARTUSCHE.get()) == 1, "Leere Kartusche kommt zurück ins Inventar");
        player.getInventory().clearContent();
    }

    private static void meilenstein2Ergebnis(ServerLevel level) {
        kanonenTest(level);
        check(traveler != null && traveler.isRemoved() && traveler.getRemovalReason() == Entity.RemovalReason.CHANGED_DIMENSION,
                "Rift bringt ein Schwein in die Kristallwelt" + (traveler != null ? " (" + traveler.getRemovalReason() + ")" : ""));
        if (level.getBlockEntity(grinder) instanceof ProcessingBlockEntity m) {
            ItemStack lens = m.items().getStackInSlot(m.outputSlot());
            ItemStack dust = m.items().getStackInSlot(m.outputSlot() + 1);
            check(lens.is(ModItems.FLUXLINSE.get()) && dust.is(ModItems.FLUXSTAUB.get()) && dust.getCount() >= 2,
                    "Kristall-Schleifer: Fluxkristall -> Fluxlinse + 2 Fluxstaub (" + lens + ", " + dust + ")");
        }
        if (level.getBlockEntity(recycler) instanceof ProcessingBlockEntity m) {
            int polymer = 0;
            for (int i = 0; i < 4; i++) {
                ItemStack s = m.items().getStackInSlot(m.outputSlot() + i);
                if (s.is(ModItems.POLYMERFETZEN.get())) polymer += s.getCount();
            }
            check(polymer >= 2, "Recycler: Alien-Schrott -> Polymerfetzen (" + polymer + ")");
        }
        if (level.getBlockEntity(mixer) instanceof FluidMixerBlockEntity m) {
            ItemStack full = m.items().getStackInSlot(FluidMixerBlockEntity.SLOT_CART_OUT);
            check(full.is(ModItems.FLUID_KARTUSCHE_VOLL.get()) && FluidKartuscheItem.zerfall(full) > Zerfall.now(level),
                    "Fluid-Mixer: Fluxstaub + Wasser -> volle Kartusche mit Zerfallszeit (" + full + ")");
            check(m.items().getStackInSlot(FluidMixerBlockEntity.SLOT_BUCKET_OUT).is(Items.BUCKET), "Fluid-Mixer gibt den leeren Eimer zurück");
        }
        if (level.getBlockEntity(cell) instanceof XenitZelleBlockEntity z) {
            check(z.energy().stored() > 0, "Xenit-Zelle lädt aus dem Netz: " + z.energy().stored() + " FE");
        } else check(false, "Xenit-Zelle steht noch");
        if (level.getBlockEntity(cellConsumer) instanceof ProcessingBlockEntity m) {
            check(m.energy().stored() > 0, "Xenit-Zelle versorgt die Walzbank: " + m.energy().stored() + " FE");
        } else check(false, "Walzbank hinter der Zelle steht noch");
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
        meilenstein2Ergebnis(level);
        RauchTestM3.ergebnis(server, level);
        RauchTestM4.ergebnis(server, level);
        RauchTestM5.ergebnis(server, level);
        RauchTestM6.ergebnis(server, level);

        if (failures.isEmpty()) {
            Riftverse.LOGGER.info("RIFTVERSE-RAUCHTEST: BESTANDEN");
        } else {
            Riftverse.LOGGER.error("RIFTVERSE-RAUCHTEST: NICHT BESTANDEN ({} Fehler): {}", failures.size(), failures);
        }
        server.halt(false);
    }

    private RauchTest() {}
}
