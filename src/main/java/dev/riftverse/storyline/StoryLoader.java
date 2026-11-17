package dev.riftverse.storyline;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModConditions;
import dev.riftverse.universe.UniverseType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lädt alle Storylines aus {@code data/<ns>/riftverse_story/**.json}. {@code /reload} lädt sie neu, ohne dass der
 * Server neu starten muss. Kaputte Dateien werden mit einer genauen Meldung im Log übersprungen.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class StoryLoader extends SimpleJsonResourceReloadListener {
    public static final String ORDNER = "riftverse_story";
    private static final Gson GSON = new GsonBuilder().create();

    private static volatile Map<ResourceLocation, Storyline> alle = Map.of();
    private static volatile List<String> fehler = List.of();

    private StoryLoader() {
        super(GSON, ORDNER);
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new StoryLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> dateien, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, Storyline> neu = new LinkedHashMap<>();
        List<String> probleme = new ArrayList<>();
        dateien.forEach((id, json) -> {
            try {
                Storyline s = StoryParser.lesen(id, json.getAsJsonObject());
                if (s.welt().entwicklung() && !ModConditions.entwicklung()) return;   // Testwelt gibt es nicht
                neu.put(id, s);
            } catch (RuntimeException e) {
                String msg = id + ": " + e.getMessage();
                probleme.add(msg);
                Riftverse.LOGGER.warn("Riftverse-Storyline übersprungen – {}", msg);
            }
        });
        alle = java.util.Collections.unmodifiableMap(neu);
        fehler = List.copyOf(probleme);
        Riftverse.LOGGER.info("Riftverse: {} Storylines geladen{}", neu.size(), probleme.isEmpty() ? "" : ", " + probleme.size() + " mit Fehlern");
    }

    public static Collection<Storyline> alle() {
        return alle.values();
    }

    @Nullable
    public static Storyline get(ResourceLocation id) {
        return alle.get(id);
    }

    /** Storylines dieser Welt, in Ladereihenfolge. */
    public static List<Storyline> fuer(UniverseType welt) {
        List<Storyline> out = new ArrayList<>();
        for (Storyline s : alle.values()) {
            if (s.welt() == welt) out.add(s);
        }
        return out;
    }

    /** Meldungen der Dateien, die beim letzten Laden kaputt waren. */
    public static List<String> fehler() {
        return fehler;
    }
}
