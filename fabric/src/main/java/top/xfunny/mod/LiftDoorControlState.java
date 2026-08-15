package top.xfunny.mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LiftDoorControlState {

    private static final long CLIENT_OPEN_PREDICTION_TIMEOUT = 3000;

    public enum Command {
        OPEN,
        CLOSE
    }

    private static final Map<Long, Command> PENDING_COMMANDS = new ConcurrentHashMap<>();
    private static final Map<Long, ClientOpenPrediction> CLIENT_OPEN_PREDICTIONS = new ConcurrentHashMap<>();

    private LiftDoorControlState() {
    }

    public static void request(long liftId, Command command) {
        PENDING_COMMANDS.put(liftId, command);
    }

    public static Command consume(long liftId) {
        return PENDING_COMMANDS.remove(liftId);
    }

    public static void beginClientOpenPrediction(long liftId, float doorValue) {
        CLIENT_OPEN_PREDICTIONS.put(liftId, new ClientOpenPrediction(doorValue,
                System.currentTimeMillis() + CLIENT_OPEN_PREDICTION_TIMEOUT));
    }

    public static float preserveClientOpenDoorValue(long liftId, float doorValue) {
        final ClientOpenPrediction prediction = CLIENT_OPEN_PREDICTIONS.get(liftId);
        if (prediction == null) {
            return doorValue;
        }
        if (System.currentTimeMillis() > prediction.expiresAt) {
            CLIENT_OPEN_PREDICTIONS.remove(liftId, prediction);
            return doorValue;
        }
        prediction.doorValue = Math.max(prediction.doorValue, doorValue);
        return prediction.doorValue;
    }

    public static long reconcileClientOpenCoolDown(long liftId, long serverCoolDown,
            long stoppingTime, long singleDoorMoveTime) {
        final ClientOpenPrediction prediction = CLIENT_OPEN_PREDICTIONS.remove(liftId);
        if (prediction == null || System.currentTimeMillis() > prediction.expiresAt) {
            return serverCoolDown;
        }

        // The client starts opening immediately, while the server starts only
        // after receiving the button packet. Never let that later authoritative
        // start rewind the already visible opening progress.
        final long predictedCoolDown = stoppingTime
                - Math.round(Math.max(0, Math.min(prediction.doorValue, 1)) * singleDoorMoveTime);
        return Math.min(serverCoolDown, predictedCoolDown);
    }

    private static final class ClientOpenPrediction {
        private float doorValue;
        private final long expiresAt;

        private ClientOpenPrediction(float doorValue, long expiresAt) {
            this.doorValue = doorValue;
            this.expiresAt = expiresAt;
        }
    }
}
