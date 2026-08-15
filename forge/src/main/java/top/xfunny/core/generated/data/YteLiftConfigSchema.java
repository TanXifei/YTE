package top.xfunny.core.generated.data;

import org.mtr.core.serializer.ReaderBase;
import org.mtr.core.serializer.SerializedDataBaseWithId;
import org.mtr.core.serializer.WriterBase;

public abstract class YteLiftConfigSchema implements SerializedDataBaseWithId {

    protected long liftId;
    protected double speed;
    protected double acceleration;
    protected double adoDistance;
    protected double levellingDistance;
    protected double levellingSpeed;

    private static final String KEY_LIFT_ID = "lift_id";
    private static final String KEY_SPEED = "speed";
    private static final String KEY_ACCELERATION = "acceleration";
    private static final String KEY_ADO_DISTANCE = "ado_distance";
    private static final String KEY_LEVELLING_DISTANCE = "levelling_distance";
    private static final String KEY_LEVELLING_SPEED = "levelling_speed";

    public static final double DEFAULT_SPEED = 10.0;
    public static final double DEFAULT_ACCELERATION = 4.0;
    public static final double DEFAULT_ADO_DISTANCE = 0;
    public static final double DEFAULT_LEVELLING_DISTANCE = 0.3;
    public static final double DEFAULT_LEVELLING_SPEED = 0.2;
    public static final double MIN_SPEED = 0.1;
    public static final double MAX_SPEED = 20.0;
    public static final double MIN_ACCELERATION = 0.1;
    public static final double MAX_ACCELERATION = 10.0;
    public static final double MIN_ADO_DISTANCE = 0;
    public static final double MAX_ADO_DISTANCE = 2;
    public static final double MIN_LEVELLING_DISTANCE = 0;
    public static final double MAX_LEVELLING_DISTANCE = 5;
    public static final double MIN_LEVELLING_SPEED = 0;
    public static final double MAX_LEVELLING_SPEED = 5;
    public static final double STEP = 0.5;

    protected YteLiftConfigSchema(long liftId, double speed, double acceleration, double adoDistance, double levellingDistance, double levellingSpeed) {
        this.liftId = liftId;
        this.speed = speed;
        this.acceleration = acceleration;
        this.adoDistance = adoDistance;
        this.levellingDistance = levellingDistance;
        this.levellingSpeed = levellingSpeed;
    }

    protected YteLiftConfigSchema(ReaderBase readerBase) {
        speed = DEFAULT_SPEED;
        acceleration = DEFAULT_ACCELERATION;
        adoDistance = DEFAULT_ADO_DISTANCE;
        levellingDistance = DEFAULT_LEVELLING_DISTANCE;
        levellingSpeed = DEFAULT_LEVELLING_SPEED;
        updateData(readerBase);
    }

    @Override
    public void updateData(ReaderBase readerBase) {
        readerBase.unpackLong(KEY_LIFT_ID, value -> liftId = value);
        readerBase.unpackDouble(KEY_SPEED, value -> speed = value);
        readerBase.unpackDouble(KEY_ACCELERATION, value -> acceleration = value);
        readerBase.unpackDouble(KEY_ADO_DISTANCE, value -> adoDistance = value);
        readerBase.unpackDouble(KEY_LEVELLING_DISTANCE, value -> levellingDistance = value);
        readerBase.unpackDouble(KEY_LEVELLING_SPEED, value -> levellingSpeed = value);
    }

    @Override
    public void serializeData(WriterBase writerBase) {
        writerBase.writeLong(KEY_LIFT_ID, liftId);
        writerBase.writeDouble(KEY_SPEED, speed);
        writerBase.writeDouble(KEY_ACCELERATION, acceleration);
        writerBase.writeDouble(KEY_ADO_DISTANCE, adoDistance);
        writerBase.writeDouble(KEY_LEVELLING_DISTANCE, levellingDistance);
        writerBase.writeDouble(KEY_LEVELLING_SPEED, levellingSpeed);
    }

    @Override
    public String getHexId() {
        return Long.toHexString(liftId);
    }

    @Override
    public boolean isValid() {
        return liftId != 0
                && speed >= MIN_SPEED && speed <= MAX_SPEED
                && acceleration >= MIN_ACCELERATION && acceleration <= MAX_ACCELERATION
                && adoDistance >= MIN_ADO_DISTANCE && adoDistance <= MAX_ADO_DISTANCE
                && levellingDistance >= MIN_LEVELLING_DISTANCE && levellingDistance <= MAX_LEVELLING_DISTANCE
                && levellingSpeed >= MIN_LEVELLING_SPEED && levellingSpeed <= MAX_LEVELLING_SPEED;
    }

    public long getId() {
        return liftId;
    }
}
