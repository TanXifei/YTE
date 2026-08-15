package top.xfunny.mod.packet;

import org.mtr.mapping.holder.MinecraftServer;
import org.mtr.mapping.holder.ServerPlayerEntity;
import org.mtr.mapping.registry.PacketHandler;
import org.mtr.mapping.tool.PacketBufferReceiver;
import org.mtr.mapping.tool.PacketBufferSender;
import org.mtr.core.data.Lift;
import org.mtr.mod.client.MinecraftClientData;
import top.xfunny.mod.LiftDisplayDirectionState;
import top.xfunny.mod.LiftDoorControlState;

public final class PacketLiftDoorControl extends PacketHandler {

    private final long liftId;
    private final LiftDoorControlState.Command command;

    public PacketLiftDoorControl(PacketBufferReceiver packetBufferReceiver) {
        liftId = packetBufferReceiver.readLong();
        command = packetBufferReceiver.readBoolean()
                ? LiftDoorControlState.Command.OPEN
                : LiftDoorControlState.Command.CLOSE;
    }

    public PacketLiftDoorControl(long liftId, LiftDoorControlState.Command command) {
        this.liftId = liftId;
        this.command = command;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(liftId);
        packetBufferSender.writeBoolean(command == LiftDoorControlState.Command.OPEN);
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
            LiftDisplayDirectionState.get(liftId).resetForIdleDoorCycle();
        }
    }
}
