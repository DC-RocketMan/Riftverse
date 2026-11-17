package dev.riftverse.storyline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.riftverse.storyline.Storyline.Aktion;
import dev.riftverse.storyline.Storyline.Antwort;
import dev.riftverse.storyline.Storyline.Ausloeser;
import dev.riftverse.storyline.Storyline.Bedingung;
import dev.riftverse.storyline.Storyline.BlockSetzen;
import dev.riftverse.storyline.Storyline.Figur;
import dev.riftverse.storyline.Storyline.Geltung;
import dev.riftverse.storyline.Storyline.Ort;
import dev.riftverse.storyline.Storyline.Schritt;
import dev.riftverse.storyline.Storyline.Zeile;
import dev.riftverse.universe.UniverseType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Liest eine Storyline aus JSON. Fehler werfen eine {@link IllegalArgumentException} mit einer Meldung, die sagt, wo
 * in der Datei es hakt (z. B. „Schritt „gespraech“, Aktion 2: unbekannter Typ „dialg““).
 */
public final class StoryParser {
    private StoryParser() {
    }

    public static Storyline lesen(ResourceLocation id, JsonObject json) {
        UniverseType welt = UniverseType.byId(text(json, "welt"));
        if (welt == null) throw new IllegalArgumentException("unbekannte Welt „" + text(json, "welt") + "“");
        if (!welt.einzigartig()) throw new IllegalArgumentException("Welt „" + welt.id() + "“ ist nicht einzigartig");
        Geltung geltung = json.has("geltung") ? Geltung.of(text(json, "geltung")) : Geltung.WELT;
        ResourceLocation fortschritt = json.has("fortschritt") ? rl(text(json, "fortschritt")) : null;

        Map<String, Figur> figuren = new LinkedHashMap<>();
        if (json.has("figuren")) {
            for (Map.Entry<String, JsonElement> e : obj(json, "figuren").entrySet()) {
                String fid = e.getKey();
                try {
                    figuren.put(fid, figur(fid, e.getValue().getAsJsonObject()));
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("Figur „" + fid + "“: " + ex.getMessage(), ex);
                }
            }
        }

        Map<String, Schritt> schritte = new LinkedHashMap<>();
        JsonArray arr = arr(json, "schritte");
        if (arr.isEmpty()) throw new IllegalArgumentException("keine Schritte");
        for (int i = 0; i < arr.size(); i++) {
            JsonObject s = arr.get(i).getAsJsonObject();
            String sid = s.has("id") ? text(s, "id") : "schritt_" + (i + 1);
            try {
                if (schritte.containsKey(sid)) throw new IllegalArgumentException("Id doppelt");
                schritte.put(sid, schritt(sid, s));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Schritt „" + sid + "“: " + ex.getMessage(), ex);
            }
        }
        String start = json.has("start") ? text(json, "start") : schritte.keySet().iterator().next();
        Storyline story = new Storyline(id, welt, text(json, "titel"), opt(json, "beschreibung", ""), geltung, fortschritt,
                Map.copyOf(figuren), java.util.Collections.unmodifiableMap(schritte), start);
        pruefen(story);
        return story;
    }

    // ------------------------------------------------------------------ Teile

    private static Figur figur(String id, JsonObject j) {
        return new Figur(id, text(j, "name"), opt(j, "aussehen", "steve"), ort(j.get("ort")),
                j.has("laufen") && j.get("laufen").getAsBoolean(), j.has("radius") ? j.get("radius").getAsInt() : 6,
                !j.has("sofort") || j.get("sofort").getAsBoolean(), j.has("standard") ? text(j, "standard") : null);
    }

