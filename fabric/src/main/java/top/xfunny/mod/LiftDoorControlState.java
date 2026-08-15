package top.xfunny.mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LiftDoorControlState {

    public enum Command {
        OPEN,
        CLOSE
    }

    private static final Map<Long, Command> PENDING_COMMANDS = new ConcurrentHashMap<>();

    private LiftDoorControlState() {
    }

    public static void request(long liftId, Command command) {
        PENDING_COMMANDS.put(liftId, command);
    }

    public static Command consume(long liftId) {
        return PENDING_COMMANDS.remove(liftId);
    }
}
