package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.tech.menu.FluidMixerMenu;
import dev.riftverse.tech.menu.GeneratorMenu;
import dev.riftverse.tech.menu.IndustrieMenu;
import dev.riftverse.tech.menu.ProcessingMenu;
import dev.riftverse.tech.menu.RaffinerieMenu;
import dev.riftverse.tech.menu.ResonatorMenu;
import dev.riftverse.tech.menu.SpeicherMenu;
import dev.riftverse.tech.menu.WerkbankMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Riftverse.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> PROCESSING =
            MENUS.register("processing", () -> IMenuTypeExtension.create(ProcessingMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> GENERATOR =
            MENUS.register("generator", () -> IMenuTypeExtension.create(GeneratorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<WerkbankMenu>> WERKBANK =
            MENUS.register("garagen_werkbank", () -> IMenuTypeExtension.create(WerkbankMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<FluidMixerMenu>> FLUID_MIXER =
            MENUS.register("fluid_mixer", () -> IMenuTypeExtension.create(FluidMixerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<SpeicherMenu>> SPEICHER =
            MENUS.register("speicher", () -> IMenuTypeExtension.create(SpeicherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RaffinerieMenu>> RAFFINERIE =
            MENUS.register("portalfluid_raffinerie", () -> IMenuTypeExtension.create(RaffinerieMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ResonatorMenu>> RESONATOR =
            MENUS.register("fluxkristall_resonator", () -> IMenuTypeExtension.create(ResonatorMenu::new));

    /** Gemeinsame GUI der T3-Geräte aus Meilenstein 4. */
    public static final DeferredHolder<MenuType<?>, MenuType<IndustrieMenu>> INDUSTRIE =
            MENUS.register("industrie", () -> IMenuTypeExtension.create(IndustrieMenu::new));

    private ModMenus() {}
}
