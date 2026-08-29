package top.xfunny.mixin;

import org.mtr.core.data.Lift;
import org.mtr.core.data.LiftDirection;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.World;
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
import top.xfunny.mod.lift.LiftDoorButtonLightMode;
import top.xfunny.mod.lift.LiftDoorControlState;
import top.xfunny.mod.lift.LiftDisplayDirectionState;
import top.xfunny.mod.lift.LiftFloorCancelMode;
import top.xfunny.mod.client.InitClient;
import top.xfunny.mod.client.screen.widget.LiftSelectionButtonWidget;
import top.xfunny.mod.config.YteLiftConfigStore;
import top.xfunny.mod.packet.PacketLiftDoorControl;
import top.xfunny.mod.packet.PacketLiftFloorCancel;
import top.xfunny.mod.packet.PacketLiftHoldState;
import top.xfunny.mod.util.GetLiftDetails;

@Mixin(value = LiftSelectionScreen.class, remap = false)
public abstract class MixinLiftSelectionScreen extends MTRScreenBase {

    @Shadow @Final private DashboardList selectionList;
    @Shadow @Final private ObjectArrayList<BlockPos> floorLevels;
    @Shadow @Final private long liftId;

    @Unique private static final long YTE_DOUBLE_CLICK_WINDOW = 400;
    @Unique private static final long YTE_LONG_PRESS_TIME = 750;
    @Unique private static final long YTE_DOOR_REPEAT_INTERVAL = 250;
    @Unique private static final long YTE_TIMED_LIGHT_DURATION = 1000;

