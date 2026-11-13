package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.Overvoltage;
import dev.riftverse.energy.RiftEnergyBlockEntity;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.recipe.MachineRecipe;
import dev.riftverse.recipe.MachineRecipeInput;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModItems;
import dev.riftverse.tech.menu.ProcessingMenu;
import dev.riftverse.universe.UniverseId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemeinsame Block-Entity der verarbeitenden Maschinen. Was die Maschine kann, steht in {@link MachineKind}.
 * <p>
 * Slots: Eingänge 0..n-1, Ausgänge n..n+o-1, Akku-Slot n+o (nur elektrische Maschinen).
 */
public class ProcessingBlockEntity extends RiftEnergyBlockEntity
        implements MenuProvider, RiftEnergySink, Crankable, ServerTickable, DropsContents,
        dev.riftverse.tech.multiblock.MultiblockController, dev.riftverse.tech.upgrade.Aufruestbar {

    public static final int DATA_PROGRESS = 0, DATA_MAX = 1, DATA_ENERGY_LO = 2, DATA_ENERGY_HI = 3,
            DATA_CAP_LO = 4, DATA_CAP_HI = 5, DATA_MODE = 6, DATA_CRANK = 7, DATA_STEPS = 8, DATA_CHIPS = 9, DATA_KRISE = 10,
            DATA_COUNT = 11;
    /** KI-Chip: Chance pro Arbeitstick auf eine existenzielle Krise, und wie lange sie dauert. */
    public static final int KRISE_CHANCE = 2_400, KRISE_DAUER = 100;

    private final MachineKind kind;
    private final ItemStackHandler items;
    private final IItemHandler automation;
    private final ContainerData data;

    private int progress;
    private int maxProgress = 1;
    private int mode;
    private int crankTicks;
    private boolean recipeDirty = true;
    @Nullable
    private RecipeHolder<MachineRecipe> cached;
    /** Multiblock-Maschinen (Rift-Schmiede): steht der Aufbau? */
    private boolean formed;
    private int checkIn;
    /** Eingebaute Upgrade-Chips (Meilenstein 6). */
    private final java.util.EnumSet<dev.riftverse.tech.upgrade.UpgradeChip> chips =
            java.util.EnumSet.noneOf(dev.riftverse.tech.upgrade.UpgradeChip.class);
    private float progressRest;
    private int krise;

    public ProcessingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROCESSING.get(), pos, state, kindOf(state).capacity());
        this.kind = kindOf(state);
        final int inputs = kind.inputs();
        final int outputs = kind.outputs();
        this.items = new ItemStackHandler(inputs + outputs + 1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (slot < inputs) recipeDirty = true;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot >= inputs && slot < inputs + outputs) return false;
                if (slot == inputs + outputs) return ItemEnergy.of(stack) != null;
                return true;
            }
        };
        this.automation = new Automation();
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_PROGRESS -> progress;
                    case DATA_MAX -> maxProgress;
                    case DATA_ENERGY_LO -> energy.stored() & 0xFFFF;
                    case DATA_ENERGY_HI -> energy.stored() >>> 16;
                    case DATA_CAP_LO -> energy.capacity() & 0xFFFF;
                    case DATA_CAP_HI -> energy.capacity() >>> 16;
                    case DATA_MODE -> mode;
                    case DATA_CRANK -> crankTicks;
                    case DATA_STEPS -> cached != null && kind.ordered() ? cached.value().steps() : 0;
                    case DATA_CHIPS -> chipMaske();
                    case DATA_KRISE -> krise;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Nur der Client schreibt hier hinein; die Werte kommen vom Server.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static MachineKind kindOf(BlockState state) {
        return state.getBlock() instanceof ProcessingBlock block ? block.kind() : MachineKind.ERZMUEHLE;
    }

    /** Fortschritt des laufenden Vorgangs (für Jade). */
    public int arbeit() {
        return progress;
    }

    public int arbeitMax() {
        return maxProgress;
    }

    public MachineKind kind() {
        return kind;
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automationHandler() {
        return automation;
    }

    /** Erster (Haupt-)Ausgang. */
    public int outputSlot() {
        return kind.inputs();
    }

    public boolean isOutputSlot(int slot) {
        return slot >= kind.inputs() && slot < kind.inputs() + kind.outputs();
    }

    public int batterySlot() {
        return kind.inputs() + kind.outputs();
    }

    @Override
    protected boolean joinsNetwork() {
        return kind.electric();
    }

    // ------------------------------------------------------------------ Tick

    private net.minecraft.core.Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(TechBlock.FACING) ? state.getValue(TechBlock.FACING) : net.minecraft.core.Direction.NORTH;
    }

    /** Ob der Multiblock steht; Einzelmaschinen sind immer „vollständig“. */
    public boolean formed() {
        return kind.multiblock() == null || formed;
    }

    @Nullable
    @Override
    public net.minecraft.network.chat.Component checkStructure() {
        dev.riftverse.tech.multiblock.Multiblock mb = kind.multiblock();
        if (mb == null || level == null) return null;
        net.minecraft.network.chat.Component missing = mb.missingMessage(level, worldPosition, facing());
        formed = missing == null;
        checkIn = 20;
        return missing;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        if (kind.electric()) ItemEnergy.discharge(items.getStackInSlot(batterySlot()), energy, kind.tier().maxPacket());
        if (kind.multiblock() != null && --checkIn <= 0) checkStructure();

        RecipeHolder<MachineRecipe> holder = formed() ? currentRecipe() : null;
        boolean working = false;
        if (holder != null && canOutput(withTarget(holder.value().allOutputs()))) {
            MachineRecipe recipe = holder.value();
            maxProgress = recipe.time();
            boolean doppelt = chips.contains(dev.riftverse.tech.upgrade.UpgradeChip.PARALLEL) && kannDoppelt(recipe);
            if (kind.electric()) {
                int cost = verbrauch(doppelt);
                if (krise > 0) {
                    krise--;
                } else if (energy.stored() >= cost) {
                    energy.take(cost);
                    progressRest += tempo();
                    int steps = (int) progressRest;
                    progressRest -= steps;
                    progress += steps;
                    working = true;
                    if (chips.contains(dev.riftverse.tech.upgrade.UpgradeChip.KI) && level.random.nextInt(KRISE_CHANCE) == 0) kiKrise();
                }
            } else if (crankTicks > 0) {
                progress++;
                working = true;
            }
            if (progress >= maxProgress) {
                craft(recipe);
                if (doppelt && currentRecipe() == holder && canOutput(withTarget(recipe.allOutputs()))) craft(recipe);
                progress = 0;
                if (chips.contains(dev.riftverse.tech.upgrade.UpgradeChip.KI) && level.random.nextInt(6) == 0) kiSagen("kommentar", 8);
            }
            if (working) setChanged();
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
        if (crankTicks > 0) crankTicks--;
        setActive(working);
        if (working && kind == MachineKind.RIFT_SCHMIEDE) schmiedeEffekte();
    }

    // ------------------------------------------------------------------ Upgrade-Chips

    /** Verbrauch pro Tick mit allen Chips: Tempo ×2,5, Effizienz ×0,7, zwei parallele Vorgänge ×2. */
    public int verbrauch(boolean doppelt) {
        double c = kind.energyPerTick();
        if (chips.contains(dev.riftverse.tech.upgrade.UpgradeChip.TEMPO)) c *= 2.5;
        if (chips.contains(dev.riftverse.tech.upgrade.UpgradeChip.EFFIZIENZ)) c *= 0.7;
        if (doppelt) c *= 2;
        return Math.max(1, (int) Math.ceil(c));
    }

    /** Fortschritt pro Arbeitstick: Tempo ×2, KI ×1,1. */
    public float tempo() {
        float t = 1f;
        if (chips.contains(dev.riftverse.tech.upgrade.UpgradeChip.TEMPO)) t *= 2f;
        if (chips.contains(dev.riftverse.tech.upgrade.UpgradeChip.KI)) t *= 1.1f;
        return t;
    }

    /** Reichen die Zutaten für zwei Vorgänge? (Die halbierten Stapel müssen das Rezept noch erfüllen.) */
    private boolean kannDoppelt(MachineRecipe recipe) {
        List<ItemStack> half = new ArrayList<>(kind.inputs());
        for (int i = 0; i < kind.inputs(); i++) {
            ItemStack st = items.getStackInSlot(i);
            half.add(st.copyWithCount(st.getCount() / 2));
        }
        if (recipe.assignment(new MachineRecipeInput(half, input().mode())) == null) return false;
        List<ItemStack> twice = new ArrayList<>(withTarget(recipe.allOutputs()));
        twice.addAll(withTarget(recipe.allOutputs()));
        return canOutput(twice);
    }

    private int chipMaske() {
        int m = 0;
        for (dev.riftverse.tech.upgrade.UpgradeChip c : chips) m |= 1 << c.ordinal();
        return m;
    }

    @Override
    public java.util.Set<dev.riftverse.tech.upgrade.UpgradeChip> chips() {
        return java.util.Collections.unmodifiableSet(chips);
    }

    @Override
    public boolean einbauen(dev.riftverse.tech.upgrade.UpgradeChip chip) {
        if (!kind.electric() || chips.contains(chip)) return false;
        chips.add(chip);
        setChanged();
        return true;
    }

    @Override
    public List<ItemStack> ausbauen() {
        List<ItemStack> out = new ArrayList<>();
        for (dev.riftverse.tech.upgrade.UpgradeChip c : chips) out.add(new ItemStack(ModItems.chip(c)));
        chips.clear();
        krise = 0;
        setChanged();
        return out;
    }

    /** KI-Chip: Die Maschine hält 5 Sekunden inne und zweifelt am Sinn des Ganzen. */
    private void kiKrise() {
        krise = KRISE_DAUER;
        kiSagen("krise", 8);
    }

    private void kiSagen(String art, int varianten) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return;
        net.minecraft.network.chat.Component name = getBlockState().getBlock().getName();
        net.minecraft.network.chat.Component line = net.minecraft.network.chat.Component.literal("[").append(name).append("] ")
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                .append(net.minecraft.network.chat.Component.translatable("ki.riftverse." + art + "." + sl.random.nextInt(varianten))
                        .withStyle(net.minecraft.ChatFormatting.WHITE));
        for (Player p : sl.players()) {
            if (p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)) < 16 * 16) p.displayClientMessage(line, false);
        }
    }

    /** Für den Rauchtest. */
    public int krise() {
        return krise;
    }

    /** Rift-Schmiede: Das Mini-Portal über dem Amboss wirbelt, ab und zu fällt ein Hammerschlag mit Hall. */
    private void schmiedeEffekte() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return;
        long t = level.getGameTime();
        BlockPos portal = dev.riftverse.tech.multiblock.Multiblock.at(worldPosition, facing(), dev.riftverse.tech.multiblock.Multiblock.SCHMIEDE_PORTAL);
        if (t % 5 == 0) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, portal.getX() + 0.5, portal.getY() + 0.5, portal.getZ() + 0.5,
                    6, 0.25, 0.25, 0.25, 0.4);
        }
        if (t % 40 == 0) {
            sl.playSound(null, portal, net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.7f, 0.6f);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, portal.getX() + 0.5, portal.getY() - 0.4, portal.getZ() + 0.5,
                    10, 0.3, 0.05, 0.3, 0.15);
        }
    }

    private MachineRecipeInput input() {
        List<ItemStack> stacks = new ArrayList<>(kind.inputs());
        for (int i = 0; i < kind.inputs(); i++) stacks.add(items.getStackInSlot(i));
        String m = kind.hasModes() ? kind.modes().get(Math.floorMod(mode, kind.modes().size())) : "";
        return new MachineRecipeInput(stacks, m);
    }

    @Nullable
    private RecipeHolder<MachineRecipe> currentRecipe() {
        if (recipeDirty && level != null) {
            cached = level.getRecipeManager().getRecipeFor(kind.recipeType(), input(), level).orElse(null);
            // Der Dimensions-Scanner liest nur gefüllte Universumsproben
            if (cached != null && kind == MachineKind.DIMENSIONS_SCANNER && scannedTarget() == null) cached = null;
            recipeDirty = false;
        }
        return cached;
    }

    /** Universum der Probe im Dimensions-Scanner, oder null. */
    @Nullable
    private UniverseId scannedTarget() {
        for (int i = 0; i < kind.inputs(); i++) {
            UniverseId id = items.getStackInSlot(i).get(ModDataComponents.ZIEL.get());
            if (id != null) return id;
        }
        return null;
    }

    /** Dimensions-Scanner: Die Koordinatenkarte bekommt das Universum der Probe. */
    private List<ItemStack> withTarget(List<ItemStack> products) {
        if (kind != MachineKind.DIMENSIONS_SCANNER) return products;
        UniverseId target = scannedTarget();
        if (target == null) return products;
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack p : products) {
            ItemStack c = p.copy();
            c.set(ModDataComponents.ZIEL.get(), target);
            out.add(c);
        }
        return out;
    }

    /** Passen alle möglichen Produkte gleichzeitig in die Ausgänge? */
    private boolean canOutput(List<ItemStack> products) {
        List<ItemStack> outs = new ArrayList<>();
        for (int i = 0; i < kind.outputs(); i++) outs.add(items.getStackInSlot(outputSlot() + i).copy());
        for (ItemStack product : products) {
            if (!insertInto(outs, product.copy())) return false;
        }
        return true;
    }

    /** Legt {@code stack} in die Liste (erst zusammenführen, dann freier Platz). */
    private static boolean insertInto(List<ItemStack> outs, ItemStack stack) {
        for (ItemStack o : outs) {
            if (!o.isEmpty() && ItemStack.isSameItemSameComponents(o, stack)) {
                int move = Math.min(stack.getCount(), o.getMaxStackSize() - o.getCount());
                o.grow(move);
                stack.shrink(move);
                if (stack.isEmpty()) return true;
            }
        }
        for (int i = 0; i < outs.size(); i++) {
            if (outs.get(i).isEmpty()) {
                outs.set(i, stack);
                return true;
            }
        }
        return false;
    }

    private void craft(MachineRecipe recipe) {
        MachineRecipeInput in = input();
        int[] slots = recipe.assignment(in);
        if (slots == null) {
            recipeDirty = true;
            return;
        }
        List<ItemStack> products = withTarget(recipe.roll(level.random));
        uebernehmen(products, slots);
        for (int i = 0; i < slots.length; i++) {
            items.extractItem(slots[i], recipe.ingredients().get(i).count(), false);
        }
        List<ItemStack> outs = new ArrayList<>();
        for (int i = 0; i < kind.outputs(); i++) outs.add(items.getStackInSlot(outputSlot() + i).copy());
        for (ItemStack product : products) insertInto(outs, product);
        for (int i = 0; i < kind.outputs(); i++) {
            if (!ItemStack.matches(outs.get(i), items.getStackInSlot(outputSlot() + i))) items.setStackInSlot(outputSlot() + i, outs.get(i));
        }
    }

    /**
     * Ausbaustufen behalten, was eingebaut ist: Wird aus einem Rüstungsteil, Gadget oder einer Kanone das nächste
     * gebaut, wandern Module, Ladung, Tank, Schaltzustand und gespeicherte Ziele mit.
     */
    private void uebernehmen(List<ItemStack> products, int[] slots) {
        for (ItemStack product : products) {
            for (int slot : slots) {
                ItemStack from = items.getStackInSlot(slot);
                if (!gleicheFamilie(from, product)) continue;
                copy(from, product, ModDataComponents.MODULE.get());
                copy(from, product, ModDataComponents.ENERGY.get());
                copy(from, product, ModDataComponents.KANONEN_TANK.get());
                copy(from, product, ModDataComponents.KANONEN_ZIELE.get());
                copy(from, product, ModDataComponents.ZUSTAND.get());
                copy(from, product, ModDataComponents.MODUS.get());
                break;
            }
        }
    }

    private static boolean gleicheFamilie(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;
        if (a.getItem() instanceof dev.riftverse.ruestung.MultiversalArmorItem x && b.getItem() instanceof dev.riftverse.ruestung.MultiversalArmorItem y) {
            return x.getType() == y.getType();
        }
        if (a.getItem() instanceof dev.riftverse.gadget.MultitoolItem && b.getItem() instanceof dev.riftverse.gadget.MultitoolItem) return true;
        return a.getItem() instanceof dev.riftverse.item.PortalKanoneItem && b.getItem() instanceof dev.riftverse.item.PortalKanoneItem;
    }

    private static <T> void copy(ItemStack from, ItemStack to, net.minecraft.core.component.DataComponentType<T> type) {
        T v = from.get(type);
        if (v != null) to.set(type, v);
    }

    private void setActive(boolean active) {
        if (level == null) return;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(TechBlock.ACTIVE) && state.getValue(TechBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(TechBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ Handbetrieb

    @Override
    public void crank(Player player) {
        if (!kind.electric()) crankTicks = 3;
    }

    // ------------------------------------------------------------------ Betriebsart (Walzbank)

    public void cycleMode() {
        if (!kind.hasModes()) return;
        mode = (mode + 1) % kind.modes().size();
        progress = 0;
        recipeDirty = true;
        setChanged();
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getDemandedEnergy() {
        return kind.electric() ? energy.space() : 0;
    }

    @Override
    public VoltageTier getSinkTier() {
        VoltageTier t = kind.tier();
        if (chips.contains(dev.riftverse.tech.upgrade.UpgradeChip.TRANSFORMATOR) && t.ordinal() + 1 < VoltageTier.values().length) {
            return VoltageTier.values()[t.ordinal() + 1];
        }
        return t;
    }

    @Override
    public int injectEnergy(int amount, VoltageTier tier) {
        return energy.add(amount);
    }

    @Override
    public void onOvervoltage(VoltageTier tier) {
        if (level != null) Overvoltage.blowUp(level, worldPosition, tier);
    }

    // ------------------------------------------------------------------ Inventar

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, items.getStackInSlot(i));
        }
        for (dev.riftverse.tech.upgrade.UpgradeChip c : chips) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.chip(c)));
        }
    }

    /** Für Trichter und Rohre: rein nur in die Eingänge, raus nur aus dem Ausgang. */
    private class Automation implements IItemHandler {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < kind.inputs() ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isOutputSlot(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < kind.inputs();
        }
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", items.serializeNBT(registries));
        tag.putInt("Progress", progress);
        tag.putInt("Mode", mode);
        if (!chips.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (dev.riftverse.tech.upgrade.UpgradeChip c : chips) list.add(net.minecraft.nbt.StringTag.valueOf(c.name()));
            tag.put("Chips", list);
        }
        tag.putInt("Krise", krise);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        mode = tag.getInt("Mode");
        chips.clear();
        net.minecraft.nbt.ListTag list = tag.getList("Chips", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            dev.riftverse.tech.upgrade.UpgradeChip c = dev.riftverse.tech.upgrade.UpgradeChip.byName(list.getString(i));
            if (c != null) chips.add(c);
        }
        krise = tag.getInt("Krise");
        recipeDirty = true;
    }

    // ------------------------------------------------------------------ GUI

    @Override
    public Component getDisplayName() {
        return Component.translatable(kind.translationKey());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ProcessingMenu(containerId, inventory, this, data);
    }
}
