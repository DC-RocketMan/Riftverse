package dev.riftverse.tech;

import dev.riftverse.energy.ItemEnergy;
import dev.riftverse.energy.Overvoltage;
import dev.riftverse.energy.RiftEnergySink;
import dev.riftverse.energy.VoltageTier;
import dev.riftverse.registry.ModBlockEntities;
import dev.riftverse.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Klon-Tank (T3): züchtet aus Biomasse und Nährserum einen Klonkörper (1.500 FE/t beim Wachsen, 50 FE/t Erhalt).
 * Stirbt der Besitzer, wacht er im fertigen Klon wieder auf, mit allem, was er dabeihatte (siehe {@link Klone}).
 * Ohne Erhaltungsstrom stirbt der Klon nach 10 Sekunden ab.
 * <p>
 * Slots: 0 Biomasse-Pulpe, 1 Nährserum, 2 Akku.
 */
public class KlonTankBlockEntity extends IndustrieBlockEntity implements RiftEnergySink {
    public static final int SLOT_BIOMASSE = 0, SLOT_SERUM = 1, SLOT_BATTERY = 2;
    public static final int GROW_CONSUMPTION = 1_500, KEEP_CONSUMPTION = 50;
    /** Wachstum in Ticks (2 Minuten = 3,6 Mio. FE). */
    public static final int WACHSTUM = 2_400;
    public static final int BIOMASSE = 16, SERUM = 4;
    /** So lange hält ein fertiger Klon ohne Strom durch. */
    public static final int HUNGER_MAX = 200;

    public static final int STATE_UNGEBUNDEN = 0, STATE_ZUTATEN = 1, STATE_WAECHST = 2, STATE_BEREIT = 3, STATE_STROM = 4;

    @Nullable
    private UUID owner;
    private String ownerName = "";
    private int wachstum;
    private boolean bereit;
    private int hunger;
    private boolean powered;

