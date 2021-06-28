package agency.highlysuspect.dokokashiradoor.mixin.client;

import agency.highlysuspect.dokokashiradoor.client.MatrixCache;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void smuggleMatrices(MatrixStack viewStack, float a, long b, boolean c, Camera d, GameRenderer e, LightmapTextureManager f, Matrix4f projection, CallbackInfo ci) {
		MatrixCache.PROJECTION_MATRIX = projection.copy();
		MatrixCache.VIEW_MATRIX = viewStack.peek().getModel().copy();
	}
}
