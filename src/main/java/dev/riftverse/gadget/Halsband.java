package dev.riftverse.gadget;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModItems;
import dev.riftverse.universe.UniverseType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Was ein Tier mit Übersetzer-Halsband tut. Die Ladung steckt in den persistenten Daten des Tiers; 2 FE pro Sekunde
 * (10.000 FE reichen gut 80 Minuten). Ist sie leer, verstummt es.
 */
@EventBusSubscriber(modid = Riftverse.MODID)
public final class Halsband {
    public static final String KEY = "riftverse:halsband";
    public static final int PRO_SEKUNDE = 2, SAMMEL_RADIUS = 6, WARN_RADIUS = 12;

    private record Blase(Display.TextDisplay display, long weg) {}

    private static final List<Blase> BLASEN = new ArrayList<>();

    public static boolean hat(Entity e) {
        return e.getPersistentData().contains(KEY);
    }

    public static int ladung(Entity e) {
        return e.getPersistentData().getCompound(KEY).getInt("Energie");
    }

    public static void anlegen(TamableAnimal pet, int energie) {
        CompoundTag t = new CompoundTag();
        t.putInt("Energie", energie);
        pet.getPersistentData().put(KEY, t);
        pet.setPersistenceRequired();
    }

    /** Sprechblase über dem Tier (3 Sekunden), dazu eine Zeile im Chat des Besitzers. */
    public static void sagen(TamableAnimal pet, Component text) {
        if (!(pet.level() instanceof ServerLevel sl)) return;
        Component line = Component.literal("[").append(pet.getDisplayName()).append("] ").withStyle(ChatFormatting.GOLD)
                .append(text.copy().withStyle(ChatFormatting.WHITE));
        if (pet.getOwner() instanceof Player owner && owner.distanceToSqr(pet) < 64 * 64) owner.displayClientMessage(line, false);
        sl.sendParticles(ParticleTypes.NOTE, pet.getX(), pet.getY() + pet.getBbHeight() + 0.4, pet.getZ(), 1, 0, 0, 0, 0);
        try {
            Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(sl);
            if (d == null) return;
            CompoundTag tag = new CompoundTag();
            tag.putString("text", Component.Serializer.toJson(text, sl.registryAccess()));
            tag.putString("billboard", "center");
            d.load(tag);
            d.setPos(pet.getX(), pet.getY() + pet.getBbHeight() + 0.6, pet.getZ());
            sl.addFreshEntity(d);
            BLASEN.add(new Blase(d, sl.getGameTime() + 60));
        } catch (RuntimeException e) {
            Riftverse.LOGGER.debug("Sprechblase nicht möglich", e);
        }
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof TamableAnimal pet) || !(pet.level() instanceof ServerLevel sl)) return;
        if (pet.tickCount % 20 != 0 || !hat(pet)) return;
        CompoundTag t = pet.getPersistentData().getCompound(KEY);
        int e = t.getInt("Energie");
        if (e <= 0) return;
        e = Math.max(0, e - PRO_SEKUNDE);
        t.putInt("Energie", e);
        if (e == 0) {
            sagen(pet, Component.translatable("halsband.riftverse.leer"));
            return;
        }
        if (!(pet.getOwner() instanceof Player owner) || owner.level() != pet.level() || owner.distanceToSqr(pet) > 48 * 48) return;

        // Einsammeln: was in der Nähe des Tiers liegt, landet im Inventar des Besitzers
        int gesammelt = 0;
        for (ItemEntity item : sl.getEntitiesOfClass(ItemEntity.class, pet.getBoundingBox().inflate(SAMMEL_RADIUS),
                i -> i.isAlive() && !i.hasPickUpDelay())) {
            ItemStack s = item.getItem().copy();
            if (owner.getInventory().add(s)) {
                item.discard();
                gesammelt++;
            } else {
                item.setItem(s);
            }
        }
        if (gesammelt > 0 && sl.random.nextInt(4) == 0) sagen(pet, Component.translatable("halsband.riftverse.gesammelt", gesammelt));

        // Warnen: ein Feind nahe beim Besitzer, höchstens alle 10 Sekunden
        long now = sl.getGameTime();
        if (now - t.getLong("Gewarnt") > 200) {
            for (LivingEntity mob : sl.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(WARN_RADIUS),
                    m -> m instanceof Enemy && m.isAlive())) {
                t.putLong("Gewarnt", now);
                sagen(pet, Component.translatable("halsband.riftverse.warnung", mob.getDisplayName()));
                return;
            }
        }
        // Kommentare: etwa jede Minute einer, passend zur Lage
        if (now - t.getLong("Gesagt") > 1200 && sl.random.nextInt(20) == 0) {
            t.putLong("Gesagt", now);
            UniverseType type = UniverseType.of(sl.dimension());
            Component c;
            if (owner.getHealth() < owner.getMaxHealth() * 0.3f) c = Component.translatable("halsband.riftverse.verletzt");
            else if (type != null && sl.random.nextBoolean()) c = Component.translatable("halsband.riftverse.welt." + type.id());
            else c = Component.translatable("halsband.riftverse.kommentar." + sl.random.nextInt(8));
            sagen(pet, c);
        }
    }

    /** Schleichen + Rechtsklick mit leerer Hand nimmt das Halsband ab. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!(event.getTarget() instanceof TamableAnimal pet) || !hat(pet) || !player.isShiftKeyDown()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty() || !pet.isOwnedBy(player)) return;
        if (!player.level().isClientSide) {
            ItemStack collar = new ItemStack(ModItems.UEBERSETZER_HALSBAND.get());
            EnergieGadget.setEnergy(collar, ladung(pet));
            pet.getPersistentData().remove(KEY);
            if (!player.getInventory().add(collar)) player.drop(collar, false);
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || BLASEN.isEmpty()) return;
        Iterator<Blase> it = BLASEN.iterator();
        while (it.hasNext()) {
            Blase b = it.next();
            if (b.display().level() != sl) continue;
            if (sl.getGameTime() >= b.weg() || !b.display().isAlive()) {
                b.display().discard();
                it.remove();
            }
        }
    }

    private Halsband() {}
}
