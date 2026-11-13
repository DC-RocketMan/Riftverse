package dev.riftverse.compat;

import dev.riftverse.Riftverse;
import dev.riftverse.ruestung.Modul;
import dev.riftverse.tech.upgrade.UpgradeChip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Infoseiten für JEI und EMI: Geräte ohne eigenes Rezept-Fenster (Generatoren, Multiblöcke, Sondermaschinen aus T3 und
 * T4), Erze mit besonderem Fundort, Gadgets, Rüstung, Module und Chips. Der Text steht unter
 * {@code info.riftverse.<schlüssel>} in den Sprachdateien.
 */
public final class Infoseiten {
    private Infoseiten() {
    }

    public record Seite(String schluessel, List<String> items) {
        public Component text() {
            return Component.translatable("info." + Riftverse.MODID + "." + schluessel);
        }

        /** Die Items der Seite; unbekannte Namen fallen weg. */
        public List<Item> itemListe() {
            List<Item> out = new ArrayList<>();
            for (String id : items) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Riftverse.MODID, id));
                if (item != Items.AIR) out.add(item);
            }
            return out;
        }
    }

    /** Je ein Eintrag, dessen Text genau zu diesem Item gehört. */
    public static final List<String> EINZELN = List.of(
            // Erze und Fundorte
            "xenit_ore", "tin_ore", "fluxkristall_ore", "schrott_haufen", "meteoritenkern", "gravitit_ore", "nullmaterie_erz",
            "meteoritenpeiler", "saeuregel",
            // T0/T1 Strom
            "kurbel_dynamo", "handkurbel", "tonnenreaktor", "xenit_zelle", "einspeiser", "ausspeiser", "kupferkabel",
            // T2
            "umspanner", "flux_zelle", "chrono_beschleuniger", "biomasse_kessel", "zeitschleifen_solar",
            "fluxkristall_resonator", "gravitations_pendel",
            // T3
            "chrono_speicher", "materie_rekonfigurator", "klon_tank", "universums_destillator", "rift_kabelknoten",
            "mikrokosmos_zelle", "parallelzapfer",
            // T4
            "weltenformer", "singularitaetskern", "eindaemmungsring",
            // Kanonen und Werkzeuge
            "portal_kanone_mk1", "portal_kanone_mk2", "portal_kanone_mk3", "portal_kanone_mk4", "fluid_kartusche",
            "universe_scanner", "rift_anker", "garagen_hammer", "loetkolben", "universumsprobe", "koordinatenkarte",
            // Gadgets
            "einzweck_drohne", "holo_koeder", "schrumpfstrahler", "anti_grav_granate", "uebersetzer_halsband",
            "erinnerungsphiole", "tarn_detektor", "multiversal_multitool", "zeitstopper", "mutagen_spritze", "aufgaben_box");

    public static List<Seite> alle() {
        List<Seite> out = new ArrayList<>();
        for (String id : EINZELN) out.add(new Seite(id, List.of(id)));

        List<String> ruestung = new ArrayList<>();
        for (String teil : List.of("analyse_visier", "kernmantel", "exo_rahmen", "gravi_stiefel")) {
            for (String stufe : List.of("rohling", "kalibriert", "perfektioniert")) ruestung.add(teil + "_" + stufe);
        }
        out.add(new Seite("ruestung", ruestung));

        List<String> module = new ArrayList<>();
        for (Modul m : Modul.values()) module.add(m.itemName());
        out.add(new Seite("modul", module));

        List<String> chips = new ArrayList<>();
        for (UpgradeChip c : UpgradeChip.values()) chips.add(c.itemName());
        out.add(new Seite("chip", chips));
        out.add(new Seite("multitool_mk", List.of("multiversal_multitool_mk2", "multiversal_multitool_mk3")));
        return out;
    }
}
