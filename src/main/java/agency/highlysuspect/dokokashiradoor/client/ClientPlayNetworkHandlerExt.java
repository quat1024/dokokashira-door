package agency.highlysuspect.dokokashiradoor.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;

public interface ClientPlayNetworkHandlerExt {
	ClientPlayerGatewayData dokodoor$getExtension();
	
	static ClientPlayNetworkHandlerExt cast(ClientPlayNetworkHandler a) {
		return (ClientPlayNetworkHandlerExt) a;
	}
}
