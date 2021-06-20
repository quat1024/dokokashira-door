package agency.highlysuspect.dokokashiradoor.mixin;

import agency.highlysuspect.dokokashiradoor.util.ServerPlayNetworkHandlerExt;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin implements ServerPlayNetworkHandlerExt {
	@Unique
	private int gatewayModCount = -1;
	
	@Override
	public int dokodoor$getGatewayChecksum() {
		return gatewayModCount;
	}
	
	@Override
	public void dokodoor$setGatewayChecksum(int modCount) {
		gatewayModCount = modCount;
	}
}
