package agency.highlysuspect.dokokashiradoor.net;

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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class DokoServerNet {
	public static void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(DokoMessages.DELTA_GATEWAY_ACK, (server, player, handler, buf, responseSender) -> {
			Identifier worldKeyAck = buf.readIdentifier();
			int checksum = buf.readInt();
			
			server.execute(() -> {
				//Obtain the client's mentioned world without creating a RegistryKey.
				//RegistryKey.of caches its return values until the end of time.
				//It's not safe to call that on user-controlled data without checking, memory-exhaustion vector.
				for(ServerWorld world : server.getWorlds()) {
					if(world.getRegistryKey().getValue().equals(worldKeyAck)) {
						ServerPlayNetworkHandlerExt.cast(player.networkHandler).dokodoor$recvChecksum(world, checksum);
						return;
					}
				}
			});
		});
	}
	
	public static void sendFullUpdate(ServerPlayerEntity player, RegistryKey<World> wkey, GatewayMap gateways) {
		Init.LOGGER.info("Full update to player {}", player.getEntityName());
		Init.LOGGER.info("\tWorld:    {}", wkey);
		Init.LOGGER.info("\tContents: {}", gateways);
		PacketByteBuf buf = PacketByteBufs.create();
		
		buf.writeIdentifier(wkey.getValue());
		
		NbtCompound nbt = new NbtCompound();
		nbt.put("full_update", Util.writeNbt(GatewayMap.CODEC, gateways));
		buf.writeNbt(nbt);
		
		ServerPlayNetworking.send(player, DokoMessages.FULL_GATEWAY_UPDATE, buf);
	}
	
	public static void sendDeltaUpdate(ServerPlayerEntity player, RegistryKey<World> wkey, GatewayMap additions, GatewayMap removals) {
		Init.LOGGER.info("Delta update to player {}", player.getEntityName());
		Init.LOGGER.info("\tWorld:     {}", wkey.getValue());
		Init.LOGGER.info("\tAdditions: {}", additions);
		Init.LOGGER.info("\tRemovals:  {}", removals);
		PacketByteBuf buf = PacketByteBufs.create();
		
		buf.writeIdentifier(wkey.getValue());
		
		NbtCompound nbt = new NbtCompound();
		nbt.put("additions", Util.writeNbt(GatewayMap.CODEC, additions));
		nbt.put("removals", Util.writeNbt(GatewayMap.CODEC, removals));
		buf.writeNbt(nbt);
		
		ServerPlayNetworking.send(player, DokoMessages.DELTA_GATEWAY_UPDATE, buf);
	}
}
