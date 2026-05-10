package top.xfunny.mod.item;

import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.DirectionHelper;
import org.mtr.mapping.mapper.ItemExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.block.BlockLiftTrackBase;
import org.mtr.mod.block.BlockLiftTrackFloor;

import javax.annotation.Nonnull;


public class FloorAutoSetter extends ItemExtension implements DirectionHelper {
    public FloorAutoSetter(ItemSettings itemSettings) {
        super(itemSettings.maxCount(1));
    }

    @Nonnull
    public ActionResult useOnBlock2(ItemUsageContext context) {
        if (!context.getWorld().isClient()) {
            if (this.clickCondition(context)) {
                CompoundTag compoundTag = context.getStack().getOrCreateTag();
                compoundTag.putLong("pos", context.getBlockPos().asLong());
                this.onClick(context, compoundTag);
                return ActionResult.SUCCESS;
            } else {
                return ActionResult.FAIL;
            }
        } else {
            return super.useOnBlock2(context);
        }
    }

    protected void onClick(ItemUsageContext context, CompoundTag compoundTag) {
        PathFinder pathFinder = new PathFinder();
        final PlayerEntity playerEntity = context.getPlayer();

        final World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        int number = 0;
        int floorCount = 0;
        int floorNumber = 0;
        boolean ding = false;
        String floorNumber2 = "";
        final BlockEntity floorEntity = world.getBlockEntity(pos);

        //todo：后续改为gui操作，提供更多楼层填充策略
        //确定初始楼层
        if (floorEntity != null && floorEntity.data instanceof BlockLiftTrackFloor.BlockEntity) {
            String checkFloorNumber = ((BlockLiftTrackFloor.BlockEntity) floorEntity.data).getFloorNumber();
            if (checkFloorNumber != null) {
                floorNumber2 = ((BlockLiftTrackFloor.BlockEntity) floorEntity.data).getFloorNumber();
            } else {
                floorNumber2 = "1";
            }
            ding = ((BlockLiftTrackFloor.BlockEntity) floorEntity.data).getShouldDing();
        }
        //判断初始楼层是否为整数
        if (floorNumber2.matches("\\d+")) {
            floorNumber = Integer.parseInt(floorNumber2);
        }


        while (floorNumber2.matches("\\d+")) {
            if (world.getBlockState(pos).getBlock().data instanceof BlockLiftTrackBase) {
                final BlockEntity floorEntity1 = world.getBlockEntity(pos);
                if (floorEntity1 != null && floorEntity1.data instanceof BlockLiftTrackFloor.BlockEntity) {
                    ((BlockLiftTrackFloor.BlockEntity) floorEntity1.data).setData(String.valueOf(floorNumber), "", ding);
                }

                Object[] apos = pathFinder.findPath(context, pos);
                pos = (BlockPos) apos[0];

                if (world.getBlockState(pos).getBlock().data instanceof BlockLiftTrackFloor) {
                    floorCount++;
                }

            } else {
                if (playerEntity != null) {
                    playerEntity.sendMessage(Text.cast(TextHelper.translatable("message.floor_auto_setter_status_failed")), true);
                }
                break;
            }

            if (number == pathFinder.getMark().size()) {
                if (playerEntity != null) {
                    playerEntity.sendMessage(Text.cast(TextHelper.translatable("message.floor_auto_setter_status_finished", floorCount + 1)), true);
                }
                break;
            }
            number++;//循环计数器

            if (world.getBlockState(pos.up(1)).getBlock().data instanceof BlockLiftTrackFloor) {
                floorNumber++;
            } else if (world.getBlockState(pos.west(1)).getBlock().data instanceof BlockLiftTrackFloor) {
                floorNumber++;
            } else if (world.getBlockState(pos.east(1)).getBlock().data instanceof BlockLiftTrackFloor) {
                floorNumber++;
            } else if (world.getBlockState(pos.north(1)).getBlock().data instanceof BlockLiftTrackFloor) {
                floorNumber++;
            } else if (world.getBlockState(pos.south(1)).getBlock().data instanceof BlockLiftTrackFloor) {
                floorNumber++;
            }
        }
    }

    protected boolean clickCondition(ItemUsageContext context) {
        final Block block = context.getWorld().getBlockState(context.getBlockPos()).getBlock();
        return block.data instanceof BlockLiftTrackFloor;
    }
}
