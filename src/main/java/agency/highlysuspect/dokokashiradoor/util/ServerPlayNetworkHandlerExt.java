package agency.highlysuspect.dokokashiradoor.util;

import net.minecraft.server.network.ServerPlayNetworkHandler;

public interface ServerPlayNetworkHandlerExt {
	int dokodoor$getGatewayChecksum();
	void dokodoor$setGatewayChecksum(int modCount);
	
	default void dokodoor$invalidateGatewayChecksum() {
		//quick-and-dirty way to tell the server to send a full-update, might want to change this
		dokodoor$setGatewayChecksum(-1);
	}
	
	static ServerPlayNetworkHandlerExt cast(ServerPlayNetworkHandler a) {
		return (ServerPlayNetworkHandlerExt) a;
	}
}
