package top.xfunny.mod.item;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mod.generated.lang.TranslationProvider;
import top.xfunny.mod.block.base.LiftButtonsBase;
import top.xfunny.mod.block.base.LiftDestinationDispatchTerminalBase;
import top.xfunny.mod.block.base.LiftPanelBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class YTEItemBlockClickingBase extends ItemExtension {
    public static final String TAG_POS = "pos";
    public static final String TAG_SECOND_POS = "secondPos";

    public YTEItemBlockClickingBase(ItemSettings itemSettings) {
        super(itemSettings);
    }

    @Nonnull
    @Override
    public ActionResult useOnBlock2(ItemUsageContext context) {
        if (!context.getWorld().isClient()) {//pos start在context中
            if (clickCondition(context)) {
                final CompoundTag compoundTag = context.getStack().getOrCreateTag();


                if (compoundTag.contains(TAG_SECOND_POS)) {//如果已点击两个点,第三次点击
                    final BlockPos posEnd = BlockPos.fromLong(compoundTag.getLong(TAG_POS));
                    final BlockPos posStart = BlockPos.fromLong(compoundTag.getLong(TAG_SECOND_POS));
                    final BlockPos posThird = context.getBlockPos();
                    onThirdClick(context, posStart, posEnd, posThird, compoundTag);
                    compoundTag.remove(TAG_SECOND_POS);
                    compoundTag.remove(TAG_POS);

                } else if (compoundTag.contains(TAG_POS)) {//如果已点击一个点，第二次点击
                    final BlockPos posEnd = BlockPos.fromLong(compoundTag.getLong(TAG_POS));
                    final BlockPos posStart = context.getBlockPos();
                    World world = context.getWorld();

                    final Block blockEnd = world.getBlockState(posEnd).getBlock();
                    final Block blockStart = world.getBlockState(posStart).getBlock();
                    final boolean isBlockEndValid = blockEnd.data instanceof LiftButtonsBase || blockEnd.data instanceof LiftDestinationDispatchTerminalBase || blockEnd.data instanceof LiftPanelBase;
                    final boolean isBlockStartValid = blockStart.data instanceof LiftButtonsBase || blockStart.data instanceof LiftDestinationDispatchTerminalBase || blockStart.data instanceof LiftPanelBase;
                    if (isBlockEndValid) {
                        if (isBlockStartValid) {//第一个点和第二个点都是外呼或显示屏或到站灯
                            onEndClick(context, posEnd, compoundTag);
                            compoundTag.putLong(TAG_SECOND_POS, posStart.asLong());//获取第二个点
                        } else {
                            onEndClick(context, posEnd, compoundTag);
                            compoundTag.remove(TAG_POS);
                        }
                    } else {
                        onEndClick(context, posEnd, compoundTag);
                        compoundTag.remove(TAG_POS);
                    }
                } else {//如果还没点击第一个点
                    compoundTag.putLong(TAG_POS, context.getBlockPos().asLong());
                    onStartClick(context, compoundTag);
                }


                return ActionResult.SUCCESS;
            } else {
                return ActionResult.FAIL;
            }
        } else {
            return super.useOnBlock2(context);
        }
    }

    @Override
    public void addTooltips(ItemStack stack, @Nullable World world, List<MutableText> tooltip, TooltipContext options) {
        final CompoundTag compoundTag = stack.getOrCreateTag();
        final long posLong = compoundTag.getLong(TAG_POS);
        if (posLong != 0) {
            tooltip.add(TranslationProvider.TOOLTIP_MTR_SELECTED_BLOCK.getMutableText(BlockPos.fromLong(posLong).toShortString()).formatted(TextFormatting.GOLD));
        }
    }

    protected abstract void onStartClick(ItemUsageContext context, CompoundTag compoundTag);

    protected abstract void onEndClick(ItemUsageContext context, BlockPos posEnd, CompoundTag compoundTag);

    protected abstract void onThirdClick(ItemUsageContext context, BlockPos pos1, BlockPos pos2, BlockPos pos3, CompoundTag compoundTag);

    protected abstract boolean clickCondition(ItemUsageContext context);
}
