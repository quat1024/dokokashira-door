package agency.highlysuspect.dokokashiradoor.net;

import agency.highlysuspect.dokokashiradoor.GatewayPersistentState;
import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.util.GatewayMap;
import agency.highlysuspect.dokokashiradoor.util.ServerPlayNetworkHandlerExt;
import agency.highlysuspect.dokokashiradoor.util.Util;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class DokoServerNet {
	public static void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(DokoMessages.GATEWAY_ACK, (server, player, handler, buf, responseSender) -> {
			int checksum = buf.readInt();
			
			server.execute(() -> {
				//update last known checksum on this player
				ServerPlayNetworkHandlerExt.cast(player.networkHandler).dokodoor$acknowledgeChecksum(checksum);
				
				//if it's the wrong checksum, send a full update
				GatewayPersistentState gps = GatewayPersistentState.getFor(player.getServerWorld());
				if(gps.gatewayChecksum != checksum) {
					Init.LOGGER.error("Sending full update as checksums did not match, expected {}, found {}", gps.gatewayChecksum, checksum);
					
					sendFullUpdate(player, gps.gateways);
				}
			});
		});
		
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			GatewayPersistentState gps = GatewayPersistentState.getFor(player.getServerWorld());
			Init.LOGGER.info("Sending full update to player");
			sendFullUpdate(player, gps.gateways);
		});
	}
	
	public static void sendFullUpdate(ServerPlayerEntity player, GatewayMap gateways) {
		Init.LOGGER.info("Full update to player {}, {}", player.getName().asString(), gateways);
		PacketByteBuf buf = PacketByteBufs.create();
		
		NbtCompound nbt = new NbtCompound();
		nbt.put("full_update", Util.writeNbt(GatewayMap.CODEC, gateways));
		buf.writeNbt(nbt);
		
		ServerPlayNetworking.send(player, DokoMessages.FULL_GATEWAY_UPDATE, buf);
	}
	
	public static void sendDeltaUpdate(ServerPlayerEntity player, GatewayMap additions, GatewayMap removals) {
		Init.LOGGER.info("Delta update to player " + player.getName().asString());
		PacketByteBuf buf = PacketByteBufs.create();
		
		NbtCompound nbt = new NbtCompound();
		nbt.put("additions", Util.writeNbt(GatewayMap.CODEC, additions));
		nbt.put("removals", Util.writeNbt(GatewayMap.CODEC, removals));
		buf.writeNbt(nbt);
		
		ServerPlayNetworking.send(player, DokoMessages.DELTA_GATEWAY_UPDATE, buf);
	}
}
