package top.xfunny.mod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mtr.core.data.Position;
import org.mtr.mapping.holder.BlockPos;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.Registry;
import top.xfunny.mod.packet.PacketLanternSoundInstruction;
import top.xfunny.mod.packet.PacketUpdatePATRS01RailwaySignConfig;
import top.xfunny.mod.packet.PacketYTEOpenBlockEntityScreen;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Init {
    public static final String MOD_ID = "yte";
    public static final Logger LOGGER = LogManager.getLogger("Yunzhu Transit Extension");
    public static final Registry REGISTRY = new Registry();
    public static int HAS_UPDATE = -1;

    private static final String[] LOGO = {
            "__  __                  __         ______                      _ __  ______     __                  _           ",
            "\\ \\/ /_  ______  ____  / /_  __  _/_  __/________ _____  _____(_) /_/ ____/  __/ /____  ____  _____(_)___  ____ ",
            " \\  / / / / __ \\/_  / / __ \\/ / / // / / ___/ __ `/ __ \\/ ___/ / __/ __/ | |/_/ __/ _ \\/ __ \\/ ___/ / __ \\/ __ \\",
            " / / /_/ / / / / / /_/ / / / /_/ // / / /  / /_/ / / / (__  ) / /_/ /____>  </ /_/  __/ / / (__  ) / /_/ / / / /",
            "/_/\\__,_/_/ /_/ /___/_/ /_/\\__,_//_/ /_/   \\__,_/_/ /_/____/_/\\__/_____/_/|_|\\__/\\___/_/ /_/____/_/\\____/_/ /_/ "
    };
    private static final int LOGO_WIDTH = Arrays.stream(LOGO).mapToInt(String::length).max().orElse(0);

    public static void init() {
        for (String line : LOGO) LOGGER.info(line);
        char SEPARATOR_CHAR = '─';
        String versionTag = " " + Keys.MOD_VERSION + " ";
        String suffix = String.valueOf(SEPARATOR_CHAR);
        char[] barChars = new char[Math.max(0, LOGO_WIDTH - versionTag.length() - suffix.length())];
        Arrays.fill(barChars, SEPARATOR_CHAR);
        LOGGER.info("{}{}{}", new String(barChars), versionTag, suffix);
        long startTime = System.currentTimeMillis();
        Map<String, Runnable> initSteps = new LinkedHashMap<>();

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
        });

        int currentStep = 1;
        for (Map.Entry<String, Runnable> step : initSteps.entrySet()) {
            LOGGER.info("Registering {} ({}/{})", step.getKey(), currentStep, initSteps.size());
            step.getValue().run();
            currentStep++;
        }

        LOGGER.info("Yunzhu Transit Extension initialized successfully in {} ms.", System.currentTimeMillis() - startTime);
        REGISTRY.init();
    }

    public static Position blockPosToPosition(BlockPos blockPos) {
        return new Position(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static BlockPos positionToBlockPos(Position position) {
        return new BlockPos((int) position.getX(), (int) position.getY(), (int) position.getZ());
    }
}

