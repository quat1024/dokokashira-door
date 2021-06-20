package agency.highlysuspect.dokokashiradoor;

import agency.highlysuspect.dokokashiradoor.net.DokoServerNet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Init implements ModInitializer {
	public static final String MODID = "dokokashira_door";
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	
//	public static final PointOfInterestType DOOR_TOP_HALVES = PointOfInterestHelper.register(
//		id("door_tops"),
//		0, //ticket count
//		1, //search distance (might need to raise this)
//		Stream.of(Blocks.SPRUCE_DOOR, Blocks.BIRCH_DOOR, Blocks.DARK_OAK_DOOR, Blocks.CRIMSON_DOOR, Blocks.WARPED_DOOR) //all the opaque doors, under vanilla textures at least
//			.flatMap(b -> b.getStateManager().getStates().stream())
//			.filter(s -> s.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER)
//			.collect(Collectors.toList())
//	);
	
	@Override
	public void onInitialize() {
		DokoServerNet.onInitialize();
		
		ServerTickEvents.START_WORLD_TICK.register(world -> GatewayPersistentState.getFor(world).tick(world));
	}
	
	public static Identifier id(String path) {
		return new Identifier(MODID, path);
	}
}
