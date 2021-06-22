package agency.highlysuspect.dokokashiradoor.client;
import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.net.DokoMessages;
import agency.highlysuspect.dokokashiradoor.util.GatewayMap;
import agency.highlysuspect.dokokashiradoor.util.Util;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class DokoClientNet implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(DokoMessages.FULL_GATEWAY_UPDATE, (client, handler, buf, responseSender) -> {
			Identifier worldKeyId = buf.readIdentifier();
			NbtCompound nbt = buf.readNbt();
				
			client.execute(() -> {
				if(client.player == null) { Init.LOGGER.error("Recv gateway update but no player. Ignoring"); return; }
				
				RegistryKey<World> worldKey = RegistryKey.of(Registry.WORLD_KEY, worldKeyId);
				
				if(nbt == null) { Init.LOGGER.error("Recv null gatewaymap nbt"); return; }
				GatewayMap map = Util.readNbt(GatewayMap.CODEC, nbt.get("full_update"));
				if(map == null) { Init.LOGGER.error("Recv invalid gatewaymap nbt"); return; }
				
				ClientPlayerGatewayData.get().ifPresent(d -> d.fullGatewayUpdate(worldKey, map));
			});
		});
		
		ClientPlayNetworking.registerGlobalReceiver(DokoMessages.DELTA_GATEWAY_UPDATE, (client, handler, buf, responseSender) -> {
			Identifier worldKeyId = buf.readIdentifier();
			NbtCompound nbt = buf.readNbt();
			
			client.execute(() -> {
				if(nbt == null) { Init.LOGGER.error("Received null deltaupdate gateway map nbt"); return; }
				
				RegistryKey<World> worldKey = RegistryKey.of(Registry.WORLD_KEY, worldKeyId);
				
				GatewayMap additions = Util.readNbt(GatewayMap.CODEC, nbt.get("additions"));
				if(additions == null) { Init.LOGGER.error("received invalid gatewaymap additions"); return; }
				
				GatewayMap removals = Util.readNbt(GatewayMap.CODEC, nbt.get("removals"));
				if(removals == null) { Init.LOGGER.error("received invalid gatewaymap removals"); return; }
				
				ClientPlayerGatewayData.get().ifPresent(d -> {
					PacketByteBuf yes = PacketByteBufs.create();
					yes.writeIdentifier(worldKey.getValue());
					yes.writeInt(d.deltaGatewayUpdate(worldKey, additions, removals));
					responseSender.sendPacket(DokoMessages.DELTA_GATEWAY_ACK, yes);
				});
			});
		});
		
		ClientPlayNetworking.registerGlobalReceiver(DokoMessages.ADD_RANDOM_SEEDS, (client, handler, buf, responseSender) -> {
			IntList newSeeds = buf.readIntList();
			
			client.execute(() -> ClientPlayerGatewayData.get().ifPresent(d -> {
				PacketByteBuf yes = PacketByteBufs.create();
				yes.writeInt(d.deltaRandomSeeds(newSeeds));
				responseSender.sendPacket(DokoMessages.RANDOM_SEEDS_ACK, yes);
			}));
		});
		
		ClientPlayNetworking.registerGlobalReceiver(DokoMessages.SET_RANDOM_SEEDS, (client, handler, buf, responseSender) -> {
			IntList newSeeds = buf.readIntList();
			client.execute(() -> ClientPlayerGatewayData.get().ifPresent(d -> d.fullRandomSeeds(newSeeds)));
		});
	}
}
