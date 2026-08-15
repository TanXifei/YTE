package top.xfunny.mod.packet;

import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.core.data.Lift;
import org.mtr.core.data.Vehicle;
import org.mtr.mod.client.MinecraftClientData;
import top.xfunny.mixin.MixinLiftSchema;
import top.xfunny.mod.LiftDisplayDirectionState;
import top.xfunny.mod.LiftDoorControlState;

public final class PacketLiftDoorControl extends PacketHandler {

    private final long liftId;
    private final LiftDoorControlState.Command command;
    private final long stoppingCoolDown;
    private final boolean resetIdleDirection;

    public PacketLiftDoorControl(PacketBufferReceiver packetBufferReceiver) {
        liftId = packetBufferReceiver.readLong();
        command = packetBufferReceiver.readBoolean()
                ? LiftDoorControlState.Command.OPEN
                : LiftDoorControlState.Command.CLOSE;
        stoppingCoolDown = packetBufferReceiver.readLong();
        resetIdleDirection = packetBufferReceiver.readBoolean();
    }

    public PacketLiftDoorControl(long liftId, LiftDoorControlState.Command command) {
        this.liftId = liftId;
        this.command = command;
        stoppingCoolDown = -1;
        resetIdleDirection = false;
    }

    public PacketLiftDoorControl(long liftId, LiftDoorControlState.Command command,
            long stoppingCoolDown, boolean resetIdleDirection) {
        this.liftId = liftId;
        this.command = command;
        this.stoppingCoolDown = stoppingCoolDown;
        this.resetIdleDirection = resetIdleDirection;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(liftId);
        packetBufferSender.writeBoolean(command == LiftDoorControlState.Command.OPEN);
        packetBufferSender.writeLong(stoppingCoolDown);
        packetBufferSender.writeBoolean(resetIdleDirection);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity) {
        LiftDoorControlState.request(liftId, command);
    }

    @Override
    public void runClient() {
        if (command != LiftDoorControlState.Command.OPEN) {
            return;
        }
        final Lift lift = MinecraftClientData.getLift(liftId);
        if (lift != null) {
            if (stoppingCoolDown >= 0) {
                ((MixinLiftSchema) lift).setStoppingCoolDown(LiftDoorControlState.reconcileClientOpenCoolDown(
                        liftId, stoppingCoolDown, Vehicle.DOOR_MOVE_TIME + 2500, Vehicle.DOOR_MOVE_TIME / 2));
            }
            if (resetIdleDirection) {
                LiftDisplayDirectionState.get(liftId).resetForIdleDoorCycle();
            }
        }
    }
}
