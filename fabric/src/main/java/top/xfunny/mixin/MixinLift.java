package top.xfunny.mixin;

import org.mtr.core.data.*;
import org.mtr.core.data.Lift;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Utilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.xfunny.mod.config.YteLiftConfigStore;
import top.xfunny.mod.Init;
import top.xfunny.mod.LiftDisplayDirection;
import top.xfunny.mod.LiftDisplayDirectionState;
import top.xfunny.mod.LiftDoorControlState;
import top.xfunny.mod.DisplayDirectionMode;
import top.xfunny.mod.LiftDisplayState;

@Mixin(value = Lift.class, remap = false)
public abstract class MixinLift implements MixinLiftSchema, MixinLiftFields, MixinNameColorDataBaseSchema, LiftDisplayDirection {

    @Unique
    private static final long YTE_LIFT_STOPPING_TIME = Vehicle.DOOR_MOVE_TIME + 2500;

    @Unique
    private static final long YTE_BRAKE_HOLD_TIME = 200;

    @Unique
    private static final long YTE_ARRIVAL_DIRECTION_DELAY = 100;

    @Unique
    private static final long YTE_DOOR_CLOSED_DELAY = 500;

    @Unique
    private static final long YTE_SINGLE_DOOR_MOVE_TIME = Vehicle.DOOR_MOVE_TIME / 2;

    @Unique
    private static final long YTE_DOOR_CLOSE_PROTECTION_TIME = 300;

    /**
     * MTR's door curve becomes negative when the cooldown is extended beyond its
     * native stopping time. Clamp that short brake-hold section to fully closed.
     */
    @Inject(method = "getDoorValue", at = @At("RETURN"), cancellable = true)
    private void yte$clampBrakeHoldDoorValue(CallbackInfoReturnable<Float> cir) {
        if (cir.getReturnValue() < 0) {
            cir.setReturnValue(0F);
        }
    }

    /**
     * Keep dispatching on MTR's original server-side direction while exposing a
     * persistent, elevator-style travel direction to every client display.
     */
    @Inject(method = "getDirection", at = @At("HEAD"), cancellable = true)
    private void yte$getDisplayDirection(CallbackInfoReturnable<LiftDirection> cir) {
        if (isClientside()) {
            cir.setReturnValue(yte$getDisplayDirection(DisplayDirectionMode.LATCH_UNTIL_DOOR_CLOSE));
        }
    }

