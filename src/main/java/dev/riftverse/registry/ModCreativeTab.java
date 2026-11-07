package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Riftverse.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.riftverse"))
                    .icon(() -> new ItemStack(ModItems.GARAGEN_WERKBANK.get()))
                    .displayItems((params, output) -> {
                        ModItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                        output.accept(ModItems.ENERGIEZELLE.get().charged());
                    })
                    .build());

    private ModCreativeTab() {}
}
