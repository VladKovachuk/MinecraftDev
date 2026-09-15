package com.example.mixin.client;

import com.example.ExampleModClient;
import com.example.shader.ShaderManager;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Updates effects once per frame; Joint runs before GUI, cigarettes retain their final-frame pass.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render(FJZ)V", at = @At("HEAD"))
    private void smokemod$updateEffects(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        ShaderManager manager = ExampleModClient.getShaderManager();
        if (manager != null) manager.updateUniforms(tickDelta);
    }

    @Inject(method = "renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V", at = @At("TAIL"))
    private void smokemod$renderJoint(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        ShaderManager manager = ExampleModClient.getShaderManager();
        if (manager != null) manager.renderJoint(tickDelta);
    }

    @Inject(method = "render(FJZ)V", at = @At("TAIL"))
    private void smokemod$renderPostEffect(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        ShaderManager shaderManager = ExampleModClient.getShaderManager();
        if (shaderManager != null) {
            shaderManager.render(tickDelta);
        }
    }
}
