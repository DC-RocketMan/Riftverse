package dev.riftverse.devtest;

import com.mojang.authlib.GameProfile;
import dev.riftverse.cable.CableBlock;
import dev.riftverse.energy.KreativQuelleBlockEntity;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.gadget.AntiGravGranateEntity;
import dev.riftverse.gadget.EinzweckDrohneItem;
import dev.riftverse.gadget.EnergieGadget;
import dev.riftverse.gadget.ErinnerungsphioleItem;
import dev.riftverse.gadget.Halsband;
import dev.riftverse.gadget.Helfer;
import dev.riftverse.gadget.HoloKoederEntity;
import dev.riftverse.gadget.MultitoolItem;
import dev.riftverse.gadget.MutagenSpritzeItem;
import dev.riftverse.gadget.SchrumpfstrahlerItem;
import dev.riftverse.gadget.TarnDetektorItem;
import dev.riftverse.gadget.Zeitstopp;
import dev.riftverse.item.PortalKanoneItem;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModDataComponents.KanonenTank;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModRecipes;
import dev.riftverse.ruestung.Modul;
import dev.riftverse.ruestung.MultiversalArmorItem;
import dev.riftverse.ruestung.Ruestung;
import dev.riftverse.tech.ProcessingBlockEntity;
import dev.riftverse.tech.upgrade.UpgradeChip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.UUID;

import static dev.riftverse.devtest.RauchTest.check;

/** Rauchtest für Meilenstein 6 (Gadgets, Rüstung, Maschinen-Upgrades). */
final class RauchTestM6 {
    private static BlockPos muehleChips, muehleOhne, erz, feld, chest;
    private static Wolf wolf;
    private static Zombie gefroren;
    private static IronGolem helferErz, helferErnte;
    private static int halsbandStart;

