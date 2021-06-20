package agency.highlysuspect.dokokashiradoor.mixin;

import agency.highlysuspect.dokokashiradoor.util.PlayerEntityExt;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements PlayerEntityExt {
	@Unique private static final TrackedData<Integer> gatewayRandomSeed = DataTracker.registerData(PlayerEntity.class, TrackedDataHandlerRegistry.INTEGER);
	
	@Inject(method = "initDataTracker", at = @At("RETURN"))
	private void whenInittingDataTracker(CallbackInfo ci) {
		//TODO read this from nbt or whatever
		((PlayerEntity) (Object) this).getDataTracker().startTracking(gatewayRandomSeed, 0);
	}
	
	@Override
	public int dokodoor$getGatewayRandomSeed() {
		return ((PlayerEntity) (Object) this).getDataTracker().get(gatewayRandomSeed);
	}
	
	@Override
	public void dokodoor$setGatewayRandomSeed(int seed) {
		((PlayerEntity) (Object) this).getDataTracker().set(gatewayRandomSeed, seed);
	}
}
