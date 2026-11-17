package dev.riftverse.dorf;

import dev.riftverse.registry.ModDorf;
import dev.riftverse.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

import java.util.Optional;

/**
 * Handel der Gleebs (M10-12): Sie kaufen Rohstoffe aus fremden Welten gegen Sternenstaub und verkaufen dafür
 * Leuchtpflanzen, Deko, Sternenbrot und Magisches. Jeder Beruf hat seine eigene Liste; die Angebote füllen sich
 * jeden Morgen wieder auf.
 */
public final class Handel {
    private Handel() {
    }

    private static MerchantOffer kauf(ItemLike fremd, int anzahl, int staub) {
        return new MerchantOffer(new ItemCost(fremd, anzahl), new ItemStack(ModDorf.STERNENSTAUB.get(), staub), 12, 4, 0.05f);
    }

    private static MerchantOffer verkauf(int staub, ItemStack ware, int max) {
        return new MerchantOffer(new ItemCost(ModDorf.STERNENSTAUB.get(), staub), ware, max, 2, 0.05f);
    }

    private static MerchantOffer verkauf(int staub, ItemLike ware, int anzahl, int max) {
        return verkauf(staub, new ItemStack(ware, anzahl), max);
    }

    private static Item pflanze(String name) {
        return ModDorf.PFLANZEN.get(name).get().asItem();
    }

    public static MerchantOffers angebote(Beruf beruf) {
        MerchantOffers o = new MerchantOffers();
        switch (beruf) {
            case BAECKERIN -> {
                o.add(kauf(ModItems.BIOMASSE_PULPE.get(), 6, 1));
                o.add(kauf(Items.WHEAT, 16, 1));
                o.add(verkauf(2, ModDorf.STERNENBROT.get(), 3, 16));
                o.add(verkauf(1, Items.COOKIE, 8, 16));
                o.add(verkauf(3, Items.CAKE, 1, 6));
                o.add(verkauf(2, Items.PUMPKIN_PIE, 3, 12));
            }
            case KRAEUTERHEXE -> {
                o.add(kauf(ModItems.SAEUREGEL.get(), 4, 1));
                o.add(kauf(ModItems.TOXINREST.get(), 4, 1));
                o.add(verkauf(1, pflanze("sternenblume_weiss"), 4, 16));
                o.add(verkauf(1, pflanze("leuchtglocke_blau"), 4, 16));
                o.add(verkauf(1, pflanze("leuchtglocke_rosa"), 4, 16));
                o.add(verkauf(1, ModDorf.MONDKRAUT.get(), 4, 16));
                o.add(verkauf(2, ModDorf.ZAUBERPILZ_VIOLETT.get(), 3, 12));
                o.add(verkauf(2, ModDorf.KRAEUTERBUENDEL.get(), 2, 12));
                o.add(verkauf(3, ModDorf.LICHTRANKE.get(), 4, 12));
            }
            case ALCHEMIST -> {
                o.add(kauf(ModItems.FLUXKRISTALL.get(), 3, 1));
                o.add(kauf(ModItems.CHRONOGLAS_SAND.get(), 8, 1));
                o.add(verkauf(3, ModDorf.STERNGLAS.get(), 8, 12));
                o.add(verkauf(2, ModDorf.FEENLICHT.get(), 3, 12));
                o.add(verkauf(4, PotionContents.createItemStack(Items.POTION, Potions.NIGHT_VISION), 6));
                o.add(verkauf(5, PotionContents.createItemStack(Items.POTION, Potions.SLOW_FALLING), 6));
                o.add(verkauf(6, PotionContents.createItemStack(Items.POTION, Potions.LEAPING), 6));
            }
            case RUNENSCHMIED -> {
                o.add(kauf(ModItems.ALIEN_SCHROTT.get(), 8, 1));
                o.add(kauf(ModItems.GRAVITIT.get(), 1, 2));
                o.add(new MerchantOffer(new ItemCost(ModDorf.STERNENSTAUB.get(), 4),
                        Optional.of(new ItemCost(ModItems.BASIS_SCHALTKREIS.get(), 1)),
                        new ItemStack(ModDorf.STERNEN_CHIP.get()), 8, 6, 0.05f));
                o.add(verkauf(2, ModDorf.SCHWEBELATERNE.get(), 2, 12));
                o.add(verkauf(1, ModDorf.BUNTES_KOPFSTEINPFLASTER.get(), 16, 16));
                o.add(verkauf(1, ModDorf.DACHZIEGEL.get(), 16, 16));
                o.add(verkauf(1, ModDorf.FACHWERK.get(), 12, 16));
            }
            case STERNENKUNDLERIN -> {
                o.add(kauf(ModItems.SCHWEBSTEIN_BROCKEN.get(), 8, 1));
                o.add(kauf(ModItems.INVERSIONSSPLITTER.get(), 1, 2));
                o.add(verkauf(5, Items.SPYGLASS, 1, 4));
                o.add(verkauf(2, ModDorf.FUNKELHOLZ_STAMM.get(), 8, 12));
                o.add(verkauf(1, ModDorf.FUNKELLAUB.get(), 8, 12));
                o.add(verkauf(3, pflanze("sternenblume_gold"), 4, 12));
                o.add(verkauf(8, Items.AMETHYST_SHARD, 6, 6));
            }
            case BUERGER -> { }
        }
        return o;
    }
}
