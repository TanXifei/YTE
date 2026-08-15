package top.xfunny.mod.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局电梯配置存储，供 Mixin 快速访问
 * 由 YteSimulator (服务端) 和 YteData (客户端) 各自填充
 */
public final class YteLiftConfigStore {

    private static final Map<Long, Double> speedMap = new ConcurrentHashMap<>();
    private static final Map<Long, Double> accelerationMap = new ConcurrentHashMap<>();
    private static final Map<Long, Double> adoDistanceMap = new ConcurrentHashMap<>();
    private static final Map<Long, Double> levellingDistanceMap = new ConcurrentHashMap<>();
    private static final Map<Long, Double> levellingSpeedMap = new ConcurrentHashMap<>();

    private static final double DEFAULT_SPEED = 10.0;
    private static final double DEFAULT_ACCELERATION = 4.0;
    private static final double DEFAULT_ADO_DISTANCE = 0;
    private static final double DEFAULT_LEVELLING_DISTANCE = 0.3;
    private static final double DEFAULT_LEVELLING_SPEED = 0.2;

    private YteLiftConfigStore() {}

    public static void put(long liftId, double speed, double acceleration, double adoDistance, double levellingDistance, double levellingSpeed) {
        speedMap.put(liftId, speed);
        accelerationMap.put(liftId, acceleration);
        adoDistanceMap.put(liftId, adoDistance);
        levellingDistanceMap.put(liftId, levellingDistance);
        levellingSpeedMap.put(liftId, levellingSpeed);
    }

    public static double getSpeed(long liftId) {
        return speedMap.getOrDefault(liftId, DEFAULT_SPEED);
    }

    public static double getAcceleration(long liftId) {
        return accelerationMap.getOrDefault(liftId, DEFAULT_ACCELERATION);
    }

    public static double getAdoDistance(long liftId) { return adoDistanceMap.getOrDefault(liftId, DEFAULT_ADO_DISTANCE); }

    public static double getLevellingDistance(long liftId) { return levellingDistanceMap.getOrDefault(liftId, DEFAULT_LEVELLING_DISTANCE); }

    public static double getLevellingSpeed(long liftId) { return levellingSpeedMap.getOrDefault(liftId, DEFAULT_LEVELLING_SPEED); }

    public static void remove(long liftId) {
        speedMap.remove(liftId);
        accelerationMap.remove(liftId);
        adoDistanceMap.remove(liftId);
        levellingDistanceMap.remove(liftId);
        levellingSpeedMap.remove(liftId);
    }

    public static void clear() {
        speedMap.clear();
        accelerationMap.clear();
        adoDistanceMap.clear();
        levellingDistanceMap.clear();
        levellingSpeedMap.clear();
    }
}
