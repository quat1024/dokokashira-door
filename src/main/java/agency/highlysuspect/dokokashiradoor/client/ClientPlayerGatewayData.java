package agency.highlysuspect.dokokashiradoor.client;

import agency.highlysuspect.dokokashiradoor.util.GatewayMap;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

public class ClientPlayerGatewayData {
	private GatewayMap gateways;
	private int randomSeed;
	
	public @Nullable GatewayMap getGateways() {
		return gateways;
	}
	
	public void setGateways(GatewayMap gateways) {
		this.gateways = gateways;
	}
	
	public int getRandomSeed() {
		return randomSeed;
	}
	
	public void setRandomSeed(int randomSeed) {
		this.randomSeed = randomSeed;
	}
	
	public static ClientPlayerGatewayData get() {
		if(MinecraftClient.getInstance().player == null) return null;
		else return ClientPlayNetworkHandlerExt.cast(MinecraftClient.getInstance().player.networkHandler).doko$getData();
	}
}
