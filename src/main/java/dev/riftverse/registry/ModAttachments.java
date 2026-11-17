package dev.riftverse.registry;

import com.mojang.serialization.Codec;
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

    /** Anfängerschutz: welche der drei T1-Rohstoffwelten schon angesteuert wurden (Bitmaske, siehe {@link dev.riftverse.universe.Anfaengerschutz}). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> ANFAENGERSCHUTZ = ATTACHMENT_TYPES.register("anfaengerschutz",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    /** Rift-Tagebuch schon einmal verteilt (beim ersten Story-Ereignis). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> TAGEBUCH_ERHALTEN = ATTACHMENT_TYPES.register("tagebuch_erhalten",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    private ModAttachments() {}
}
