package agency.highlysuspect.dokokashiradoor.util;

import agency.highlysuspect.dokokashiradoor.net.DokoServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;

public interface ServerPlayNetworkHandlerExt {
	DokoServerPlayNetworkHandler dokodoor$getExtension();
	
	default void dokodoor$recvChecksum(ServerWorld world, int checksum) {
		dokodoor$getExtension().recvChecksum((ServerPlayNetworkHandler) this, world, checksum);
	}
	
	default void dokodoor$dimensionChange(ServerWorld world) {
		dokodoor$getExtension().onDimensionChange((ServerPlayNetworkHandler) this, world);
	}
	
	static ServerPlayNetworkHandlerExt cast(ServerPlayNetworkHandler a) {
		return (ServerPlayNetworkHandlerExt) a;
	}
}