    /**
     * @author YTE
     * @reason Replace MAX_SPEED and ACCELERATION_DEFAULT with per-lift custom values
     */
    @Overwrite
    public void tick(long millisElapsed) {
        final long id = ((Lift) (Object) this).getId();
        final double customMaxSpeed = YteLiftConfigStore.getSpeed(id) / 1000.0;
        final double customAccel = YteLiftConfigStore.getAcceleration(id) / 1_000_000.0;
        final double adoDistance = YteLiftConfigStore.getAdoDistance(id);
        final double levellingDistance = YteLiftConfigStore.getLevellingDistance(id);
        final double levellingSpeed = YteLiftConfigStore.getLevellingSpeed(id) / 1000.0;

        if (!isClientside()) {
            final LiftDoorControlState.Command doorCommand = LiftDoorControlState.consume(id);
            if (doorCommand != null) {
                yte$applyDoorCommand(doorCommand);
            }
        }

        final boolean adoLevelling = getStoppingCoolDown() > 0 && getSpeed() != 0 && !getInstructions().isEmpty();

        if (getStoppingCoolDown() > 0 && !adoLevelling) {
            setStoppingCoolDown(Math.max(getStoppingCoolDown() - millisElapsed, 0));
            if (getStoppingCoolDown() == 0) {
                if (isClientside()) {
                    setStoppingCoolDown(1);
                } else {
                    setNeedsUpdate(true);
                }
            }
        } else {
            if (adoLevelling) {
                setStoppingCoolDown(Math.max(getStoppingCoolDown() - millisElapsed, 0));
            }

            if (getInstructions().isEmpty()) {
                setSpeed(Math.max(Math.abs(getSpeed()) - customAccel * millisElapsed, 0) * Math.signum(getSpeed()));
            } else {
                final long nextInstructionProgress = invokeGetProgress(getInstructions().get(0).getFloor());
                final double distanceToTarget = Math.abs(nextInstructionProgress - getRailProgress());
                final double absoluteSpeed = Math.abs(getSpeed());
                final boolean useLevellingApproach = levellingDistance > 0 && levellingSpeed > 0;
                final double distanceToBrakingTarget = useLevellingApproach
                        ? Math.max(distanceToTarget - levellingDistance, 0)
                        : distanceToTarget;
                final double brakingTargetSpeed = useLevellingApproach ? levellingSpeed : 0;
                final double requiredBrakingDistance = Math.max(
                        (absoluteSpeed * absoluteSpeed - brakingTargetSpeed * brakingTargetSpeed) / (2 * customAccel), 0);
                final double movementThisTick = absoluteSpeed * millisElapsed;

                if (useLevellingApproach && absoluteSpeed > levellingSpeed && movementThisTick >= distanceToBrakingTarget) {
                    // Last-resort guard for discrete ticks: never enter the levelling
                    // zone faster than its configured speed, even if normal braking
                    // could not finish before the boundary.
                    setSpeed(levellingSpeed * Math.signum(getSpeed()));
                } else if (absoluteSpeed > brakingTargetSpeed && requiredBrakingDistance + movementThisTick > distanceToBrakingTarget) {
                    setSpeed(Math.max(absoluteSpeed - customAccel * millisElapsed, customAccel) * Math.signum(getSpeed()));
                } else {
                    setSpeed(Utilities.clamp(getSpeed() + customAccel * millisElapsed * Math.signum(nextInstructionProgress - getRailProgress()), -customMaxSpeed, customMaxSpeed));
                }

                if (getSpeed() != 0 && levellingDistance > 0 && levellingSpeed > 0 && distanceToTarget <= levellingDistance) {
                    final double levellingDeceleration = levellingSpeed * levellingSpeed / (2 * levellingDistance);
                    final double levellingTargetSpeed = Math.sqrt(2 * levellingDeceleration * distanceToTarget);
                    setSpeed(Math.min(Math.abs(getSpeed()), levellingTargetSpeed) * Math.signum(getSpeed()));
                }

                final double updatedMovementThisTick = Math.abs(getSpeed() * millisElapsed);
                if (adoDistance > 0 && !isClientside() && !adoLevelling && getSpeed() != 0 && distanceToTarget <= adoDistance + updatedMovementThisTick) {
                    setStoppingCoolDown(YTE_LIFT_STOPPING_TIME);
                    Init.sendLiftAdoStart(id, YTE_LIFT_STOPPING_TIME);
                }

                if (Math.abs(getRailProgress() - nextInstructionProgress) <= Math.abs(getSpeed() * millisElapsed)) {
                    setRailProgress(nextInstructionProgress);
                    setSpeed(0);
                    if (!isClientside()) {
                        getInstructions().remove(0);
                        if (getStoppingCoolDown() == 0) {
                            setStoppingCoolDown(YTE_LIFT_STOPPING_TIME + (adoDistance <= 0 ? YTE_BRAKE_HOLD_TIME : 0));
                        }
                        setNeedsUpdate(true);
                    }
                }
            }

            setRailProgress(Utilities.clamp(getRailProgress() + getSpeed() * millisElapsed, 0, invokeGetProgress(Integer.MAX_VALUE)));
        }

        if (isClientside()) {
            yte$updateDisplayFacts(levellingDistance);
            yte$updateDisplayDirection(millisElapsed);
        }

        if (getData() instanceof Simulator) {
            ((Simulator) getData()).clients.forEach(client -> {
                if (Utilities.isBetween(client.getPosition(), getMinPosition(), getMaxPosition(), client.getUpdateRadius())) {
                    client.update((Lift) (Object) this, getNeedsUpdate());
                }
            });

            setNeedsUpdate(false);
        }
    }

