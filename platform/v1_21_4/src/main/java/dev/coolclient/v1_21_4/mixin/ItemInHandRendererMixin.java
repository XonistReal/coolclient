package dev.coolclient.v1_21_4.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.coolclient.core.CoolClient;
import dev.coolclient.core.modules.cpvp.CrystalPvpTweaks;
import dev.coolclient.core.modules.visual.ViewModel;
import dev.coolclient.v1_21_4.Hooks21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;
    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;
    @Shadow private float offHandHeight;
    @Shadow private float oOffHandHeight;

    // ---------------------------------------------------------------- hand chams (own hand only)

    @ModifyArg(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IFFLnet/minecraft/world/entity/HumanoidArm;)V"),
            index = 1)
    private MultiBufferSource cool$armChams(MultiBufferSource buffers) {
        return Hooks21.chams(buffers, true);
    }

    @ModifyArg(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            index = 5)
    private MultiBufferSource cool$itemChams(MultiBufferSource buffers) {
        return Hooks21.chams(buffers, false);
    }

    // ---------------------------------------------------------------- view model

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER))
    private void cool$viewModel(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swing,
                                  ItemStack stack, float equip, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!Hooks21.ready()) return;
        ViewModel vm = CoolClient.get().modules().get(ViewModel.class);
        if (vm == null || !vm.hasTransform()) return;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        pose.translate(vm.x.f() * side, vm.y.f(), vm.z.f());
        pose.mulPose(new Quaternionf().rotationXYZ((float) Math.toRadians(vm.rotX.f()),
                (float) Math.toRadians(vm.rotY.f() * side), (float) Math.toRadians(vm.rotZ.f() * side)));
        float s = vm.scale.f();
        pose.scale(s, s, s);
    }

    // ---------------------------------------------------------------- low shield

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void cool$lowShield(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swing,
                                  ItemStack stack, float equip, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!Hooks21.ready() || !stack.is(Items.SHIELD)) return;
        CrystalPvpTweaks.ShieldTweaks t = CoolClient.get().modules().get(CrystalPvpTweaks.ShieldTweaks.class);
        if (t == null || !t.isEnabled()) return;
        boolean blocking = player.isUsingItem() && player.getUsedItemHand() == hand;
        if (t.onlyIdle.on() && blocking) return;
        pose.translate(0f, -t.lower.f(), 0f);
        float s = t.scale.f();
        pose.scale(s, s, s);
    }

    // ---------------------------------------------------------------- no re-equip animation

    @Inject(method = "tick", at = @At("TAIL"))
    private void cool$noEquip(CallbackInfo ci) {
        if (!Hooks21.ready()) return;
        ViewModel vm = CoolClient.get().modules().get(ViewModel.class);
        if (vm == null || !vm.isEnabled() || !vm.noEquipAnimation.on()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mainHandItem = mc.player.getMainHandItem();
        offHandItem = mc.player.getOffhandItem();
        mainHandHeight = oMainHandHeight = 1f;
        offHandHeight = oOffHandHeight = 1f;
    }
}
