package top.xfunny.mixin;

import org.mtr.core.data.Lift;
import org.mtr.core.data.LiftDirection;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.World;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.IGui;
import org.mtr.mod.screen.DashboardList;
import org.mtr.mod.screen.DashboardListItem;
import org.mtr.mod.screen.LiftSelectionScreen;
import org.mtr.mod.screen.MTRScreenBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.xfunny.mod.LiftDoorControlState;
import top.xfunny.mod.LiftDisplayDirectionState;
import top.xfunny.mod.client.InitClient;
import top.xfunny.mod.config.YteLiftConfigStore;
import top.xfunny.mod.packet.PacketLiftDoorControl;
import top.xfunny.mod.util.GetLiftDetails;

@Mixin(value = LiftSelectionScreen.class, remap = false)
public abstract class MixinLiftSelectionScreen extends MTRScreenBase {

    @Shadow @Final private DashboardList selectionList;
    @Shadow @Final private ObjectArrayList<BlockPos> floorLevels;
    @Shadow @Final private long liftId;

    @Unique private ButtonWidgetExtension yte$holdOpenButton;
    @Unique private ButtonWidgetExtension yte$openDoorButton;
    @Unique private ButtonWidgetExtension yte$closeDoorButton;
    @Unique private boolean yte$clearDoorButtonFocus;
    @Unique private boolean yte$lastHoldEnabled;