    static void aufbauen(MinecraftServer server, ServerLevel level, RecipeManager recipes, BlockPos base) {
        for (int cx = 0; cx <= 2; cx++) {
            for (int cz = base.getZ() >> 4; cz <= (base.getZ() >> 4) + 4; cz++) level.setChunkForced(cx, cz, true);
        }
        HoloKoederEntity.abgelenkt = 0;

        // --- Registrierung und Rezepte ---------------------------------------------------------------------
        int gadgets = 0;
        for (var e : ModItems.ITEMS.getEntries()) if (e.get() instanceof dev.riftverse.gadget.Gadget) gadgets++;
        check(gadgets >= 13, "Gadgets registriert (" + gadgets + ")");
        check(ModItems.MODULE.size() == Modul.values().length && ModItems.CHIPS.size() == UpgradeChip.values().length,
                "Alle " + Modul.values().length + " Module und " + UpgradeChip.values().length + " Upgrade-Chips registriert");
        for (String teil : new String[]{"analyse_visier", "kernmantel", "exo_rahmen", "gravi_stiefel"}) {
            check(rezept(recipes, ModRecipes.PRAEZISIONS_MONTIERER_TYPE.get(), teil + "_rohling")
                            && rezept(recipes, ModRecipes.QUANTEN_LITHOGRAF_TYPE.get(), teil + "_kalibriert")
                            && rezept(recipes, ModRecipes.RIFT_SCHMIEDE_TYPE.get(), teil + "_perfektioniert"),
                    "Rüstungsleiter " + teil + ": Montierer → Lithograf → Rift-Schmiede");
        }
        int modulRezepte = 0;
        for (Modul m : Modul.values()) {
            if (rezept(recipes, ModRecipes.PRAEZISIONS_MONTIERER_TYPE.get(), m.itemName())
                    || rezept(recipes, ModRecipes.QUANTEN_LITHOGRAF_TYPE.get(), m.itemName())
                    || rezept(recipes, ModRecipes.RIFT_SCHMIEDE_TYPE.get(), m.itemName())) modulRezepte++;
        }
        check(modulRezepte == Modul.values().length, "Jedes Modul hat ein Rezept (" + modulRezepte + ")");

        // --- Module an der Werkbank ein- und ausbauen ---------------------------------------------------------
        ItemStack helm = new ItemStack(ModItems.ANALYSE_VISIER_ROHLING.get());
        CraftingInput rein = CraftingInput.of(2, 1, List.of(helm, new ItemStack(ModItems.modul(Modul.NACHTSICHT_LINSE))));
        var einbau = recipes.getRecipeFor(RecipeType.CRAFTING, rein, level);
        ItemStack mitModul = einbau.map(h -> h.value().assemble(rein, level.registryAccess())).orElse(ItemStack.EMPTY);
        check(MultiversalArmorItem.has(mitModul, Modul.NACHTSICHT_LINSE), "Werkbank: Nachtsicht-Linse ins Analyse-Visier eingebaut");
        CraftingInput falsch = CraftingInput.of(2, 1, List.of(helm, new ItemStack(ModItems.modul(Modul.FALL_DAEMPFER))));
        check(recipes.getRecipeFor(RecipeType.CRAFTING, falsch, level).isEmpty(), "Werkbank: Stiefel-Modul passt nicht in den Helm");
        CraftingInput raus = CraftingInput.of(1, 1, List.of(mitModul));
        var ausbau = recipes.getRecipeFor(RecipeType.CRAFTING, raus, level);
        check(ausbau.isPresent() && MultiversalArmorItem.module(ausbau.get().value().assemble(raus, level.registryAccess())).isEmpty()
                        && ausbau.get().value().getRemainingItems(raus).get(0).is(ModItems.modul(Modul.NACHTSICHT_LINSE)),
                "Werkbank: Modul wieder ausgebaut, es bleibt im Raster liegen");
        check(MultiversalArmorItem.werte(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, MultiversalArmorItem.Ausbau.PERFEKTIONIERT)[0] == 10,
                "Kernmantel perfektioniert: 10 Rüstung");

        // --- Maschinen-Upgrades: zwei Erzmühlen, eine mit Tempo, Parallel, Effizienz und Transformator ----------
        muehleChips = base.east(4);
        muehleOhne = base.east(4).south(4);
        mill(level, muehleChips, 2, ModBlocks.FLUX_LEITUNG.get().defaultBlockState());
        mill(level, muehleOhne, 1, ModBlocks.KUPFERKABEL.get().defaultBlockState());
        if (level.getBlockEntity(muehleChips) instanceof ProcessingBlockEntity m) {
            for (UpgradeChip c : List.of(UpgradeChip.TEMPO, UpgradeChip.PARALLEL, UpgradeChip.EFFIZIENZ, UpgradeChip.TRANSFORMATOR)) {
                check(m.einbauen(c), "Chip eingebaut: " + c);
            }
            check(!m.einbauen(UpgradeChip.TEMPO), "Jeder Chip nur einmal");
            check(m.getSinkTier() == VoltageTier.II, "Transformator-Chip: Erzmühle verträgt Stufe II");
            check(m.verbrauch(false) == 53 && m.verbrauch(true) == 105, "Verbrauch mit Tempo und Effizienz: 53 FE/t, parallel 105 FE/t");
            check(Math.abs(m.tempo() - 2f) < 1e-4, "Tempo-Chip: doppelte Geschwindigkeit");
            m.items().setStackInSlot(0, new ItemStack(ModItems.RAW_TIN.get(), 32));
        }
        if (level.getBlockEntity(muehleOhne) instanceof ProcessingBlockEntity m) {
            m.items().setStackInSlot(0, new ItemStack(ModItems.RAW_TIN.get(), 32));
        }

        // --- Multiversal-Rüstung an einem FakePlayer --------------------------------------------------------
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("00000000-0000-0000-0000-00000000a606"), "RiftverseM6"));
        p.setPos(base.getX() + 0.5, base.getY() + 10, base.getZ() + 20.5);
        ItemStack h = voll(ModItems.ANALYSE_VISIER_PERFEKTIONIERT.get(), Modul.NACHTSICHT_LINSE, Modul.ANALYSE_HUD);
        ItemStack c = voll(ModItems.KERNMANTEL_PERFEKTIONIERT.get(), Modul.NOT_PORTAL, Modul.GADGET_HALTERUNG, Modul.UNIVERSUMS_KERN);
        ItemStack l = voll(ModItems.EXO_RAHMEN_PERFEKTIONIERT.get(), Modul.SERVO_VERSTAERKER, Modul.GRAVITATIONSREGLER, Modul.SCHRUMPF_MODUL);
        ItemStack f = voll(ModItems.GRAVI_STIEFEL_PERFEKTIONIERT.get(), Modul.FALL_DAEMPFER, Modul.PHASENSCHRITT);
        c.set(ModDataComponents.KANONEN_TANK.get(), new KanonenTank(1_000, 0, true));
        l.set(ModDataComponents.ZUSTAND.get(), 3 | 4);   // Gravitation 200 %, geschrumpft
        p.setItemSlot(EquipmentSlot.HEAD, h);
        p.setItemSlot(EquipmentSlot.CHEST, c);
        p.setItemSlot(EquipmentSlot.LEGS, l);
        p.setItemSlot(EquipmentSlot.FEET, f);
        check(Ruestung.setBonus(p) && Ruestung.kanonenKosten(p, 25) == 12, "Set-Bonus „Multiversale Resonanz“: halber Kanonenverbrauch");
        int vorher = MultiversalArmorItem.energy(l);
        for (int i = 0; i < 20; i++) NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
        check(p.getAttribute(Attributes.BLOCK_BREAK_SPEED).getModifier(ResourceLocation.fromNamespaceAndPath("riftverse", "modul_servo")) != null,
                "Servo-Verstärker: schnelleres Abbauen");
        check(Math.abs(p.getAttributeValue(Attributes.SCALE) - 0.5) < 1e-3, "Schrumpf-Modul: halbe Größe (" + p.getAttributeValue(Attributes.SCALE) + ")");
        check(Math.abs(p.getAttributeValue(Attributes.GRAVITY) - 0.16) < 1e-3, "Gravitationsregler 200 %: Gravitation "
                + p.getAttributeValue(Attributes.GRAVITY));
        check(p.hasEffect(MobEffects.NIGHT_VISION), "Nachtsicht-Linse wirkt");
        check(MultiversalArmorItem.energy(l) < vorher, "Module ziehen Strom (" + (vorher - MultiversalArmorItem.energy(l)) + " FE)");

        // Fall-Dämpfer
        LivingFallEvent fall = new LivingFallEvent(p, 30f, 1f);
        int fe = MultiversalArmorItem.energy(f);
        Ruestung.onFall(fall);
        check(fall.getDamageMultiplier() == 0f && MultiversalArmorItem.energy(f) == fe - Ruestung.FALL_FE, "Fall-Dämpfer: kein Fallschaden, 500 FE");
        // Phasenschritt
        Vec3 vor = p.position();
        p.setYRot(-90f);
        p.setXRot(0f);
        check(Ruestung.phasenschritt(p) && p.position().distanceTo(vor) > 5.5, "Phasenschritt: 6 Blöcke Kurzteleport");
        // Brusttank und Kanone
        ItemStack gun = new ItemStack(ModItems.PORTAL_KANONE_MK2.get());
        p.getInventory().add(gun);
        ItemStack cartridge = new ItemStack(ModItems.FLUID_KARTUSCHE_STABIL.get());
        check(Ruestung.brusttankFuellen(p, c, cartridge, 2) && c.get(ModDataComponents.KANONEN_TANK.get()).mb() == 2_000,
                "Brusttank: Kartusche hinein (2.000 mB)");
        KanonenTank geschossen = ((PortalKanoneItem) gun.getItem()).shoot(p, PortalKanoneItem.find(p));
        int brust = c.get(ModDataComponents.KANONEN_TANK.get()).mb();
        check(geschossen != null && brust == 1_000, "Kanone tankt aus dem Brusttank nach (Brusttank " + brust + " mB)");
        check(PortalKanoneItem.tank(PortalKanoneItem.find(p)).mb() == 1_000 - 50, "Set-Bonus: Mk II verbraucht 50 statt 100 mB");
        // Gadget-Halterung: löst den Holo-Köder aus dem Inventar aus
        p.getInventory().add(ModItems.HOLO_KOEDER.get().charged());
        int holos = level.getEntitiesOfClass(HoloKoederEntity.class, new AABB(p.blockPosition()).inflate(20)).size();
        Ruestung.gadgetAusloesen(p, 0);
        check(level.getEntitiesOfClass(HoloKoederEntity.class, new AABB(p.blockPosition()).inflate(20)).size() == holos + 1,
                "Gadget-Halterung: Holo-Köder per Taste ausgelöst");
        // Not-Portal: tödlicher Treffer
        LivingDeathEvent tod = new LivingDeathEvent(p, level.damageSources().generic());
        int vorTod = MultiversalArmorItem.energy(c);
        Ruestung.onDeath(tod);
        check(tod.isCanceled() && MultiversalArmorItem.energy(c) <= vorTod - Ruestung.NOT_PORTAL_FE
                && c.get(ModDataComponents.KANONEN_TANK.get()).mb() == 900, "Not-Portal: Tod abgewendet (200.000 FE + 100 mB)");
        LivingDeathEvent nochmal = new LivingDeathEvent(p, level.damageSources().generic());
        Ruestung.onDeath(nochmal);
        check(!nochmal.isCanceled(), "Not-Portal: 10 Minuten Abklingzeit");

        // --- Gadgets ---------------------------------------------------------------------------------------
        BlockPos g = base.south(10);
        // Drohne: Kiste sortieren
        chest = g;
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity box) {
            box.setItem(0, new ItemStack(Items.STONE, 10));
            box.setItem(5, new ItemStack(Items.DIRT, 3));
            box.setItem(9, new ItemStack(Items.STONE, 20));
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, chest, null);
            if (handler != null) EinzweckDrohneItem.sortieren(handler, p);
            check(box.getItem(0).is(Items.DIRT) && box.getItem(1).is(Items.STONE) && box.getItem(1).getCount() == 30,
                    "Einzweck-Drohne sortiert die Kiste");
        }
        // Holo-Köder: eine Spinne lässt sich ablenken
        Spider spider = EntityType.SPIDER.create(level);
        if (spider != null) {
            spider.moveTo(g.getX() + 6.5, g.getY(), g.getZ() + 0.5);
            level.addFreshEntity(spider);
            ItemStack holo = ModItems.HOLO_KOEDER.get().charged();
            p.setItemInHand(InteractionHand.MAIN_HAND, holo);
            p.moveTo(g.getX() + 2.5, g.getY(), g.getZ() + 0.5, -90, 0);
            holo.use(level, p, InteractionHand.MAIN_HAND);
            check(EnergieGadget.energy(holo) == 20_000 - 2_000, "Holo-Köder: 2.000 FE pro Einsatz");
        }
        // Schrumpfstrahler
        Pig pig = EntityType.PIG.create(level);
        if (pig != null) {
            pig.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 4.5);
            level.addFreshEntity(pig);
            SchrumpfstrahlerItem.setFaktor(pig, 0.25);
            check(Math.abs(pig.getScale() - 0.25) < 1e-3 && Math.abs(SchrumpfstrahlerItem.faktor(pig) - 0.25) < 1e-3, "Schrumpfstrahler: Schwein auf 0,25");
            pig.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600));
            check(TarnDetektorItem.aufdecken(level, p) >= 1 && pig.hasEffect(MobEffects.GLOWING), "Tarn-Detektor deckt das unsichtbare Schwein auf");
            check(AntiGravGranateEntity.zuenden(level, pig) >= 1 && pig.hasEffect(MobEffects.LEVITATION), "Anti-Grav-Granate: Schwein schwebt");
        }
        // Übersetzer-Halsband
        wolf = EntityType.WOLF.create(level);
        if (wolf != null) {
            wolf.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 8.5);
            wolf.tame(p);
            level.addFreshEntity(wolf);
            Halsband.anlegen(wolf, 10_000);
            halsbandStart = Halsband.ladung(wolf);
            check(Halsband.hat(wolf), "Übersetzer-Halsband angelegt");
        }
        // Erinnerungsphiole
        BlockPos ort = g.east(12).above(3);
        Vec3 vorPhiole = p.position();
        ErinnerungsphioleItem.erinnern(p, GlobalPos.of(level.dimension(), ort));
        check(p.position().distanceTo(vorPhiole) > 5 && p.blockPosition().closerThan(ort, 9), "Erinnerungsphiole bringt einen zum Ort");
        // Multitool
        ItemStack tool = ModItems.MULTIVERSAL_MULTITOOL.get().charged();
        ItemStack leer = new ItemStack(ModItems.MULTIVERSAL_MULTITOOL.get());
        ItemStack mk2 = ModItems.MULTIVERSAL_MULTITOOL_MK2.get().charged();
        BlockState stone = Blocks.STONE.defaultBlockState(), obsidian = Blocks.OBSIDIAN.defaultBlockState();
        check(tool.getDestroySpeed(stone) >= 8f && leer.getDestroySpeed(stone) == 1f, "Multitool bohrt nur mit Strom");
        check(!tool.isCorrectToolForDrops(obsidian) && mk2.isCorrectToolForDrops(obsidian), "Multitool Mk II schafft Obsidian, Mk I nicht");
        tool.set(ModDataComponents.MODUS.get(), MultitoolItem.SCHLUESSEL);
        check(tool.getDestroySpeed(stone) == 1f, "Multitool im Schlüssel-Modus bohrt nicht");
        // Zeitstopper
        gefroren = EntityType.ZOMBIE.create(level);
        if (gefroren != null) {
            gefroren.moveTo(g.getX() + 0.5, g.getY(), g.getZ() - 4.5);
            level.addFreshEntity(gefroren);
            check(Zeitstopp.anhalten(level, gefroren.position(), p) >= 1 && gefroren.isNoAi(), "Zeitstopper friert den Zombie ein");
        }
        // Mutagen-Spritze
        Cow cow = EntityType.COW.create(level);
        if (cow != null) {
            cow.moveTo(g.getX() - 4.5, g.getY(), g.getZ() + 0.5);
            level.addFreshEntity(cow);
            MutagenSpritzeItem.mutieren(level, cow, RandomSource.create(7));
            check(cow.getPersistentData().getInt("riftverse:mutationen") == 1 || cow.getPersistentData().getBoolean(MutagenSpritzeItem.FEHLMUTATION),
                    "Mutagen-Spritze mutiert die Kuh");
        }
        // Aufgaben-Box: Erzader und Ernte
        erz = base.south(24).east(4);
        for (int i = 0; i < 4; i++) level.setBlock(erz.east(i), Blocks.IRON_ORE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(erz.east(1).above(), Blocks.IRON_ORE.defaultBlockState(), Block.UPDATE_ALL);
        helferErz = Helfer.beschwoeren(level, p, Helfer.Aufgabe.ERZ, erz, erz.north());
        feld = base.south(24).east(16);
        for (int i = 0; i < 3; i++) {
            level.setBlock(feld.east(i).below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(feld.east(i), ((CropBlock) Blocks.WHEAT).getStateForAge(7), Block.UPDATE_ALL);
        }
        helferErnte = Helfer.beschwoeren(level, p, Helfer.Aufgabe.ERNTE, feld, feld.north());
        check(helferErz != null && helferErnte != null, "Aufgaben-Box beschwört Helfer");
    }

    static void ergebnis(MinecraftServer server, ServerLevel level) {
        int mitChips = staub(level, muehleChips), ohne = staub(level, muehleOhne);
        check(mitChips >= 3 * Math.max(1, ohne), "Erzmühle mit Tempo und Parallel: " + mitChips + " Staub, ohne Chips: " + ohne);
        check(level.getBlockState(muehleChips.west(2)).getBlock() instanceof CableBlock
                        && level.getBlockState(muehleChips).is(ModBlocks.ERZMUEHLE.get()),
                "Transformator-Chip: Erzmühle übersteht Stufe II");
        check(HoloKoederEntity.abgelenkt >= 1, "Holo-Köder lenkt Mobs ab (" + HoloKoederEntity.abgelenkt + ")");
        if (wolf != null) check(Halsband.ladung(wolf) < halsbandStart, "Übersetzer-Halsband verbraucht Strom (" + Halsband.ladung(wolf) + " FE)");
        if (gefroren != null) check(!gefroren.isNoAi(), "Zeitstopp ist nach 6 Sekunden vorbei");
        int erzRest = 0;
        for (int i = 0; i < 4; i++) if (level.getBlockState(erz.east(i)).is(Blocks.IRON_ORE)) erzRest++;
        if (level.getBlockState(erz.east(1).above()).is(Blocks.IRON_ORE)) erzRest++;
        check(erzRest == 0, "Helfer hat die ganze Erzader abgebaut (übrig: " + erzRest + ")");
        int reif = 0;
        for (int i = 0; i < 3; i++) {
            BlockState s = level.getBlockState(feld.east(i));
            if (s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s)) reif++;
        }
        check(reif == 0 && level.getBlockState(feld).is(Blocks.WHEAT), "Helfer hat geerntet und neu gesät");
        long items = level.getEntitiesOfClass(ItemEntity.class, new AABB(erz).inflate(8), e -> e.getItem().is(Items.RAW_IRON)).size();
        check(items >= 1, "Erz des Helfers liegt bereit");
    }

    private static boolean rezept(RecipeManager recipes, RecipeType<?> type, String name) {
        return recipes.byKey(ResourceLocation.fromNamespaceAndPath("riftverse", type == ModRecipes.PRAEZISIONS_MONTIERER_TYPE.get()
                ? "praezisions_montierer/" + name : type == ModRecipes.QUANTEN_LITHOGRAF_TYPE.get() ? "quanten_lithograf/" + name
                : "rift_schmiede/" + name)).isPresent();
    }

    private static ItemStack voll(net.minecraft.world.item.Item item, Modul... module) {
        ItemStack s = new ItemStack(item);
        s.set(ModDataComponents.ENERGY.get(), ((MultiversalArmorItem) item).capacity());
        MultiversalArmorItem.setModule(s, List.of(module));
        return s;
    }

    /** Erzmühle mit Kreativ-Quelle (Stufe {@code tier}) und zwei Leitungen davor. */
    private static void mill(ServerLevel level, BlockPos mill, int tier, BlockState cable) {
        level.setBlock(mill, ModBlocks.ERZMUEHLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(mill.west(1), cable, Block.UPDATE_ALL);
        level.setBlock(mill.west(2), cable, Block.UPDATE_ALL);
        level.setBlock(mill.west(3), ModBlocks.KREATIV_QUELLE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(mill.west(3)) instanceof KreativQuelleBlockEntity q) {
            for (int i = 1; i < tier; i++) q.cycleTier();
        }
        for (int i = 1; i <= 2; i++) {
            BlockPos cpos = mill.west(i);
            level.setBlock(cpos, Block.updateFromNeighbourShapes(level.getBlockState(cpos), level, cpos), Block.UPDATE_ALL);
        }
    }

    private static int staub(ServerLevel level, BlockPos mill) {
        return level.getBlockEntity(mill) instanceof ProcessingBlockEntity m ? m.items().getStackInSlot(m.outputSlot()).getCount() : -1;
    }

    private RauchTestM6() {}
}
