package dev.riftverse.devtest;

import dev.riftverse.energy.KreativQuelleBlockEntity;
import dev.riftverse.energy.UmspannerBlock;
import dev.riftverse.item.PortalKanoneItem;
import dev.riftverse.recipe.MachineRecipeInput;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModDataComponents.KanonenTank;
import dev.riftverse.registry.ModDataComponents.KanonenZiele;
import dev.riftverse.registry.ModFluids;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModRecipes;
import dev.riftverse.tech.ChronoSpeicherBlockEntity;
import dev.riftverse.tech.KlonTankBlock;
import dev.riftverse.tech.KlonTankBlockEntity;
import dev.riftverse.tech.MaterieRekonfiguratorBlockEntity;
import dev.riftverse.tech.MikrokosmosZelleBlockEntity;
import dev.riftverse.tech.ParallelzapferBlockEntity;
import dev.riftverse.tech.ProcessingBlockEntity;
import dev.riftverse.tech.RaffinerieBlockEntity;
import dev.riftverse.tech.RiftKabelknotenBlockEntity;
import dev.riftverse.tech.TechBlock;
import dev.riftverse.tech.UniversumsDestillatorBlockEntity;
import dev.riftverse.tech.multiblock.Multiblock;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.MultiverseData;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import dev.riftverse.universe.UniverseType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

import static dev.riftverse.devtest.RauchTest.check;

/** Rauchtest für Meilenstein 4 (T3 Multiversum-Industrie). */
final class RauchTestM4 {
    private static BlockPos lithograf, gravCable, fluxMelt, umspannerMachine, chrono, chronoA, chronoB, rekonfigurator,
            destillator, klon, knotenA, knotenB, zelle, zapfer, raffinerie;
    private static ServerLevel knotenLevel;

