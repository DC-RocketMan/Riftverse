package dev.riftverse.gadget;

import dev.riftverse.Riftverse;
import dev.riftverse.registry.ModDataComponents;
import dev.riftverse.registry.ModItems;
import dev.riftverse.tech.TechBlock;
import dev.riftverse.tech.upgrade.Aufruestbar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Multiversal-Multitool in drei Stufen (T2 100.000 FE, T3 500.000 FE, T4 2 Mio. FE). Schleichen + Rechtsklick in die
 * Luft klappt den nächsten Kopf aus:
 * <ul>
 *   <li><b>Bohrer:</b> Spitzhacke, Schaufel und Axt in einem (Eisen-, Diamant-, Netherit-Stufe), 50 FE pro Block.</li>
 *   <li><b>Schlüssel:</b> Rechtsklick dreht eine Riftverse-Maschine; Schleichen + Rechtsklick baut erst die
 *       Upgrade-Chips aus, dann die Maschine selbst ab (mit Inhalt).</li>
 *   <li><b>Scanner:</b> Rechtsklick zeigt wie der Universums-Scanner alles über das aktuelle Universum.</li>
 * </ul>
 */
public class MultitoolItem extends EnergieGadget {
    public static final int BOHRER = 0, SCHLUESSEL = 1, SCANNER = 2;
    public static final int PRO_BLOCK = 50, DREHEN = 10, ABBAUEN = 200;
    private final int stufe;

    public MultitoolItem(Properties properties, int stufe) {
        super(properties.component(DataComponents.TOOL, tool(stufe)), new int[]{100_000, 500_000, 2_000_000}[stufe - 1],
                new int[]{500, 2_000, 8_000}[stufe - 1]);
        this.stufe = stufe;
    }

    public int stufe() {
        return stufe;
    }

    private static Tool tool(int stufe) {
        TagKey<Block> falsch = switch (stufe) {
            case 1 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 2 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
        float speed = new float[]{8f, 12f, 16f}[stufe - 1];
        return new Tool(List.of(Tool.Rule.deniesDrops(falsch), Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, speed),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, speed), Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, speed),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_HOE, speed)), 1.0f, 0);
    }

    public static int modus(ItemStack stack) {
        return Math.floorMod(stack.getOrDefault(ModDataComponents.MODUS.get(), BOHRER), 3);
    }

    private boolean bohrt(ItemStack stack) {
        return modus(stack) == BOHRER && energy(stack) >= PRO_BLOCK;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return bohrt(stack) ? super.getDestroySpeed(stack, state) : 1.0f;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return bohrt(stack) && super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0 && modus(stack) == BOHRER) {
            if (!(miner instanceof Player p && p.getAbilities().instabuild)) setEnergy(stack, energy(stack) - PRO_BLOCK);
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int next = (modus(stack) + 1) % 3;
                stack.set(ModDataComponents.MODUS.get(), next);
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.PLAYERS, 0.8f, 1.4f);
                player.displayClientMessage(Component.translatable("message.riftverse.multitool.modus",
                        Component.translatable("gui.riftverse.multitool." + next)).withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (modus(stack) == SCANNER) {
            if (!level.isClientSide && !ziehen(player, stack, 100)) return InteractionResultHolder.fail(stack);
            ModItems.UNIVERSE_SCANNER.get().use(level, player, hand);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        if (player == null || modus(stack) != SCHLUESSEL) return InteractionResult.PASS;
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!Riftverse.MODID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (player.isShiftKeyDown()) {
            if (be instanceof Aufruestbar a && !a.chips().isEmpty()) {
                for (ItemStack chip : a.ausbauen()) {
                    if (!player.getInventory().add(chip)) player.drop(chip, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1f, 1.2f);
                return InteractionResult.SUCCESS;
            }
            if (!ziehen(player, stack, ABBAUEN)) return InteractionResult.FAIL;
            level.destroyBlock(pos, !player.getAbilities().instabuild, player);
            return InteractionResult.SUCCESS;
        }
        if (state.hasProperty(TechBlock.FACING)) {
            if (!ziehen(player, stack, DREHEN)) return InteractionResult.FAIL;
            Direction d = state.getValue(TechBlock.FACING);
            level.setBlock(pos, state.setValue(TechBlock.FACING, d.getClockWise()), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7f, 1.6f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("message.riftverse.multitool.modus", Component.translatable("gui.riftverse.multitool." + modus(stack)))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.riftverse.multitool.stufe", stufe).withStyle(ChatFormatting.DARK_GRAY));
    }
}
