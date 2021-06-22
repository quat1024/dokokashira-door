package agency.highlysuspect.dokokashiradoor.util;

import agency.highlysuspect.dokokashiradoor.net.DokoServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public interface ServerPlayNetworkHandlerExt {
	DokoServerPlayNetworkHandler dokodoor$getExtension();
	
	static ServerPlayNetworkHandlerExt cast(ServerPlayNetworkHandler a) {
		return (ServerPlayNetworkHandlerExt) a;
	}
}
