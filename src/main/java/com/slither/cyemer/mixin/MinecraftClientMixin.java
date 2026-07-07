package com.slither.cyemer.mixin;

import com.slither.cyemer.Cyemer;
import com.slither.cyemer.friend.FriendHelper;
import com.slither.cyemer.friend.FriendKeybindManager;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.util.ModuleAccess;
import com.slither.cyemer.util.Renderer;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1041;
import net.minecraft.class_310;
import net.minecraft.class_408;
import net.minecraft.class_437;
import net.minecraft.class_746;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin({class_310.class})
public abstract class MinecraftClientMixin {
    private Map<Integer, Boolean> wasPressedMap;
    @Shadow
    @Nullable
    public class_746 field_1724;
    @Shadow
    public class_437 field_1755;

    @Shadow
    public abstract class_1041 method_22683();

    @Inject(
        method = {"method_1574()V"},
        at = {@At("HEAD")}
    )
    private void onTick(CallbackInfo ci) {
        if (!Cyemer.selfDestructed) {
            class_310 client = (class_310)(Object)this;
            if (client.field_1724 != null) {
                if (this.wasPressedMap == null) {
                    this.wasPressedMap = new HashMap<>();
                }

                Cyemer.getInstance().getModuleManager().onTick();
                boolean allowKeybinds = client.field_1755 == null || this.shouldHandleKeybindsInScreen(client.field_1755);
                if (allowKeybinds) {
                    for (Module module : Cyemer.getInstance().getModuleManager().getModules()) {
                        int key = module.getKeyCode();
                        if (key == -1) {
                            String moduleName = module.getName();
                            if ("ClickGUI".equalsIgnoreCase(moduleName)) {
                                key = 344;
                            }
                        }

                        if (key != -1) {
                            boolean isPressed;
                            if (key < 0) {
                                int button = key + 100;
                                isPressed = GLFW.glfwGetMouseButton(client.method_22683().method_4490(), button) == 1;
                            } else {
                                isPressed = GLFW.glfwGetKey(client.method_22683().method_4490(), key) == 1;
                            }

                            if (isPressed && !this.wasPressedMap.getOrDefault(key, false)) {
                                module.toggle();
                            }

                            this.wasPressedMap.put(key, isPressed);
                        }
                    }

                    int friendKey = FriendKeybindManager.getInstance().getKeyCode();
                    if (friendKey != -1) {
                        boolean friendKeyPressed;
                        if (friendKey < 0) {
                            int button = friendKey + 100;
                            friendKeyPressed = GLFW.glfwGetMouseButton(client.method_22683().method_4490(), button) == 1;
                        } else {
                            friendKeyPressed = GLFW.glfwGetKey(client.method_22683().method_4490(), friendKey) == 1;
                        }

                        if (friendKeyPressed && !this.wasPressedMap.getOrDefault(friendKey, false)) {
                            FriendHelper.toggleTargetedPlayer();
                        }

                        this.wasPressedMap.put(friendKey, friendKeyPressed);
                    }
                } else if (!this.wasPressedMap.isEmpty()) {
                    this.wasPressedMap.clear();
                }
            }
        }
    }

    @Inject(
        method = {"method_1574()V"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_638;method_18116()V",
            shift = Shift.BEFORE
        )}
    )
    private void onTickPost(CallbackInfo ci) {
        if (!Cyemer.selfDestructed) {
            class_310 client = (class_310)(Object)this;
            if (client.field_1724 != null) {
                Cyemer.getInstance().getModuleManager().onTickPost();
            }
        }
    }

    @Unique
    private boolean shouldHandleKeybindsInScreen(class_437 screen) {
        return !(screen instanceof class_408);
    }

    @Inject(
        method = {"method_1536()Z"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (!Cyemer.selfDestructed) {
            Module maceSwapModule = Cyemer.getInstance().getModuleManager().getModule("MaceSwap");
            if (maceSwapModule != null && maceSwapModule.isEnabled()) {
                boolean handled = ModuleAccess.invokeBoolean(maceSwapModule, "handleAttack", false, null);
                if (handled) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }

    @Inject(
        method = {"close()V"},
        at = {@At("HEAD")}
    )
    private void onClose(CallbackInfo ci) {
        Renderer.get().cleanup();
    }

    @Inject(
        method = {"method_20539(Z)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onOpenGameMenu(boolean pauseOnly, CallbackInfo ci) {
        if (!Cyemer.selfDestructed) {
            if (this.field_1755 == null) {
                Module preventModule = Cyemer.getInstance().getModuleManager().getModule("Prevent");
                boolean preventEscape = ModuleAccess.invokeBoolean(preventModule, "shouldPreventEscape", false, null);
                if (preventModule != null && preventEscape) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(
        method = {"method_15995(Z)V"},
        at = {@At("HEAD")},
        cancellable = true
    )
    private void onWindowFocusChanged(boolean focused, CallbackInfo ci) {
        if (!Cyemer.selfDestructed) {
            Module preventModule = Cyemer.getInstance().getModuleManager().getModule("Prevent");
            boolean preventTabOut = ModuleAccess.invokeBoolean(preventModule, "shouldPreventTabOut", false, null);
            if (preventModule != null && preventTabOut) {
                if (!focused) {
                    ci.cancel();
                } else {
                    long handle = this.method_22683().method_4490();
                    int width = this.method_22683().method_4480();
                    int height = this.method_22683().method_4507();
                    GLFW.glfwSetCursorPos(handle, width / 2.0, height / 2.0);
                }
            }
        }
    }
}