    static void aufbauen(MinecraftServer server, ServerLevel level, RecipeManager recipes, BlockPos base) {
        for (int cx = 0; cx <= 1; cx++) {
            for (int cz = base.getZ() >> 4; cz <= (base.getZ() >> 4) + 4; cz++) level.setChunkForced(cx, cz, true);
        }

        // --- Rezepte und Grundwerte --------------------------------------------------------------
        check(recipes.getAllRecipesFor(ModRecipes.QUANTEN_LITHOGRAF_TYPE.get()).size() >= 3, "Quanten-Lithograf-Rezepte geladen");
        check(recipes.getRecipeFor(ModRecipes.QUANTEN_LITHOGRAF_TYPE.get(), new MachineRecipeInput(lithografZutaten(), ""), level)
                .map(h -> h.value().result().is(ModItems.QUANTEN_SCHALTKREIS.get())).orElse(false),
                "Lithograf: Rift-Schaltkreis + Chronoglas + 2 Golddraht -> Quanten-Schaltkreis");
        check(recipes.getAllRecipesFor(ModRecipes.PRAEZISIONS_MONTIERER_TYPE.get()).stream()
                .anyMatch(h -> h.value().result().is(ModItems.PORTAL_KANONE_MK3.get())), "Montierer kennt die Kanone Mk III");
        check(recipes.getAllRecipesFor(ModRecipes.RAFFINERIE_TYPE.get()).size() >= 2, "Raffinerie kennt Portalfluid-Konzentrat");
        check(new ItemStack(ModItems.FRAGMENT_REPLIK.get()).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                dev.riftverse.Riftverse.id("story_fragmente"))), "Fragment-Replik steht im Tag riftverse:story_fragmente");
        check(UniverseType.reachableWith(3).size() == 8, "Kanone Mk III erreicht acht Universumstypen");
        check(Math.abs(UniverseProfile.get(new UniverseId(UniverseType.HOCHGRAVITATION, 0, 0)).gravity - 2.5) < 1e-9,
                "Hochgravitationswelt hat 2,5 g");

        // Spiegelwelt: vertauschte Tageszeit (Mixin) und gespiegeltes Gelände
        ServerLevel spiegel = server.getLevel(UniverseType.SPIEGEL.dimension());
        if (spiegel != null) {
            float mittag = spiegel.dimensionType().timeOfDay(6_000L);
            float mitternacht = level.dimensionType().timeOfDay(18_000L);
            check(Math.abs(mittag - mitternacht) < 1e-4, "Spiegelwelt: Mittag in der Overworld ist dort Mitternacht ("
                    + mittag + " / " + mitternacht + ")");
        } else check(false, "Spiegelwelt geladen");
        UniverseProfile sp = UniverseProfile.get(new UniverseId(UniverseType.SPIEGEL, 2, -1));
        int axis = Multiverse.cellCenter(2);
        int[] a = new int[2], b = new int[2];
        boolean la = sp.column(axis + 137, Multiverse.cellCenter(-1) + 50, a);
        boolean lb = sp.column(axis - 137, Multiverse.cellCenter(-1) + 50, b);
        check(la == lb && a[1] == b[1], "Spiegelwelt: Gelände ist an der Mittelachse gespiegelt (" + a[1] + " / " + b[1] + ")");

        // --- Kanonen-Ziele (Mk III) --------------------------------------------------------------------
        UniverseId z1 = new UniverseId(UniverseType.SPIEGEL, 1, 1), z2 = new UniverseId(UniverseType.HOCHGRAVITATION, -3, 4);
        KanonenZiele ziele = KanonenZiele.LEER.merken(z1).merken(z2);
        check(ziele.ziele().size() == 2 && z2.equals(ziele.gewaehlt()), "Mk III merkt sich Ziele und wählt das neueste");
        KanonenZiele w = KanonenZiele.LEER.weiter();
        check(w.auswahl() == KanonenZiele.KURZSTRECKE && w.weiter().auswahl() == KanonenZiele.ZUFALL,
                "Ziel-Taste: Zufall -> Kurzstrecke -> Zufall (ohne gespeicherte Ziele)");
        KanonenZiele voll = KanonenZiele.LEER;
        for (int i = 0; i < 10; i++) voll = voll.merken(new UniverseId(UniverseType.SCHROTT, i, 0));
        check(voll.ziele().size() == KanonenZiele.MAX, "Höchstens 8 gespeicherte Ziele");

        // --- Netz Stufe III: Quelle III -> Gravitit-Leitung -> Quanten-Lithograf --------------------------
        BlockPos line = base;
        source(level, line, 3);
        place(level, line.east(1), ModBlocks.GRAVITIT_LEITUNG.get().defaultBlockState());
        place(level, line.east(2), ModBlocks.QUANTEN_LITHOGRAF.get().defaultBlockState());
        connect(level, line.east(1));
        gravCable = line.east(1);
        lithograf = line.east(2);
        if (level.getBlockEntity(lithograf) instanceof ProcessingBlockEntity m) {
            List<ItemStack> in = lithografZutaten();
            for (int i = 0; i < 3; i++) m.items().setStackInSlot(i, in.get(i));
        } else check(false, "Quanten-Lithograf hat eine Block-Entity");

        // Flux-Leitung (Stufe II) an einer Stufe-III-Quelle schmilzt
        BlockPos melt = base.south(3);
        source(level, melt, 3);
        place(level, melt.east(1), ModBlocks.FLUX_LEITUNG.get().defaultBlockState());
        place(level, melt.east(2), ModBlocks.ERZMUEHLE.get().defaultBlockState());
        connect(level, melt.east(1));
        fluxMelt = melt.east(1);

        // --- Umspanner III <-> IV: Quelle IV -> Umspanner -> doppelt isolierte Gravitit-Leitung -> T3-Maschine --
        BlockPos up = base.south(6);
        source(level, up, 4);
        place(level, up.east(1), ModBlocks.UMSPANNER_III.get().defaultBlockState().setValue(UmspannerBlock.FACING, Direction.WEST));
        place(level, up.east(2), ModBlocks.GRAVITIT_LEITUNG_DOPPELT.get().defaultBlockState());
        place(level, up.east(3), ModBlocks.QUANTEN_LITHOGRAF.get().defaultBlockState());
        connect(level, up.east(2));
        umspannerMachine = up.east(3);

        // --- Chrono-Speicher: lädt höchstens 2.048 FE/t; zwei Speicher im Verbund gleichen sich aus --------
        BlockPos cl = base.south(9);
        source(level, cl, 3);
        place(level, cl.east(1), ModBlocks.GRAVITIT_LEITUNG_ISOLIERT.get().defaultBlockState());
        place(level, cl.east(2), ModBlocks.CHRONO_SPEICHER.get().defaultBlockState());
        connect(level, cl.east(1));
        chrono = cl.east(2);
        chronoA = base.south(12);
        chronoB = chronoA.east();
        place(level, chronoA, ModBlocks.CHRONO_SPEICHER.get().defaultBlockState());
        place(level, chronoB, ModBlocks.CHRONO_SPEICHER.get().defaultBlockState());
        if (level.getBlockEntity(chronoA) instanceof ChronoSpeicherBlockEntity c) c.energy().set(1_000_000);

        // --- Materie-Rekonfigurator: Alien-Schrott -> Fluxkristall (T1-Universen) ------------------------
        rekonfigurator = base.south(15);
        place(level, rekonfigurator, ModBlocks.MATERIE_REKONFIGURATOR.get().defaultBlockState());
        if (level.getBlockEntity(rekonfigurator) instanceof MaterieRekonfiguratorBlockEntity r) {
            r.items().setStackInSlot(MaterieRekonfiguratorBlockEntity.SLOT_IN, new ItemStack(ModItems.ALIEN_SCHROTT.get(), 2));
            r.items().setStackInSlot(MaterieRekonfiguratorBlockEntity.SLOT_MUSTER, new ItemStack(ModItems.FLUXKRISTALL.get()));
            r.items().setStackInSlot(MaterieRekonfiguratorBlockEntity.SLOT_CART_IN, new ItemStack(ModItems.FLUID_KARTUSCHE_STABIL.get()));
            r.energy().set(r.energy().capacity());
            check(MaterieRekonfiguratorBlockEntity.gemeinsameStufe(new ItemStack(ModItems.GRAVITIT.get()),
                    new ItemStack(ModItems.FLUXKRISTALL.get())) == null, "Rekonfigurator: Gravitit und Fluxkristall passen nicht zusammen");
        } else check(false, "Materie-Rekonfigurator hat eine Block-Entity");

        // --- Universums-Destillator: Probe aus der Hochgravitationswelt -> Gravitit -----------------------
        destillator = base.south(15).east(3);
        place(level, destillator, ModBlocks.UNIVERSUMS_DESTILLATOR.get().defaultBlockState());
        if (level.getBlockEntity(destillator) instanceof UniversumsDestillatorBlockEntity d) {
            ItemStack probe = new ItemStack(ModItems.UNIVERSUMSPROBE.get());
            probe.set(ModDataComponents.ZIEL.get(), new UniverseId(UniverseType.HOCHGRAVITATION, 5, 5));
            d.items().setStackInSlot(UniversumsDestillatorBlockEntity.SLOT_PROBE, probe);
            d.energy().set(d.energy().capacity());
        } else check(false, "Universums-Destillator hat eine Block-Entity");

        // --- Klon-Tank: zwei Blöcke hoch, gebunden, wächst ----------------------------------------------
        klon = base.south(18);
        BlockState lower = ModBlocks.KLON_TANK.get().defaultBlockState();
        place(level, klon, lower);
        place(level, klon.above(), lower.setValue(KlonTankBlock.HALF, DoubleBlockHalf.UPPER));
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        if (level.getBlockEntity(klon) instanceof KlonTankBlockEntity k) {
            k.bind(player);
            k.items().setStackInSlot(KlonTankBlockEntity.SLOT_BIOMASSE, new ItemStack(ModItems.BIOMASSE_PULPE.get(), 16));
            k.items().setStackInSlot(KlonTankBlockEntity.SLOT_SERUM, new ItemStack(ModItems.NAEHRSERUM.get(), 4));
            k.energy().set(k.energy().capacity());
        } else check(false, "Klon-Tank hat unten eine Block-Entity");
        check(level.getBlockEntity(klon.above()) == null, "Klon-Tank: obere Hälfte ohne Block-Entity");

        // --- Rift-Kabelknoten: Overworld -> Schrottwelt ---------------------------------------------------
        knotenA = base.south(21);
        place(level, knotenA, ModBlocks.RIFT_KABELKNOTEN.get().defaultBlockState());
        knotenLevel = server.getLevel(UniverseType.SCHROTT.dimension());
        if (knotenLevel != null) {
            int c = Multiverse.cellCenter(0);
            knotenB = new BlockPos(c + 3, 250, c + 3);
            knotenLevel.getChunk(knotenB.getX() >> 4, knotenB.getZ() >> 4);
            place(knotenLevel, knotenB, ModBlocks.RIFT_KABELKNOTEN.get().defaultBlockState());
            if (level.getBlockEntity(knotenA) instanceof RiftKabelknotenBlockEntity ka
                    && knotenLevel.getBlockEntity(knotenB) instanceof RiftKabelknotenBlockEntity kb) {
                RiftKabelknotenBlockEntity.koppeln(ka, kb);
                ka.energy().set(ka.energy().capacity());
                ka.itemHandler().insertItem(0, new ItemStack(Items.DIAMOND, 5), false);
                ka.fluidHandler().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
                check(kb.itemHandler().insertItem(0, new ItemStack(Items.DIRT), true).getCount() == 1, "Empfangender Knoten nimmt keine Items an");
            } else check(false, "Rift-Kabelknoten haben Block-Entities");
        } else check(false, "Schrottwelt für den Kabelknoten-Test geladen");

        // --- Mikrokosmos-Zelle: Nahrung und Material ---------------------------------------------------------
        zelle = base.south(24);
        place(level, zelle, ModBlocks.MIKROKOSMOS_ZELLE.get().defaultBlockState());
        if (level.getBlockEntity(zelle) instanceof MikrokosmosZelleBlockEntity z) {
            z.items().setStackInSlot(MikrokosmosZelleBlockEntity.SLOT_NAHRUNG, new ItemStack(Items.BREAD, 64));
            z.items().setStackInSlot(MikrokosmosZelleBlockEntity.SLOT_MATERIAL, new ItemStack(Items.IRON_INGOT, 64));
            check(!z.items().isItemValid(MikrokosmosZelleBlockEntity.SLOT_NAHRUNG, new ItemStack(Items.STONE)), "Mikrokosmos-Zelle: Stein ist keine Nahrung");
        } else check(false, "Mikrokosmos-Zelle hat eine Block-Entity");

        // --- Parallelzapfer (Multiblock 3 × 3 × 3) ----------------------------------------------------------
        zapfer = base.south(28).east(2);
        buildZapfer(level, zapfer);
        if (level.getBlockEntity(zapfer) instanceof ParallelzapferBlockEntity p) {
            check(p.checkStructure() == null, "Parallelzapfer: Multiblock erkannt");
            int filled = p.fluidHandler().fill(new FluidStack(ModFluids.PORTALFLUID_KONZENTRAT.get(), 1000), IFluidHandler.FluidAction.EXECUTE);
            check(filled == 1000 && p.tank().mb() == 2000, "Parallelzapfer: Konzentrat zählt doppelt (" + p.tank().mb() + " mB)");
        } else check(false, "Parallelzapfer hat eine Block-Entity");

        // --- Raffinerie: Roh-Portalfluid + Chronoglas + Stabilisator -> Konzentrat ---------------------------------
        raffinerie = base.south(28).east(8);
        place(level, raffinerie, ModBlocks.PORTALFLUID_RAFFINERIE.get().defaultBlockState().setValue(TechBlock.FACING, Direction.NORTH));
        for (Multiblock.Part part : Multiblock.RAFFINERIE.parts()) {
            BlockState s = part.hintKey().equals("block.riftverse.labor_huelle") ? ModBlocks.LABOR_HUELLE.get().defaultBlockState()
                    : Blocks.GLASS.defaultBlockState();
            place(level, Multiblock.at(raffinerie, Direction.NORTH, part), s);
        }
        if (level.getBlockEntity(raffinerie) instanceof RaffinerieBlockEntity r) {
            r.checkStructure();
            r.items().setStackInSlot(RaffinerieBlockEntity.SLOT_IN_A, new ItemStack(ModItems.CHRONOGLAS_SCHEIBE.get()));
            r.items().setStackInSlot(RaffinerieBlockEntity.SLOT_IN_B, new ItemStack(ModItems.FLUX_STABILISATOR.get()));
            r.fluidHandler().fill(new FluidStack(ModFluids.ROH_PORTALFLUID.get(), 2000), IFluidHandler.FluidAction.EXECUTE);
            r.energy().set(r.energy().capacity());
        } else check(false, "Zweite Raffinerie hat eine Block-Entity");
    }

    static void ergebnis(MinecraftServer server, ServerLevel level) {
        check(level.getBlockState(gravCable).is(ModBlocks.GRAVITIT_LEITUNG.get()), "Gravitit-Leitung verträgt Stufe III");
        if (level.getBlockEntity(lithograf) instanceof ProcessingBlockEntity m) {
            ItemStack out = m.items().getStackInSlot(m.outputSlot());
            check(out.is(ModItems.QUANTEN_SCHALTKREIS.get()), "Quanten-Lithograf mit Netzstrom (Stufe III) -> Quanten-Schaltkreis (" + out + ")");
        } else check(false, "Quanten-Lithograf steht noch");
        check(level.getBlockState(fluxMelt).isAir(), "Flux-Leitung schmilzt unter Stufe III");
        if (level.getBlockEntity(umspannerMachine) instanceof ProcessingBlockEntity m) {
            check(m.energy().stored() > 0, "Umspanner III <-> IV versorgt eine T3-Maschine (" + m.energy().stored() + " FE)");
        } else check(false, "T3-Maschine hinter dem Umspanner III <-> IV steht noch");

        if (level.getBlockEntity(chrono) instanceof ChronoSpeicherBlockEntity c) {
            int e = c.energy().stored();
            check(e > 0 && e <= ChronoSpeicherBlockEntity.CHARGE_RATE * 400, "Chrono-Speicher lädt höchstens 2.048 FE/t (" + e + " FE)");
        } else check(false, "Chrono-Speicher steht noch");
        if (level.getBlockEntity(chronoA) instanceof ChronoSpeicherBlockEntity ca && level.getBlockEntity(chronoB) instanceof ChronoSpeicherBlockEntity cb) {
            check(Math.abs(ca.energy().stored() - cb.energy().stored()) < 10 && cb.energy().stored() > 0,
                    "Chrono-Speicher im Verbund gleichen sich aus (" + ca.energy().stored() + " / " + cb.energy().stored() + ")");
        }

        if (level.getBlockEntity(rekonfigurator) instanceof MaterieRekonfiguratorBlockEntity r) {
            ItemStack out = r.items().getStackInSlot(MaterieRekonfiguratorBlockEntity.SLOT_OUT);
            check(out.is(ModItems.FLUXKRISTALL.get()) && r.tank().mb() == 1000 - MaterieRekonfiguratorBlockEntity.FLUID_PER_OP,
                    "Rekonfigurator: Alien-Schrott -> Fluxkristall für 50 mB (" + out + ", " + r.tank().mb() + " mB)");
        }
        if (level.getBlockEntity(destillator) instanceof UniversumsDestillatorBlockEntity d) {
            ItemStack out = d.items().getStackInSlot(UniversumsDestillatorBlockEntity.SLOT_OUT);
            ItemStack probe = d.items().getStackInSlot(UniversumsDestillatorBlockEntity.SLOT_PROBE);
            check(out.is(ModItems.GRAVITIT.get()) && probe.getOrDefault(ModDataComponents.VERSCHLEISS.get(), 0) == 1,
                    "Destillator: Hochgravitations-Probe -> Gravitit, Probe nutzt sich ab (" + out + ")");
        }
        if (level.getBlockEntity(klon) instanceof KlonTankBlockEntity k) {
            check(k.wachstum() > 0 && k.items().getStackInSlot(KlonTankBlockEntity.SLOT_BIOMASSE).isEmpty(),
                    "Klon-Tank: Zutaten verbraucht, Klon wächst (" + k.wachstum() + "/" + KlonTankBlockEntity.WACHSTUM + ")");
            check(dev.riftverse.tech.Klone.get(server).tank(k.owner()) != null, "Klon-Tank ist für seinen Besitzer gemerkt");
            klonTod(level, k);
        } else check(false, "Klon-Tank steht noch");

        if (knotenLevel != null && knotenB != null && knotenLevel.getBlockEntity(knotenB) instanceof RiftKabelknotenBlockEntity kb) {
            int diamonds = 0;
            for (int i = 0; i < kb.items().getSlots(); i++) {
                if (kb.items().getStackInSlot(i).is(Items.DIAMOND)) diamonds += kb.items().getStackInSlot(i).getCount();
            }
            check(diamonds == 5, "Rift-Kabelknoten bringt Items in die Schrottwelt (" + diamonds + ")");
            check(kb.energy().stored() > 0, "Rift-Kabelknoten überträgt Strom zwischen Dimensionen (" + kb.energy().stored() + " FE)");
            check(kb.fluid().getAmount() == 2000, "Rift-Kabelknoten überträgt Fluids (" + kb.fluid().getAmount() + " mB)");
            check(knotenLevel.getForcedChunks().contains(ChunkPos.asLong(knotenB.getX() >> 4, knotenB.getZ() >> 4)),
                    "Rift-Kabelknoten hält seinen Chunk geladen");
            UniverseId id = Multiverse.at(knotenLevel, knotenB);
            MultiverseData.Record rec = id != null ? MultiverseData.get(server).existing(id) : null;
            check(rec != null && rec.anchors.contains(knotenB.asLong()), "Rift-Kabelknoten verankert sein Universum");
        } else check(false, "Empfangender Rift-Kabelknoten steht noch");

        if (level.getBlockEntity(zelle) instanceof MikrokosmosZelleBlockEntity z) {
            check(z.energy().stored() > 0 && z.zufriedenheit() > 60, "Mikrokosmos-Zelle: versorgte Bewohner machen Strom ("
                    + z.energy().stored() + " FE, Zufriedenheit " + z.zufriedenheit() + " %)");
            check(z.items().getStackInSlot(MikrokosmosZelleBlockEntity.SLOT_NAHRUNG).getCount() < 64, "Mikrokosmos-Zelle isst");
        }
        if (level.getBlockEntity(zapfer) instanceof ParallelzapferBlockEntity p) {
            check(p.formed() && p.energy().stored() > 0 && p.tank().mb() < 2000,
                    "Parallelzapfer erzeugt Strom aus Portalfluid (" + p.energy().stored() + " FE, " + p.tank().mb() + " mB)");
            check(p.getSourceTier() == dev.riftverse.energy.VoltageTier.IV, "Parallelzapfer speist mit Stufe IV");
        } else check(false, "Parallelzapfer steht noch");
        if (level.getBlockEntity(raffinerie) instanceof RaffinerieBlockEntity r) {
            check(r.stabil().is(ModFluids.PORTALFLUID_KONZENTRAT.get()) && r.stabil().getAmount() == 500,
                    "Raffinerie: 1.000 mB Roh-Portalfluid -> 500 mB Konzentrat (" + r.stabil().getAmount() + " mB)");
        }

        kanoneMk3(level);
    }

    /** Der Besitzer stirbt mit fertigem Klon: Inventar wird beiseitegelegt, der Tank ist danach leer. */
    private static void klonTod(ServerLevel level, KlonTankBlockEntity k) {
        net.minecraft.nbt.CompoundTag tag = k.saveWithoutMetadata(level.registryAccess());
        tag.putBoolean("Bereit", true);
        k.loadWithComponents(tag, level.registryAccess());
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        dev.riftverse.tech.Klone.onDeath(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(player,
                level.damageSources().generic()));
        check(player.getInventory().isEmpty() && player.getPersistentData().contains(dev.riftverse.tech.Klone.INVENTAR)
                        && dev.riftverse.tech.Klone.hatRespawnZiel(player) && !k.bereit(),
                "Klon-Tank: Tod mit fertigem Klon behält das Inventar und verbraucht den Klon");
        player.getPersistentData().remove(dev.riftverse.tech.Klone.INVENTAR);
        player.getPersistentData().remove(dev.riftverse.tech.Klone.ZIEL);
    }

    /** Mk III nimmt nur Konzentrat (50 mB pro Schuss); Mk II schießt mit Konzentrat auch in T3-Welten. */
    private static void kanoneMk3(ServerLevel level) {
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.getInventory().clearContent();
        PortalKanoneItem mk3 = ModItems.PORTAL_KANONE_MK3.get();
        ItemStack gun = new ItemStack(mk3);
        player.getInventory().add(new ItemStack(ModItems.FLUID_KARTUSCHE_STABIL.get()));
        player.getCooldowns().removeCooldown(mk3);
        check(!mk3.fire(player, gun), "Mk III lehnt stabiles Portalfluid ab");
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(ModItems.FLUID_KARTUSCHE_KONZENTRAT.get()));
        player.getCooldowns().removeCooldown(mk3);
        KanonenTank shot = mk3.shoot(player, gun);
        check(shot != null && shot.konzentrat() && PortalKanoneItem.tank(gun).mb() == 950,
                "Mk III lädt Konzentrat und verbraucht 50 mB (" + PortalKanoneItem.tank(gun).mb() + " übrig)");

        PortalKanoneItem mk2 = ModItems.PORTAL_KANONE_MK2.get();
        ItemStack old = new ItemStack(mk2);
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(ModItems.FLUID_KARTUSCHE_KONZENTRAT.get()));
        player.getCooldowns().removeCooldown(mk2);
        KanonenTank mk2Shot = mk2.shoot(player, old);
        check(mk2Shot != null && mk2Shot.stufe() == 3, "Mk II schießt auch mit Konzentrat (T3-Welten möglich)");
        player.getInventory().clearContent();
    }

    // ------------------------------------------------------------------ Hilfen

    private static List<ItemStack> lithografZutaten() {
        return new ArrayList<>(List.of(new ItemStack(ModItems.RIFT_SCHALTKREIS.get()), new ItemStack(ModItems.CHRONOGLAS_SCHEIBE.get()),
                new ItemStack(ModItems.GOLDDRAHT.get(), 2)));
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

    private static void buildZapfer(ServerLevel level, BlockPos controller) {
        Direction facing = Direction.NORTH;
        place(level, controller, ModBlocks.PARALLELZAPFER.get().defaultBlockState().setValue(TechBlock.FACING, facing));
        for (Multiblock.Part part : Multiblock.PARALLELZAPFER.parts()) {
            BlockState s;
            if (part.hintKey().equals("block.riftverse.graphit_huelle")) s = ModBlocks.GRAPHIT_HUELLE.get().defaultBlockState();
            else if (part.hintKey().equals("block.minecraft.lightning_rod")) s = Blocks.LIGHTNING_ROD.defaultBlockState();
            else s = Blocks.AIR.defaultBlockState();
            place(level, Multiblock.at(controller, facing, part), s);
        }
    }

    private RauchTestM4() {}
}
