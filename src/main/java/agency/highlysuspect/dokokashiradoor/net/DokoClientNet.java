package agency.highlysuspect.dokokashiradoor.net;

import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.gateway.GatewayMap;
import agency.highlysuspect.dokokashiradoor.net.payload.AcknowledgeDeltaGatewayC2SPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.AcknowledgeRandomSeedsPayloadC2S;
import agency.highlysuspect.dokokashiradoor.net.payload.AddRandomSeedsPayloadS2C;
import agency.highlysuspect.dokokashiradoor.net.payload.DeltaGatewayUpdateS2CPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.DoorTeleportRequestC2SPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.FullGatewayUpdateS2CPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.SetRandomSeedsPayloadS2C;
import agency.highlysuspect.dokokashiradoor.tp.DokoClientPlayNetworkHandler;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DokoClientNet {
	public static void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(FullGatewayUpdateS2CPayload.ID, (payload, context) -> {
			MinecraftClient client = context.client();

			Identifier worldKeyId = payload.worldKeyId();
			GatewayMap map = payload.gateways();

			client.execute(() -> {
				if(client.player == null) { Init.LOGGER.error("Recv gateway update but no player. Ignoring"); return; }
				
				RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldKeyId);
				
				DokoClientPlayNetworkHandler.get(client.player).fullGatewayUpdate(worldKey, map);
			});
		});
		
		ClientPlayNetworking.registerGlobalReceiver(DeltaGatewayUpdateS2CPayload.ID, (payload, context) -> {
			MinecraftClient client = context.client();

			Identifier worldKeyId = payload.worldKeyId();
			GatewayMap additions = payload.additions();
			GatewayMap removals = payload.removals();
			
			client.execute(() -> {
				if(client.player == null) { Init.LOGGER.error("Recv gateway update but no player. Ignoring"); return; }
				
				RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, worldKeyId);
				
				DokoClientPlayNetworkHandler cpgd = DokoClientPlayNetworkHandler.get(client.player);
				
				context.responseSender().sendPacket(new AcknowledgeDeltaGatewayC2SPayload(worldKey.getValue(), cpgd.deltaGatewayUpdate(worldKey, additions, removals)));
			});
		});
		
		ClientPlayNetworking.registerGlobalReceiver(AddRandomSeedsPayloadS2C.ID, (payload, context) -> {
			MinecraftClient client = context.client();

			IntList newSeeds = payload.newSeeds();
			
			client.execute(() -> {
				if(client.player == null) { Init.LOGGER.error("Recv random seeds but no player. Ignoring"); return; }
				DokoClientPlayNetworkHandler cpgd = DokoClientPlayNetworkHandler.get(client.player);
				
				context.responseSender().sendPacket(new AcknowledgeRandomSeedsPayloadC2S(cpgd.deltaRandomSeeds(newSeeds)));
			});
		});
		
		ClientPlayNetworking.registerGlobalReceiver(SetRandomSeedsPayloadS2C.ID, (payload, context) -> {
			MinecraftClient client = context.client();
			IntList newSeeds = payload.newSeeds();
			client.execute(() -> {
				if(client.player == null) { Init.LOGGER.error("Recv random seeds but no player. Ignoring"); return; }
				DokoClientPlayNetworkHandler.get(client.player).fullRandomSeeds(newSeeds);
			});
		});
	}
	
	public static void sendDoorTeleport(BlockPos leftFrom, BlockPos destination) {
		ClientPlayNetworking.send(new DoorTeleportRequestC2SPayload(leftFrom, destination));
	}
}