    @Redirect(
            method = "lambda$new$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/mtr/mod/render/RenderLifts;getLiftDetails(Lorg/mtr/mapping/holder/World;Lorg/mtr/core/data/Lift;Lorg/mtr/mapping/holder/BlockPos;)Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectObjectImmutablePair;"
            )
    )
    private ObjectObjectImmutablePair<LiftDirection, ObjectObjectImmutablePair<String, String>> yte$getRealFloorDetailsForSelection(
            World world, Lift lift, BlockPos blockPos) {
        return GetLiftDetails.getLiftDetails(world, lift, blockPos);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void yte$createDoorButtons(long liftId, CallbackInfo ci) {
        yte$holdOpenButton = new ButtonWidgetExtension(0, 0, 0, IGui.SQUARE_SIZE,
                TextHelper.translatable("gui.yte.lift_hold_open"), button -> yte$sendDoorCommand(LiftDoorControlState.Command.HOLD_OPEN));
        yte$openDoorButton = new ButtonWidgetExtension(0, 0, 0, IGui.SQUARE_SIZE,
                TextHelper.literal("◀▶"), button -> yte$sendDoorCommand(LiftDoorControlState.Command.OPEN));
        yte$closeDoorButton = new ButtonWidgetExtension(0, 0, 0, IGui.SQUARE_SIZE,
                TextHelper.literal("▶◀"), button -> yte$sendDoorCommand(LiftDoorControlState.Command.CLOSE));
    }

    @Inject(method = "onPress", at = @At("HEAD"))
    private void yte$resetDirectionForCurrentFloorCarCall(
            DashboardListItem ignoredItem, int index, CallbackInfo ci) {
        final Lift lift = MinecraftClientData.getLift(liftId);
        if (lift == null || index < 0 || index >= floorLevels.size()) {
            return;
        }

        final MixinLiftSchema schema = (MixinLiftSchema) lift;
        final int selectedFloor = lift.getFloorIndex(org.mtr.mod.Init.blockPosToPosition(
                floorLevels.get(floorLevels.size() - index - 1)));
        final int currentFloor = lift.getFloorIndex(lift.getCurrentFloor().getPosition());
        if (selectedFloor == currentFloor
                && schema.getSpeed() == 0
                && schema.getInstructions().isEmpty()
                && schema.getStoppingCoolDown() <= 500
                && lift.getDoorValue() == 0) {
            LiftDisplayDirectionState.get(liftId).resetForCarSameFloorOpen();
        }
    }

    @Inject(method = "init2", at = @At("TAIL"))
    private void yte$initDoorButtons(CallbackInfo ci) {
        selectionList.height = Math.max(selectionList.height - IGui.SQUARE_SIZE, IGui.SQUARE_SIZE * 2);
        final int buttonY = selectionList.y + selectionList.height;

        addChild(new ClickableWidget(yte$holdOpenButton));
        addChild(new ClickableWidget(yte$openDoorButton));
        addChild(new ClickableWidget(yte$closeDoorButton));

        yte$lastHoldEnabled = YteLiftConfigStore.isDoorHoldEnabled(liftId);
        yte$updateDoorButtonLayout(buttonY);
    }

    @Inject(method = "tick2", at = @At("TAIL"))
    private void yte$updateDoorButtonAvailability(CallbackInfo ci) {
        if (yte$clearDoorButtonFocus) {
            setFocused(null);
            yte$clearDoorButtonFocus = false;
        }

        final boolean holdEnabled = YteLiftConfigStore.isDoorHoldEnabled(liftId);
        if (holdEnabled != yte$lastHoldEnabled) {
            yte$lastHoldEnabled = holdEnabled;
            yte$updateDoorButtonLayout(selectionList.y + selectionList.height);
        }

        final Lift lift = MinecraftClientData.getLift(liftId);
        final boolean stoppedAtFloor = lift != null && yte$isStoppedAtFloor(lift);
        yte$holdOpenButton.active = stoppedAtFloor;
        yte$openDoorButton.active = stoppedAtFloor;

        if (!stoppedAtFloor) {
            yte$closeDoorButton.active = false;
            return;
        }

        final long coolDown = ((MixinLiftSchema) lift).getStoppingCoolDown();
        final boolean doorFullyOpen = lift.getDoorValue() >= 0.999F;
        if (holdEnabled && doorFullyOpen) {
            yte$closeDoorButton.active = coolDown > yte$closeStartCoolDown();
        } else {
            yte$closeDoorButton.active = doorFullyOpen
                    && coolDown <= yte$fullOpenCoolDown() - 300
                    && coolDown > yte$closeStartCoolDown();
        }
    }

    @Unique
    private void yte$updateDoorButtonLayout(int buttonY) {
        final boolean holdEnabled = YteLiftConfigStore.isDoorHoldEnabled(liftId);
        yte$holdOpenButton.setVisibleMapped(holdEnabled);

        if (holdEnabled) {
            final int buttonWidth = selectionList.width / 3;
            yte$holdOpenButton.setX2(selectionList.x);
            yte$holdOpenButton.setY2(buttonY);
            yte$holdOpenButton.setWidth2(buttonWidth);
            yte$openDoorButton.setX2(selectionList.x + buttonWidth);
            yte$openDoorButton.setY2(buttonY);
            yte$openDoorButton.setWidth2(buttonWidth);
            yte$closeDoorButton.setX2(selectionList.x + buttonWidth * 2);
            yte$closeDoorButton.setY2(buttonY);
            yte$closeDoorButton.setWidth2(selectionList.width - buttonWidth * 2);
        } else {
            final int buttonWidth = selectionList.width / 2;
            yte$openDoorButton.setX2(selectionList.x);
            yte$openDoorButton.setY2(buttonY);
            yte$openDoorButton.setWidth2(buttonWidth);
            yte$closeDoorButton.setX2(selectionList.x + buttonWidth);
            yte$closeDoorButton.setY2(buttonY);
            yte$closeDoorButton.setWidth2(selectionList.width - buttonWidth);
        }
    }

    @Unique
    private void yte$sendDoorCommand(LiftDoorControlState.Command command) {
        if (command == LiftDoorControlState.Command.OPEN || command == LiftDoorControlState.Command.HOLD_OPEN) {
            yte$applyClientOpenCommand();
        }
        InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketLiftDoorControl(liftId, command));
        // Minecraft keeps the last clicked active button keyboard-focused,
        // and assigns that focus after the press callback has returned. Clear
        // it on the next screen tick instead of too early in this callback.
        yte$clearDoorButtonFocus = true;
    }

    @Unique
    private void yte$applyClientOpenCommand() {
        final Lift lift = MinecraftClientData.getLift(liftId);
        if (lift == null || !yte$isStoppedAtFloor(lift)) {
            return;
        }

        final MixinLiftSchema schema = (MixinLiftSchema) lift;
        final long coolDown = schema.getStoppingCoolDown();
        final long singleDoorMoveTime = org.mtr.core.data.Vehicle.DOOR_MOVE_TIME / 2;
        final long stoppingTime = org.mtr.core.data.Vehicle.DOOR_MOVE_TIME + 2500;
        final long fullOpenCoolDown = stoppingTime - singleDoorMoveTime;
        final long closeStartCoolDown = 500 + singleDoorMoveTime;
        final float doorValue = Math.max(0, Math.min(lift.getDoorValue(), 1));

        if (doorValue >= 1) {
            schema.setStoppingCoolDown(fullOpenCoolDown);
            LiftDoorControlState.beginClientOpenPrediction(liftId, doorValue);
        } else if (doorValue > 0 && coolDown <= closeStartCoolDown) {
            // Reverse from the exact locally rendered position immediately;
            // waiting for the server round trip causes a visible forward jump.
            schema.setStoppingCoolDown(stoppingTime - Math.round(doorValue * singleDoorMoveTime));
            LiftDoorControlState.beginClientOpenPrediction(liftId, doorValue);
        } else if (doorValue <= 0 && coolDown < 500) {
            schema.setStoppingCoolDown(stoppingTime);
            LiftDoorControlState.beginClientOpenPrediction(liftId, doorValue);
            if (schema.getInstructions().isEmpty()) {
                LiftDisplayDirectionState.get(liftId).resetForIdleDoorCycle();
            }
        }
    }

    @Unique
    private static boolean yte$isStoppedAtFloor(Lift lift) {
        final MixinLiftSchema schema = (MixinLiftSchema) lift;
        if (schema.getSpeed() != 0) {
            return false;
        }
        for (int i = 0; i < schema.getFloors().size(); i++) {
            if (Math.abs(schema.getRailProgress() - ((MixinLiftFields) lift).invokeGetProgress(i)) < 0.000001) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static long yte$fullOpenCoolDown() {
        return 2500 + org.mtr.core.data.Vehicle.DOOR_MOVE_TIME / 2;
    }

    @Unique
    private static long yte$closeStartCoolDown() {
        return 500 + org.mtr.core.data.Vehicle.DOOR_MOVE_TIME / 2;
    }
}
