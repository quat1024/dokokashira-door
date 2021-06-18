package agency.highlysuspect.dokokashiradoor.mixin;

import agency.highlysuspect.dokokashiradoor.Gateway;
import agency.highlysuspect.dokokashiradoor.GatewayPersistentState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorBlock.class)
public class DoorBlockMixin extends Block {
	public DoorBlockMixin(Settings settings) {
		super(settings);
		throw new AssertionError("Dummy constructor");
	}
	
	@Inject(
		method = "onUse",
		at = @At(
			value = "INVOKE",
			//The last thing that DoorBlock does before returning a successful ActionResult
			target = "Lnet/minecraft/world/World;emitGameEvent(Lnet/minecraft/entity/Entity;Lnet/minecraft/world/event/GameEvent;Lnet/minecraft/util/math/BlockPos;)V",
			shift = At.Shift.AFTER
		)
	)
	private void whenUsed(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
		if(player != null &&
			!world.isClient() &&
			world instanceof ServerWorld sworld &&
			player instanceof ServerPlayerEntity splayer &&
			state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER &&
			state.get(DoorBlock.OPEN)
		) {
			//TODO: Check that the player is standing very close to the door and looking directly at it
			
			//If the player is interacting with a gateway
			Gateway thisGateway = Gateway.readFromWorld(sworld, pos);
			if(thisGateway == null) return;
			
			GatewayPersistentState gps = GatewayPersistentState.getFor(sworld);
			gps.addGateway(thisGateway);
			//gps.validateLoadedGateways(sworld);
			
			//find a matching gateway
			@Nullable Gateway other = gps.findDifferentGateway(sworld, thisGateway, world.random);
			
			//and teleport them to it
			if(other == null) return;
			other.arrive(world, thisGateway, splayer);
		}
	}
	
	@Override
	//@SoftOverride (Mixin bug? This method absolutely exists in AbstractBlock)
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if(!world.isClient && world instanceof ServerWorld sworld && state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
			GatewayPersistentState.getFor(sworld).addCheckdoor(pos.toImmutable());
		}
	}
}
