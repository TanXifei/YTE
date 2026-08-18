package top.xfunny.mod;

import org.mtr.core.data.LiftDirection;

@FunctionalInterface
public interface DisplayDirectionPolicy {
    LiftDirection getDirection(LiftDisplayState state);
}
