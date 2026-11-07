package dev.riftverse.item;

import dev.riftverse.portal.RiftPortalEntity;
import dev.riftverse.registry.ModItems;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseId;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Die Rift-Kanone.
 * Rechtsklick: Portal schießen (verbraucht 1 Portal-Fluid-Kartusche).
 * Schleichen + Rechtsklick: Modus wechseln (Zufall → Heimat → Letztes Universum).
 */
public class RiftGunItem extends Item {
    public enum Mode {
        RANDOM("Zufälliges Universum", ChatFormatting.GREEN),
        HOME("Heimat", ChatFormatting.AQUA),
        LAST("Letztes Universum", ChatFormatting.GOLD);

        final String label;
        final ChatFormatting color;

        Mode(String label, ChatFormatting color) {
            this.label = label;
            this.color = color;
        }
    }

    private final int tier;

    public RiftGunItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int range() {
        return tier >= 2 ? 64 : 32;
    }

    // ------------------------------------------------------------------ Daten am Item

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void edit(ItemStack stack, java.util.function.Consumer<CompoundTag> editor) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, editor);
    }

    public static Mode mode(ItemStack stack) {
        int i = data(stack).getInt("Mode");
        return Mode.values()[Math.floorMod(i, Mode.values().length)];
    }

    // ------------------------------------------------------------------ Benutzung

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                Mode next = Mode.values()[(mode(gun).ordinal() + 1) % Mode.values().length];
                edit(gun, tag -> tag.putInt("Mode", next.ordinal()));
                player.displayClientMessage(Component.literal("Modus: ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(next.label).withStyle(next.color)), true);
                level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6f, 1.4f);
            }
            return InteractionResultHolder.sidedSuccess(gun, level.isClientSide);
        }

        if (level.isClientSide) return InteractionResultHolder.success(gun);
        ServerLevel server = (ServerLevel) level;
        Mode mode = mode(gun);
        CompoundTag tag = data(gun);

        // --- Munition prüfen ---
        if (!player.getAbilities().instabuild && findAmmo(player) < 0) {
            player.displayClientMessage(Component.literal("Kein Portal-Fluid! Stell Kartuschen im Fluid-Synthesizer her.")
                    .withStyle(ChatFormatting.RED), true);
            level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1f, 1.2f);
            return InteractionResultHolder.fail(gun);
        }

        // --- Ziel bestimmen ---
        ResourceKey<Level> targetDim;
        UniverseId universe = null;
        Vec3 exact = null;
        int style;
        switch (mode) {
            case HOME -> {
                style = RiftPortalEntity.STYLE_HOME;
                if (tag.contains("HomeDim")) {
                    targetDim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("HomeDim")));
                    exact = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
                } else {
                    targetDim = Level.OVERWORLD;
                    BlockPos spawn = server.getServer().overworld().getSharedSpawnPos();
                    exact = Vec3.atBottomCenterOf(spawn);
                }
            }
            case LAST -> {
                style = RiftPortalEntity.STYLE_RANDOM;
                universe = UniverseId.load(tag.getCompound("Last"));
                if (universe == null) {
                    player.displayClientMessage(Component.literal("Noch kein Universum gespeichert – erst im Zufallsmodus reisen.")
                            .withStyle(ChatFormatting.YELLOW), true);
                    return InteractionResultHolder.fail(gun);
                }
                targetDim = Multiverse.dimension(universe.variant());
            }
            default -> {
                style = RiftPortalEntity.STYLE_RANDOM;
                universe = Multiverse.random(level.random);
                targetDim = Multiverse.dimension(universe.variant());
            }
        }

        // Abflugort außerhalb des Multiversums als Heimat merken
        if (!Multiverse.isMultiverse(level) && mode != Mode.HOME) {
            Vec3 home = player.position();
            String dim = level.dimension().location().toString();
            edit(gun, t -> {
                t.putString("HomeDim", dim);
                t.putDouble("HomeX", home.x);
                t.putDouble("HomeY", home.y);
                t.putDouble("HomeZ", home.z);
            });
        }
        if (universe != null) {
            UniverseId saved = universe;
            edit(gun, t -> t.put("Last", saved.save()));
        }

        // --- Portal platzieren ---
        Vec3 spot = portalSpot(level, player);
        float yaw = player.getYRot() + 180f;
        RiftPortalEntity portal = RiftPortalEntity.create(level, spot, yaw, style, tier >= 2 ? 900 : 600);
        if (exact != null) portal.setExactTarget(targetDim, exact);
        else portal.setUniverseTarget(targetDim, universe);
        level.addFreshEntity(portal);

        // --- Kosten & Feedback ---
        if (!player.getAbilities().instabuild && !(tier >= 2 && level.random.nextBoolean())) {
            int slot = findAmmo(player);
            if (slot >= 0) player.getInventory().getItem(slot).shrink(1);
        }
        player.getCooldowns().addCooldown(this, tier >= 2 ? 20 : 50);
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.9f);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.6f);
        Vec3 eye = player.getEyePosition();
        Vec3 dir = spot.add(0, 1.2, 0).subtract(eye);
        for (int i = 0; i < 20; i++) {
            Vec3 p = eye.add(dir.scale(i / 20.0));
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }

        if (universe != null) {
            UniverseProfile profile = UniverseProfile.get(universe);
            player.displayClientMessage(Component.literal("Rift geöffnet → ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(profile.title()).withStyle(ChatFormatting.WHITE)), true);
        } else {
            player.displayClientMessage(Component.literal("Rift geöffnet → Heimat").withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResultHolder.sidedSuccess(gun, false);
    }

    /** Portal an der anvisierten Stelle, auf dem Boden stehend und dem Spieler zugewandt. */
    private Vec3 portalSpot(Level level, Player player) {
        HitResult hit = player.pick(range(), 1.0f, false);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 spot;
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 h = hit.getLocation();
            spot = h.subtract(look.scale(0.7));
            if (hit instanceof BlockHitResult bhr && bhr.getDirection() == net.minecraft.core.Direction.UP) {
                spot = h.subtract(look.x * 0.7, 0, look.z * 0.7);
            }
        } else {
            spot = eye.add(look.scale(4));
        }
        // auf den Boden absenken (max. 4 Blöcke)
        BlockPos.MutableBlockPos pos = BlockPos.containing(spot).mutable();
        for (int i = 0; i < 4 && level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty(); i++) {
            pos.move(0, -1, 0);
        }
        return new Vec3(spot.x, pos.getY(), spot.z);
    }

    private static int findAmmo(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModItems.RIFT_FLUID_CELL.get())) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ Tooltip

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Mode mode = mode(stack);
        CompoundTag tag = data(stack);
        tooltip.add(Component.literal("Modus: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(mode.label).withStyle(mode.color)));
        if (tag.contains("HomeDim")) {
            tooltip.add(Component.literal(String.format(Locale.ROOT, "Heimat: %.0f / %.0f / %.0f",
                    tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"))).withStyle(ChatFormatting.DARK_AQUA));
        }
        UniverseId last = UniverseId.load(tag.getCompound("Last"));
        if (last != null) {
            tooltip.add(Component.literal("Zuletzt: Universum " + last.code()).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.literal("Rechtsklick: Rift öffnen · Schleichen: Modus").withStyle(ChatFormatting.DARK_GRAY));
        if (tier >= 2) tooltip.add(Component.literal("Mk II: doppelte Reichweite, spart oft Fluid").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier >= 2;
    }
}
