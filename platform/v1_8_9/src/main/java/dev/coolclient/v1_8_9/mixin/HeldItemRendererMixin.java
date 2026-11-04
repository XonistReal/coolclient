package dev.coolclient.v1_8_9.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.legacy.LegacyModules;
import dev.coolclient.core.modules.visual.HandChams;
import dev.coolclient.core.modules.visual.ViewModel;
import dev.coolclient.core.modules.visual.VisualTweaks;
import dev.coolclient.v1_8_9.Chams189;
import dev.coolclient.v1_8_9.Hooks189;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow private ItemStack mainHand;
    @Shadow private float equipProgress;
    @Shadow private float lastEquipProgress;

    @Unique private float cool$swing;
    @Unique private boolean cool$armPass;
    @Unique private boolean cool$firstPerson;

    @Shadow
    public abstract void renderItem(net.minecraft.entity.LivingEntity entity, ItemStack stack, ModelTransformation.Mode mode);

    @Unique
    private static LegacyModules.OldAnimations cool$old() {
        if (!Hooks189.ready()) return null;
        LegacyModules.OldAnimations o = CoolClient.get().modules().get(LegacyModules.OldAnimations.class);
        return o != null && o.isEnabled() ? o : null;
    }

    @Inject(method = "renderArmHoldingItem", at = @At("HEAD"))
    private void cool$start(float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        cool$swing = mc.player == null ? 0f : mc.player.getHandSwingProgress(tickDelta);
        cool$firstPerson = true;
    }

    @Inject(method = "renderArmHoldingItem", at = @At("RETURN"))
    private void cool$end(float tickDelta, CallbackInfo ci) {
        cool$firstPerson = false;
    }

    // ---------------------------------------------------------------- 1.7 animations
    // applyEquipAndSwingOffset(equip, swing) ordinals: 0 none, 1 eat/drink, 2 block, 3 bow.

    @ModifyArg(method = "renderArmHoldingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipAndSwingOffset(FF)V", ordinal = 0), index = 1)
    private float cool$useNone(float swing) {
        LegacyModules.OldAnimations o = cool$old();
        return o != null && o.punchWhileUsing.on() ? cool$swing : swing;
    }

    @ModifyArg(method = "renderArmHoldingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipAndSwingOffset(FF)V", ordinal = 1), index = 1)
    private float cool$useEat(float swing) {
        LegacyModules.OldAnimations o = cool$old();
        return o != null && o.eating.on() ? cool$swing : swing;
    }

    @ModifyArg(method = "renderArmHoldingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipAndSwingOffset(FF)V", ordinal = 2), index = 1)
    private float cool$useBlock(float swing) {
        LegacyModules.OldAnimations o = cool$old();
        return o != null && o.blockHit.on() ? cool$swing : swing;
    }

    @ModifyArg(method = "renderArmHoldingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipAndSwingOffset(FF)V", ordinal = 3), index = 1)
    private float cool$useBow(float swing) {
        LegacyModules.OldAnimations o = cool$old();
        return o != null && o.bow.on() ? cool$swing : swing;
    }

    // ---------------------------------------------------------------- view model

    @Inject(method = "renderArmHoldingItem", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;pushMatrix()V", ordinal = 0, shift = At.Shift.AFTER))
    private void cool$viewModel(float tickDelta, CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        ViewModel vm = CoolClient.get().modules().get(ViewModel.class);
        if (vm == null || !vm.hasTransform()) return;
        GlStateManager.translate(vm.x.f(), vm.y.f(), vm.z.f());
        GlStateManager.rotate(vm.rotX.f(), 1f, 0f, 0f);
        GlStateManager.rotate(vm.rotY.f(), 0f, 1f, 0f);
        GlStateManager.rotate(vm.rotZ.f(), 0f, 0f, 1f);
    }

    /** Scale around the item itself: scaling at the camera (above) shrinks it toward the eye and looks identical. */
    @Inject(method = "renderArmHoldingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;)V"))
    private void cool$viewModelScale(float tickDelta, CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        ViewModel vm = CoolClient.get().modules().get(ViewModel.class);
        if (vm == null || !vm.hasTransform()) return;
        float s = vm.scale.f();
        GlStateManager.scale(s, s, s);
    }

    @Inject(method = "updateHeldItems", at = @At("TAIL"))
    private void cool$noEquip(CallbackInfo ci) {
        if (!Hooks189.ready()) return;
        ViewModel vm = CoolClient.get().modules().get(ViewModel.class);
        if (vm != null && vm.isEnabled() && vm.noEquipAnimation.on()) {
            equipProgress = 1f;
            lastEquipProgress = 1f;
        }
    }

    // ---------------------------------------------------------------- hand chams (own hand only)

    @Unique
    private static HandChams cool$chams() {
        if (!Hooks189.ready()) return null;
        HandChams c = Chams189.module();
        return c != null && c.isEnabled() ? c : null;
    }

    @Inject(method = "renderArmHoldingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;)V"))
    private void cool$itemChamsStart(float tickDelta, CallbackInfo ci) {
        HandChams c = cool$chams();
        if (c != null && c.item()) Chams189.begin(c.itemColor.color(), true);
    }

    @Inject(method = "renderArmHoldingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformation$Mode;)V",
            shift = At.Shift.AFTER))
    private void cool$itemChamsEnd(float tickDelta, CallbackInfo ci) {
        HandChams c = cool$chams();
        if (c == null || !c.item()) return;
        Chams189.end();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (c.wireframe() && mainHand != null && mc.player != null) {
            Chams189.beginWireframe(c.itemColor.color(), c.lineWidth.f(), true);
            renderItem(mc.player, mainHand, ModelTransformation.Mode.FIRST_PERSON);
            Chams189.endWireframe();
        }
        if (c.glow()) ((HeldItemRendererInvoker) this).cool$applyLighting(mc.player);
    }

    @Inject(method = "renderArm", at = @At("HEAD"))
    private void cool$armStart(AbstractClientPlayerEntity player, float equip, float swing, CallbackInfo ci) {
        HandChams c = cool$chams();
        if (c == null || !c.arm() || !cool$firstPerson || Chams189.wireframePass) return;
        Chams189.begin(c.armColor.color(), false);
    }

    @Inject(method = "renderArm", at = @At("RETURN"))
    private void cool$armEnd(AbstractClientPlayerEntity player, float equip, float swing, CallbackInfo ci) {
        HandChams c = cool$chams();
        if (c == null || !c.arm() || !cool$firstPerson || Chams189.wireframePass || cool$armPass) return;
        Chams189.end();
        if (c.wireframe()) {
            cool$armPass = true;
            Chams189.beginWireframe(c.armColor.color(), c.lineWidth.f(), false);
            ((HeldItemRendererInvoker) this).cool$renderArm(player, equip, swing);
            Chams189.endWireframe();
            cool$armPass = false;
        }
        if (c.glow()) ((HeldItemRendererInvoker) this).cool$applyLighting(player);
    }

    // ---------------------------------------------------------------- low fire

    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private void cool$fireStart(float tickDelta, CallbackInfo ci) {
        GlStateManager.pushMatrix();
        if (Hooks189.ready()) {
            VisualTweaks.LowFire lf = CoolClient.get().modules().get(VisualTweaks.LowFire.class);
            if (lf != null && lf.isEnabled()) GlStateManager.translate(0f, -lf.height.f(), 0f);
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private void cool$fireEnd(float tickDelta, CallbackInfo ci) {
        GlStateManager.popMatrix();
    }

    @ModifyArg(method = "renderFireOverlay", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/GlStateManager;color(FFFF)V"), index = 3)
    private float cool$fireAlpha(float alpha) {
        if (!Hooks189.ready()) return alpha;
        VisualTweaks.LowFire lf = CoolClient.get().modules().get(VisualTweaks.LowFire.class);
        return lf != null && lf.isEnabled() ? alpha * lf.opacity.f() / 100f : alpha;
    }
}
