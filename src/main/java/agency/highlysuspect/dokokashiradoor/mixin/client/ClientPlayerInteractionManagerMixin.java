package agency.highlysuspect.dokokashiradoor.mixin.client;

import agency.highlysuspect.dokokashiradoor.util.ClientPlayerInteractionManagerExt;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin implements ClientPlayerInteractionManagerExt {
	@Unique private boolean skipBlockInteractionPacket = false;
	
	@Inject(method = "interactBlock", at = @At("HEAD"))
	private void startInteracting(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
		skipBlockInteractionPacket = false;
	}
	
	@Inject(
		method = "interactBlock",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/util/ActionResult;isAccepted()Z"
		),
		cancellable = true,
		locals = LocalCapture.CAPTURE_FAILSOFT //This is not a very important feature of the mod.
	)
	private void beforeSending(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir, BlockPos a, ItemStack b, boolean c, ActionResult result) {
		if(skipBlockInteractionPacket && result.isAccepted()) {
			skipBlockInteractionPacket = false;
			cir.setReturnValue(result); //Return early before sending the packet.
		}
	}
	
	@Override
	public void dokodoor$skipInteractionPacket() {
		skipBlockInteractionPacket = true;
	}
}
