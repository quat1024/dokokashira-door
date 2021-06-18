package agency.highlysuspect.anywheredoor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Init implements ModInitializer {
	public static final String MODID = "anywheredoor";
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	
	@Override
	public void onInitialize() {
		ServerTickEvents.START_WORLD_TICK.register(world -> GatewayPersistentState.getFor(world).tick(world));
	}
}
