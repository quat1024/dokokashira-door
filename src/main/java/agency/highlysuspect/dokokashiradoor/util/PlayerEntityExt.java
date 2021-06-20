package agency.highlysuspect.dokokashiradoor.util;

import net.minecraft.entity.player.PlayerEntity;

public interface PlayerEntityExt {
	int dokodoor$getGatewayRandomSeed();
	void dokodoor$setGatewayRandomSeed(int seed);
	
	static PlayerEntityExt cast(PlayerEntity player) {
		return (PlayerEntityExt) player;
	}
}
