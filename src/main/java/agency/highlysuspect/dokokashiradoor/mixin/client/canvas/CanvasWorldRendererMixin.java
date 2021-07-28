package agency.highlysuspect.dokokashiradoor.mixin.client.canvas;

import agency.highlysuspect.dokokashiradoor.client.MatrixCache;
import grondag.canvas.render.world.CanvasWorldRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo //It's okay if Canvas isn't installed
@Mixin(CanvasWorldRenderer.class)
public class CanvasWorldRendererMixin {
	// "make sure all your mixin target methods are full signatures or it will break in production" - FoundationGames
	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;FJZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/util/math/Matrix4f;)V", at = @At("HEAD"))
	private void smuggleMatrices(MatrixStack viewStack, float a, long b, boolean c, Camera d, GameRenderer e, LightmapTextureManager f, Matrix4f projection, CallbackInfo ci) {
		MatrixCache.PROJECTION_MATRIX = projection.copy();
		MatrixCache.VIEW_MATRIX = viewStack.peek().getModel().copy();
	}
}
