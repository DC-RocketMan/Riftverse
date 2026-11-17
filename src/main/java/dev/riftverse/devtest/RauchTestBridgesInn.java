package dev.riftverse.devtest;

import com.mojang.authlib.GameProfile;
import dev.riftverse.dorf.Beruf;
import dev.riftverse.dorf.DorfNacht;
import dev.riftverse.dorf.DorfPlan;
import dev.riftverse.dorf.Gleeb;
import dev.riftverse.dorf.Handel;
import dev.riftverse.dorf.HausTyp;
import dev.riftverse.dorf.Irrlicht;
import dev.riftverse.dorf.LeuchtLogik;
import dev.riftverse.registry.ModDorf;
import dev.riftverse.registry.ModEntities;
import dev.riftverse.universe.Einzigartig;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseProfile;
import dev.riftverse.universe.UniverseType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.riftverse.devtest.RauchTest.check;

/**
 * Rauchtest für Bridge's Inn (Meilenstein 10 der neuen Universen): Typ und Gewicht, Weltplan (Fluss, See, Hügel,
 * Dörfer, Orte in der Natur), das erzeugte Hauptdorf mit Wirtshaus und Gleebs, Leuchtpflanzen, Handel, die
 * Huhn-Strafe, Sternschnuppen und Irrlichter.
 */
final class RauchTestBridgesInn {
    private static final UUID TESTER = UUID.fromString("b21d9e00-0000-4000-8000-00000000d0ef");

    private RauchTestBridgesInn() {
    }

