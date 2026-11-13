package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final class Items {
        /** Zählt im Tonnenreaktor doppelt. */
        public static final TagKey<Item> SCHROTT = TagKey.create(Registries.ITEM, Riftverse.id("schrott"));
        /** Brennt im Biomasse-Kessel (Pflanzen; Biomasse-Pulpe brennt viermal so lange). */
        public static final TagKey<Item> BIOMASSE = TagKey.create(Registries.ITEM, Riftverse.id("biomasse"));

        private Items() {}
    }

    public static final class Blocks {
        /** Wächst in der organischen Welt nach dem Abbauen nach. */
        public static final TagKey<Block> WAECHST_NACH = TagKey.create(Registries.BLOCK, Riftverse.id("waechst_nach"));

        private Blocks() {}
    }

    private ModTags() {}
}
