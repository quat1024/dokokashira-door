package agency.highlysuspect.dokokashiradoor.client;
import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.net.DokoMessages;
import agency.highlysuspect.dokokashiradoor.util.GatewayMap;
import agency.highlysuspect.dokokashiradoor.util.Util;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

public class DokoClientNet implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(DokoMessages.FULL_GATEWAY_UPDATE, (client, handler, buf, responseSender) -> {
			NbtCompound uwu = buf.readNbt();
				
			client.execute(() -> {
				if(client.player == null) {
					Init.LOGGER.error("Recv gateway update but no player. Ignoring");
					return;
					
				}
				if(uwu == null) {
					Init.LOGGER.error("Recv null gatewaymap nbt");
					return;
				}
				
				GatewayMap update = Util.readNbt(GatewayMap.CODEC, uwu.get("full_update"));
				if(update == null) {
					Init.LOGGER.error("Recv invalid gatewaymap nbt");
					return;
				}
				
				ClientPlayerGatewayData data = ClientPlayerGatewayData.get();
				if(data != null) {
					data.setGateways(update);
					Init.LOGGER.info("Recv: {}", update);
					sendAck(responseSender, update.checksum());
				}
			});
		});
		
		ClientPlayNetworking.registerGlobalReceiver(DokoMessages.DELTA_GATEWAY_UPDATE, (client, handler, buf, responseSender) -> {
			NbtCompound uwu = buf.readNbt();
			
			client.execute(() -> {
				if(uwu == null) {
					Init.LOGGER.error("Received null deltaupdate gateway map nbt");
					return;
				}
				
				GatewayMap additions = Util.readNbt(GatewayMap.CODEC, uwu.get("additions"));
				if(additions == null) {
					Init.LOGGER.error("received invalid gatewaymap additions");
					return;
				}
				
				GatewayMap removals = Util.readNbt(GatewayMap.CODEC, uwu.get("removals"));
				if(removals == null) {
					Init.LOGGER.error("received invalid gatewaymap removals");
					return;
				}
				
				//TODO: Do something with the GatewayMaps
				ClientPlayerGatewayData data = ClientPlayerGatewayData.get();
				if(data != null) {
					GatewayMap map = data.getGateways();
					if(map == null) {
						map = new GatewayMap();
						data.setGateways(map);
					}
					
					map.putAll(additions);
					map.removeIf(removals::containsValue);
					
					sendAck(responseSender, map.checksum());
				}
			});
		});
	}
	
	public void sendAck(PacketSender resp, int checksum) {
		PacketByteBuf yes = PacketByteBufs.create();
		yes.writeInt(checksum);
		resp.sendPacket(DokoMessages.GATEWAY_ACK, yes);
	}
}
