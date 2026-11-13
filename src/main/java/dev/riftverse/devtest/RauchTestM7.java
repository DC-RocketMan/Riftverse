package dev.riftverse.devtest;

import dev.riftverse.Config;
import dev.riftverse.compat.BlockInfo;
import dev.riftverse.compat.Infoseiten;
import dev.riftverse.compat.RezeptAnsicht;
import dev.riftverse.recipe.MachineRecipe;
import dev.riftverse.registry.ModBlocks;
import dev.riftverse.registry.ModItems;
import dev.riftverse.registry.ModRecipes;
import dev.riftverse.worldgen.ConfigMengePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

import static dev.riftverse.devtest.RauchTest.check;

/**
 * Rauchtest für Meilenstein 7: Config-Werte und Dichte-Modifier, Abdeckung der Rezeptkategorien für JEI und EMI,
 * Infoseiten, Jade-Zeilen und die neu abgestimmten Rezepte.
 */
final class RauchTestM7 {
    private static BlockPos muehle, resonator, mikrokosmos, schmiede;

    private RauchTestM7() {
    }

    static void aufbauen(MinecraftServer server, ServerLevel level, RecipeManager recipes, BlockPos base) {
        for (int cx = (base.getX() - 2) >> 4; cx <= (base.getX() + 12) >> 4; cx++) {
            for (int cz = (base.getZ() - 2) >> 4; cz <= (base.getZ() + 4) >> 4; cz++) level.setChunkForced(cx, cz, true);
        }
        for (int x = -2; x <= 12; x++) {
            for (int z = -2; z <= 4; z++) level.setBlock(base.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        muehle = base;
        resonator = base.east(3);
        mikrokosmos = base.east(6);
        schmiede = base.east(9);
        place(level, muehle, ModBlocks.ERZMUEHLE.get().defaultBlockState());
        place(level, resonator, ModBlocks.FLUXKRISTALL_RESONATOR.get().defaultBlockState());
        place(level, mikrokosmos, ModBlocks.MIKROKOSMOS_ZELLE.get().defaultBlockState());
        place(level, schmiede, ModBlocks.RIFT_SCHMIEDE.get().defaultBlockState());
    }

    static void ergebnis(MinecraftServer server, ServerLevel level) {
        RecipeManager recipes = server.getRecipeManager();
        configTests(server);
        rezeptAnsichten(recipes);
        infoseiten();
        jade(level);
        balancing(recipes);
    }

    // ------------------------------------------------------------------ Config

    private static void configTests(MinecraftServer server) {
        check(Config.CABLE_LOSS.get() == 1.0 && Config.TIN_DENSITY.get() == 1.0 && Config.XENIT_DENSITY.get() == 1.0
                        && Config.FLUX_DENSITY.get() == 1.0,
                "Config: Kabelverlust und Dichten stehen auf 1");
        check(Config.isEnabled("tin_ore"), "Config: Zinnerz ist an");

        RandomSource r = RandomSource.create(7);
        int summe = 0;
        for (int i = 0; i < 1000; i++) summe += ConfigMengePlacement.kopien(2.5, r);
        check(ConfigMengePlacement.kopien(0, r) == 0 && ConfigMengePlacement.kopien(1, r) == 1 && summe > 2300 && summe < 2700,
                "Dichte-Modifier: 0 → 0, 1 → 1, 2,5 → im Mittel " + summe / 1000.0);

        var placed = server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        for (String name : List.of("ore_tin", "ore_tin_small", "ore_xenit_krater", "ore_fluxkristall")) {
            PlacedFeature f = placed.get(ResourceLocation.fromNamespaceAndPath("riftverse", name));
            boolean ok = f != null && !f.placement().isEmpty() && f.placement().get(0) instanceof ConfigMengePlacement;
            check(ok, "Platzierung " + name + " beginnt mit riftverse:config_menge");
        }
    }

    // ------------------------------------------------------------------ JEI und EMI

    private static void rezeptAnsichten(RecipeManager recipes) {
        List<RezeptAnsicht.Kategorie> kategorien = RezeptAnsicht.kategorien();
        check(kategorien.size() == dev.riftverse.tech.MachineKind.values().length + 3,
                "Eine Rezeptkategorie pro Maschine plus Werkbank, Fluid-Mixer und Raffinerie (" + kategorien.size() + ")");
        int gesamt = 0;
        StringBuilder leer = new StringBuilder();
        StringBuilder kaputt = new StringBuilder();
        for (RezeptAnsicht.Kategorie k : kategorien) {
            if (k.symbol().isEmpty()) kaputt.append(k.id()).append(" (kein Block) ");
            int roh = recipes.getAllRecipesFor((net.minecraft.world.item.crafting.RecipeType) k.typ()).size();
            List<RezeptAnsicht> ansichten = RezeptAnsicht.alle(recipes, k);
            if (ansichten.isEmpty()) leer.append(k.id()).append(' ');
            if (ansichten.size() != roh) kaputt.append(k.id()).append(" (").append(ansichten.size()).append('/').append(roh).append(") ");
            for (RezeptAnsicht a : ansichten) {
                gesamt++;
                boolean ein = false, aus = false, passt = a.zeilenY() + a.zeilen().size() * RezeptAnsicht.ZEILE <= RezeptAnsicht.JEI_HOEHE;
                for (RezeptAnsicht.Slot s : a.slots()) {
                    ein |= s.eingang() && (s.istFluid() || !s.items().isEmpty());
                    aus |= !s.eingang() && (s.istFluid() ? !s.fluid().isEmpty() : !s.items().isEmpty());
                    passt &= s.x() >= 0 && s.x() + RezeptAnsicht.SLOT <= RezeptAnsicht.JEI_BREITE && s.y() + RezeptAnsicht.SLOT <= a.zeilenY();
                }
                passt &= a.pfeilX() + RezeptAnsicht.PFEIL_B <= RezeptAnsicht.JEI_BREITE;
                if (!ein || !aus || !passt) kaputt.append(a.id()).append(ein ? "" : " ohne Eingang").append(aus ? "" : " ohne Ausgang")
                        .append(passt ? "" : " passt nicht").append(' ');
            }
        }
        check(leer.isEmpty(), "Jede Rezeptkategorie hat Rezepte" + (leer.isEmpty() ? "" : " – leer: " + leer));
        check(kaputt.isEmpty(), "Alle " + gesamt + " Rezepte lassen sich für JEI/EMI darstellen" + (kaputt.isEmpty() ? "" : " – " + kaputt));

        // Stichproben: Chance und Montage-Schritte kommen an
        RezeptAnsicht.Kategorie recycler = kategorien.stream().filter(k -> k.id().equals("recycler")).findFirst().orElseThrow();
        boolean chance = RezeptAnsicht.alle(recipes, recycler).stream()
                .anyMatch(a -> a.slots().stream().anyMatch(s -> !s.eingang() && s.chance() < 1f));
        check(chance, "Recycler zeigt Nebenprodukte mit Chance");
        RezeptAnsicht.Kategorie montierer = kategorien.stream().filter(k -> k.id().equals("praezisions_montierer")).findFirst().orElseThrow();
        boolean schritte = RezeptAnsicht.alle(recipes, montierer).stream()
                .allMatch(a -> a.slots().stream().filter(RezeptAnsicht.Slot::eingang).allMatch(s -> s.hinweis() != null));
        check(schritte, "Präzisions-Montierer zeigt die Schrittnummern");
    }

    private static void infoseiten() {
        int fehlt = 0;
        StringBuilder namen = new StringBuilder();
        for (Infoseiten.Seite s : Infoseiten.alle()) {
            if (s.itemListe().size() != s.items().size()) {
                fehlt++;
                namen.append(s.schluessel()).append(' ');
            }
        }
        check(fehlt == 0, "Infoseiten zeigen nur bekannte Items (" + Infoseiten.alle().size() + " Seiten)" + (fehlt == 0 ? "" : " – " + namen));
    }

    // ------------------------------------------------------------------ Jade

    private static void jade(ServerLevel level) {
        check(hat(level.getBlockEntity(muehle), "jade.riftverse.spannung_ein"), "Jade: Erzmühle zeigt ihre Spannungsstufe");
        check(hat(level.getBlockEntity(resonator), "jade.riftverse.hitze"), "Jade: Resonator zeigt die Hitze");
        check(hat(level.getBlockEntity(mikrokosmos), "gui.riftverse.mikrokosmos.stufe")
                        || hat(level.getBlockEntity(mikrokosmos), "gui.riftverse.mikrokosmos.streik"),
                "Jade: Mikrokosmos-Zelle zeigt die Zivilisationsstufe");
        check(hat(level.getBlockEntity(schmiede), "jade.riftverse.struktur_fehlt"), "Jade: Rift-Schmiede ohne Aufbau meldet das fehlende Teil");
        check(!hat(level.getBlockEntity(muehle), "jade.riftverse.struktur_ok"), "Jade: Einzelmaschinen zeigen keinen Aufbau");

        // Die Zeilen gehen als JSON zum Client
        BlockEntity be = level.getBlockEntity(resonator);
        if (be != null) {
            boolean rund = true;
            for (Component z : BlockInfo.zeilen(be)) {
                String json = Component.Serializer.toJson(z, level.registryAccess());
                Component zurueck = Component.Serializer.fromJson(json, level.registryAccess());
                rund &= zurueck != null && zurueck.getString().equals(z.getString());
            }
            check(rund, "Jade-Zeilen überstehen den Weg als JSON");
        }
    }

    private static boolean hat(BlockEntity be, String key) {
        if (be == null) return false;
        for (Component z : BlockInfo.zeilen(be)) {
            if (z.getContents() instanceof TranslatableContents t && t.getKey().equals(key)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ Balancing

    private static void balancing(RecipeManager recipes) {
        var speicher = recipes.byKey(ResourceLocation.fromNamespaceAndPath("riftverse", "garagen_werkbank/chrono_speicher"));
        long zellen = speicher.map(h -> h.value().getIngredients().stream()
                .filter(i -> i.test(new ItemStack(ModItems.FLUX_ZELLE.get()))).count()).orElse(-1L);
        check(zellen == 2, "Chrono-Speicher braucht 2 Flux-Zellen (" + zellen + ")");

        var xenitZelle = recipes.byKey(ResourceLocation.fromNamespaceAndPath("riftverse", "garagen_werkbank/xenit_zelle"));
        long energie = xenitZelle.map(h -> h.value().getIngredients().stream()
                .filter(i -> i.test(new ItemStack(ModItems.ENERGIEZELLE.get()))).count()).orElse(-1L);
        check(energie == 4, "Xenit-Zelle braucht 4 Energiezellen (" + energie + ")");

        var folie = recipes.byKey(ResourceLocation.fromNamespaceAndPath("riftverse", "garagenpresse/isolierfolie"));
        boolean folieOk = folie.map(RecipeHolder::value).filter(r -> r instanceof MachineRecipe).map(r -> (MachineRecipe) r)
                .map(r -> r.result().getCount() == 2 && r.ingredients().get(0).count() == 1).orElse(false);
        check(folieOk, "Garagenpresse: 1 Polymerfetzen → 2 Isolierfolie");

        boolean huelle = recipes.getAllRecipesFor(ModRecipes.RIFT_SCHMIEDE_TYPE.get()).stream()
                .filter(h -> h.value().result().is(ModItems.NULL_HUELLE.get()))
                .anyMatch(h -> h.value().ingredients().stream().anyMatch(c -> c.ingredient().test(new ItemStack(ModItems.XENIT_PLATTE.get()))));
        check(huelle, "Null-Hülle enthält Xenit-Platten");
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }
}
