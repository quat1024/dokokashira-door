package agency.highlysuspect.dokokashiradoor.mixin.client;

import agency.highlysuspect.dokokashiradoor.client.ClientPlayNetworkHandlerExt;
import agency.highlysuspect.dokokashiradoor.client.ClientPlayerGatewayData;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin implements ClientPlayNetworkHandlerExt {
	@Unique private final ClientPlayerGatewayData data = new ClientPlayerGatewayData();
	
	@Override
	public ClientPlayerGatewayData doko$getData() {
		return data;
	}
}