    @Unique
    private void yte$applyDoorCommand(LiftDoorControlState.Command command) {
        if (getSpeed() != 0 || !yte$isExactlyAtFloor()) {
            return;
        }

        final Lift lift = (Lift) (Object) this;
        final long coolDown = getStoppingCoolDown();
        final long fullOpenCoolDown = YTE_LIFT_STOPPING_TIME - YTE_SINGLE_DOOR_MOVE_TIME;
        final long closeStartCoolDown = YTE_DOOR_CLOSED_DELAY + YTE_SINGLE_DOOR_MOVE_TIME;
        final boolean startingIdleDoorCycle = command == LiftDoorControlState.Command.OPEN
                && getInstructions().isEmpty()
                && coolDown < YTE_DOOR_CLOSED_DELAY
                && lift.getDoorValue() <= 0;

        if (command == LiftDoorControlState.Command.OPEN) {
            final float doorValue = Utilities.clamp(lift.getDoorValue(), 0, 1);
            if (doorValue >= 1) {
                setStoppingCoolDown(fullOpenCoolDown);
            } else if (doorValue > 0 && coolDown <= closeStartCoolDown) {
                setStoppingCoolDown(YTE_LIFT_STOPPING_TIME - Math.round(doorValue * YTE_SINGLE_DOOR_MOVE_TIME));
            } else if (doorValue <= 0 && coolDown < YTE_DOOR_CLOSED_DELAY) {
                setStoppingCoolDown(YTE_LIFT_STOPPING_TIME);
                if (startingIdleDoorCycle) {
                    Init.sendIdleLiftDoorOpen(lift.getId());
                }
            }
        } else if (lift.getDoorValue() >= 0.999F
                && coolDown <= fullOpenCoolDown - YTE_DOOR_CLOSE_PROTECTION_TIME
                && coolDown > closeStartCoolDown) {
            setStoppingCoolDown(closeStartCoolDown);
        }

        setNeedsUpdate(true);
    }

