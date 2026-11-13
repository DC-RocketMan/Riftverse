package dev.riftverse.ruestung;

import dev.riftverse.Riftverse;
import dev.riftverse.gadget.Gadget;
import dev.riftverse.gadget.TarnDetektorItem;
import dev.riftverse.item.FluidKartuscheItem;
import dev.riftverse.item.PortalKanoneItem;
import dev.riftverse.portal.Heimanker;
import dev.riftverse.portal.RiftTravel;
import dev.riftverse.fluid.Zerfall;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModDataComponents.KanonenTank;
import dev.riftverse.registry.ModItems;
import dev.riftverse.universe.Multiverse;
import dev.riftverse.universe.UniverseProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Was die Multiversal-Rüstung im Spiel tut: Module, gemeinsamer Energiespeicher, Brusttank und Set-Bonus.
 * <p>
 * Strom: Jedes Teil zieht erst aus dem eigenen Speicher, dann aus dem Kernmantel. Laufende Kosten (FE/t) werden
 * gesammelt und einmal pro Sekunde abgebucht. Ist nichts mehr da, schweigen die Module des Teils.
 * <p>
 * Schaltzustand ({@link ModDataComponents#ZUSTAND}): Helm Bit 0 = Tarn-Detektor an; Hose Bits 0–1 =
 * Gravitationsregler (0 aus, 1 = 50 %, 2 = 100 %, 3 = 200 %), Bit 2 = Schrumpf-Modul an.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class Ruestung {
    public static final int BRUSTTANK = 4_000;
    public static final int SCHILD_FE = 1_000, FALL_FE = 500, SPRUNG_FE = 5, PHASE_FE = 5_000, PHASE_WEITE = 6, PHASE_CD = 60;
    public static final int NOT_PORTAL_FE = 200_000, NOT_PORTAL_MB = 100, NOT_PORTAL_CD = 12_000;
    public static final String NOT_PORTAL_KEY = "riftverse:not_portal_bereit", PHASE_KEY = "riftverse:phasenschritt_bereit";
    public static final double[] GRAVITATION = {0.5, 1.0, 2.0};

    public static final int AKTION_TARN = 0, AKTION_KANONE = 1, AKTION_GADGET_1 = 2, AKTION_GADGET_2 = 3, AKTION_GRAVITATION = 4,
            AKTION_SCHRUMPF = 5, AKTION_PHASE = 6;

    private static final EquipmentSlot[] TEILE = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, int[]> OFFEN = new HashMap<>();

    // ------------------------------------------------------------------ Abfragen

    /** Das Multiversal-Teil in diesem Slot, oder EMPTY. */
    public static ItemStack teil(Player p, EquipmentSlot slot) {
        ItemStack s = p.getItemBySlot(slot);
        return s.getItem() instanceof MultiversalArmorItem ? s : ItemStack.EMPTY;
    }

    public static boolean hat(Player p, Modul m) {
        ItemStack s = teil(p, m.slot());
        return !s.isEmpty() && MultiversalArmorItem.has(s, m);
    }

    /** Verfügbare FE für ein Teil: eigener Speicher plus Kernmantel. */
    public static int energie(Player p, EquipmentSlot slot) {
        int e = MultiversalArmorItem.energy(teil(p, slot));
        if (slot != EquipmentSlot.CHEST) e += MultiversalArmorItem.energy(teil(p, EquipmentSlot.CHEST));
        return e;
    }

    /** Eingebaut und mit Strom versorgt. */
    public static boolean aktiv(Player p, Modul m) {
        return hat(p, m) && (p.getAbilities().instabuild || energie(p, m.slot()) > 0);
    }

    public static int zustand(Player p, EquipmentSlot slot) {
        return teil(p, slot).getOrDefault(ModDataComponents.ZUSTAND.get(), 0);
    }

    /** Eigene Gravitation des Gravitationsreglers (0,5 / 1 / 2), oder −1, wenn er aus ist. */
    public static double gravitation(Player p) {
        int stufe = zustand(p, EquipmentSlot.LEGS) & 3;
        return stufe > 0 && aktiv(p, Modul.GRAVITATIONSREGLER) ? GRAVITATION[stufe - 1] : -1;
    }

    /** Set-Bonus „Multiversale Resonanz“: alle vier Teile perfektioniert, Universums-Kern im Kernmantel. */
    public static boolean setBonus(Player p) {
        for (EquipmentSlot slot : TEILE) {
            ItemStack s = teil(p, slot);
            if (s.isEmpty() || ((MultiversalArmorItem) s.getItem()).ausbau() != MultiversalArmorItem.Ausbau.PERFEKTIONIERT) return false;
        }
        return MultiversalArmorItem.has(teil(p, EquipmentSlot.CHEST), Modul.UNIVERSUMS_KERN);
    }

    /** Fluid pro Schuss mit Set-Bonus halbiert. */
    public static int kanonenKosten(Player p, int normal) {
        return setBonus(p) ? Math.max(1, normal / 2) : normal;
    }

    /**
     * Zieht FE ab: erst aus dem Teil, dann aus dem Kernmantel.
     * @param teilweise ob bei zu wenig Ladung der Rest trotzdem verbraucht wird (Dauerbetrieb)
     * @return ob die volle Menge da war
     */
    public static boolean ziehen(Player p, EquipmentSlot slot, int amount, boolean teilweise) {
        if (p.getAbilities().instabuild || amount <= 0) return true;
        ItemStack own = teil(p, slot), chest = slot == EquipmentSlot.CHEST ? ItemStack.EMPTY : teil(p, EquipmentSlot.CHEST);
        int a = MultiversalArmorItem.energy(own), b = MultiversalArmorItem.energy(chest);
        if (a + b < amount && !teilweise) return false;
        int fromOwn = Math.min(a, amount), fromChest = Math.min(b, amount - fromOwn);
        if (!own.isEmpty() && fromOwn > 0) own.set(ModDataComponents.ENERGY.get(), a - fromOwn);
        if (!chest.isEmpty() && fromChest > 0) chest.set(ModDataComponents.ENERGY.get(), b - fromChest);
        return fromOwn + fromChest >= amount;
    }

    // ------------------------------------------------------------------ Tick

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.isSpectator()) return;
        // Haftsohlen laufen auf beiden Seiten (die Bewegung des Spielers rechnet sein Client)
        if (!p.level().isClientSide || p.isControlledByLocalInstance()) {
            if (hat(p, Modul.HAFTSOHLEN) && aufFluessigkeit(p) && aktiv(p, Modul.HAFTSOHLEN)) {
                haftsohlen(p);
                if (!p.level().isClientSide) kosten(p, Modul.HAFTSOHLEN);
            }
        }
        if (!(p instanceof ServerPlayer sp)) return;
        ServerLevel level = sp.serverLevel();
        boolean any = false;
        for (EquipmentSlot slot : TEILE) any |= !teil(p, slot).isEmpty();
        if (!any) {
            if (p.tickCount % 20 == 0) attribute(p, false);
            return;
        }

        if (aktiv(p, Modul.ANALYSE_HUD)) kosten(p, Modul.ANALYSE_HUD);
        if (aktiv(p, Modul.NACHTSICHT_LINSE)) {
            kosten(p, Modul.NACHTSICHT_LINSE);
            if (p.tickCount % 20 == 0) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
        }
        if (aktiv(p, Modul.ATEMFILTER)) {
            UniverseProfile prof = Multiverse.profileAt(level, p.blockPosition());
            if (p.isUnderWater() || (prof != null && prof.atmosphere == UniverseProfile.Atmosphere.TOXIC)) kosten(p, Modul.ATEMFILTER);
        }
        if ((zustand(p, EquipmentSlot.HEAD) & 1) != 0 && aktiv(p, Modul.TARN_DETEKTOR)) {
            kosten(p, Modul.TARN_DETEKTOR);
            if (p.tickCount % 20 == 0) TarnDetektorItem.aufdecken(level, p);
        }
        if (aktiv(p, Modul.SERVO_VERSTAERKER)) kosten(p, Modul.SERVO_VERSTAERKER);
        if (p.isSprinting() && aktiv(p, Modul.SPRINT_BOOSTER)) kosten(p, Modul.SPRINT_BOOSTER);
        if (gravitation(p) > 0) kosten(p, Modul.GRAVITATIONSREGLER);
        if ((zustand(p, EquipmentSlot.LEGS) & 4) != 0 && aktiv(p, Modul.SCHRUMPF_MODUL)) kosten(p, Modul.SCHRUMPF_MODUL);

        attribute(p, true);
        if (p.tickCount % 20 == 0) abbuchen(p);
    }

    private static void kosten(Player p, Modul m) {
        int[] offen = OFFEN.computeIfAbsent(p.getUUID(), u -> new int[4]);
        offen[index(m.slot())] += m.proTick();
    }

    private static int index(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            default -> 3;
        };
    }

    private static void abbuchen(Player p) {
        int[] offen = OFFEN.get(p.getUUID());
        if (offen == null) return;
        for (int i = 0; i < 4; i++) {
            if (offen[i] > 0) ziehen(p, TEILE[i], offen[i], true);
            offen[i] = 0;
        }
    }

    // ------------------------------------------------------------------ Attribute

    private static final ResourceLocation SERVO = Riftverse.id("modul_servo"), SPRINT = Riftverse.id("modul_sprint"),
            SCHRITT = Riftverse.id("modul_sprint_schritt"), SCHRUMPF = Riftverse.id("modul_schrumpf"), SPRUNG = Riftverse.id("modul_sprungfeder"),
            ATEM = Riftverse.id("modul_atemfilter"), RUECKSTOSS = Riftverse.id("modul_servo_rueckstoss");

    private static void attribute(Player p, boolean any) {
        boolean servo = any && aktiv(p, Modul.SERVO_VERSTAERKER);
        boolean sprint = any && p.isSprinting() && aktiv(p, Modul.SPRINT_BOOSTER);
        boolean schrumpf = any && (zustand(p, EquipmentSlot.LEGS) & 4) != 0 && aktiv(p, Modul.SCHRUMPF_MODUL);
        boolean sprung = any && aktiv(p, Modul.SPRUNGFEDER);
        boolean atem = any && aktiv(p, Modul.ATEMFILTER);
        set(p, Attributes.BLOCK_BREAK_SPEED, SERVO, servo ? 0.2 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.KNOCKBACK_RESISTANCE, RUECKSTOSS, servo ? 0.5 : 0, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.MOVEMENT_SPEED, SPRINT, sprint ? 0.25 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.STEP_HEIGHT, SCHRITT, sprint ? 0.5 : 0, AttributeModifier.Operation.ADD_VALUE);
        set(p, Attributes.SCALE, SCHRUMPF, schrumpf ? -0.5 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.JUMP_STRENGTH, SPRUNG, sprung ? 0.5 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(p, Attributes.OXYGEN_BONUS, ATEM, atem ? 3.0 : 0, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void set(Player p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier m = inst.getModifier(id);
        if (Math.abs(amount) < 1.0e-6) {
            if (m != null) inst.removeModifier(id);
            return;
        }
        if (m != null && Math.abs(m.amount() - amount) < 1.0e-6) return;
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }

    // ------------------------------------------------------------------ Haftsohlen

    private static boolean aufFluessigkeit(Player p) {
        if (p.isShiftKeyDown() || p.getAbilities().flying) return false;
        Level l = p.level();
        BlockPos below = BlockPos.containing(p.getX(), p.getY() - 0.1, p.getZ());
        FluidState fs = l.getFluidState(below);
        if (fs.isEmpty() || !l.getFluidState(below.above()).isEmpty()) return false;
        double surface = below.getY() + fs.getHeight(l, below);
        return p.getY() <= surface + 0.15 && p.getY() >= surface - 0.7;
    }

    /** Hält den Spieler auf der Oberfläche einer Flüssigkeit (auch Säure). Schleichen taucht ein. */
    public static void haftsohlen(Player p) {
        Level l = p.level();
        BlockPos below = BlockPos.containing(p.getX(), p.getY() - 0.1, p.getZ());
        FluidState fs = l.getFluidState(below);
        double surface = below.getY() + fs.getHeight(l, below);
        Vec3 v = p.getDeltaMovement();
        if (v.y < 0) p.setDeltaMovement(v.x, 0, v.z);
        if (p.getY() < surface) p.setPos(p.getX(), surface, p.getZ());
        p.setOnGround(true);
        p.resetFallDistance();
    }

    // ------------------------------------------------------------------ Schaden, Tod, Fall, Sprung

    /** Energieschild: 1.000 FE pro abgefangenem Schadenspunkt, bis der Speicher leer ist. */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !hat(p, Modul.ENERGIESCHILD)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float amount = event.getAmount();
        int avail = energie(p, EquipmentSlot.CHEST);
        float absorb = Math.min(amount, avail / (float) SCHILD_FE);
        if (absorb <= 0.01f) return;
        ziehen(p, EquipmentSlot.CHEST, (int) Math.ceil(absorb * SCHILD_FE), true);
        event.setAmount(amount - absorb);
        p.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 8, 0.4, 0.6, 0.4, 0.1);
    }

    /** Not-Portal: Bei einem tödlichen Treffer öffnet sich ein Portal nach Hause (10 min Abklingzeit). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer p) || !hat(p, Modul.NOT_PORTAL)) return;
        long now = p.server.overworld().getGameTime();
        if (now < p.getPersistentData().getLong(NOT_PORTAL_KEY)) return;
        ItemStack chest = teil(p, EquipmentSlot.CHEST);
        KanonenTank tank = chest.getOrDefault(ModDataComponents.KANONEN_TANK.get(), KanonenTank.LEER);
        if (tank.mb() < NOT_PORTAL_MB || Zerfall.zerfallen(tank.zerfall(), Zerfall.now(p.level()))) return;
        if (!ziehen(p, EquipmentSlot.CHEST, NOT_PORTAL_FE, false)) return;
        chest.set(ModDataComponents.KANONEN_TANK.get(), tank.mb() > NOT_PORTAL_MB
                ? new KanonenTank(tank.mb() - NOT_PORTAL_MB, tank.zerfall(), tank.stabil(), tank.konzentrat(), tank.essenz()) : KanonenTank.LEER);
        event.setCanceled(true);
        p.setHealth(Math.max(4.0f, p.getMaxHealth() * 0.25f));
        p.clearFire();
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3));
        p.getPersistentData().putLong(NOT_PORTAL_KEY, now + NOT_PORTAL_CD);
        p.serverLevel().sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 80, 0.5, 1.0, 0.5, 0.5);
        Heimanker home = Heimanker.of(p);
        ServerLevel target = p.server.getLevel(home.dimension());
        if (target != null) RiftTravel.teleport(p, target, home.pos(), p.getYRot());
        p.level().playSound(null, p.blockPosition(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1f, 1.2f);
        p.displayClientMessage(Component.translatable("message.riftverse.not_portal").withStyle(ChatFormatting.GOLD), false);
    }

    /** Fall-Dämpfer: kein Fallschaden, 500 FE pro Landung. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide || !hat(p, Modul.FALL_DAEMPFER)) return;
        double safe = p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        if (event.getDistance() <= safe) return;
        if (!ziehen(p, EquipmentSlot.FEET, FALL_FE, false)) return;
        event.setDamageMultiplier(0f);
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 10, 0.4, 0.05, 0.4, 0.02);
    }

    /** Sprungfeder: +50 % Sprungkraft (Attribut), 5 FE pro Sprung. */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !hat(p, Modul.SPRUNGFEDER)) return;
        ziehen(p, EquipmentSlot.FEET, SPRUNG_FE, true);
    }

    /** Schleichen + Rechtsklick mit voller Kartusche füllt den Brusttank des Kernmantels (4.000 mB). */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        Player p = event.getEntity();
        ItemStack cart = event.getItemStack();
        int stufe = PortalKanoneItem.stufe(cart);
        if (stufe == 0 || !p.isShiftKeyDown()) return;
        ItemStack chest = teil(p, EquipmentSlot.CHEST);
        if (chest.isEmpty()) return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (p.level().isClientSide) return;
        if (brusttankFuellen(p, chest, cart, stufe)) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.8f, 1.4f);
        }
    }

    public static boolean brusttankFuellen(Player p, ItemStack chest, ItemStack cart, int stufe) {
        KanonenTank tank = chest.getOrDefault(ModDataComponents.KANONEN_TANK.get(), KanonenTank.LEER);
        long now = Zerfall.now(p.level());
        if (tank.mb() > 0 && Zerfall.zerfallen(tank.zerfall(), now)) tank = KanonenTank.LEER;
        int rest = tank.mb() > 0 && tank.stufe() == stufe ? tank.mb() : 0;
        if (rest + FluidKartuscheItem.INHALT > BRUSTTANK) {
            p.displayClientMessage(Component.translatable("message.riftverse.brusttank.voll").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        long z = stufe >= 2 ? 0 : FluidKartuscheItem.zerfall(cart);
        chest.set(ModDataComponents.KANONEN_TANK.get(), KanonenTank.ofStufe(rest + FluidKartuscheItem.INHALT,
                rest > 0 ? Zerfall.frueher(tank.zerfall(), z) : z, stufe));
        cart.shrink(1);
        ItemStack empty = new ItemStack(ModItems.FLUID_KARTUSCHE.get());
        if (!p.getInventory().add(empty)) p.drop(empty, false);
        p.displayClientMessage(Component.translatable("message.riftverse.brusttank", rest + FluidKartuscheItem.INHALT, BRUSTTANK)
                .withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /**
     * Die Kanone tankt aus dem Brusttank nach, bevor sie Kartuschen anfasst.
     * @return der neue Kanonentank, oder null, wenn der Brusttank nichts Passendes hat
     */
    @Nullable
    public static KanonenTank ausBrusttank(Player p, PortalKanoneItem gun, KanonenTank gunTank, int need) {
        ItemStack chest = teil(p, EquipmentSlot.CHEST);
        if (chest.isEmpty()) return null;
        KanonenTank tank = chest.getOrDefault(ModDataComponents.KANONEN_TANK.get(), KanonenTank.LEER);
        long now = Zerfall.now(p.level());
        if (tank.mb() <= 0 || Zerfall.zerfallen(tank.zerfall(), now) || !gun.accepts(tank.stufe())) return null;
        int rest = gunTank.mb() > 0 && gunTank.stufe() == tank.stufe() ? gunTank.mb() : 0;
        int move = Math.min(tank.mb(), PortalKanoneItem.TANK - rest);
        if (rest + move < need) return null;
        chest.set(ModDataComponents.KANONEN_TANK.get(), tank.mb() > move
                ? new KanonenTank(tank.mb() - move, tank.zerfall(), tank.stabil(), tank.konzentrat(), tank.essenz()) : KanonenTank.LEER);
        return KanonenTank.ofStufe(rest + move, rest > 0 ? Zerfall.frueher(gunTank.zerfall(), tank.zerfall()) : tank.zerfall(), tank.stufe());
    }

    // ------------------------------------------------------------------ Tasten

    /** Tastendruck aus dem {@link dev.riftverse.network.RuestungPayload}. */
    public static void aktion(ServerPlayer p, int aktion) {
        switch (aktion) {
            case AKTION_TARN -> umschalten(p, EquipmentSlot.HEAD, Modul.TARN_DETEKTOR, 1, "tarn");
            case AKTION_SCHRUMPF -> umschalten(p, EquipmentSlot.LEGS, Modul.SCHRUMPF_MODUL, 4, "schrumpf");
            case AKTION_GRAVITATION -> {
                if (!hat(p, Modul.GRAVITATIONSREGLER)) {
                    fehlt(p, Modul.GRAVITATIONSREGLER);
                    return;
                }
                ItemStack legs = teil(p, EquipmentSlot.LEGS);
                int z = legs.getOrDefault(ModDataComponents.ZUSTAND.get(), 0);
                int stufe = ((z & 3) + 1) % 4;
                legs.set(ModDataComponents.ZUSTAND.get(), (z & ~3) | stufe);
                p.displayClientMessage(Component.translatable("message.riftverse.gravitationsregler." + stufe).withStyle(ChatFormatting.AQUA), true);
                dev.riftverse.universe.UniverseEffects.applyGravity(p, Multiverse.profileAt(p.serverLevel(), p.blockPosition()));
            }
            case AKTION_KANONE -> {
                if (!hat(p, Modul.KANONEN_INTEGRATION)) {
                    fehlt(p, Modul.KANONEN_INTEGRATION);
                    return;
                }
                ItemStack gun = PortalKanoneItem.find(p);
                if (gun == null) {
                    p.displayClientMessage(Component.translatable("message.riftverse.keine_kanone").withStyle(ChatFormatting.RED), true);
                    return;
                }
                PortalKanoneItem item = (PortalKanoneItem) gun.getItem();
                if (p.isShiftKeyDown()) item.returnHome(p, gun);
                else item.jump(p, gun);
            }
            case AKTION_GADGET_1, AKTION_GADGET_2 -> {
                if (!hat(p, Modul.GADGET_HALTERUNG)) {
                    fehlt(p, Modul.GADGET_HALTERUNG);
                    return;
                }
                gadgetAusloesen(p, aktion == AKTION_GADGET_1 ? 0 : 1);
            }
            case AKTION_PHASE -> phasenschritt(p);
            default -> { }
        }
    }

    private static void fehlt(Player p, Modul m) {
        p.displayClientMessage(Component.translatable("message.riftverse.modul_fehlt", Component.translatable("item.riftverse." + m.itemName()))
                .withStyle(ChatFormatting.GRAY), true);
    }

    private static void umschalten(Player p, EquipmentSlot slot, Modul m, int bit, String name) {
        if (!hat(p, m)) {
            fehlt(p, m);
            return;
        }
        ItemStack s = teil(p, slot);
        int z = s.getOrDefault(ModDataComponents.ZUSTAND.get(), 0) ^ bit;
        s.set(ModDataComponents.ZUSTAND.get(), z);
        p.displayClientMessage(Component.translatable("message.riftverse.modul." + name + ((z & bit) != 0 ? ".an" : ".aus"))
                .withStyle(ChatFormatting.AQUA), true);
    }

    /** Gadget-Halterung: löst das n-te Gadget im Inventar aus, als läge es in der Haupthand. */
    public static boolean gadgetAusloesen(ServerPlayer p, int n) {
        Inventory inv = p.getInventory();
        int slot = -1, found = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i).getItem() instanceof Gadget && found++ == n) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            p.displayClientMessage(Component.translatable("message.riftverse.gadget_fehlt", n + 1).withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        int sel = inv.selected;
        ItemStack gadget = inv.items.get(slot);
        if (slot == sel) {
            InteractionResultHolder<ItemStack> r = gadget.use(p.level(), p, InteractionHand.MAIN_HAND);
            inv.items.set(sel, r.getObject());
            return r.getResult().consumesAction();
        }
        ItemStack main = inv.items.get(sel);
        inv.items.set(sel, gadget);
        InteractionResultHolder<ItemStack> r = gadget.use(p.level(), p, InteractionHand.MAIN_HAND);
        ItemStack after = r.getObject();
        inv.items.set(sel, main);
        inv.items.set(slot, after);
        return r.getResult().consumesAction();
    }

    /** Phasenschritt: Kurzteleport 6 Blöcke in Blickrichtung, 3 s Abklingzeit (mit Set-Bonus in fremden Universen keine). */
    public static boolean phasenschritt(ServerPlayer p) {
        if (!aktiv(p, Modul.PHASENSCHRITT)) return false;
        long now = p.server.overworld().getGameTime();
        boolean frei = setBonus(p) && Multiverse.isMultiverse(p.serverLevel());
        if (!frei && now < p.getPersistentData().getLong(PHASE_KEY)) return false;
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1);
        if (dir.lengthSqr() < 1.0e-4) return false;
        dir = dir.normalize();
        ServerLevel level = p.serverLevel();
        Vec3 from = p.position();
        for (int d = PHASE_WEITE; d >= 2; d--) {
            for (int up = 0; up <= 1; up++) {
                Vec3 to = from.add(dir.scale(d)).add(0, up, 0);
                if (!level.noCollision(p, p.getBoundingBox().move(to.subtract(from)))) continue;
                if (!ziehen(p, EquipmentSlot.FEET, PHASE_FE, false)) return false;
                level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 30, 0.3, 0.6, 0.3, 0.3);
                p.teleportTo(to.x, to.y, to.z);
                if (p.position().distanceToSqr(to) > 0.01) p.setPos(to.x, to.y, to.z);   // FakePlayer ohne Verbindung
                p.resetFallDistance();
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 30, 0.3, 0.6, 0.3, 0.05);
                level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.6f);
                if (!frei) p.getPersistentData().putLong(PHASE_KEY, now + PHASE_CD);
                return true;
            }
        }
        return false;
    }

    private Ruestung() {}
}