    public KlonTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KLON_TANK.get(), pos, state, GROW_CONSUMPTION * 400, 3);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public boolean bereit() {
        return bereit;
    }

    public int wachstum() {
        return wachstum;
    }

    /** Bindet den Tank an einen Spieler; ein Spieler kann mehrere Tanks haben, benutzt wird der zuletzt gebundene. */
    public void bind(Player player) {
        if (owner != null && !owner.equals(player.getUUID())) unregister();
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        if (level instanceof ServerLevel sl) Klone.get(sl.getServer()).set(owner, GlobalPos.of(sl.dimension(), worldPosition));
        setChanged();
    }

    /** Abbauen: der Spieler verliert diesen Tank als Respawn-Ort. */
    public void unregister() {
        if (owner != null && level instanceof ServerLevel sl) {
            Klone.get(sl.getServer()).remove(owner, GlobalPos.of(sl.dimension(), worldPosition));
        }
    }

    /** Der Besitzer ist gestorben und wacht im Klon auf: der Tank ist danach leer. */
    public void verbrauchen() {
        bereit = false;
        wachstum = 0;
        hunger = 0;
        setChanged();
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    30, 0.3, 0.6, 0.3, 0.1);
        }
    }

    // ------------------------------------------------------------------ Tick

    @Override
    public void serverTick() {
        if (level == null) return;
        ItemEnergy.discharge(items.getStackInSlot(SLOT_BATTERY), energy, VoltageTier.III.maxPacket());
        boolean working = false;
        powered = true;
        if (owner != null) {
            if (bereit) {
                if (energy.take(KEEP_CONSUMPTION) == KEEP_CONSUMPTION) {
                    hunger = 0;
                } else if (++hunger >= HUNGER_MAX) {
                    absterben();
                }
                powered = hunger == 0;
                working = bereit;
            } else if (wachstum == 0) {
                ItemStack bio = items.getStackInSlot(SLOT_BIOMASSE), serum = items.getStackInSlot(SLOT_SERUM);
                if (bio.getCount() >= BIOMASSE && serum.getCount() >= SERUM && energy.stored() >= GROW_CONSUMPTION) {
                    items.extractItem(SLOT_BIOMASSE, BIOMASSE, false);
                    items.extractItem(SLOT_SERUM, SERUM, false);
                    wachstum = 1;
                    setChanged();
                }
            } else {
                if (energy.stored() >= GROW_CONSUMPTION) {
                    energy.take(GROW_CONSUMPTION);
                    wachstum++;
                    working = true;
                    if (wachstum >= WACHSTUM) fertig();
                    setChanged();
                } else {
                    powered = false;
                }
            }
        }
        setActive(working);
    }

    private void fertig() {
        bereit = true;
        hunger = 0;
        if (level instanceof ServerLevel sl && owner != null) {
            sl.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0f, 0.6f);
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(owner);
            if (p != null) p.displayClientMessage(Component.translatable("message.riftverse.klon.bereit").withStyle(ChatFormatting.GREEN), false);
        }
    }

    private void absterben() {
        bereit = false;
        wachstum = 0;
        hunger = 0;
        setChanged();
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    20, 0.3, 0.6, 0.3, 0.02);
            sl.playSound(null, worldPosition, SoundEvents.SLIME_DEATH, SoundSource.BLOCKS, 1.0f, 0.5f);
            ServerPlayer p = owner != null ? sl.getServer().getPlayerList().getPlayer(owner) : null;
            if (p != null) p.displayClientMessage(Component.translatable("message.riftverse.klon.abgestorben").withStyle(ChatFormatting.RED), false);
        }
    }

    // ------------------------------------------------------------------ Netz

    @Override
    public int getDemandedEnergy() {
        return energy.space();
    }

    @Override
    public VoltageTier getSinkTier() {
        return VoltageTier.III;
    }

    @Override
    public int injectEnergy(int amount, VoltageTier tier) {
        return energy.add(amount);
    }

    @Override
    public void onOvervoltage(VoltageTier tier) {
        if (level != null) Overvoltage.blowUp(level, worldPosition, tier);
    }

    // ------------------------------------------------------------------ GUI

    private int state() {
        if (owner == null) return STATE_UNGEBUNDEN;
        if (bereit) return powered ? STATE_BEREIT : STATE_STROM;
        if (wachstum == 0) return STATE_ZUTATEN;
        return powered ? STATE_WAECHST : STATE_STROM;
    }

    @Override
    public List<GuiSlot> guiSlots() {
        return List.of(GuiSlot.in(SLOT_BIOMASSE, 36, 22), GuiSlot.in(SLOT_SERUM, 36, 42));
    }

    @Override
    public int batterySlot() {
        return SLOT_BATTERY;
    }

    @Override
    public VoltageTier guiTier() {
        return VoltageTier.III;
    }

    @Override
    public Progress progress() {
        return Progress.BAR;
    }

    @Override
    public int progressX() {
        return 58;
    }

    @Override
    public int progressY() {
        return 30;
    }

    @Override
    protected int work() {
        return bereit ? WACHSTUM : wachstum;
    }

    @Override
    protected int workMax() {
        return WACHSTUM;
    }

    @Override
    protected int extra(int index) {
        return index == 0 ? state() : 0;
    }

    @Override
    public int infoY() {
        return 42;
    }

    @Override
    public int infoX() {
        return 58;
    }

    @Override
    public List<Component> infoLines(int[] extra) {
        return List.of(Component.translatable("gui.riftverse.klon.state." + extra[0]).withStyle(
                extra[0] == STATE_BEREIT ? ChatFormatting.DARK_GREEN : extra[0] == STATE_STROM ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
    }

    @Override
    public List<Component> progressTooltip(int work, int workMax, int[] extra) {
        return List.of(Component.translatable("tooltip.riftverse.klon_wachstum", work * 100 / Math.max(1, workMax)),
                Component.translatable("tooltip.riftverse.klon_zutaten", BIOMASSE, SERUM),
                Component.translatable("tooltip.riftverse.klon_strom", GROW_CONSUMPTION, KEEP_CONSUMPTION));
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BIOMASSE -> stack.is(ModItems.BIOMASSE_PULPE.get());
            case SLOT_SERUM -> stack.is(ModItems.NAEHRSERUM.get());
            case SLOT_BATTERY -> ItemEnergy.of(stack) != null;
            default -> false;
        };
    }

    // ------------------------------------------------------------------ Speichern

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
        tag.putInt("Wachstum", wachstum);
        tag.putBoolean("Bereit", bereit);
        tag.putInt("Hunger", hunger);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        wachstum = tag.getInt("Wachstum");
        bereit = tag.getBoolean("Bereit");
        hunger = tag.getInt("Hunger");
    }

    @Override
    public Component getDisplayName() {
        return ownerName.isEmpty() ? super.getDisplayName()
                : Component.translatable("gui.riftverse.klon.titel", ownerName);
    }
}
