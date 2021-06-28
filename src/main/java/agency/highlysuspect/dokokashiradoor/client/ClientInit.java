package agency.highlysuspect.dokokashiradoor.client;

import agency.highlysuspect.dokokashiradoor.net.DokoClientNet;
import net.fabricmc.api.ClientModInitializer;

public class ClientInit implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DokoClientNet.onInitializeClient();
	}
}
