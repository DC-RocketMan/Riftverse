package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import dev.riftverse.machine.MachineMenu;
import dev.riftverse.tech.menu.GeneratorMenu;
import dev.riftverse.tech.menu.ProcessingMenu;
import dev.riftverse.tech.menu.WerkbankMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Riftverse.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE =
            MENUS.register("machine", () -> IMenuTypeExtension.create(MachineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> PROCESSING =
            MENUS.register("processing", () -> IMenuTypeExtension.create(ProcessingMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> GENERATOR =
            MENUS.register("generator", () -> IMenuTypeExtension.create(GeneratorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<WerkbankMenu>> WERKBANK =
            MENUS.register("garagen_werkbank", () -> IMenuTypeExtension.create(WerkbankMenu::new));

    private ModMenus() {}
}
