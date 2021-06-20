package agency.highlysuspect.dokokashiradoor.util;

import net.minecraft.server.network.ServerPlayNetworkHandler;

public interface ServerPlayNetworkHandlerExt {
	//The checksum clients self-report through the "acknowledgement" packet
	int dokodoor$getLastAcknowledgedChecksum();
	void dokodoor$acknowledgeChecksum(int checksum);
	
	//The checksum the server last attempted to send the client.
	//Used to prevent spamming clients with 10 "you're out of date, update" packets if the only reason the client hasn't acknowledged one yet is latency
	int dokodoor$getLastSentChecksum();
	void dokodoor$setLastSentChecksum(int checksum);
	
	default void dokodoor$invalidateGatewayChecksum() {
		//quick-and-dirty way to tell the server to send a full-update
		//might want to change this, it's kinda shite
		dokodoor$acknowledgeChecksum(-1);
		dokodoor$setLastSentChecksum(-1);
	}
	
	static ServerPlayNetworkHandlerExt cast(ServerPlayNetworkHandler a) {
		return (ServerPlayNetworkHandlerExt) a;
	}
}
