package top.xfunny.mod.lift.policy;

import org.mtr.core.data.LiftDirection;
import top.xfunny.mod.lift.LiftArrivalLanternContext;
import top.xfunny.mod.lift.LiftArrivalLanternDecision;
import top.xfunny.mod.lift.LiftArrivalLanternDisplayPhase;
import top.xfunny.mod.lift.LiftArrivalLanternFlashPattern;
import top.xfunny.mod.lift.LiftArrivalLanternPolicy;
import top.xfunny.mod.lift.LiftArrivalLanternState;
import top.xfunny.mod.lift.LiftDisplayState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 三菱 MP-VF 到站灯策略：电梯开门前约 5 秒开始慢闪（0.7 亮 / 0.4 熄），
 * 开门期间保持同频闪烁，关门后熄灭。
 */
public final class MitsubishiMPVFLanternPolicy implements LiftArrivalLanternPolicy {

    public static final MitsubishiMPVFLanternPolicy INSTANCE = new MitsubishiMPVFLanternPolicy();

    private static final LiftArrivalLanternFlashPattern SLOW_FLASH = LiftArrivalLanternFlashPattern.flashing(700, 400);
    private static final long PRE_DOOR_MILLIS = 5000;
    private static final String SOUND_CUE = "mitsubishi_mp_lantern_1";

    private final Map<Long, Integer> approachFloors = new ConcurrentHashMap<>();
    private final Map<Long, Long> approachStartMillis = new ConcurrentHashMap<>();

    private MitsubishiMPVFLanternPolicy() {
    }

    public void clear() {
        approachFloors.clear();
        approachStartMillis.clear();
    }

    @Override
    public LiftArrivalLanternDecision evaluate(LiftArrivalLanternContext context) {
        final LiftDisplayState facts = context.getFacts();
        final LiftArrivalLanternState arrivalState = context.getArrivalState();
        final long liftId = facts.getLiftId();
        final int lanternFloor = context.getLanternFloor();

        // 开门前约 5 秒：朝本层运行且预估剩余时间不超过 5 秒。触发即锁存，
        // 避免速度接近 0 时 estimateMillisToTarget() 归为无穷导致熄灯。
        if (facts.getTargetFloor() == lanternFloor && facts.isMoving()
                && context.estimateMillisToTarget() <= PRE_DOOR_MILLIS) {
            final Integer previous = approachFloors.put(liftId, lanternFloor);
            if (previous == null || previous != lanternFloor) {
                approachStartMillis.put(liftId, context.getCurrentMillis());
            }
            approachStartMillis.putIfAbsent(liftId, context.getCurrentMillis());
        }

        final boolean activeDoorCycleAtLantern = facts.getDoorValue() > 0
                && arrivalState.isActiveForFloor(lanternFloor);
        final boolean approachLatched = approachFloors.getOrDefault(liftId, -1) == lanternFloor
                && (facts.getTargetFloor() == lanternFloor || arrivalState.isActiveForFloor(lanternFloor));

        if (!approachLatched && !activeDoorCycleAtLantern) {
            clearFinishedCycle(facts, arrivalState, liftId);
            return LiftArrivalLanternDecision.inactive();
        }

        final LiftDirection direction = resolveDirection(activeDoorCycleAtLantern, facts, arrivalState);
        if (direction == LiftDirection.NONE) {
            return LiftArrivalLanternDecision.inactive();
        }

        // 关门结束后熄灭
        if (arrivalState.isArrived() && facts.getDoorValue() <= 0) {
            return LiftArrivalLanternDecision.inactive();
        }

        final long startMillis = approachStartMillis.getOrDefault(liftId, context.getCurrentMillis());
        final LiftArrivalLanternDisplayPhase phase = activeDoorCycleAtLantern
                ? LiftArrivalLanternDisplayPhase.ARRIVED
                : LiftArrivalLanternDisplayPhase.APPROACHING;
        return LiftArrivalLanternDecision.active(direction, phase, SLOW_FLASH, SOUND_CUE,
                startMillis, startMillis);
    }

    private LiftDirection resolveDirection(boolean activeDoorCycleAtLantern,
            LiftDisplayState facts, LiftArrivalLanternState arrivalState) {
        LiftDirection direction = activeDoorCycleAtLantern
                ? arrivalState.getDirection() : facts.getPlannedArrivalDirection();
        if (direction == LiftDirection.NONE) {
            direction = arrivalState.getDirection();
        }
        if (direction == LiftDirection.NONE) {
            direction = facts.getPlannedArrivalDirection();
        }
        return direction;
    }

    private void clearFinishedCycle(LiftDisplayState facts, LiftArrivalLanternState arrivalState, long liftId) {
        final Integer approachFloor = approachFloors.get(liftId);
        if (!facts.isDoorCycle() && (approachFloor == null
                || !arrivalState.isActiveForFloor(approachFloor) && facts.getTargetFloor() != approachFloor)) {
            approachFloors.remove(liftId);
            approachStartMillis.remove(liftId);
        }
    }
}
