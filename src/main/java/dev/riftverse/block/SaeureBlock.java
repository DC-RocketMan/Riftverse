package dev.riftverse.block;

import dev.riftverse.registry.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Säure aus den Seen der toxischen Welt: verätzt Lebewesen und frisst Rüstung an. */
public class SaeureBlock extends LiquidBlock {
    public SaeureBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living) || entity.tickCount % 10 != 0) return;
        living.hurt(ModDamageTypes.source(level, ModDamageTypes.SAEURE), 2.0f);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS}) {
            ItemStack armor = living.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamageableItem()) armor.hurtAndBreak(1, living, slot);
        }
    }
}
