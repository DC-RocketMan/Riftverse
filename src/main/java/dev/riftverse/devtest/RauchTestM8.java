package dev.riftverse.devtest;

import dev.riftverse.Config;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModItems;
import dev.riftverse.tech.GaragenAkkuBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.io.InputStream;
import java.util.List;

import static dev.riftverse.devtest.RauchTest.check;

/**
 * Rauchtest für Handbuch und Garagen-Akku: Der Akku lädt an einer Stufe-I-Quelle, sein Rezept kommt ohne Xenit aus,
 * das Handbuch ist registriert, nachbaubar und seine Seiten liegen in der Jar.
 */
final class RauchTestM8 {
    private static BlockPos akku;

    private RauchTestM8() {
    }

    static void aufbauen(MinecraftServer server, ServerLevel level, RecipeManager recipes, BlockPos base) {
        for (int cx = (base.getX() - 2) >> 4; cx <= (base.getX() + 4) >> 4; cx++) {
            for (int cz = (base.getZ() - 2) >> 4; cz <= (base.getZ() + 2) >> 4; cz++) level.setChunkForced(cx, cz, true);
        }
        for (int x = -1; x <= 3; x++) {
            for (int z = -1; z <= 1; z++) level.setBlock(base.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        // Kreativ-Quelle (Stufe I) -> Kupferkabel -> Garagen-Akku
        akku = base.east(2);
        level.setBlock(base, ModBlocks.KREATIV_QUELLE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(base.east(1), ModBlocks.KUPFERKABEL.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(akku, ModBlocks.GARAGEN_AKKU.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockPos kabel = base.east(1);
        level.setBlock(kabel, Block.updateFromNeighbourShapes(level.getBlockState(kabel), level, kabel), Block.UPDATE_ALL);
    }

    static void ergebnis(MinecraftServer server, ServerLevel level) {
        RecipeManager recipes = server.getRecipeManager();

        // --- Garagen-Akku ---------------------------------------------------------------
        if (level.getBlockEntity(akku) instanceof GaragenAkkuBlockEntity a) {
            check(a.energy().capacity() == GaragenAkkuBlockEntity.AKKU_CAPACITY && a.tier() == VoltageTier.I,
                    "Garagen-Akku: 100.000 FE auf Stufe I (" + a.energy().capacity() + ", " + a.tier() + ")");
            check(a.energy().stored() > 0, "Garagen-Akku lädt an der Kreativ-Quelle: " + a.energy().stored() + " FE");
        } else {
            check(false, "Garagen-Akku hat seine Block-Entity");
        }

        var rezept = recipes.byKey(ResourceLocation.fromNamespaceAndPath("riftverse", "garagen_werkbank/garagen_akku"));
        check(rezept.isPresent(), "Rezept für den Garagen-Akku geladen");
        List<Item> xenit = List.of(ModItems.XENIT_PLATTE.get(), ModItems.XENIT_INGOT.get(), ModItems.XENIT_STANGE.get(),
                ModItems.XENIT_STAUB.get(), ModItems.XENIT_GEHAEUSE.get(), ModItems.XENIT_SPULE.get(), ModItems.ENERGIEZELLE.get());
        boolean ohneXenit = rezept.map(h -> h.value().getIngredients().stream().noneMatch(i -> enthaelt(i, xenit))).orElse(false);
        check(ohneXenit, "Garagen-Akku kommt ohne Xenit aus");

        // --- Handbuch -------------------------------------------------------------------
        var buch = recipes.byKey(ResourceLocation.fromNamespaceAndPath("riftverse", "handbuch"));
        check(buch.map(h -> h.value().getResultItem(server.registryAccess()).is(ModItems.HANDBUCH.get())).orElse(false),
                "Handbuch lässt sich nachbauen");
        check(Config.GUIDE_ON_JOIN.get(), "Config: Handbuch beim ersten Betreten ist an");
        check(seite("index.md") && seite("grundlagen/kabel.md") && seite("energie/speicher.md"),
                "Handbuch-Seiten liegen in der Jar");
    }

    private static boolean enthaelt(Ingredient ingredient, List<Item> items) {
        for (Item item : items) {
            if (ingredient.test(new ItemStack(item))) return true;
        }
        return false;
    }

    private static boolean seite(String pfad) {
        try (InputStream in = RauchTestM8.class.getResourceAsStream("/assets/riftverse/guides/riftverse/handbuch/" + pfad)) {
            return in != null;
        } catch (java.io.IOException e) {
            return false;
        }
    }
}
