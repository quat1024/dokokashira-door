package agency.highlysuspect.dokokashiradoor;

import agency.highlysuspect.dokokashiradoor.gateway.GatewayPersistentState;
import agency.highlysuspect.dokokashiradoor.net.DokoMessages;
import agency.highlysuspect.dokokashiradoor.net.DokoServerNet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Init implements ModInitializer {
	public static final String MODID = "dokokashira_door";
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	
	public static final TagKey<Block> OPAQUE_DOORS = TagKey.of(RegistryKeys.BLOCK, id("opaque_doors"));
	
	@Override
	public void onInitialize() {
		DokoMessages.onInitialize();
		DokoServerNet.onInitialize();
		
		ServerTickEvents.END_WORLD_TICK.register(world -> GatewayPersistentState.getFor(world).tick(world));
	}
	
	public static Identifier id(String path) {
		return Identifier.of(MODID, path);
	}
}
