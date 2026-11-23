package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.BattleCamera;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.p1nero.cobblemon.rumblecore.client.combat.RenderAimRay;
import com.p1nero.cobblemon.rumblecore.client.combat.StableBattleCamera;
import com.p1nero.cobblemon.rumblecore.client.combat.ReticleProjection;
import java.util.function.Predicate;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.joml.Matrix4f;

@Mixin(value = BattleCamera.class, remap = false)
abstract class BattleCameraInterpolation {
    @Shadow private static float shoulder;
    @Shadow public static float sideView;
    @Shadow private static float aimX;
    @Shadow private static float aimY;
    @Shadow private static boolean aimOnScreen;

    @Inject(method = "reset", at = @At("TAIL"))
    private static void rumble$resetRig(CallbackInfo ci) {
        StableBattleCamera.reset();
        ReticleProjection.reset();
    }

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private static void rumble$stableSideView(Camera camera, BlockGetter level, Entity entity, float partial,
                                         CallbackInfoReturnable<BattleCamera.Pose> cir) {
        if (!ClientSmash.active() || entity != Minecraft.getInstance().player) {
            StableBattleCamera.reset();
            ReticleProjection.reset();
        } else if (sideView > 0) {
            // RTC returns side-view poses before reaching the normal rig calls below.
            cir.setReturnValue(rumble$stablePose(level, entity, partial));
        }
    }

    // Replace the base pose, leaving RTC free to apply its Z-Move camera on top.
    @ModifyExpressionValue(method = "place", at = {
            @At(value = "INVOKE", target = "Lcom/johnn/realtimecobblemon/client/BattleCamera;duel(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;FF)Lcom/johnn/realtimecobblemon/client/BattleCamera$Pose;"),
            @At(value = "INVOKE", target = "Lcom/johnn/realtimecobblemon/client/BattleCamera;shoulder(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;FF)Lcom/johnn/realtimecobblemon/client/BattleCamera$Pose;")
    }, require = 2)
    private static BattleCamera.Pose rumble$stableBasePose(BattleCamera.Pose original, Camera camera,
                                                            BlockGetter level, Entity entity, float partial) {
        return rumble$stablePose(level, entity, partial);
    }

    @Unique
    private static BattleCamera.Pose rumble$stablePose(BlockGetter level, Entity entity, float partial) {
        LivingEntity target = BattleCamera.target();
        return StableBattleCamera.place(level, entity, target, partial, shoulder, sideView);
    }

    // Project the reticle from the same in-between position used to draw the target, not its tick AABB.
    @Redirect(method = "onRenderLevel0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private static AABB rumble$interpolateTarget(LivingEntity target, RenderLevelStageEvent event) {
        float partial = event.getCamera().getPartialTickTime();
        return RenderAimRay.renderBounds(target, partial);
    }

    @ModifyExpressionValue(method = "onRenderLevel0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaPartialTick(Z)F"))
    private static float rumble$sameFrameTime(float original, RenderLevelStageEvent event) {
        return event.getCamera().getPartialTickTime();
    }

    @Redirect(method = "onRenderLevel0", at = @At(value = "INVOKE",
            target = "Lcom/johnn/realtimecobblemon/client/BattleCamera;project(Lnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lnet/minecraft/world/phys/Vec3;)V"))
    private static void rumble$preciseProjection(Camera camera, Matrix4f projection, Vec3 point, RenderLevelStageEvent event) {
        // Use the exact view/projection matrices of this world frame, including roll and projection effects.
        ReticleProjection.project(camera.getPosition(), event.getModelViewMatrix(), projection, point);
        aimOnScreen = ReticleProjection.visible();
        aimX = (float) ReticleProjection.x();
        aimY = (float) ReticleProjection.y();
    }

    @Redirect(method = "ring", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getPosition(F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 rumble$ringAtRenderedFeet(LivingEntity target, float partial) {
        return RenderAimRay.renderPosition(target, partial);
    }

    @Redirect(method = "aimPoint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 rumble$rayFromRenderedBody(LocalPlayer player, float partial) {
        return RenderAimRay.renderPosition(player, partial).add(0, player.getEyeHeight(), 0);
    }

    // Free aim already interpolates the eye ray, but vanilla projectile picking still used tick hitboxes.
    @Redirect(method = "aimPoint", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"))
    private static EntityHitResult rumble$interpolateFreeAim(Entity viewer, Vec3 from, Vec3 blockHit, AABB bounds,
            Predicate<Entity> canHit, double rangeSquared, LocalPlayer player, float partial) {
        return RenderAimRay.pick(viewer, from, blockHit, bounds, canHit, rangeSquared, partial);
    }
}
