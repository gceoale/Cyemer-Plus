package com.slither.cyemer.manager;

import com.slither.cyemer.module.implementation.ArrayListModule;
import com.slither.cyemer.module.implementation.AutoDrain;
import com.slither.cyemer.module.implementation.AutoGG;
import com.slither.cyemer.module.implementation.AutoTool;
import com.slither.cyemer.module.implementation.AutoTotem;
import com.slither.cyemer.module.implementation.Blink;
import com.slither.cyemer.module.implementation.ClickGUIModule;
import com.slither.cyemer.module.implementation.ConfigManagerModule;
import com.slither.cyemer.module.implementation.CustomCapeModule;
import com.slither.cyemer.module.implementation.CustomFont;
import com.slither.cyemer.module.implementation.CustomRender;
import com.slither.cyemer.module.implementation.ESP;
import com.slither.cyemer.module.implementation.ElytraSwap;
import com.slither.cyemer.module.implementation.Fakelag;
import com.slither.cyemer.module.implementation.FastPlace;
import com.slither.cyemer.module.implementation.FullBright;
import com.slither.cyemer.module.implementation.HitAnimations;
import com.slither.cyemer.module.implementation.HoverTotem;
import com.slither.cyemer.module.implementation.HudEditor;
import com.slither.cyemer.module.implementation.Interface;
import com.slither.cyemer.module.implementation.KeyPearl;
import com.slither.cyemer.module.implementation.LagReach;
import com.slither.cyemer.module.implementation.MaceSwap;
import com.slither.cyemer.module.implementation.NoBreakDelay;
import com.slither.cyemer.module.implementation.NPotRefill;
import com.slither.cyemer.module.implementation.Notifications;
import com.slither.cyemer.module.implementation.PotRefill;
import com.slither.cyemer.module.implementation.ObsidianGlow;
import com.slither.cyemer.module.implementation.PearlCatch;
import com.slither.cyemer.module.implementation.Prevent;
import com.slither.cyemer.module.implementation.Scaffold;
import com.slither.cyemer.module.implementation.SelfDestruct;
import com.slither.cyemer.module.implementation.Sprint;
import com.slither.cyemer.module.implementation.WindClutch;
import com.slither.cyemer.module.implementation.StreamProof;
import com.slither.cyemer.module.implementation.StreamerModeModule;
import com.slither.cyemer.module.implementation.TargetEffect;
import com.slither.cyemer.module.implementation.ViewModel;
import com.slither.cyemer.module.implementation.Watermark;
import com.slither.cyemer.module.implementation.WebBreaker;
import com.slither.cyemer.module.implementation.WindChargeKey;
import com.slither.cyemer.module.implementation.combat.AimAssist;
import com.slither.cyemer.module.implementation.donutsmp.FarmFinder;
import com.slither.cyemer.module.implementation.donutsmp.SpotFinder;
import com.slither.cyemer.module.implementation.combat.AnchorMacro;
import com.slither.cyemer.module.implementation.combat.AntiDivebomb;
import com.slither.cyemer.module.implementation.combat.AutoAnchor;
import com.slither.cyemer.module.implementation.combat.AutoCrit;
import com.slither.cyemer.module.implementation.combat.AutoCrystal;
import com.slither.cyemer.module.implementation.combat.AutoElytra;
import com.slither.cyemer.module.implementation.combat.AutoJumpReset;
import com.slither.cyemer.module.implementation.combat.AutoMace;
import com.slither.cyemer.module.implementation.combat.AutoObsidian;
import com.slither.cyemer.module.implementation.combat.AutoPot;
import com.slither.cyemer.module.implementation.combat.AutoShield;
import com.slither.cyemer.module.implementation.combat.AutoShieldBreak;
import com.slither.cyemer.module.implementation.combat.AutoWindCharge;
import com.slither.cyemer.module.implementation.combat.BowAimbot;
import com.slither.cyemer.module.implementation.combat.CartRefill;
import com.slither.cyemer.module.implementation.combat.GrappleAimbot;
import com.slither.cyemer.module.implementation.combat.InstaCart;
import com.slither.cyemer.module.implementation.combat.Lungemacro;
import com.slither.cyemer.module.implementation.combat.Nick;
import com.slither.cyemer.module.implementation.combat.PearlCharge;
import com.slither.cyemer.module.implementation.combat.PearlChaser;
import com.slither.cyemer.module.implementation.combat.PearlMacro;
import com.slither.cyemer.module.implementation.combat.Shielddrain;
import com.slither.cyemer.module.implementation.combat.TotemHit;
import com.slither.cyemer.module.implementation.combat.TriggerBot;
import com.slither.cyemer.module.implementation.combat.WTap;
import com.slither.cyemer.module.implementation.combat.WindChargeHitter;
import com.slither.cyemer.module.implementation.combat.XbowCart;
import com.slither.cyemer.module.implementation.movement.SafeWalk;
import com.slither.cyemer.module.implementation.movement.SnapTap;
import com.slither.cyemer.module.implementation.render.BlockESP;
import com.slither.cyemer.module.implementation.render.Effectesp;
import com.slither.cyemer.module.implementation.render.HandCham;
import com.slither.cyemer.module.implementation.render.Nametags;
import com.slither.cyemer.module.Module;
import com.slither.cyemer.module.implementation.HotReloadModule;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Every module, in the order the GUI lists them.
 *
 * ModuleManager reaches this class only by name, never by reference, and
 * nothing in the core does either: that is what lets HotReload load a new
 * build of it and of every module it lists while the game runs. existing
 * hands back a module to keep as it is (one the core still refers to, so it
 * is not reloaded) instead of making a new one.
 */
