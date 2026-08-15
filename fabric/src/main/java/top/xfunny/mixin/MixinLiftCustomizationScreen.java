package top.xfunny.mixin;

import org.mtr.core.data.Lift;
import org.mtr.mapping.holder.ClickableWidget;
import org.mtr.mapping.holder.Text;
import org.mtr.mapping.mapper.ButtonWidgetExtension;
import org.mtr.mapping.mapper.GraphicsHolder;
import org.mtr.mapping.mapper.GuiDrawing;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mapping.mapper.TextFieldWidgetExtension;
import org.mtr.mapping.tool.TextCase;
import org.mtr.mod.data.IGui;
import org.mtr.mod.screen.LiftCustomizationScreen;
import org.mtr.mod.screen.MTRScreenBase;
import org.mtr.mod.screen.WidgetShorterSlider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.xfunny.core.data.YteLiftConfig;
import top.xfunny.core.operation.YteUpdateDataRequest;
import top.xfunny.mod.client.InitClient;
import top.xfunny.mod.client.YteMinecraftClientData;
import top.xfunny.mod.config.YteLiftConfigStore;
import top.xfunny.mod.packet.YtePacketUpdateData;

import java.util.Locale;

@Mixin(value = LiftCustomizationScreen.class, remap = false)
public abstract class MixinLiftCustomizationScreen extends MTRScreenBase {

    @Shadow
    @Final
    private Lift lift;

    @Shadow
    private int width2;

    @Unique
    private WidgetShorterSlider yte$sliderSpeed;

    @Unique
    private WidgetShorterSlider yte$sliderAcceleration;

    @Unique private WidgetShorterSlider yte$sliderAdoDistance;
    @Unique private WidgetShorterSlider yte$sliderLevellingDistance;
    @Unique private WidgetShorterSlider yte$sliderLevellingSpeed;
    @Unique private ButtonWidgetExtension yte$professionalModeButton;
    @Unique private TextFieldWidgetExtension yte$speedField;
    @Unique private TextFieldWidgetExtension yte$accelerationField;
    @Unique private TextFieldWidgetExtension yte$adoDistanceField;
    @Unique private TextFieldWidgetExtension yte$levellingDistanceField;
    @Unique private TextFieldWidgetExtension yte$levellingSpeedField;

    @Unique
    private static final int SPEED_SLIDER_MAX = 40;

    @Unique
    private static final int ACCEL_SLIDER_MAX = 20;

    @Unique private static final int ADO_DISTANCE_SLIDER_MAX = 30;
    @Unique private static final int LEVELLING_DISTANCE_SLIDER_MAX = 20;
    @Unique private static final int LEVELLING_SPEED_SLIDER_MAX = 20;
    @Unique private static boolean yte$professionalMode;

    @Unique
    private double yte$lastSentSpeed = -1;

    @Unique
    private double yte$lastSentAccel = -1;
    @Unique private double yte$lastSentAdoDistance = -1;
    @Unique private double yte$lastSentLevellingDistance = -1;
    @Unique private double yte$lastSentLevellingSpeed = -1;
    @Unique private final double[] yte$easyModeValues = new double[5];
    @Unique private final int[] yte$easyModeSliderAnchors = new int[5];
    @Unique private final boolean[] yte$easyModeSliderTouched = new boolean[5];
    @Unique private double yte$scrollOffset;
    @Unique private boolean yte$contentTransformPushed;
    @Unique private boolean yte$scrollbarDragging;
    @Unique private double yte$scrollbarDragOffset;

