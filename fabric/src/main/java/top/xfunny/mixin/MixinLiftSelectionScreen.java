package top.xfunny.mixin;

import org.mtr.core.data.Lift;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.client.MinecraftClientData;
import org.mtr.mod.data.IGui;
import org.mtr.mod.screen.DashboardList;
import org.mtr.mod.screen.LiftSelectionScreen;
import org.mtr.mod.screen.MTRScreenBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.xfunny.mod.LiftDoorControlState;
import top.xfunny.mod.LiftDisplayDirectionState;
import top.xfunny.mod.client.InitClient;
import top.xfunny.mod.packet.PacketLiftDoorControl;

@Mixin(value = LiftSelectionScreen.class, remap = false)
public abstract class MixinLiftSelectionScreen extends MTRScreenBase {

    @Shadow @Final private DashboardList selectionList;
    @Shadow @Final private long liftId;

    @Unique private ButtonWidgetExtension yte$openDoorButton;
    @Unique private ButtonWidgetExtension yte$closeDoorButton;
    @Unique private boolean yte$clearDoorButtonFocus;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void yte$createDoorButtons(long liftId, CallbackInfo ci) {
        yte$openDoorButton = new ButtonWidgetExtension(0, 0, 0, IGui.SQUARE_SIZE,
                TextHelper.literal("◀▶"), button -> yte$sendDoorCommand(LiftDoorControlState.Command.OPEN));
        yte$closeDoorButton = new ButtonWidgetExtension(0, 0, 0, IGui.SQUARE_SIZE,
                TextHelper.literal("▶◀"), button -> yte$sendDoorCommand(LiftDoorControlState.Command.CLOSE));
    }

    @Inject(method = "init2", at = @At("TAIL"))
    private void yte$initDoorButtons(CallbackInfo ci) {
        selectionList.height = Math.max(selectionList.height - IGui.SQUARE_SIZE, IGui.SQUARE_SIZE * 2);
        final int buttonWidth = selectionList.width / 2;
        final int buttonY = selectionList.y + selectionList.height;

        yte$openDoorButton.setX2(selectionList.x);
        yte$openDoorButton.setY2(buttonY);
        yte$openDoorButton.setWidth2(buttonWidth);
        yte$closeDoorButton.setX2(selectionList.x + buttonWidth);
        yte$closeDoorButton.setY2(buttonY);
        yte$closeDoorButton.setWidth2(selectionList.width - buttonWidth);

        addChild(new ClickableWidget(yte$openDoorButton));
        addChild(new ClickableWidget(yte$closeDoorButton));
    }

    @Inject(method = "tick2", at = @At("TAIL"))
    private void yte$updateDoorButtonAvailability(CallbackInfo ci) {
        if (yte$clearDoorButtonFocus) {
            setFocused(null);
            yte$clearDoorButtonFocus = false;
        }

        final Lift lift = MinecraftClientData.getLift(liftId);
        final boolean stoppedAtFloor = lift != null && yte$isStoppedAtFloor(lift);
        yte$openDoorButton.active = stoppedAtFloor;

        if (!stoppedAtFloor) {
            yte$closeDoorButton.active = false;
            return;
        }

        final long coolDown = ((MixinLiftSchema) lift).getStoppingCoolDown();
        yte$closeDoorButton.active = lift.getDoorValue() >= 0.999F
                && coolDown <= yte$fullOpenCoolDown() - 300
                && coolDown > yte$closeStartCoolDown();
    }

    @Unique
    private void yte$sendDoorCommand(LiftDoorControlState.Command command) {
        if (command == LiftDoorControlState.Command.OPEN) {
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
        } else if (doorValue > 0 && coolDown <= closeStartCoolDown) {
            // Reverse from the exact locally rendered position immediately;
            // waiting for the server round trip causes a visible forward jump.
            schema.setStoppingCoolDown(stoppingTime - Math.round(doorValue * singleDoorMoveTime));
        } else if (doorValue <= 0 && coolDown < 500) {
            schema.setStoppingCoolDown(stoppingTime);
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