    @Unique private LiftSelectionButtonWidget yte$holdOpenButton;
    @Unique private LiftSelectionButtonWidget yte$openDoorButton;
    @Unique private LiftSelectionButtonWidget yte$closeDoorButton;
    @Unique private boolean yte$clearDoorButtonFocus;
    @Unique private boolean yte$lastHoldEnabled;
    @Unique private LiftDoorControlState.Command yte$pressedDoorCommand;
    @Unique private long yte$nextDoorRepeatTime;
    @Unique private long yte$holdOpenLightUntil;
    @Unique private long yte$openDoorLightUntil;
    @Unique private long yte$closeDoorLightUntil;
    @Unique private int yte$lastFloorClickIndex = -1;
    @Unique private long yte$lastFloorClickTime;
    @Unique private int yte$heldFloorIndex = -1;
    @Unique private long yte$floorHoldStartTime;
    @Unique private boolean yte$floorHoldArmed;
    @Unique private boolean yte$floorHoldPointerInside;

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
        yte$holdOpenButton = new LiftSelectionButtonWidget(0, 0, 0, IGui.SQUARE_SIZE,
                TextHelper.translatable("gui.yte.lift_hold_open"), button -> yte$sendDoorCommand(LiftDoorControlState.Command.HOLD_OPEN));
        yte$openDoorButton = new LiftSelectionButtonWidget(0, 0, 0, IGui.SQUARE_SIZE,
                TextHelper.literal("◀▶"), button -> yte$sendDoorCommand(LiftDoorControlState.Command.OPEN));
        yte$closeDoorButton = new LiftSelectionButtonWidget(0, 0, 0, IGui.SQUARE_SIZE,
                TextHelper.literal("▶◀"), button -> yte$sendDoorCommand(LiftDoorControlState.Command.CLOSE));
    }

    @Inject(method = "onPress", at = @At("HEAD"))
    private void yte$handleFloorButtonPress(
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

    @Redirect(
            method = "onPress",
            at = @At(value = "INVOKE", target = "Lorg/mtr/mod/screen/LiftSelectionScreen;onClose2()V")
    )
    private void yte$keepSelectionScreenOpen(LiftSelectionScreen ignoredScreen) {
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
        InitClient.REGISTRY_CLIENT.sendPacketToServer(PacketLiftHoldState.query(liftId));
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
        yte$holdOpenButton.setActiveMapped(true);
        yte$openDoorButton.setActiveMapped(true);
        yte$closeDoorButton.setActiveMapped(true);

        final long currentTime = System.currentTimeMillis();
        if (yte$pressedDoorCommand != null) {
            final LiftSelectionButtonWidget pressedButton = yte$getDoorButton(yte$pressedDoorCommand);
            if (pressedButton == null || !pressedButton.getVisibleMapped()) {
                yte$finishDoorButtonPress(currentTime);
            } else if (yte$pressedDoorCommand != LiftDoorControlState.Command.HOLD_OPEN
                    && currentTime >= yte$nextDoorRepeatTime) {
                yte$sendDoorCommand(yte$pressedDoorCommand);
                yte$nextDoorRepeatTime = currentTime + YTE_DOOR_REPEAT_INTERVAL;
            }
        }

        if (yte$floorHoldArmed) {
            if (selectionList.getHoverItemIndex() != yte$heldFloorIndex) {
                yte$resetFloorHold();
            } else if (currentTime - yte$floorHoldStartTime >= YTE_LONG_PRESS_TIME) {
                yte$cancelHeldFloorIfAllowed(lift);
            }
        }

        yte$updateDoorButtonLights(currentTime);
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

    @Override
    public boolean mouseClicked2(double mouseX, double mouseY, int button) {
        if (button == 0) {
            final LiftDoorControlState.Command command = yte$getDoorCommandAt(mouseX, mouseY);
            if (command != null) {
                yte$startDoorButtonPress(command);
                return true;
            }

            if (yte$isFloorListPosition(mouseX, mouseY)) {
                final Lift lift = MinecraftClientData.getLift(liftId);
                final int selectionIndex = selectionList.getHoverItemIndex();
                if (lift != null && selectionIndex >= 0 && selectionIndex < floorLevels.size()) {
                    final int floorIndex = yte$getFloorIndex(lift, selectionIndex);
                    final boolean selected = lift.hasInstruction(floorIndex).contains(LiftDirection.NONE);
                    if (YteLiftConfigStore.getFloorCancelMode(liftId) == LiftFloorCancelMode.DOUBLE_CLICK) {
                        if (!selected) {
                            yte$resetFloorClickTracking();
                            return super.mouseClicked2(mouseX, mouseY, button);
                        }
                        final long currentTime = System.currentTimeMillis();
                        if (yte$lastFloorClickIndex == selectionIndex
                                && currentTime - yte$lastFloorClickTime <= YTE_DOUBLE_CLICK_WINDOW) {
                            yte$sendFloorCancellation(floorIndex);
                            yte$resetFloorClickTracking();
                            return true;
                        }
                        yte$lastFloorClickIndex = selectionIndex;
                        yte$lastFloorClickTime = currentTime;
                        // The first click of the cancellation gesture only arms
                        // the double-click; do not send another normal car call.
                        return true;
                    } else if (selected) {
                        yte$heldFloorIndex = selectionIndex;
                        yte$floorHoldStartTime = System.currentTimeMillis();
                        yte$floorHoldArmed = true;
                        yte$floorHoldPointerInside = true;
                        // A short press on an already selected floor is a no-op;
                        // holding it long enough sends the cancellation instead.
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked2(mouseX, mouseY, button);
    }

    @Inject(method = "mouseMoved2", at = @At("TAIL"))
    private void yte$trackFloorHoldPointer(double mouseX, double mouseY, CallbackInfo ci) {
        if (yte$floorHoldArmed) {
            yte$floorHoldPointerInside = yte$isFloorListPosition(mouseX, mouseY)
                    && selectionList.getHoverItemIndex() == yte$heldFloorIndex;
            if (!yte$floorHoldPointerInside) {
                yte$resetFloorHold();
            }
        }
    }

    @Override
    public boolean mouseReleased2(double mouseX, double mouseY, int button) {
        if (button == 0 && yte$pressedDoorCommand != null) {
            yte$finishDoorButtonPress(System.currentTimeMillis());
            return true;
        }
        final boolean handled = super.mouseReleased2(mouseX, mouseY, button);
        if (button == 0) {
            yte$resetFloorHold();
        }
        return handled;
    }

    @Unique
    private LiftDoorControlState.Command yte$getDoorCommandAt(double mouseX, double mouseY) {
        if (yte$isClickableDoorButton(yte$holdOpenButton, mouseX, mouseY)) {
            return LiftDoorControlState.Command.HOLD_OPEN;
        }
        if (yte$isClickableDoorButton(yte$openDoorButton, mouseX, mouseY)) {
            return LiftDoorControlState.Command.OPEN;
        }
        if (yte$isClickableDoorButton(yte$closeDoorButton, mouseX, mouseY)) {
            return LiftDoorControlState.Command.CLOSE;
        }
        return null;
    }

    @Unique
    private static boolean yte$isClickableDoorButton(LiftSelectionButtonWidget button, double mouseX, double mouseY) {
        return button.getVisibleMapped() && button.isMouseOver2(mouseX, mouseY);
    }

    @Unique
    private boolean yte$isFloorListPosition(double mouseX, double mouseY) {
        return mouseX >= selectionList.x && mouseX < selectionList.x + selectionList.width
                && mouseY >= selectionList.y + 24 && mouseY < selectionList.y + selectionList.height;
    }

    @Unique
    private void yte$startDoorButtonPress(LiftDoorControlState.Command command) {
        final long currentTime = System.currentTimeMillis();
        if (yte$pressedDoorCommand != null) {
            yte$finishDoorButtonPress(currentTime);
        }
        yte$pressedDoorCommand = command;
        yte$nextDoorRepeatTime = currentTime + YTE_DOOR_REPEAT_INTERVAL;
        yte$sendDoorCommand(command);
        yte$updateDoorButtonLights(currentTime);
    }

    @Unique
    private void yte$finishDoorButtonPress(long currentTime) {
        if (yte$pressedDoorCommand == null) {
            return;
        }
        if (yte$pressedDoorCommand == LiftDoorControlState.Command.CLOSE) {
            yte$releaseCloseDoorButton();
        }
        if (YteLiftConfigStore.getDoorButtonLightMode(liftId) == LiftDoorButtonLightMode.TIMED) {
            yte$setDoorLightUntil(yte$pressedDoorCommand, currentTime + YTE_TIMED_LIGHT_DURATION);
        }
        yte$pressedDoorCommand = null;
        yte$updateDoorButtonLights(currentTime);
    }

    @Unique
    private void yte$releaseCloseDoorButton() {
        if (!YteLiftConfigStore.getServiceMode(liftId).acceptsHallCalls()) {
            final Lift lift = MinecraftClientData.getLift(liftId);
            if (lift != null && lift.getDoorValue() > 0) {
                yte$applyClientOpenCommand();
            }
            InitClient.REGISTRY_CLIENT.sendPacketToServer(
                    new PacketLiftDoorControl(liftId, LiftDoorControlState.Command.RELEASE_CLOSE));
        }
    }

    @Unique
    private void yte$updateDoorButtonLights(long currentTime) {
        final LiftDoorButtonLightMode lightMode = YteLiftConfigStore.getDoorButtonLightMode(liftId);
        yte$holdOpenButton.setLit(LiftDoorControlState.isClientHoldActive(liftId) || lightMode.isLit(
                yte$pressedDoorCommand == LiftDoorControlState.Command.HOLD_OPEN,
                currentTime < yte$holdOpenLightUntil, false));
        yte$openDoorButton.setLit(lightMode.isLit(
                yte$pressedDoorCommand == LiftDoorControlState.Command.OPEN,
                currentTime < yte$openDoorLightUntil, false));
        yte$closeDoorButton.setLit(lightMode.isLit(
                yte$pressedDoorCommand == LiftDoorControlState.Command.CLOSE,
                currentTime < yte$closeDoorLightUntil, false));
    }

    @Unique
    private void yte$setDoorLightUntil(LiftDoorControlState.Command command, long lightUntil) {
        switch (command) {
            case HOLD_OPEN:
                yte$holdOpenLightUntil = lightUntil;
                break;
            case OPEN:
                yte$openDoorLightUntil = lightUntil;
                break;
            case CLOSE:
                yte$closeDoorLightUntil = lightUntil;
                break;
            default:
                break;
        }
    }

    @Unique
    private LiftSelectionButtonWidget yte$getDoorButton(LiftDoorControlState.Command command) {
        switch (command) {
            case HOLD_OPEN:
                return yte$holdOpenButton;
            case OPEN:
                return yte$openDoorButton;
            case CLOSE:
                return yte$closeDoorButton;
            default:
                return null;
        }
    }

    @Unique
    private void yte$cancelHeldFloorIfAllowed(Lift lift) {
        if (lift != null && yte$floorHoldPointerInside
                && yte$heldFloorIndex >= 0 && yte$heldFloorIndex < floorLevels.size()) {
            final int floorIndex = yte$getFloorIndex(lift, yte$heldFloorIndex);
            yte$sendFloorCancellation(floorIndex);
        }
        yte$resetFloorHold();
    }

    @Unique
    private int yte$getFloorIndex(Lift lift, int selectionIndex) {
        return lift.getFloorIndex(org.mtr.mod.Init.blockPosToPosition(
                floorLevels.get(floorLevels.size() - selectionIndex - 1)));
    }

    @Unique
    private void yte$sendFloorCancellation(int floorIndex) {
        InitClient.REGISTRY_CLIENT.sendPacketToServer(new PacketLiftFloorCancel(liftId, floorIndex));
    }

    @Unique
    private void yte$resetFloorClickTracking() {
        yte$lastFloorClickIndex = -1;
        yte$lastFloorClickTime = 0;
    }

    @Unique
    private void yte$resetFloorHold() {
        yte$heldFloorIndex = -1;
        yte$floorHoldStartTime = 0;
        yte$floorHoldArmed = false;
        yte$floorHoldPointerInside = false;
    }

    @Unique
    private void yte$sendDoorCommand(LiftDoorControlState.Command command) {
        if (command == LiftDoorControlState.Command.CLOSE) {
            yte$holdOpenLightUntil = 0;
        }
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
        final long closeStartCoolDown = 500 + singleDoorMoveTime;
        final float doorValue = Math.max(0, Math.min(lift.getDoorValue(), 1));

        if (doorValue >= 1) {
            LiftDoorControlState.beginClientOpenPrediction(liftId, doorValue, singleDoorMoveTime);
        } else if (doorValue > 0 && coolDown <= closeStartCoolDown) {
            LiftDoorControlState.beginClientOpenPrediction(liftId, doorValue, singleDoorMoveTime);
        } else if (doorValue <= 0 && coolDown <= 500) {
            LiftDoorControlState.beginClientOpenPrediction(liftId, doorValue, singleDoorMoveTime);
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

}