    @Unique private static final int YTE_CONTENT_ROWS = 22;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstructed(Lift liftParam, CallbackInfo ci) {
        final long liftId = liftParam.getId();
        final YteLiftConfig config = YteMinecraftClientData.getInstance().getConfig(liftId);

        final double currentSpeed = config != null ? config.getSpeed() : YteLiftConfig.DEFAULT_SPEED;
        final double currentAccel = config != null ? config.getAcceleration() : YteLiftConfig.DEFAULT_ACCELERATION;
        final double currentAdoDistance = config != null ? config.getAdoDistance() : YteLiftConfig.DEFAULT_ADO_DISTANCE;
        final double currentLevellingDistance = config != null ? config.getLevellingDistance() : YteLiftConfig.DEFAULT_LEVELLING_DISTANCE;
        final double currentLevellingSpeed = config != null ? config.getLevellingSpeed() : YteLiftConfig.DEFAULT_LEVELLING_SPEED;

        // 不显示内置值文字，由 render 手绘
        yte$sliderSpeed = new WidgetShorterSlider(0, 60, SPEED_SLIDER_MAX,
                value -> "", null);
        yte$sliderSpeed.setValue(speedToValue(currentSpeed));

        yte$sliderAcceleration = new WidgetShorterSlider(0, 60, ACCEL_SLIDER_MAX,
                value -> "", null);
        yte$sliderAcceleration.setValue(accelToValue(currentAccel));

        yte$sliderAdoDistance = new WidgetShorterSlider(0, 60, ADO_DISTANCE_SLIDER_MAX,
                value -> "", null);
        yte$sliderAdoDistance.setValue(adoDistanceToValue(currentAdoDistance));

        yte$sliderLevellingDistance = new WidgetShorterSlider(0, 60, LEVELLING_DISTANCE_SLIDER_MAX,
                value -> "", null);
        yte$sliderLevellingDistance.setValue(levellingDistanceToValue(currentLevellingDistance));

        yte$sliderLevellingSpeed = new WidgetShorterSlider(0, 60, LEVELLING_SPEED_SLIDER_MAX,
                value -> "", null);
        yte$sliderLevellingSpeed.setValue(levellingSpeedToValue(currentLevellingSpeed));

        yte$setEasyModeValues(currentSpeed, currentAccel, currentAdoDistance, currentLevellingDistance, currentLevellingSpeed);

        yte$professionalModeButton = new ButtonWidgetExtension(0, 0, 0, IGui.SQUARE_SIZE,
                TextHelper.literal(""), button -> yte$toggleProfessionalMode());

        yte$speedField = yte$createNumberField(currentSpeed);
        yte$accelerationField = yte$createNumberField(currentAccel);
        yte$adoDistanceField = yte$createNumberField(currentAdoDistance);
        yte$levellingDistanceField = yte$createNumberField(currentLevellingDistance);
        yte$levellingSpeedField = yte$createNumberField(currentLevellingSpeed);

        yte$lastSentSpeed = currentSpeed;
        yte$lastSentAccel = currentAccel;
        yte$lastSentAdoDistance = currentAdoDistance;
        yte$lastSentLevellingDistance = currentLevellingDistance;
        yte$lastSentLevellingSpeed = currentLevellingSpeed;
    }

