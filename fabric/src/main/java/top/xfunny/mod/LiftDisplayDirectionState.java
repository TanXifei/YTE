package top.xfunny.mod;

import org.mtr.core.data.LiftDirection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LiftDisplayDirectionState {

    private static final Map<Long, LiftDisplayDirectionState> STATES = new ConcurrentHashMap<>();

    public LiftDirection direction = LiftDirection.NONE;
    public int arrivalFloor = -1;
    public LiftDirection arrivalDirection = LiftDirection.NONE;
    public long arrivalMillis;
    public int previousInstructionCount;
    public LiftDirection sameFloorCallDirection = LiftDirection.NONE;
    public LiftDirection deferredSameFloorCallDirection = LiftDirection.NONE;
    public boolean sameFloorCallDoorCycleStarted;
    public long sameFloorCallWaitMillis;

    private LiftDisplayDirectionState() {
    }

    public static LiftDisplayDirectionState get(long liftId) {
        return STATES.computeIfAbsent(liftId, ignored -> new LiftDisplayDirectionState());
    }

    public void setSameFloorCallDirection(LiftDirection direction) {
        this.direction = direction;
        sameFloorCallDirection = direction;
        sameFloorCallDoorCycleStarted = false;
        sameFloorCallWaitMillis = 0;
        arrivalFloor = -1;
        arrivalDirection = LiftDirection.NONE;
        arrivalMillis = 0;
    }

    public void deferSameFloorCallDirection(LiftDirection direction) {
        deferredSameFloorCallDirection = direction;
    }

    public void resetForIdleDoorCycle() {
        // A same-floor hall call has an explicit requested direction and must
        // keep it. Manual door opening and a car call for the current floor do not.
        if (sameFloorCallDirection != LiftDirection.NONE) {
            return;
        }
        direction = LiftDirection.NONE;
        arrivalFloor = -1;
        arrivalDirection = LiftDirection.NONE;
        arrivalMillis = 0;
        deferredSameFloorCallDirection = LiftDirection.NONE;
    }
}
