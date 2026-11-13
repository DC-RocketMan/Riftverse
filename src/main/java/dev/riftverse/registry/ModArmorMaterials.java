package dev.riftverse.registry;

import dev.riftverse.Riftverse;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/**
 * Materialien der Multiversal-Rüstung, eins je Ausbaustufe (eigene Rüstungstexturen). Rüstung und Härte setzt
 * {@link dev.riftverse.ruestung.MultiversalArmorItem} je Teil selbst; hier stehen nur Werte für Anzeige und Verzauberbarkeit.
 */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Riftverse.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ROHLING = material("multiversal_rohling", 3, 8, 6, 3, 10);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> KALIBRIERT = material("multiversal_kalibriert", 4, 9, 7, 4, 15);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PERFEKTIONIERT = material("multiversal_perfektioniert", 5, 10, 8, 4, 20);

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> material(String name, int helm, int brust, int hose, int stiefel, int ench) {
        return MATERIALS.register(name, () -> {
            EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
            defense.put(ArmorItem.Type.HELMET, helm);
            defense.put(ArmorItem.Type.CHESTPLATE, brust);
            defense.put(ArmorItem.Type.LEGGINGS, hose);
            defense.put(ArmorItem.Type.BOOTS, stiefel);
            defense.put(ArmorItem.Type.BODY, brust);
            return new ArmorMaterial(defense, ench, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(Riftverse.id(name))), 0.0f, 0.0f);
        });
    }

    private ModArmorMaterials() {}
}
