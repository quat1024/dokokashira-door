package agency.highlysuspect.dokokashiradoor.net;

import agency.highlysuspect.dokokashiradoor.gateway.GatewayMap;
import agency.highlysuspect.dokokashiradoor.net.payload.AcknowledgeDeltaGatewayC2SPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.AcknowledgeRandomSeedsPayloadC2S;
import agency.highlysuspect.dokokashiradoor.net.payload.AddRandomSeedsPayloadS2C;
import agency.highlysuspect.dokokashiradoor.net.payload.DeltaGatewayUpdateS2CPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.DoorTeleportRequestC2SPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.FullGatewayUpdateS2CPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.SetRandomSeedsPayloadS2C;
import agency.highlysuspect.dokokashiradoor.tp.DokoServerPlayNetworkHandler;
import agency.highlysuspect.dokokashiradoor.tp.ServerDoorTp;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DokoServerNet {
	public static void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(AcknowledgeDeltaGatewayC2SPayload.ID, (payload, context) -> {
			MinecraftServer server = context.server();

			Identifier worldKeyAck = payload.worldKeyId();
			int checksum = payload.checksum();
			
			server.execute(() -> {
				DokoServerPlayNetworkHandler ext = DokoServerPlayNetworkHandler.getFor(context.player());
				
				//Obtain the client's mentioned world without creating a RegistryKey.
				//RegistryKey.of caches its return values until the end of time.
				//It's not safe to call that on user-controlled data without checking, memory-exhaustion vector.
				for(ServerWorld world : server.getWorlds()) {
					if(world.getRegistryKey().getValue().equals(worldKeyAck)) {
						ext.ackGatewayChecksum(world, checksum);
						return;
					}
				}
			});
		});
		
		ServerPlayNetworking.registerGlobalReceiver(AcknowledgeRandomSeedsPayloadC2S.ID, (payload, context) -> {
			MinecraftServer server = context.server();
			int checksum = payload.checksum();
			server.execute(() -> DokoServerPlayNetworkHandler.getFor(context.player()).ackRandomSeedChecksum(checksum));
		});
		
		ServerPlayNetworking.registerGlobalReceiver(DoorTeleportRequestC2SPayload.ID, (payload, context) -> {
			MinecraftServer server = context.server();
			BlockPos leftFromPos = payload.leftFrom();
			BlockPos destPos = payload.destination();
			server.execute(() -> ServerDoorTp.confirmDoorTeleport(leftFromPos, destPos, context.player()));
		});
	}
	
	public static void sendFullGatewayUpdate(ServerPlayerEntity player, RegistryKey<World> wkey, GatewayMap gateways) {
		ServerPlayNetworking.send(player, new FullGatewayUpdateS2CPayload(wkey.getValue(), gateways));
	}
	
	public static void sendDeltaGatewayUpdate(ServerPlayerEntity player, RegistryKey<World> wkey, GatewayMap additions, GatewayMap removals) {
		ServerPlayNetworking.send(player, new DeltaGatewayUpdateS2CPayload(wkey.getValue(), additions, removals));
	}
	
	public static void addRandomSeeds(ServerPlayerEntity player, IntList moreSeeds) {
		ServerPlayNetworking.send(player, new AddRandomSeedsPayloadS2C(moreSeeds));
	}
	
	public static void setRandomSeeds(ServerPlayerEntity player, IntList randomSeeds) {
		ServerPlayNetworking.send(player, new SetRandomSeedsPayloadS2C(randomSeeds));
	}
}