    private static Schritt schritt(String id, JsonObject j) {
        List<Ausloeser> ausloeser = new ArrayList<>();
        JsonElement a = j.get("ausloeser");
        if (a == null) throw new IllegalArgumentException("„ausloeser“ fehlt");
        JsonArray list = a.isJsonArray() ? a.getAsJsonArray() : wrap(a);
        for (int i = 0; i < list.size(); i++) {
            try {
                ausloeser.add(ausloeser(list.get(i).getAsJsonObject()));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Auslöser " + (i + 1) + ": " + ex.getMessage(), ex);
            }
        }
        return new Schritt(id, opt(j, "ziel", ""), List.copyOf(ausloeser), bedingung(j.get("bedingung")),
                aktionen(j.get("aktionen")), j.has("weiter") ? text(j, "weiter") : null,
                j.has("abschluss") && j.get("abschluss").getAsBoolean());
    }

    private static Bedingung bedingung(@Nullable JsonElement e) {
        if (e == null) return Bedingung.IMMER;
        JsonObject j = e.getAsJsonObject();
        return new Bedingung(strings(j.get("flags")), strings(j.get("nicht")));
    }

    private static Ausloeser ausloeser(JsonObject j) {
        String typ = text(j, "typ");
        return switch (typ) {
            case "ankunft" -> new Ausloeser.Ankunft();
            case "bereich" -> new Ausloeser.Bereich(ort(j.get("pos")), j.has("radius") ? j.get("radius").getAsDouble() : 6);
            case "item" -> new Ausloeser.Item(rl(text(j, "item")), anzahl(j));
            case "abgeben" -> new Ausloeser.Abgeben(text(j, "figur"), rl(text(j, "item")), anzahl(j));
            case "besiegt" -> new Ausloeser.Besiegt(rl(text(j, "mob")), anzahl(j));
            case "reden" -> new Ausloeser.Reden(text(j, "figur"));
            case "block" -> new Ausloeser.Block(blockPos(j.get("pos")));
            case "notiz_gelesen" -> new Ausloeser.NotizGelesen(text(j, "notiz"));
            case "zeit" -> new Ausloeser.Zeit(switch (text(j, "wann")) {
                case "nacht" -> true;
                case "tag" -> false;
                default -> throw new IllegalArgumentException("„wann“ muss \"tag\" oder \"nacht\" sein");
            });
            case "flag" -> new Ausloeser.Flag(text(j, "flag"));
            case "antwort" -> new Ausloeser.Antwort(text(j, "antwort"));
            default -> throw new IllegalArgumentException("unbekannter Typ „" + typ + "“");
        };
    }

