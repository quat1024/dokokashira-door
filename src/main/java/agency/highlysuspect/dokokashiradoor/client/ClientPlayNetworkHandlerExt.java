package agency.highlysuspect.dokokashiradoor.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;

public interface ClientPlayNetworkHandlerExt {
	ClientPlayerGatewayData doko$getData();
	void doko$setData(ClientPlayerGatewayData data);
	
	static ClientPlayNetworkHandlerExt cast(ClientPlayNetworkHandler a) {
		return (ClientPlayNetworkHandlerExt) a;
	}
}
