package top.xfunny.mod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.core.data.Position;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.Registry;
import top.xfunny.mod.packet.PacketLanternSoundInstruction;
import top.xfunny.mod.packet.PacketYTEOpenBlockEntityScreen;
import top.xfunny.mod.packet.PacketUpdatePATRS01RailwaySignConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Init {
    public static final String MOD_ID = "yte";
    public static final Logger LOGGER = LogManager.getLogger("Yunzhu Transit Extension");
    public static final Registry REGISTRY = new Registry();
    public static int HAS_UPDATE = -1;


    public static void init() {
        long startTime = System.currentTimeMillis();
        Map<String, Runnable> initSteps = new LinkedHashMap<>();

        //UpdateCheckerUtil.init();
        initSteps.put("Sound Events", SoundEvents::init);
        initSteps.put("Blocks", Blocks::init);
        initSteps.put("Block Entity Types", BlockEntityTypes::init);
        initSteps.put("Items", Items::init);
        initSteps.put("Creative Mode Tabs", CreativeModeTabs::init);
        initSteps.put("MTR Packet", () -> {
            REGISTRY.setupPackets(new Identifier(MOD_ID, "packet"));
            REGISTRY.registerPacket(PacketYTEOpenBlockEntityScreen.class, PacketYTEOpenBlockEntityScreen::new);
            REGISTRY.registerPacket(PacketUpdatePATRS01RailwaySignConfig.class, PacketUpdatePATRS01RailwaySignConfig::new);
            REGISTRY.registerPacket(PacketLanternSoundInstruction.class, PacketLanternSoundInstruction::new);
            REGISTRY.init();
        });

        int currentStep = 1;

        for (Map.Entry<String, Runnable> step : initSteps.entrySet()) {
            LOGGER.info("Registering {} ({}/{})", step.getKey(), currentStep, initSteps.size());
            step.getValue().run();
            currentStep++;
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        LOGGER.info("Yunzhu Transit Extension initialized successfully in {} ms.", duration);
    }

    public static Position blockPosToPosition(BlockPos blockPos) {
        return new Position(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static BlockPos positionToBlockPos(Position position) {
        return new BlockPos((int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
}

