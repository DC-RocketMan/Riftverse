package dev.riftverse.ruestung;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ein Teil der Multiversal-Rüstung in einer Ausbaustufe. Keine Haltbarkeit: Die Rüstung lebt von FE, jedes Teil hat
 * einen eigenen Speicher, der Kernmantel den großen Hauptspeicher, aus dem alle Teile nachziehen.
 */
public class MultiversalArmorItem extends ArmorItem {
    /** Rohling, Kalibriert, Perfektioniert. */
    public enum Ausbau { ROHLING, KALIBRIERT, PERFEKTIONIERT }

    private final Ausbau ausbau;

    public MultiversalArmorItem(Holder<ArmorMaterial> material, Type type, Ausbau ausbau) {
        super(material, type, new Properties().stacksTo(1).fireResistant()
                .rarity(ausbau == Ausbau.PERFEKTIONIERT ? Rarity.EPIC : ausbau == Ausbau.KALIBRIERT ? Rarity.RARE : Rarity.UNCOMMON)
                .attributes(modifiers(type, ausbau)));
        this.ausbau = ausbau;
    }

    public Ausbau ausbau() {
        return ausbau;
    }

    /** Rüstung / Härte je Teil und Stufe (Rohling → Kalibriert → Perfektioniert). */
    public static int[] werte(Type type, Ausbau ausbau) {
        int i = ausbau.ordinal();
        return switch (type) {
            case HELMET -> new int[][]{{3, 2}, {4, 3}, {5, 4}}[i];
            case CHESTPLATE -> new int[][]{{8, 3}, {9, 4}, {10, 5}}[i];
            case LEGGINGS -> new int[][]{{6, 2}, {7, 3}, {8, 4}}[i];
            default -> new int[][]{{3, 2}, {4, 3}, {4, 4}}[i];
        };
    }

    private static ItemAttributeModifiers modifiers(Type type, Ausbau ausbau) {
        int[] w = werte(type, ausbau);
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.getSlot());
        ResourceLocation id = Riftverse.id("multiversal_" + type.getName());
        ItemAttributeModifiers.Builder b = ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(id, w[0], AttributeModifier.Operation.ADD_VALUE), group)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, w[1], AttributeModifier.Operation.ADD_VALUE), group);
        // Grundeigenschaften: Exo-Rahmen +10 % Abbautempo, Gravi-Stiefel kein Fallschaden bis 10 Blöcke
        if (type == Type.LEGGINGS) {
            b.add(Attributes.BLOCK_BREAK_SPEED, new AttributeModifier(id, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), group);
        }
        if (type == Type.BOOTS) {
            b.add(Attributes.SAFE_FALL_DISTANCE, new AttributeModifier(id, 7.0, AttributeModifier.Operation.ADD_VALUE), group);
        }
        return b.build();
    }

    /** Modul-Plätze: Helm 3, Brust 4, Hose 3, Stiefel 2. */
    public int slots() {
        return switch (getType()) {
            case HELMET, LEGGINGS -> 3;
            case CHESTPLATE -> 4;
            default -> 2;
        };
    }

    /** FE-Speicher: Helm und Hose 500.000, Brust 2 Mio. (Hauptspeicher), Stiefel 250.000. */
    public int capacity() {
        return switch (getType()) {
            case HELMET, LEGGINGS -> 500_000;
            case CHESTPLATE -> 2_000_000;
            default -> 250_000;
        };
    }

    public static int energy(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    public static List<Modul> module(ItemStack stack) {
        List<Modul> out = new ArrayList<>();
        for (String k : stack.getOrDefault(ModDataComponents.MODULE.get(), List.<String>of())) {
            Modul m = Modul.byKey(k);
            if (m != null) out.add(m);
        }
        return out;
    }

    public static boolean has(ItemStack stack, Modul modul) {
        return stack.getOrDefault(ModDataComponents.MODULE.get(), List.<String>of()).contains(modul.key());
    }

    /** Lässt sich {@code modul} in dieses Teil einbauen? (richtiges Teil, Platz frei, noch nicht drin) */
    public boolean canInstall(ItemStack stack, Modul modul) {
        if (modul.slot() != getType().getSlot() || has(stack, modul)) return false;
        if (modul == Modul.UNIVERSUMS_KERN && ausbau != Ausbau.PERFEKTIONIERT) return false;
        return module(stack).size() < slots();
    }

    public static void setModule(ItemStack stack, List<Modul> module) {
        List<String> keys = new ArrayList<>();
        for (Modul m : module) keys.add(m.key());
        if (keys.isEmpty()) stack.remove(ModDataComponents.MODULE.get());
        else stack.set(ModDataComponents.MODULE.get(), List.copyOf(keys));
    }

    // ------------------------------------------------------------------ Anzeige

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * energy(stack) / capacity());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x39FF6A;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.riftverse.energy", String.format(Locale.GERMANY, "%,d", energy(stack)),
                String.format(Locale.GERMANY, "%,d", capacity())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.riftverse.ruestung." + getType().getName()).withStyle(ChatFormatting.DARK_GREEN));
        List<Modul> module = module(stack);
        tooltip.add(Component.translatable("tooltip.riftverse.module", module.size(), slots()).withStyle(ChatFormatting.GRAY));
        for (Modul m : module) {
            tooltip.add(Component.literal(" • ").append(Component.translatable("item.riftverse." + m.itemName()))
                    .withStyle(ChatFormatting.AQUA));
        }
        if (getType() == Type.CHESTPLATE) {
            var tank = stack.get(ModDataComponents.KANONEN_TANK.get());
            tooltip.add(Component.translatable("tooltip.riftverse.brusttank", tank == null ? 0 : tank.mb(), Ruestung.BRUSTTANK)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
