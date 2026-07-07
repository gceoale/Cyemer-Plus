package com.slither.cyemer.gui.new_ui.old;

import com.slither.cyemer.gui.new_ui.FriendlistPanel;
import com.slither.cyemer.module.Category;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.util.Renderer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class LegacyClickGUI extends class_437 {
    private final List<Panel> panels = new ArrayList<>();
    private FriendlistPanel friendlistPanel;
    public static double cameraX = 0.0;
    public static double cameraY = 0.0;
    public static double zoom = 1.0;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 2.0;
    private static final double SCROLL_SPEED = 10.0;
    private static final double ZOOM_SENSITIVITY = 0.05;
    private static final double PAN_LIMIT = 500.0;
    private double openAnimationProgress = 0.0;
    private final double animationDuration = 0.45;
    private static final double STAGGER_DELAY = 0.07;
    private double originalBackgroundBlur = -1.0;
    private static final double PANEL_SPACING = 10.0;
    private static final double PANEL_WIDTH = 125.0;
    private static final double PANEL_HEIGHT = 22.0;

    public LegacyClickGUI() {
        super(class_2561.method_43470("Click GUI"));
        List<Category> categories = new ArrayList<>(List.of(Category.values()));
        categories.sort(Comparator.comparing(Enum::ordinal));
        int totalPanels = categories.size() + 1;
        double totalGroupWidth = totalPanels * 125.0 + (totalPanels - 1) * 10.0;
        double currentX = -(totalGroupWidth / 2.0);
        double currentY = 0.0;

        for (Category category : categories) {
            this.panels.add(new Panel(category, currentX, currentY, 125.0, 22.0));
            currentX += 135.0;
        }

        this.friendlistPanel = new FriendlistPanel(currentX, currentY, 125.0, 22.0);
    }

    protected void method_25426() {
        super.method_25426();
        this.openAnimationProgress = 0.0;
        cameraX = 0.0;
        cameraY = 0.0;
        if (this.field_22787 != null) {
        }

        Renderer.get().init();
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        if (this.openAnimationProgress < 1.0) {
            float durationInTicks = 9.0F;
            this.openAnimationProgress += delta / durationInTicks;
            this.openAnimationProgress = Math.min(this.openAnimationProgress, 1.0);
        }

        cameraX = class_3532.method_15350(cameraX, -500.0, 500.0);
        cameraY = class_3532.method_15350(cameraY, -500.0, 500.0);
        double guiScale = this.field_22787.method_22683().method_4495();
        double fitZoom = this.field_22789 * 0.94 / Math.max(1.0, this.computePanelGroupWidth());
        double autoZoom = Math.min(2.0 / guiScale, fitZoom);
        double finalZoom = zoom * autoZoom;
        float pixelRatio = (float)(guiScale * ClickGUIModule.getUiResolutionScale());
        if (Renderer.get().beginFrame(this.field_22789, this.field_22790, pixelRatio)) {
            try {
                float originX = this.field_22789 / 2.0F;
                float originY = this.field_22790 / 6.0F;
                Vector2d transformedMouse = this.transformMouse(mouseX, mouseY, finalZoom, 1.0, originX, originY);
                String activeTooltip = null;

                for (int i = 0; i < this.panels.size(); i++) {
                    Panel panel = this.panels.get(i);
                    double panelDelay = i * 0.07;
                    double localProgress = class_3532.method_15350((this.openAnimationProgress - panelDelay) / (1.0 - this.panels.size() * 0.07), 0.0, 1.0);
                    if (!(localProgress <= 0.0)) {
                        double easedPop = this.backOut(localProgress);
                        double alpha = class_3532.method_15350(localProgress * 2.5, 0.0, 1.0);
                        Renderer.get().save();
                        Renderer.get().translate(originX, originY);
                        Renderer.get().scale((float)finalZoom, (float)finalZoom);
                        Renderer.get().translate((float)cameraX, (float)cameraY);
                        float pCenterX = (float)(panel.x + panel.width / 2.0);
                        float pCenterY = (float)(panel.y + panel.headerHeight / 2.0);
                        Renderer.get().translate(pCenterX, pCenterY);
                        Renderer.get().scale((float)easedPop, (float)easedPop);
                        Renderer.get().translate(-pCenterX, -pCenterY);
                        String tooltip = panel.render(context, (int)transformedMouse.x, (int)transformedMouse.y, delta, alpha);
                        if (tooltip != null) {
                            activeTooltip = tooltip;
                        }

                        Renderer.get().restore();
                    }
                }

                if (this.friendlistPanel != null) {
                    double flDelay = this.panels.size() * 0.07;
                    double flProgress = class_3532.method_15350((this.openAnimationProgress - flDelay) / (1.0 - flDelay), 0.0, 1.0);
                    if (flProgress > 0.0) {
                        double flEased = this.backOut(flProgress);
                        double flAlpha = class_3532.method_15350(flProgress * 2.5, 0.0, 1.0);
                        Renderer.get().save();
                        Renderer.get().translate(originX, originY);
                        Renderer.get().scale((float)finalZoom, (float)finalZoom);
                        Renderer.get().translate((float)cameraX, (float)cameraY);
                        float flCenterX = (float)(this.friendlistPanel.x + this.friendlistPanel.width / 2.0);
                        float flCenterY = (float)(this.friendlistPanel.y + this.friendlistPanel.headerHeight / 2.0);
                        Renderer.get().translate(flCenterX, flCenterY);
                        Renderer.get().scale((float)flEased, (float)flEased);
                        Renderer.get().translate(-flCenterX, -flCenterY);
                        String tooltip = this.friendlistPanel.render(context, (int)transformedMouse.x, (int)transformedMouse.y, delta, flAlpha);
                        if (tooltip != null) {
                            activeTooltip = tooltip;
                        }

                        Renderer.get().restore();
                    }
                }

                if (activeTooltip != null && !activeTooltip.isEmpty()) {
                    this.renderTooltip(context, activeTooltip, mouseX, mouseY, finalZoom);
                }

                Renderer.get().endFrame();
            } catch (Exception var31) {
                Renderer.forceVanillaRenderer();
            }
        }
    }

    private double backOut(double t) {
        double s = 1.70158;
        return --t * t * (2.70158 * t + 1.70158) + 1.0;
    }

    private Vector2d transformMouse(double mouseX, double mouseY, double finalZoom, double animScale, float originX, float originY) {
        double cx = mouseX - originX;
        double cy = mouseY - originY;
        cx /= animScale;
        cy /= animScale;
        cx /= finalZoom;
        cy /= finalZoom;
        cx -= cameraX;
        cy -= cameraY;
        return new Vector2d(cx, cy);
    }

    private void renderTooltip(class_332 context, String text, int mouseX, int mouseY, double finalZoom) {
        float zoomFactor = (float)finalZoom;
        float fontSize = 10.0F * zoomFactor;
        float padding = 4.0F * zoomFactor;
        float textWidth = Renderer.get().getTextWidth(text, fontSize);
        float textHeight = Renderer.get().getTextHeight(fontSize);
        float offset = 8.0F * zoomFactor;
        float x = mouseX + offset;
        float y = mouseY + offset;
        float totalWidth = textWidth + padding * 2.0F;
        float totalHeight = textHeight + padding * 2.0F;
        if (x + totalWidth > this.field_22789) {
            x -= totalWidth + offset * 2.0F;
        }

        if (y + totalHeight > this.field_22790) {
            y -= totalHeight + offset * 2.0F;
        }

        float tooltipRadius = 2.0F * zoomFactor;
        Renderer.get().drawRoundedRect(context, x, y, totalWidth, totalHeight, tooltipRadius, ClickGUIModule.getTooltipBackground());
        Renderer.get().drawRoundedRectOutline(context, x, y, totalWidth, totalHeight, tooltipRadius, 1.0F * zoomFactor, ClickGUIModule.getTooltipBorder());
        float textY = y + (totalHeight - textHeight) / 2.0F;
        Renderer.get().drawText(context, text, x + padding, textY, fontSize, ClickGUIModule.getTooltipText(), false);
    }

    private boolean isControlDown() {
        if (this.field_22787 == null) {
            return false;
        } else {
            long handle = this.field_22787.method_22683().method_4490();
            return GLFW.glfwGetKey(handle, 341) == 1 || GLFW.glfwGetKey(handle, 345) == 1;
        }
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.isControlDown()) {
            double scrollAmount = verticalAmount != 0.0 ? verticalAmount : horizontalAmount;
            double zoomFactor = 1.0 + scrollAmount * 0.05;
            double newZoom = zoom * zoomFactor;
            zoom = Math.max(0.5, Math.min(newZoom, 2.0));
        } else {
            cameraY += verticalAmount * 10.0;
        }

        return true;
    }

    public boolean method_25402(class_11909 click, boolean doubleClick) {
        return this.handleMouseInput(click, true, doubleClick);
    }

    public boolean method_25406(class_11909 click) {
        return this.handleMouseInput(click, false, false);
    }

    private boolean handleMouseInput(class_11909 click, boolean isClick, boolean doubleClick) {
        double mouseX = click.comp_4798();
        double mouseY = click.comp_4799();
        int button = click.method_74245();
        double guiScale = this.field_22787.method_22683().method_4495();
        double fitZoom = this.field_22789 * 0.94 / Math.max(1.0, this.computePanelGroupWidth());
        double autoZoom = Math.min(2.0 / guiScale, fitZoom);
        double finalZoom = zoom * autoZoom;
        float originX = this.field_22789 / 2.0F;
        float originY = this.field_22790 / 6.0F;
        Vector2d m = this.transformMouse(mouseX, mouseY, finalZoom, 1.0, originX, originY);
        if (isClick) {
            this.panels.forEach(p -> p.mouseClicked(m.x, m.y, button));
            if (this.friendlistPanel != null) {
                this.friendlistPanel.mouseClicked(m.x, m.y, button);
            }

            return super.method_25402(click, doubleClick);
        } else {
            this.panels.forEach(p -> p.mouseReleased(m.x, m.y, button));
            if (this.friendlistPanel != null) {
                this.friendlistPanel.mouseReleased(m.x, m.y, button);
            }

            return super.method_25406(click);
        }
    }

    public boolean method_25404(class_11908 keyInput) {
        int keyCode = keyInput.comp_4795();
        int scanCode = keyInput.comp_4796();
        int modifiers = keyInput.comp_4797();
        boolean aModuleIsBinding = this.panels.stream().flatMap(panel -> panel.getButtons().stream()).anyMatch(button -> button.module.isBinding());
        this.panels.forEach(p -> p.keyPressed(keyCode, scanCode, modifiers));
        if (this.friendlistPanel != null) {
            this.friendlistPanel.keyPressed(keyCode, scanCode, modifiers);
        }

        return aModuleIsBinding && keyCode == 256 ? true : super.method_25404(keyInput);
    }

    public boolean method_25400(class_11905 input) {
        if (input.method_74227() && Character.isBmpCodePoint(input.comp_4793())) {
            char chr = (char)input.comp_4793();
            int modifiers = input.comp_4794();
            this.panels.forEach(p -> p.charTyped(chr, modifiers));
            if (this.friendlistPanel != null) {
                this.friendlistPanel.charTyped(chr, modifiers);
            }
        }

        return super.method_25400(input);
    }

    public void method_25419() {
        if (this.field_22787 != null && this.originalBackgroundBlur != -1.0) {
            this.field_22787.field_1690.method_57702().method_41748((int)this.originalBackgroundBlur);
        }

        super.method_25419();
    }

    public boolean method_25421() {
        return false;
    }

    private double computePanelGroupWidth() {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;

        for (Panel panel : this.panels) {
            minX = Math.min(minX, panel.x);
            maxX = Math.max(maxX, panel.x + panel.width);
        }

        if (this.friendlistPanel != null) {
            minX = Math.min(minX, this.friendlistPanel.getX());
            maxX = Math.max(maxX, this.friendlistPanel.getX() + this.friendlistPanel.width);
        }

        return Double.isFinite(minX) && Double.isFinite(maxX) && !(maxX <= minX) ? maxX - minX : 125.0;
    }
}