    @Inject(method = "init2", at = @At("TAIL"))
    private void onInit2(CallbackInfo ci) {
        // 与原版全宽控件对齐：x=0, width=width2
        // Speed: row 11 文字, row 12 滑块
        // Accel: row 13 文字, row 14 滑块
        final int sliderY1 = IGui.SQUARE_SIZE * 13;
        final int sliderY2 = IGui.SQUARE_SIZE * 15;

        yte$professionalModeButton.setX2(0);
        yte$professionalModeButton.setY2(IGui.SQUARE_SIZE * 11);
        yte$professionalModeButton.setWidth2(width2);

        yte$sliderSpeed.setX2(0);
        yte$sliderSpeed.setY2(sliderY1);
        yte$sliderSpeed.setHeight(IGui.SQUARE_SIZE);
        yte$sliderSpeed.setWidth2(width2);

        yte$sliderAcceleration.setX2(0);
        yte$sliderAcceleration.setY2(sliderY2);
        yte$sliderAcceleration.setHeight(IGui.SQUARE_SIZE);
        yte$sliderAcceleration.setWidth2(width2);

        yte$positionSlider(yte$sliderAdoDistance, 17);
        yte$positionSlider(yte$sliderLevellingDistance, 19);
        yte$positionSlider(yte$sliderLevellingSpeed, 21);

        addChild(new ClickableWidget(yte$professionalModeButton));
        addChild(new ClickableWidget(yte$sliderSpeed));
        addChild(new ClickableWidget(yte$sliderAcceleration));
        addChild(new ClickableWidget(yte$sliderAdoDistance));
        addChild(new ClickableWidget(yte$sliderLevellingDistance));
        addChild(new ClickableWidget(yte$sliderLevellingSpeed));

        yte$positionField(yte$speedField, 13);
        yte$positionField(yte$accelerationField, 15);
        yte$positionField(yte$adoDistanceField, 17);
        yte$positionField(yte$levellingDistanceField, 19);
        yte$positionField(yte$levellingSpeedField, 21);
        addChild(new ClickableWidget(yte$speedField));
        addChild(new ClickableWidget(yte$accelerationField));
        addChild(new ClickableWidget(yte$adoDistanceField));
        addChild(new ClickableWidget(yte$levellingDistanceField));
        addChild(new ClickableWidget(yte$levellingSpeedField));

        // Text fields are recreated by the screen initialization lifecycle.
        // Restore their visible text after they have been attached to the screen.
        yte$syncFieldsFromValues(yte$lastSentSpeed, yte$lastSentAccel, yte$lastSentAdoDistance,
                yte$lastSentLevellingDistance, yte$lastSentLevellingSpeed);
        yte$updateModeWidgets();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/screen/MTRScreenBase;render(Lorg/mtr/mapping/mapper/GraphicsHolder;IIF)V", shift = At.Shift.BEFORE))
    private void yte$beginScrollableContent(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        graphicsHolder.push();
        graphicsHolder.translate(0, -yte$scrollOffset, 0);
        yte$contentTransformPushed = true;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int yte$adjustMouseYForScroll(int mouseY) {
        return mouseY + (int) yte$scrollOffset;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float delta,
            CallbackInfo ci) {
        // "Speed: X.X m/s" 文字行（滑块上方），左对齐
        final int labelY1 = IGui.SQUARE_SIZE * 12 + IGui.TEXT_PADDING;
        final int labelY2 = IGui.SQUARE_SIZE * 14 + IGui.TEXT_PADDING;

        final double speed = yte$professionalMode
                ? yte$parseNumber(yte$speedField, yte$lastSentSpeed, YteLiftConfig.MIN_SPEED, YteLiftConfig.MAX_SPEED)
                : yte$getEasyModeValue(0, yte$sliderSpeed, valueToSpeed(yte$sliderSpeed.getIntValue()));
        final double accel = yte$professionalMode
                ? yte$parseNumber(yte$accelerationField, yte$lastSentAccel, YteLiftConfig.MIN_ACCELERATION, YteLiftConfig.MAX_ACCELERATION)
                : yte$getEasyModeValue(1, yte$sliderAcceleration, valueToAccel(yte$sliderAcceleration.getIntValue()));
        final double adoDistance = yte$professionalMode
                ? yte$parseNumber(yte$adoDistanceField, yte$lastSentAdoDistance, YteLiftConfig.MAX_ADO_DISTANCE)
                : yte$getEasyModeValue(2, yte$sliderAdoDistance, valueToAdoDistance(yte$sliderAdoDistance.getIntValue()));
        final double levellingDistance = yte$professionalMode
                ? yte$parseNumber(yte$levellingDistanceField, yte$lastSentLevellingDistance, YteLiftConfig.MAX_LEVELLING_DISTANCE)
                : yte$getEasyModeValue(3, yte$sliderLevellingDistance, valueToLevellingDistance(yte$sliderLevellingDistance.getIntValue()));
        final double levellingSpeed = yte$professionalMode
                ? yte$parseNumber(yte$levellingSpeedField, yte$lastSentLevellingSpeed, YteLiftConfig.MAX_LEVELLING_SPEED)
                : yte$getEasyModeValue(4, yte$sliderLevellingSpeed, valueToLevellingSpeed(yte$sliderLevellingSpeed.getIntValue()));

        graphicsHolder.drawText(TextHelper.translatable("gui.yte.lift_speed_value", speed),
                0, labelY1, IGui.ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
        graphicsHolder.drawText(TextHelper.translatable("gui.yte.lift_acceleration_value", accel),
                0, labelY2, IGui.ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
        yte$drawModeLabel(graphicsHolder, "gui.yte.lift_ado_distance", "gui.yte.lift_ado_distance_value", adoDistance, 16);
        yte$drawModeLabel(graphicsHolder, "gui.yte.lift_levelling_distance", "gui.yte.lift_levelling_distance_value", levellingDistance, 18);
        yte$drawModeLabel(graphicsHolder, "gui.yte.lift_levelling_speed", "gui.yte.lift_levelling_speed_value", levellingSpeed, 20);

        if (speed != yte$lastSentSpeed || accel != yte$lastSentAccel
                || adoDistance != yte$lastSentAdoDistance || levellingDistance != yte$lastSentLevellingDistance
                || levellingSpeed != yte$lastSentLevellingSpeed) {
            yte$lastSentSpeed = speed;
            yte$lastSentAccel = accel;
            yte$lastSentAdoDistance = adoDistance;
            yte$lastSentLevellingDistance = levellingDistance;
            yte$lastSentLevellingSpeed = levellingSpeed;

            final long liftId = lift.getId();
            final YteLiftConfig config = new YteLiftConfig(liftId, speed, accel, adoDistance, levellingDistance, levellingSpeed);
            YteLiftConfigStore.put(liftId, speed, accel, adoDistance, levellingDistance, levellingSpeed);

            final YteUpdateDataRequest request = new YteUpdateDataRequest(
                    config, YteMinecraftClientData.getInstance());
            InitClient.REGISTRY_CLIENT.sendPacketToServer(
                    new YtePacketUpdateData(request));
        }

        if (yte$contentTransformPushed) {
            graphicsHolder.pop();
            yte$contentTransformPushed = false;
        }
        yte$drawScrollbar(graphicsHolder);
    }

    @Override
    public boolean mouseScrolled2(double mouseX, double mouseY, double amount) {
        if (mouseX >= 0 && mouseX <= width2 && yte$getMaxScroll() > 0) {
            yte$scrollOffset = Math.max(0, Math.min(yte$getMaxScroll(), yte$scrollOffset - amount * IGui.SQUARE_SIZE));
            return true;
        }
        return super.mouseScrolled2(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked2(double mouseX, double mouseY, int button) {
        if (button == 0 && yte$getMaxScroll() > 0 && mouseX >= width2 - 4 && mouseX <= width2) {
            final int thumbY = yte$getScrollbarThumbY();
            final int thumbHeight = yte$getScrollbarThumbHeight();
            yte$scrollbarDragging = true;
            yte$scrollbarDragOffset = mouseY >= thumbY && mouseY <= thumbY + thumbHeight
                    ? mouseY - thumbY : thumbHeight / 2.0;
            yte$setScrollFromThumb(mouseY - yte$scrollbarDragOffset);
            return true;
        }
        return super.mouseClicked2(mouseX, mouseY + yte$scrollOffset, button);
    }

    @Override
    public boolean mouseDragged2(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (yte$scrollbarDragging) {
            yte$setScrollFromThumb(mouseY - yte$scrollbarDragOffset);
            return true;
        }
        return super.mouseDragged2(mouseX, mouseY + yte$scrollOffset, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased2(double mouseX, double mouseY, int button) {
        if (yte$scrollbarDragging) {
            yte$scrollbarDragging = false;
            return true;
        }
        return super.mouseReleased2(mouseX, mouseY + yte$scrollOffset, button);
    }

    @Unique
    private static TextFieldWidgetExtension yte$createNumberField(double value) {
        final TextFieldWidgetExtension field = new TextFieldWidgetExtension(0, 0, 0, IGui.SQUARE_SIZE, 12, TextCase.DEFAULT, null, "0");
        field.setText2(Double.toString(value));
        return field;
    }

    @Unique
    private void yte$positionField(TextFieldWidgetExtension field, int row) {
        field.setX2(0);
        field.setY2(IGui.SQUARE_SIZE * row);
        field.setWidth2(width2);
    }

    @Unique
    private void yte$positionSlider(WidgetShorterSlider slider, int row) {
        slider.setX2(0);
        slider.setY2(IGui.SQUARE_SIZE * row);
        slider.setHeight(IGui.SQUARE_SIZE);
        slider.setWidth2(width2);
    }

    @Unique
    private void yte$toggleProfessionalMode() {
        if (yte$professionalMode) {
            yte$setEasyModeValues(
                    yte$parseNumber(yte$speedField, yte$lastSentSpeed, YteLiftConfig.MIN_SPEED, YteLiftConfig.MAX_SPEED),
                    yte$parseNumber(yte$accelerationField, yte$lastSentAccel, YteLiftConfig.MIN_ACCELERATION, YteLiftConfig.MAX_ACCELERATION),
                    yte$parseNumber(yte$adoDistanceField, yte$lastSentAdoDistance, YteLiftConfig.MAX_ADO_DISTANCE),
                    yte$parseNumber(yte$levellingDistanceField, yte$lastSentLevellingDistance, YteLiftConfig.MAX_LEVELLING_DISTANCE),
                    yte$parseNumber(yte$levellingSpeedField, yte$lastSentLevellingSpeed, YteLiftConfig.MAX_LEVELLING_SPEED));
        } else {
            yte$syncFieldsFromValues(
                    yte$getEasyModeValue(0, yte$sliderSpeed, valueToSpeed(yte$sliderSpeed.getIntValue())),
                    yte$getEasyModeValue(1, yte$sliderAcceleration, valueToAccel(yte$sliderAcceleration.getIntValue())),
                    yte$getEasyModeValue(2, yte$sliderAdoDistance, valueToAdoDistance(yte$sliderAdoDistance.getIntValue())),
                    yte$getEasyModeValue(3, yte$sliderLevellingDistance, valueToLevellingDistance(yte$sliderLevellingDistance.getIntValue())),
                    yte$getEasyModeValue(4, yte$sliderLevellingSpeed, valueToLevellingSpeed(yte$sliderLevellingSpeed.getIntValue())));
        }
        yte$professionalMode = !yte$professionalMode;
        yte$updateModeWidgets();
    }

    @Unique
    private void yte$setEasyModeValues(double speed, double acceleration, double adoDistance,
            double levellingDistance, double levellingSpeed) {
        yte$easyModeValues[0] = speed;
        yte$easyModeValues[1] = acceleration;
        yte$easyModeValues[2] = adoDistance;
        yte$easyModeValues[3] = levellingDistance;
        yte$easyModeValues[4] = levellingSpeed;

        yte$sliderSpeed.setValue(speedToValue(speed));
        yte$sliderAcceleration.setValue(accelToValue(acceleration));
        yte$sliderAdoDistance.setValue(adoDistanceToValue(adoDistance));
        yte$sliderLevellingDistance.setValue(levellingDistanceToValue(levellingDistance));
        yte$sliderLevellingSpeed.setValue(levellingSpeedToValue(levellingSpeed));

        yte$easyModeSliderAnchors[0] = yte$sliderSpeed.getIntValue();
        yte$easyModeSliderAnchors[1] = yte$sliderAcceleration.getIntValue();
        yte$easyModeSliderAnchors[2] = yte$sliderAdoDistance.getIntValue();
        yte$easyModeSliderAnchors[3] = yte$sliderLevellingDistance.getIntValue();
        yte$easyModeSliderAnchors[4] = yte$sliderLevellingSpeed.getIntValue();
        for (int i = 0; i < yte$easyModeSliderTouched.length; i++) {
            yte$easyModeSliderTouched[i] = false;
        }
    }

    @Unique
    private double yte$getEasyModeValue(int index, WidgetShorterSlider slider, double sliderValue) {
        if (slider.getIntValue() != yte$easyModeSliderAnchors[index]) {
            yte$easyModeSliderTouched[index] = true;
        }
        return yte$easyModeSliderTouched[index] ? sliderValue : yte$easyModeValues[index];
    }

    @Unique
    private void yte$syncFieldsFromValues(double speed, double acceleration, double adoDistance,
            double levellingDistance, double levellingSpeed) {
        yte$speedField.setText2(Double.toString(speed));
        yte$accelerationField.setText2(Double.toString(acceleration));
        yte$adoDistanceField.setText2(Double.toString(adoDistance));
        yte$levellingDistanceField.setText2(Double.toString(levellingDistance));
        yte$levellingSpeedField.setText2(Double.toString(levellingSpeed));
    }

    @Unique
    private void yte$updateModeWidgets() {
        yte$professionalModeButton.setMessage2(new Text(TextHelper.translatable(yte$professionalMode
                ? "gui.yte.lift_professional_mode_on"
                : "gui.yte.lift_professional_mode_off").data));

        yte$sliderSpeed.setVisibleMapped(!yte$professionalMode);
        yte$sliderAcceleration.setVisibleMapped(!yte$professionalMode);
        yte$sliderAdoDistance.setVisibleMapped(!yte$professionalMode);
        yte$sliderLevellingDistance.setVisibleMapped(!yte$professionalMode);
        yte$sliderLevellingSpeed.setVisibleMapped(!yte$professionalMode);

        yte$speedField.setVisibleMapped(yte$professionalMode);
        yte$accelerationField.setVisibleMapped(yte$professionalMode);
        yte$adoDistanceField.setVisibleMapped(yte$professionalMode);
        yte$levellingDistanceField.setVisibleMapped(yte$professionalMode);
        yte$levellingSpeedField.setVisibleMapped(yte$professionalMode);
    }

    @Unique
    private void yte$drawInputLabel(GraphicsHolder graphicsHolder, String key, int row) {
        graphicsHolder.drawText(TextHelper.translatable(key), 0,
                IGui.SQUARE_SIZE * row + IGui.TEXT_PADDING,
                IGui.ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
    }

    @Unique
    private void yte$drawModeLabel(GraphicsHolder graphicsHolder, String inputKey, String valueKey, double value, int row) {
        graphicsHolder.drawText(TextHelper.translatable(yte$professionalMode ? inputKey : valueKey,
                        String.format(Locale.ROOT, "%.2f", value)), 0,
                IGui.SQUARE_SIZE * row + IGui.TEXT_PADDING,
                IGui.ARGB_WHITE, false, GraphicsHolder.getDefaultLight());
    }

    @Unique
    private static double yte$parseNumber(TextFieldWidgetExtension field, double fallback, double maximum) {
        return yte$parseNumber(field, fallback, 0, maximum);
    }

    @Unique
    private static double yte$parseNumber(TextFieldWidgetExtension field, double fallback, double minimum, double maximum) {
        try {
            final double value = Double.parseDouble(field.getText2().trim().replace(',', '.'));
            return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Unique
    private double yte$getMaxScroll() {
        return Math.max(0, YTE_CONTENT_ROWS * IGui.SQUARE_SIZE - getHeightMapped());
    }

    @Unique
    private void yte$drawScrollbar(GraphicsHolder graphicsHolder) {
        final double maxScroll = yte$getMaxScroll();
        if (maxScroll <= 0) {
            return;
        }
        final int screenHeight = getHeightMapped();
        final int trackWidth = 4;
        final int trackX = Math.max(0, width2 - trackWidth);
        final int thumbHeight = yte$getScrollbarThumbHeight();
        final int thumbY = yte$getScrollbarThumbY();
        final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        guiDrawing.beginDrawingRectangle();
        guiDrawing.drawRectangle(trackX, 0, width2, screenHeight, 0x66000000);
        guiDrawing.drawRectangle(trackX, thumbY, width2, thumbY + thumbHeight, 0xFFAAAAAA);
        guiDrawing.finishDrawingRectangle();
    }

    @Unique
    private int yte$getScrollbarThumbHeight() {
        final int screenHeight = getHeightMapped();
        return Math.max(IGui.SQUARE_SIZE, screenHeight * screenHeight / (YTE_CONTENT_ROWS * IGui.SQUARE_SIZE));
    }

    @Unique
    private int yte$getScrollbarThumbY() {
        final int availableHeight = getHeightMapped() - yte$getScrollbarThumbHeight();
        return (int) Math.round(yte$scrollOffset / yte$getMaxScroll() * availableHeight);
    }

    @Unique
    private void yte$setScrollFromThumb(double thumbY) {
        final int availableHeight = getHeightMapped() - yte$getScrollbarThumbHeight();
        if (availableHeight > 0) {
            yte$scrollOffset = Math.max(0, Math.min(yte$getMaxScroll(), thumbY / availableHeight * yte$getMaxScroll()));
        }
    }

    @Unique
    private static double valueToSpeed(int sliderValue) {
        return sliderValue == 0 ? 0.1 : sliderValue * 0.5;
    }

    @Unique
    private static int speedToValue(double speed) {
        return yte$floorToSlider(speed, 0.5, SPEED_SLIDER_MAX);
    }

    @Unique
    private static double valueToAccel(int sliderValue) {
        return sliderValue == 0 ? 0.1 : sliderValue * 0.5;
    }

    @Unique
    private static int accelToValue(double accel) {
        return yte$floorToSlider(accel, 0.5, ACCEL_SLIDER_MAX);
    }

    @Unique
    private static double valueToAdoDistance(int sliderValue) {
        return sliderValue / 100.0;
    }

    @Unique
    private static int adoDistanceToValue(double distance) {
        return yte$floorToSlider(distance, 0.01, ADO_DISTANCE_SLIDER_MAX);
    }

    @Unique
    private static double valueToLevellingDistance(int sliderValue) {
        return sliderValue / 20.0;
    }

    @Unique
    private static int levellingDistanceToValue(double distance) {
        return yte$floorToSlider(distance, 0.05, LEVELLING_DISTANCE_SLIDER_MAX);
    }

    @Unique
    private static double valueToLevellingSpeed(int sliderValue) {
        return sliderValue / 20.0;
    }

    @Unique
    private static int levellingSpeedToValue(double speed) {
        return yte$floorToSlider(speed, 0.05, LEVELLING_SPEED_SLIDER_MAX);
    }

    @Unique
    private static int yte$floorToSlider(double value, double step, int maximum) {
        return Math.max(0, Math.min(maximum, (int) Math.floor(value / step + 1E-9)));
    }
}
