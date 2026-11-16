package dev.riftverse.client.handbuch;

import dev.riftverse.handbuch.Handbuch;
import guideme.Guide;
import guideme.compiler.tags.RecipeTypeMappingSupplier;

/**
 * Baut den GuideME-Guide „Riftverse-Handbuch“ (nur auf dem Client).
 * <p>
 * Die Seiten sind deutsch und liegen direkt im Guide-Ordner; eine englische Fassung käme später nach
 * {@code _en_us/}. Zum Schreiben im laufenden Spiel: mit
 * {@code -Dguideme.riftverse.handbuch.sources=<Pfad zum Guide-Ordner>} starten, dann lädt GuideME Änderungen sofort.
 */
public final class HandbuchClient {
    private HandbuchClient() {
    }

    public static void erstellen() {
        Guide.builder(Handbuch.ID)
                .defaultLanguage("de_de")
                .extension(RecipeTypeMappingSupplier.EXTENSION_POINT, new HandbuchRezepte())
                .build();
    }
}
