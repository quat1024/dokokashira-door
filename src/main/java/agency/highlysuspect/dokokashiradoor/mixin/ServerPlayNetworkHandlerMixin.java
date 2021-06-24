package agency.highlysuspect.dokokashiradoor.mixin;

import agency.highlysuspect.dokokashiradoor.tp.DokoServerPlayNetworkHandler;
import agency.highlysuspect.dokokashiradoor.util.ServerPlayNetworkHandlerExt;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin implements ServerPlayNetworkHandlerExt {
	private final @Unique DokoServerPlayNetworkHandler ext = new DokoServerPlayNetworkHandler((ServerPlayNetworkHandler) (Object) this);
	
	@Override
	public DokoServerPlayNetworkHandler dokodoor$getExtension() {
		return ext;
	}
	
	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		ext.tick();
	}
}