    static List<Aktion> aktionen(@Nullable JsonElement e) {
        if (e == null) return List.of();
        JsonArray list = e.isJsonArray() ? e.getAsJsonArray() : wrap(e);
        List<Aktion> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            try {
                out.add(aktion(list.get(i).getAsJsonObject()));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Aktion " + (i + 1) + ": " + ex.getMessage(), ex);
            }
        }
        return List.copyOf(out);
    }

    private static Aktion aktion(JsonObject j) {
        String typ = text(j, "typ");
        return switch (typ) {
            case "notiz" -> new Aktion.Notiz(text(j, "id"), text(j, "titel"), text(j, "text"), geltung(j));
            case "dialog" -> new Aktion.Dialog(text(j, "figur"), zeilen(arr(j, "zeilen")));
            case "titel" -> new Aktion.Titel(text(j, "titel"), opt(j, "untertitel", ""));
            case "nachricht" -> new Aktion.Nachricht(text(j, "text"));
            case "item" -> new Aktion.ItemGeben(rl(text(j, "item")), anzahl(j), j.has("name") ? text(j, "name") : null);
            case "xp" -> new Aktion.Xp(j.get("menge").getAsInt());
            case "sound" -> new Aktion.Sound(rl(text(j, "sound")), j.has("lautstaerke") ? j.get("lautstaerke").getAsFloat() : 1f,
                    j.has("tonhoehe") ? j.get("tonhoehe").getAsFloat() : 1f);
            case "flag" -> new Aktion.FlagSetzen(text(j, "flag"), !j.has("wert") || j.get("wert").getAsBoolean(), geltung(j));
            case "figur" -> new Aktion.FigurAktion(text(j, "figur"), switch (opt(j, "aktion", "erscheinen")) {
                case "erscheinen" -> true;
                case "verschwinden" -> false;
                default -> throw new IllegalArgumentException("„aktion“ muss \"erscheinen\" oder \"verschwinden\" sein");
            }, j.has("ort") ? ort(j.get("ort")) : null);
            case "mob" -> new Aktion.Mob(rl(text(j, "mob")), ort(j.get("ort")), anzahl(j));
            case "bloecke" -> new Aktion.Bloecke(bloecke(arr(j, "bloecke")));
            case "schritt" -> new Aktion.Schritt(text(j, "schritt"));
            case "abschluss" -> new Aktion.Abschluss();
            default -> throw new IllegalArgumentException("unbekannter Typ „" + typ + "“");
        };
    }

    private static List<Zeile> zeilen(JsonArray arr) {
        List<Zeile> out = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonElement e = arr.get(i);
            try {
                if (e.isJsonPrimitive()) {
                    out.add(new Zeile(e.getAsString(), null, null, List.of()));
                    continue;
                }
                JsonObject z = e.getAsJsonObject();
                List<Antwort> antworten = new ArrayList<>();
                if (z.has("antworten")) {
                    JsonArray a = arr(z, "antworten");
                    if (a.size() > 4) throw new IllegalArgumentException("höchstens 4 Antworten");
                    for (int k = 0; k < a.size(); k++) {
                        try {
                            antworten.add(antwort(a.get(k).getAsJsonObject()));
                        } catch (RuntimeException ex) {
                            throw new IllegalArgumentException("Antwort " + (k + 1) + ": " + ex.getMessage(), ex);
                        }
                    }
                }
                out.add(new Zeile(text(z, "text"), z.has("wenn") ? text(z, "wenn") : null,
                        z.has("sprecher") ? text(z, "sprecher") : null, List.copyOf(antworten)));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Zeile " + (i + 1) + ": " + ex.getMessage(), ex);
            }
        }
        if (out.isEmpty()) throw new IllegalArgumentException("Dialog ohne Zeilen");
        return List.copyOf(out);
    }

    private static Antwort antwort(JsonObject j) {
        List<Aktion> aktionen = new ArrayList<>(aktionen(j.get("aktionen")));
        if (j.has("flag")) aktionen.add(new Aktion.FlagSetzen(text(j, "flag"), true, null));
        boolean verzweigt = false;
        if (j.has("schritt")) {
            aktionen.add(new Aktion.Schritt(text(j, "schritt")));
            verzweigt = true;
        }
        for (Aktion a : aktionen) verzweigt |= a instanceof Aktion.Schritt || a instanceof Aktion.Abschluss;
        return new Antwort(text(j, "text"), j.has("id") ? text(j, "id") : null, List.copyOf(aktionen), verzweigt);
    }

    private static List<BlockSetzen> bloecke(JsonArray arr) {
        List<BlockSetzen> out = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject b = arr.get(i).getAsJsonObject();
            ResourceLocation block = rl(text(b, "block"));
            if (b.has("von")) {
                out.add(new BlockSetzen(null, 0, blockPos(b.get("von")), blockPos(b.get("bis")), block));
            } else {
                out.add(new BlockSetzen(ort(b.get("pos")), b.has("ueber_boden") ? b.get("ueber_boden").getAsInt() : 0,
                        null, null, block));
            }
        }
        return List.copyOf(out);
    }

    // ------------------------------------------------------------------ Querverweise

    /** Prüft Verweise auf Schritte und Figuren. */
    private static void pruefen(Storyline s) {
        if (!s.schritte().containsKey(s.start())) throw new IllegalArgumentException("Start-Schritt „" + s.start() + "“ gibt es nicht");
        for (Schritt schritt : s.schritte().values()) {
            String wo = "Schritt „" + schritt.id() + "“: ";
            if (schritt.weiter() != null && !s.schritte().containsKey(schritt.weiter())) {
                throw new IllegalArgumentException(wo + "„weiter“ zeigt auf „" + schritt.weiter() + "“, das gibt es nicht");
            }
            for (Ausloeser a : schritt.ausloeser()) {
                String f = a instanceof Ausloeser.Reden r ? r.figur() : a instanceof Ausloeser.Abgeben g ? g.figur() : null;
                if (f != null && !s.figuren().containsKey(f)) throw new IllegalArgumentException(wo + "unbekannte Figur „" + f + "“");
            }
            pruefeAktionen(s, wo, schritt.aktionen());
        }
    }

    private static void pruefeAktionen(Storyline s, String wo, List<Aktion> aktionen) {
        for (Aktion a : aktionen) {
            if (a instanceof Aktion.Schritt sp && !s.schritte().containsKey(sp.schritt())) {
                throw new IllegalArgumentException(wo + "Sprung zu „" + sp.schritt() + "“, das gibt es nicht");
            }
            if (a instanceof Aktion.Dialog d) {
                if (!s.figuren().containsKey(d.figur())) throw new IllegalArgumentException(wo + "Dialog mit unbekannter Figur „" + d.figur() + "“");
                for (Zeile z : d.zeilen()) for (Antwort an : z.antworten()) pruefeAktionen(s, wo, an.aktionen());
            }
            if (a instanceof Aktion.FigurAktion fa && !s.figuren().containsKey(fa.figur())) {
                throw new IllegalArgumentException(wo + "unbekannte Figur „" + fa.figur() + "“");
            }
        }
    }

    // ------------------------------------------------------------------ JSON-Helfer

    private static String text(JsonObject j, String key) {
        JsonElement e = j.get(key);
        if (e == null || e.isJsonNull()) throw new IllegalArgumentException("„" + key + "“ fehlt");
        return e.getAsString();
    }

    private static String opt(JsonObject j, String key, String standard) {
        return j.has(key) ? j.get(key).getAsString() : standard;
    }

    private static JsonObject obj(JsonObject j, String key) {
        JsonElement e = j.get(key);
        if (e == null || !e.isJsonObject()) throw new IllegalArgumentException("„" + key + "“ fehlt oder ist kein Objekt");
        return e.getAsJsonObject();
    }

    private static JsonArray arr(JsonObject j, String key) {
        JsonElement e = j.get(key);
        if (e == null || !e.isJsonArray()) throw new IllegalArgumentException("„" + key + "“ fehlt oder ist keine Liste");
        return e.getAsJsonArray();
    }

    private static JsonArray wrap(JsonElement e) {
        JsonArray a = new JsonArray();
        a.add(e);
        return a;
    }

    private static int anzahl(JsonObject j) {
        return j.has("anzahl") ? Math.max(1, j.get("anzahl").getAsInt()) : 1;
    }

    @Nullable
    private static Geltung geltung(JsonObject j) {
        return j.has("geltung") ? Geltung.of(text(j, "geltung")) : null;
    }

    private static Set<String> strings(@Nullable JsonElement e) {
        if (e == null) return Set.of();
        Set<String> out = new LinkedHashSet<>();
        if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) out.add(x.getAsString());
        } else {
            out.add(e.getAsString());
        }
        return Set.copyOf(out);
    }

    private static ResourceLocation rl(String s) {
        ResourceLocation r = ResourceLocation.tryParse(s);
        if (r == null) throw new IllegalArgumentException("ungültige Id „" + s + "“");
        return r;
    }

    /** {@code [x, y, z]} oder {@code [x, z]} (dann auf dem Boden). */
    private static Ort ort(@Nullable JsonElement e) {
        if (e == null || !e.isJsonArray()) throw new IllegalArgumentException("Ort fehlt (als [x, z] oder [x, y, z])");
        JsonArray a = e.getAsJsonArray();
        if (a.size() == 2) return new Ort(a.get(0).getAsInt(), null, a.get(1).getAsInt());
        if (a.size() == 3) return new Ort(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
        throw new IllegalArgumentException("Ort braucht 2 oder 3 Zahlen");
    }

    private static BlockPos blockPos(@Nullable JsonElement e) {
        Ort o = ort(e);
        if (o.y() == null) throw new IllegalArgumentException("hier braucht der Ort eine Höhe: [x, y, z]");
        return new BlockPos(o.x(), o.y(), o.z());
    }
}
