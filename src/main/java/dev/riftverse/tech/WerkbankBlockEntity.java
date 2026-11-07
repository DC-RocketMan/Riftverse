package dev.riftverse.tech;

import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModItems;
import dev.riftverse.tech.menu.WerkbankMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** Hält die Werkzeuge der Garagen-Werkbank: Hammer, Lötkolben, Lötzinn. Das Raster leert sich beim Schließen. */
public class WerkbankBlockEntity extends BlockEntity implements MenuProvider, DropsContents {
    public static final int SLOT_HAMMER = 0, SLOT_LOETKOLBEN = 1, SLOT_LOETZINN = 2;

    private final ItemStackHandler tools = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isTool(slot, stack);
        }
    };

    public WerkbankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WERKBANK.get(), pos, state);
    }

    public static boolean isTool(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_HAMMER -> stack.is(ModItems.GARAGEN_HAMMER.get());
            case SLOT_LOETKOLBEN -> stack.is(ModItems.LOETKOLBEN.get());
            case SLOT_LOETZINN -> stack.is(ModItems.LOETZINN.get());
            default -> false;
        };
    }

    public ItemStackHandler tools() {
        return tools;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < tools.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, tools.getStackInSlot(i));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tools", tools.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tools.deserializeNBT(registries, tag.getCompound("Tools"));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.riftverse.garagen_werkbank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new WerkbankMenu(containerId, inventory, this, ContainerLevelAccess.create(player.level(), worldPosition));
    }
}
