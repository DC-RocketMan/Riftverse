package dev.riftverse.compat.jade;

import dev.riftverse.Riftverse;
import dev.riftverse.cable.CableBlock;
import dev.riftverse.compat.BlockInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;
import java.util.Locale;

/**
 * Jade-Anbindung. Der Server baut die Zeilen aus {@link BlockInfo} und schickt sie als JSON-Texte; der Client
 * hängt sie an den Tooltip. Kabel zeigen Stufe und Verlust direkt aus dem Block. Wird nur geladen, wenn Jade
 * installiert ist.
 */
@WailaPlugin
public class RiftverseJadePlugin implements IWailaPlugin {
    static final ResourceLocation MASCHINE = ResourceLocation.fromNamespaceAndPath(Riftverse.MODID, "maschine");
    static final ResourceLocation KABEL = ResourceLocation.fromNamespaceAndPath(Riftverse.MODID, "kabel");
    private static final String SCHLUESSEL = "riftverse_zeilen";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Daten.INSTANCE, BaseEntityBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Anzeige.INSTANCE, BaseEntityBlock.class);
        registration.registerBlockComponent(Kabel.INSTANCE, CableBlock.class);
    }

    private static boolean unser(BlockEntity be) {
        return be != null && Riftverse.MODID.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).getNamespace());
    }

    /** Server: Zeilen als JSON-Liste in die Jade-Daten. */
    enum Daten implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity be = accessor.getBlockEntity();
            if (!unser(be)) return;
            List<Component> zeilen = BlockInfo.zeilen(be);
            if (zeilen.isEmpty()) return;
            ListTag liste = new ListTag();
            var registries = accessor.getLevel().registryAccess();
            for (Component z : zeilen) liste.add(StringTag.valueOf(Component.Serializer.toJson(z, registries)));
            data.put(SCHLUESSEL, liste);
        }

        @Override
        public boolean shouldRequestData(BlockAccessor accessor) {
            return unser(accessor.getBlockEntity());
        }

        @Override
        public ResourceLocation getUid() {
            return MASCHINE;
        }
    }

    /** Client: die geschickten Zeilen anzeigen. */
    enum Anzeige implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(SCHLUESSEL, Tag.TAG_LIST)) return;
            var registries = accessor.getLevel().registryAccess();
            ListTag liste = data.getList(SCHLUESSEL, Tag.TAG_STRING);
            for (int i = 0; i < liste.size(); i++) {
                Component z = Component.Serializer.fromJson(liste.getString(i), registries);
                if (z != null) tooltip.add(z);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return MASCHINE;
        }
    }

    /** Kabel: Spannungsstufe und Grundverlust pro Block. */
    enum Kabel implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlock() instanceof CableBlock kabel)) return;
            tooltip.add(Component.translatable("jade.riftverse.spannung", kabel.tier().describe()));
            String verlust = String.format(Locale.GERMANY, "%.2f", kabel.lossPerBlock()).replaceAll(",?0+$", "");
            tooltip.add(Component.translatable("jade.riftverse.verlust", verlust));
            if (!kabel.insulated()) tooltip.add(Component.translatable("jade.riftverse.blank"));
        }

        @Override
        public ResourceLocation getUid() {
            return KABEL;
        }
    }
}