@Environment(EnvType.CLIENT)
public final class ModuleRegistry {
    private ModuleRegistry() {
    }

    public static List<Module> create(Function<Class<?>, Module> existing) {
        List<Module> out = new ArrayList<>();
        add(out, existing, Sprint.class, Sprint::new);
        add(out, existing, ArrayListModule.class, ArrayListModule::new);
        add(out, existing, FullBright.class, FullBright::new);
        add(out, existing, TriggerBot.class, TriggerBot::new);
        add(out, existing, AimAssist.class, AimAssist::new);
        add(out, existing, ESP.class, ESP::new);
        add(out, existing, WTap.class, WTap::new);
        add(out, existing, AutoShieldBreak.class, AutoShieldBreak::new);
        add(out, existing, AutoCrystal.class, AutoCrystal::new);
        add(out, existing, KeyPearl.class, KeyPearl::new);
        add(out, existing, AutoAnchor.class, AutoAnchor::new);
        add(out, existing, Nick.class, Nick::new);
        add(out, existing, AutoObsidian.class, AutoObsidian::new);
        add(out, existing, Notifications.class, Notifications::new);
        add(out, existing, HoverTotem.class, HoverTotem::new);
        add(out, existing, MaceSwap.class, MaceSwap::new);
        add(out, existing, WebBreaker.class, WebBreaker::new);
        add(out, existing, StreamerModeModule.class, StreamerModeModule::new);
        add(out, existing, SelfDestruct.class, SelfDestruct::new);
        add(out, existing, NoBreakDelay.class, NoBreakDelay::new);
        add(out, existing, FastPlace.class, FastPlace::new);
        add(out, existing, Fakelag.class, Fakelag::new);
        add(out, existing, CustomCapeModule.class, CustomCapeModule::new);
        add(out, existing, HitAnimations.class, HitAnimations::new);
        add(out, existing, ViewModel.class, ViewModel::new);
        add(out, existing, AutoMace.class, AutoMace::new);
        add(out, existing, AutoElytra.class, AutoElytra::new);
        add(out, existing, GrappleAimbot.class, GrappleAimbot::new);
        add(out, existing, AntiDivebomb.class, AntiDivebomb::new);
        add(out, existing, HudEditor.class, HudEditor::new);
        add(out, existing, ObsidianGlow.class, ObsidianGlow::new);
        add(out, existing, AutoPot.class, AutoPot::new);
        add(out, existing, AutoTool.class, AutoTool::new);
        add(out, existing, SafeWalk.class, SafeWalk::new);
        add(out, existing, ConfigManagerModule.class, ConfigManagerModule::new);
        add(out, existing, AutoJumpReset.class, AutoJumpReset::new);
        add(out, existing, Blink.class, Blink::new);
        add(out, existing, Watermark.class, Watermark::new);
        add(out, existing, Prevent.class, Prevent::new);
        add(out, existing, AutoDrain.class, AutoDrain::new);
        add(out, existing, Effectesp.class, Effectesp::new);
        add(out, existing, AutoWindCharge.class, AutoWindCharge::new);
        add(out, existing, AutoTotem.class, AutoTotem::new);
        add(out, existing, HandCham.class, HandCham::new);
        add(out, existing, WindChargeKey.class, WindChargeKey::new);
        add(out, existing, Nametags.class, Nametags::new);
        add(out, existing, PearlCatch.class, PearlCatch::new);
        add(out, existing, WindClutch.class, WindClutch::new);
        add(out, existing, ElytraSwap.class, ElytraSwap::new);
        add(out, existing, ClickGUIModule.class, ClickGUIModule::new);
        add(out, existing, CustomFont.class, CustomFont::new);
        add(out, existing, Interface.class, Interface::new);
        add(out, existing, TargetEffect.class, TargetEffect::new);
        add(out, existing, CustomRender.class, CustomRender::new);
        add(out, existing, AnchorMacro.class, AnchorMacro::new);
        add(out, existing, PearlMacro.class, PearlMacro::new);
        add(out, existing, PearlCharge.class, PearlCharge::new);
        add(out, existing, BowAimbot.class, BowAimbot::new);
        add(out, existing, AutoShield.class, AutoShield::new);
        add(out, existing, Lungemacro.class, Lungemacro::new);
        add(out, existing, Shielddrain.class, Shielddrain::new);
        add(out, existing, InstaCart.class, InstaCart::new);
        add(out, existing, CartRefill.class, CartRefill::new);
        add(out, existing, XbowCart.class, XbowCart::new);
        add(out, existing, AutoCrit.class, AutoCrit::new);
        add(out, existing, SnapTap.class, SnapTap::new);
        add(out, existing, AutoGG.class, AutoGG::new);
        add(out, existing, PotRefill.class, PotRefill::new);
        add(out, existing, NPotRefill.class, NPotRefill::new);
        add(out, existing, StreamProof.class, StreamProof::new);
        add(out, existing, LagReach.class, LagReach::new);
        add(out, existing, Scaffold.class, Scaffold::new);
        add(out, existing, FarmFinder.class, FarmFinder::new);
        add(out, existing, SpotFinder.class, SpotFinder::new);
        add(out, existing, WindChargeHitter.class, WindChargeHitter::new);
        add(out, existing, PearlChaser.class, PearlChaser::new);
        add(out, existing, TotemHit.class, TotemHit::new);
        add(out, existing, BlockESP.class, BlockESP::new);
        add(out, existing, HotReloadModule.class, HotReloadModule::new);
        return out;
    }

    private static <T extends Module> void add(List<Module> out, Function<Class<?>, Module> existing, Class<T> type, Supplier<T> make) {
        Module kept = existing.apply(type);
        out.add(kept != null ? kept : make.get());
    }
}
