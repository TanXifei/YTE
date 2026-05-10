package top.xfunny.mod.client.view;

import org.mtr.mapping.holder.*;
import org.mtr.mod.Init;
import org.mtr.mod.block.IBlock;
import org.mtr.mod.render.QueuedRenderLayer;
import top.xfunny.mod.client.InitClient;
import top.xfunny.mod.keymapping.DefaultButtonsKeyMapping;
import top.xfunny.mod.packet.PacketLanternSoundInstruction;
import top.xfunny.mod.util.TransformPositionX;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.mtr.mapping.mapper.DirectionHelper.FACING;

public class ButtonView extends ImageView {

    private static final Map<String, Boolean> LAST_ACTIVE_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Long> CLICK_TIME_MAP = new ConcurrentHashMap<>();
    private static final long CLICK_COOLDOWN = 200; // 0.2 秒
    private int hoverColor;
    private int pressedColor;
    private int defaultColor;
    private boolean isFocused;
    private boolean isPressed; // 仅由 activate/reset 控制
    private boolean isAlwaysOn;
    private DefaultButtonsKeyMapping keyMapping;

    // ===============================
    // 🔒 工业级点击冷却系统
    // ===============================
    private final float[] location;
    private final float[] dimension;
    private final float[] uv;
    private boolean wasUseKeyDown = false; // 边缘检测（实例级）

    private String lanternSoundInstruction = null;
    private String buttonSound = null;

    public ButtonView() {
        location = new float[2];
        dimension = new float[2];
        uv = new float[]{1, 1, 0, 0};
    }

    public void setBasicsAttributes(World world, BlockPos blockPos, DefaultButtonsKeyMapping keyMapping) {
        this.keyMapping = keyMapping;
        super.setBasicsAttributes(world, blockPos);
    }

    public void setBasicsAttributes(World world, BlockPos blockPos) {
        super.setBasicsAttributes(world, blockPos);
    }

    @Override
    public void render() {

        location[0] = x;
        location[1] = y;
        dimension[0] = width;
        dimension[1] = height;

        final MinecraftClient client = MinecraftClient.getInstance();
        final HitResult hitResult = client.getCrosshairTargetMapped();

        if (hitResult != null && keyMapping != null && world != null && blockPos != null) {

            keyMapping.registerButton(id, location, dimension);

            BlockState blockState = world.getBlockState(blockPos);
            Direction facing = IBlock.getStatePropertySafe(blockState, FACING);

            final Vector3d hitLocation = hitResult.getPos();

            final String inButton = keyMapping.mapping(
                    TransformPositionX.transform(
                            MathHelper.fractionalPart(hitLocation.getXMapped()),
                            MathHelper.fractionalPart(hitLocation.getZMapped()),
                            facing
                    ),
                    MathHelper.fractionalPart(hitLocation.getYMapped())
            );

            final boolean inBlock = Init.newBlockPos(
                    hitLocation.getXMapped(),
                    hitLocation.getYMapped(),
                    hitLocation.getZMapped()
            ).equals(blockPos);

            isFocused = inBlock && Objects.equals(inButton, id);

            // ============================================
            // 🔊 稳定物理点击系统（防机关枪 + 防实例重建）
            // ============================================

            boolean isUseKeyDown = client.getOptionsMapped().getKeyUseMapped().isPressed();
            long now = System.currentTimeMillis();

            if (isFocused && isUseKeyDown && !wasUseKeyDown) {

                String clickKey = blockPos.getX() + "_" +
                        blockPos.getY() + "_" +
                        blockPos.getZ();

                Long lastTime = CLICK_TIME_MAP.get(clickKey);

                if (lastTime == null || now - lastTime > CLICK_COOLDOWN) {

                    if (buttonSound != null) {
                        InitClient.REGISTRY_CLIENT.sendPacketToServer(
                                new PacketLanternSoundInstruction(blockPos, buttonSound)
                        );
                    }

                    CLICK_TIME_MAP.put(clickKey, now);
                }
            }

            wasUseKeyDown = isUseKeyDown;
        }

        // ============================================
        // 🛗 到站灯逻辑（完全保持原结构）
        // ============================================

        if (isFocused || isPressed || isAlwaysOn) {
            setQueuedRenderLayer(QueuedRenderLayer.LIGHT_TRANSLUCENT);
        }

        setUv(uv);

        color = isPressed
                ? pressedColor
                : isFocused
                  ? hoverColor
                  : defaultColor;

        super.render();
    }

    // ============================================
    // 🛗 电梯逻辑控制（服务器调用）
    // ============================================

    public void activate() {
        isPressed = true;

        if (lanternSoundInstruction == null || world == null || blockPos == null) {
            return;
        }

        final String key = makeSoundKey();
        if (key == null) return;

        Boolean already = LAST_ACTIVE_MAP.get(key);
        if (already != null && already) return;

        InitClient.REGISTRY_CLIENT.sendPacketToServer(
                new PacketLanternSoundInstruction(blockPos, lanternSoundInstruction)
        );

        LAST_ACTIVE_MAP.put(key, true);
    }

    public void resetLanternSound() {
        isPressed = false;

        final String key = makeSoundKey();
        if (key == null) return;

        LAST_ACTIVE_MAP.put(key, false);
    }

    private String makeSoundKey() {
        if (world == null || blockPos == null || lanternSoundInstruction == null) return null;
        return blockPos.getX() + "_" +
                blockPos.getY() + "_" +
                blockPos.getZ() + "_" +
                lanternSoundInstruction;
    }

    // ============================================
    // setter 区
    // ============================================

    public void setLanternSound(String soundInstruction) {
        this.lanternSoundInstruction = soundInstruction;
    }

    public void setButtonSound(String sound) {
        this.buttonSound = sound;
    }

    public void setDefaultColor(int defaultColor, boolean isAlwaysOn) {
        this.defaultColor = defaultColor;
        this.isAlwaysOn = isAlwaysOn;
    }

    public void setDefaultColor(int defaultColor) {
        this.defaultColor = defaultColor;
    }

    public void setHoverColor(int hoverColor) {
        this.hoverColor = hoverColor;
    }

    public void setPressedColor(int pressedColor) {
        this.pressedColor = pressedColor;
    }

    public void setFlip(boolean flipVertical, boolean flipHorizontal) {
        if (flipVertical) {
            final float tempV = uv[0];
            uv[0] = uv[2];
            uv[2] = tempV;
        }
        if (flipHorizontal) {
            final float tempU = uv[1];
            uv[1] = uv[3];
            uv[3] = tempU;
        }
    }
}