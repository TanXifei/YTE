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
 * 日立 GHL-668/673/820 到站灯策略（模式1：无及时预报）。
 * 电梯运行中、按运动方向距本层还剩不超过 2 层时开始闪烁（400 亮 / 400 熄）
 * 并播报一次提示音，保持闪烁直到关门结束。
 */
public final class HitachiGHLLanternPolicy implements LiftArrivalLanternPolicy {

    public static final HitachiGHLLanternPolicy GHL668 = new HitachiGHLLanternPolicy("hitachi_ca_lantern_2");
    public static final HitachiGHLLanternPolicy GHL673 = new HitachiGHLLanternPolicy("hitachi_ca_lantern_1");
    public static final HitachiGHLLanternPolicy GHL820 = new HitachiGHLLanternPolicy("hitachi_ca_lantern_1");

    private static final LiftArrivalLanternFlashPattern FLASH = LiftArrivalLanternFlashPattern.flashing(400, 400);
    private static final int APPROACH_FLOORS = 2;

    private final String soundCue;
    private final Map<Long, Integer> approachFloors = new ConcurrentHashMap<>();
    private final Map<Long, Long> approachStartMillis = new ConcurrentHashMap<>();

    private HitachiGHLLanternPolicy(String soundCue) {
        this.soundCue = soundCue;
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

        // 提前 2 层触发：电梯运行中、按运动方向距本层还剩 1~2 层
        final int remaining = remainingFloors(facts, lanternFloor);
        if (remaining >= 1 && remaining <= APPROACH_FLOORS && facts.isMoving()) {
            final Integer previous = approachFloors.put(liftId, lanternFloor);
            if (previous == null || previous != lanternFloor) {
                approachStartMillis.put(liftId, context.getCurrentMillis());
            }
            approachStartMillis.putIfAbsent(liftId, context.getCurrentMillis());
        }

        final boolean activeDoorCycleAtLantern = facts.getDoorValue() > 0
                && arrivalState.isActiveForFloor(lanternFloor);
        final boolean approachLatched = approachFloors.getOrDefault(liftId, -1) == lanternFloor
                && (remaining >= 0 || arrivalState.isActiveForFloor(lanternFloor));

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
        return LiftArrivalLanternDecision.active(direction, phase, FLASH, soundCue,
                startMillis, startMillis);
    }

    /** 按运动方向计算距离本层的剩余楼层数；非朝本层运动返回 -1。 */
    private int remainingFloors(LiftDisplayState facts, int lanternFloor) {
        final int currentFloor = facts.getExactFloor() >= 0
                ? facts.getExactFloor() : facts.getDisplayedFloor();
        if (currentFloor < 0) {
            return -1;
        }
        switch (facts.getMovementDirection()) {
            case UP:
                return lanternFloor - currentFloor;
            case DOWN:
                return currentFloor - lanternFloor;
            default:
                return -1;
        }
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
        if (direction == LiftDirection.NONE) {
            direction = facts.getMovementDirection();
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