    @Unique
    private boolean yte$isExactlyAtFloor() {
        for (int i = 0; i < getFloors().size(); i++) {
            if (Math.abs(getRailProgress() - invokeGetProgress(i)) < 0.000001) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void yte$updateDisplayFacts(double levellingDistance) {
        final Lift lift = (Lift) (Object) this;
        final boolean moving = getSpeed() != 0;
        final LiftDirection movementDirection = getSpeed() > 0
                ? LiftDirection.UP
                : getSpeed() < 0 ? LiftDirection.DOWN : LiftDirection.NONE;
        int targetFloor = -1;
        double distanceToTarget = Double.POSITIVE_INFINITY;
        LiftDirection targetDirection = LiftDirection.NONE;

        if (!getInstructions().isEmpty()) {
            final LiftInstruction instruction = getInstructions().get(0);
            targetFloor = instruction.getFloor();
            final double difference = invokeGetProgress(targetFloor) - getRailProgress();
            distanceToTarget = Math.abs(difference);
            targetDirection = difference > 0
                    ? LiftDirection.UP
                    : difference < 0
                    ? LiftDirection.DOWN
                    : instruction.getDirection();
        }

        final boolean doorCycle = getStoppingCoolDown() > 1 || lift.getDoorValue() != 0;
        final boolean levelling = moving && levellingDistance > 0 && distanceToTarget <= levellingDistance;
        final boolean idle = !moving && getInstructions().isEmpty() && !doorCycle;
        final int displayedFloor = lift.getFloorIndex(lift.getCurrentFloor().getPosition());

        LiftDisplayState.get(lift.getId()).update(
                movementDirection, targetDirection, moving, levelling, doorCycle, idle,
                displayedFloor, targetFloor, getSpeed(), distanceToTarget, getStoppingCoolDown());
    }

    @Unique
    private void yte$updateDisplayDirection(long millisElapsed) {
        final Lift lift = (Lift) (Object) this;
        final LiftDisplayDirectionState displayState = LiftDisplayDirectionState.get(lift.getId());
        final int instructionCount = getInstructions().size();
        final boolean instructionAdded = instructionCount > displayState.previousInstructionCount;
        displayState.previousInstructionCount = instructionCount;
        final int floorCount = getFloors().size();
        final int displayedFloorIndex = lift.getFloorIndex(lift.getCurrentFloor().getPosition());

        final boolean activeDirectionCycle = getSpeed() != 0
                || getStoppingCoolDown() > 1
                || !getInstructions().isEmpty();
        final boolean doorCycleActive = getStoppingCoolDown() > 1 || lift.getDoorValue() != 0;

        if (!doorCycleActive) {
            displayState.deferredSameFloorCallDirection = LiftDirection.NONE;
        }

        if (displayState.sameFloorCallDirection != LiftDirection.NONE) {
            displayState.sameFloorCallWaitMillis += millisElapsed;
            if (doorCycleActive) {
                displayState.sameFloorCallDoorCycleStarted = true;
            }

            if (doorCycleActive) {
                displayState.direction = displayState.sameFloorCallDirection;
                return;
            }

            if (!displayState.sameFloorCallDoorCycleStarted && displayState.sameFloorCallWaitMillis < 10000) {
                displayState.direction = displayState.sameFloorCallDirection;
                return;
            }

            displayState.sameFloorCallDirection = LiftDirection.NONE;
            displayState.sameFloorCallDoorCycleStarted = false;
            displayState.sameFloorCallWaitMillis = 0;
        }

        if (!getInstructions().isEmpty()) {
            final LiftInstruction instruction = getInstructions().get(0);
            if (displayedFloorIndex == instruction.getFloor()) {
                final LiftDirection arrivalDirection = displayedFloorIndex == floorCount - 1
                        ? LiftDirection.DOWN
                        : displayedFloorIndex == 0
                        ? LiftDirection.UP
                        : instruction.getDirection();
                final boolean deferDirectionChange = doorCycleActive
                        && displayState.deferredSameFloorCallDirection != LiftDirection.NONE;
                if (!deferDirectionChange && (displayState.arrivalFloor != displayedFloorIndex
                        || displayState.arrivalDirection != arrivalDirection)) {
                    displayState.arrivalFloor = displayedFloorIndex;
                    displayState.arrivalDirection = arrivalDirection;
                    displayState.arrivalMillis = 0;
                }
            }
        }

        if (displayState.arrivalFloor >= 0 && displayedFloorIndex != displayState.arrivalFloor) {
            yte$resetArrivalDirectionDelay();
        } else if (activeDirectionCycle && displayState.arrivalFloor >= 0) {
            displayState.arrivalMillis += millisElapsed;
            final boolean currentInstructionIsArrival = !getInstructions().isEmpty()
                    && getInstructions().get(0).getFloor() == displayState.arrivalFloor;
            final boolean retainArrivalDirection = getStoppingCoolDown() > 1 || currentInstructionIsArrival;
            if (retainArrivalDirection
                    && displayState.arrivalMillis >= YTE_ARRIVAL_DIRECTION_DELAY
                    && displayState.arrivalDirection != LiftDirection.NONE) {
                displayState.direction = displayState.arrivalDirection;
                return;
            }
        }

        // During ADO and the complete door cycle, retain the arrival direction.
        // Client lifts keep a value of 1 as the completed sync sentinel.
        // Only larger values represent an active ADO/door cycle.
        if (getStoppingCoolDown() > 1) {
            if (instructionAdded && displayState.direction == LiftDirection.NONE) {
                yte$setDisplayDirectionFromNextInstruction(displayState);
            }
            return;
        }

        if (getInstructions().isEmpty()) {
            displayState.direction = LiftDirection.NONE;
            return;
        }

        yte$setDisplayDirectionFromNextInstruction(displayState);
    }

    @Unique
    private void yte$setDisplayDirectionFromNextInstruction(LiftDisplayDirectionState displayState) {
        final LiftInstruction instruction = getInstructions().get(0);
        final double difference = invokeGetProgress(instruction.getFloor()) - getRailProgress();
        displayState.direction = difference > 0 ? LiftDirection.UP
                : difference < 0 ? LiftDirection.DOWN
                : instruction.getDirection() != LiftDirection.NONE
                ? instruction.getDirection()
                : displayState.direction;
    }

    @Override
    public void yte$resetArrivalDirectionDelay() {
        final LiftDisplayDirectionState displayState = LiftDisplayDirectionState.get(((Lift) (Object) this).getId());
        displayState.arrivalFloor = -1;
        displayState.arrivalDirection = LiftDirection.NONE;
        displayState.arrivalMillis = 0;
    }

    @Override
    public LiftDisplayState yte$getDisplayState() {
        return LiftDisplayState.get(((Lift) (Object) this).getId());
    }

}
