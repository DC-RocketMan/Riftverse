package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
    public static final class Items {
        /** Zählt im Tonnenreaktor doppelt. */
        public static final TagKey<Item> SCHROTT = TagKey.create(Registries.ITEM, Riftverse.id("schrott"));

        private Items() {}
    }

    private ModTags() {}
}