    static void aufbauen(MinecraftServer server, ServerLevel level, BlockPos base) {
        UniverseType typ = UniverseType.MAGISCHES_DORF;
        Einzigartig e = Einzigartig.of(typ);
        check(typ.einzigartig() && !typ.entwicklung() && typ.gunTier() == 1 && typ.weight() == 4 && e != null,
                "Bridge's Inn: einzigartig, ab Mk I, Gewicht 4");
        check(UniverseType.randomWith(1).contains(typ), "Bridge's Inn ist ein Zufallsziel der Mk I");

        // --- Weltplan ---------------------------------------------------------------------------------
        UniverseProfile p = UniverseProfile.get(Einzigartig.id(typ));
        DorfPlan plan = p.dorfPlan();
        DorfPlan.Dorf haupt = plan.hauptdorf;
        Set<HausTyp> typen = EnumSet.noneOf(HausTyp.class);
        haupt.haeuser().forEach(h -> typen.add(h.typ()));
        check(typen.containsAll(List.of(HausTyp.TAVERNE, HausTyp.MAGIERTURM, HausTyp.STERNWARTE, HausTyp.BAECKEREI,
                        HausTyp.KRAEUTERHUETTE, HausTyp.ALCHEMIE, HausTyp.RUNENSCHMIEDE, HausTyp.MARKTBRUNNEN)),
                "Hauptdorf hat Wirtshaus, Magierturm, Sternwarte, Brunnen und alle Berufshäuser " + typen);
        check(haupt.haeuser().size() >= 14, "Hauptdorf: " + haupt.haeuser().size() + " Gebäude");
        check(plan.doerfer.size() >= 12, "Bridge's Inn: " + plan.doerfer().size() + " Dörfer");
        boolean alleBewohnt = true;
        for (DorfPlan.Dorf d : plan.doerfer) alleBewohnt &= d.haeuser().size() >= 5;
        check(alleBewohnt, "Jedes Dorf hat Brunnen und mindestens vier Häuser");
        long feenringe = plan.orte.stream().filter(o -> o.art() == DorfPlan.Ortsart.FEENRING).count();
        check(plan.orte.stream().anyMatch(o -> o.art() == DorfPlan.Ortsart.STEINKREIS)
                        && plan.orte.stream().anyMatch(o -> o.art() == DorfPlan.Ortsart.GROSSVATERBAUM) && feenringe >= 3,
                "Orte in der Natur: Steinkreis, Großvaterbaum, " + feenringe + " Feenringe");

        int c = Multiverse.cellCenter(0);
        int fx = c + 500;
        check(plan.hoehe(fx, (int) Math.round(plan.flussZ(fx))) < DorfPlan.WASSER, "Fluss liegt unter dem Wasserspiegel");
        check(plan.hoehe(plan.seeX + 50, plan.seeZ) < DorfPlan.WASSER - 3 && plan.hoehe(plan.seeX, plan.seeZ) >= 64,
                "See mit Insel");
        check(plan.hoehe(plan.huegelX, plan.huegelZ) > 88, "Aussichtshügel (Y " + plan.hoehe(plan.huegelX, plan.huegelZ) + ")");
        check(plan.hoehe(haupt.plaetze().get(0).x(), haupt.plaetze().get(0).z()) == haupt.hoehe(), "Dorfplatz ist eben");

        // --- Erzeugtes Hauptdorf ----------------------------------------------------------------------
        ServerLevel welt = server.getLevel(typ.dimension());
        check(welt != null, "Dimension Bridge's Inn geladen");
        if (welt == null) return;
        int y = haupt.hoehe();
        for (int cx = (c - 64) >> 4; cx <= (c + 64) >> 4; cx++) {
            for (int cz = (c - 96) >> 4; cz <= (c + 64) >> 4; cz++) welt.getChunk(cx, cz);
        }
        check(welt.getBlockState(new BlockPos(c, y, c + 20)).is(ModDorf.BUNTES_KOPFSTEINPFLASTER.get()),
                "Brücke über den Fluss mit Kopfsteinweg");
        check(welt.getBlockState(new BlockPos(c + 12, DorfPlan.WASSER, c)).is(Blocks.WATER), "Unter der Brücke fließt der Fluss");
        boolean kiste = false;
        for (BlockPos q : BlockPos.betweenClosed(c - 6, y, c - 8, c + 6, y + 9, c + 8)) {
            if (welt.getBlockEntity(q) instanceof RandomizableContainerBlockEntity k && k.getLootTable() != null) kiste = true;
        }
        check(kiste, "Bridge's Inn: Kisten mit Inhalt");
        List<Gleeb> gleebs = welt.getEntitiesOfClass(Gleeb.class, new AABB(c - 120, y - 20, c - 150, c + 120, y + 60, c + 100));
        check(gleebs.size() >= 6, "Gleebs im Hauptdorf: " + gleebs.size());
        check(gleebs.stream().anyMatch(Gleeb::besen), "Ein Gleeb fliegt auf dem Besen");
        check(gleebs.stream().anyMatch(g -> g.beruf().handelt()), "Gleebs mit Beruf");

        // --- Handel -----------------------------------------------------------------------------------
        boolean staub = true;
        for (Beruf b : Beruf.values()) {
            if (!b.handelt()) continue;
            boolean kauft = false, verkauft = false;
            for (MerchantOffer o : Handel.angebote(b)) {
                kauft |= o.getResult().is(ModDorf.STERNENSTAUB.get());
                verkauft |= o.getCostA().is(ModDorf.STERNENSTAUB.get());
            }
            staub &= kauft && verkauft;
        }
        check(staub && Handel.angebote(Beruf.BUERGER).isEmpty(), "Jeder Beruf kauft gegen Sternenstaub und verkauft dafür");

        // --- Leuchtpflanzen ---------------------------------------------------------------------------
        BlockPos beet = base.above(1);
        level.setBlock(beet.below(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(beet, ModDorf.LEUCHTGLOCKE_VIOLETT.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockState vorher = level.getBlockState(beet);
        check(vorher.is(ModDorf.LEUCHTGLOCKE_VIOLETT.get()), "Leuchtglocke wächst auf Gras");
        LeuchtLogik.anregen(level, beet, 1);
        BlockState an = level.getBlockState(beet);
        check(an.getValue(LeuchtLogik.ANGEREGT) && an.getLightEmission(level, beet) == 15, "Zauber lässt die Leuchtglocke strahlen");

        // --- Huhn-Strafe ------------------------------------------------------------------------------
        FakePlayer spieler = FakePlayerFactory.get(level, new GameProfile(TESTER, "[Riftverse-Huhn]"));
        spieler.moveTo(base.getX() + 2.5, base.getY() + 1, base.getZ() + 0.5);
        Gleeb g = ModEntities.GLEEB.get().create(level);
        if (g != null) {
            g.moveTo(base.getX() + 4.5, base.getY() + 1, base.getZ() + 0.5);
            g.einrichten(Beruf.ALCHEMIST, base, base, 16, RandomSource.create(3));
            level.addFreshEntity(g);
            float leben = g.getHealth();
            boolean getroffen = g.hurt(level.damageSources().playerAttack(spieler), 6f);
            check(!getroffen && g.getHealth() == leben, "Gleebs nehmen keinen Schaden");
            check(spieler.hasEffect(ModDorf.VERHUEHNERT), "Wer einen Gleeb schlägt, wird zum Huhn");
            BlockPos stein = base.east(6).above();
            level.setBlock(stein, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            spieler.gameMode.destroyBlock(stein);
            check(level.getBlockState(stein).is(Blocks.STONE), "Ein Huhn kann keine Blöcke abbauen");
            spieler.removeAllEffects();
            g.discard();
        }

        // --- Nachts: Sternschnuppe und Irrlicht -------------------------------------------------------
        FakePlayer wanderer = FakePlayerFactory.get(welt, new GameProfile(UUID.fromString("b21d9e00-0000-4000-8000-00000000d0f0"), "[Riftverse-Nacht]"));
        wanderer.moveTo(c + 0.5, y + 1, c - 44.5);
        RandomSource r = RandomSource.create(10);
        DorfNacht.sternschnuppe(welt, wanderer, r);
        List<ItemEntity> staubKoerner = welt.getEntitiesOfClass(ItemEntity.class, wanderer.getBoundingBox().inflate(48, 128, 48),
                i -> i.getItem().is(ModDorf.STERNENSTAUB.get()));
        check(!staubKoerner.isEmpty() && staubKoerner.get(0).isCurrentlyGlowing(), "Sternschnuppe: glitzernder Sternenstaub gelandet");
        DorfNacht.irrlicht(welt, wanderer, r);
        check(!welt.getEntitiesOfClass(Irrlicht.class, wanderer.getBoundingBox().inflate(32, 128, 32)).isEmpty(), "Ein Irrlicht erscheint");
    }
}
