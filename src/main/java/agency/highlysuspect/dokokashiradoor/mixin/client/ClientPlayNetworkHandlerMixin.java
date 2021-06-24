package agency.highlysuspect.dokokashiradoor.mixin.client;

import agency.highlysuspect.dokokashiradoor.util.ClientPlayNetworkHandlerExt;
import agency.highlysuspect.dokokashiradoor.tp.DokoClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin implements ClientPlayNetworkHandlerExt {
	@Unique private final DokoClientPlayNetworkHandler data = new DokoClientPlayNetworkHandler();
	
	@Override
	public DokoClientPlayNetworkHandler dokodoor$getExtension() {
		return data;
	}
}
