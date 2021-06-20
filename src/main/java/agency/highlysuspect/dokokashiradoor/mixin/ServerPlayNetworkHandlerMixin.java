package agency.highlysuspect.dokokashiradoor.mixin;

import agency.highlysuspect.dokokashiradoor.util.ServerPlayNetworkHandlerExt;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin implements ServerPlayNetworkHandlerExt {
	@Unique
	private int gatewayChecksumClient = -1;
	
	@Unique
	private int gatewayChecksumServer = -1;
	
	@Override
	public int dokodoor$getLastAcknowledgedChecksum() {
		return gatewayChecksumClient;
	}
	
	@Override
	public void dokodoor$acknowledgeChecksum(int checksum) {
		gatewayChecksumClient = checksum;
	}
	
	@Override
	public int dokodoor$getLastSentChecksum() {
		return gatewayChecksumServer;
	}
	
	@Override
	public void dokodoor$setLastSentChecksum(int checksum) {
		gatewayChecksumServer = checksum;
	}
}
