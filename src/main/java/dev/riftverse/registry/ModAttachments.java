package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Daten am Spieler, die auch den Tod überstehen. */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Riftverse.MODID);

    /** Heimanker: wohin die Rückkehr-Taste der Portal-Kanone führt (siehe {@link dev.riftverse.portal.Heimanker}). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompoundTag>> HEIMANKER = ATTACHMENT_TYPES.register("heimanker",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC).copyOnDeath().build());

    private ModAttachments() {}
}
