package agency.highlysuspect.dokokashiradoor.mixin;

import agency.highlysuspect.dokokashiradoor.gateway.GatewayPersistentState;
import agency.highlysuspect.dokokashiradoor.tp.ClientDoorTp;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
		if(player != null && state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER && state.get(DoorBlock.OPEN)) {
			if(world.isClient()) {
				boolean todo = ClientDoorTp.playerUseDoorClient(world, pos, state, player);
			}
		}
	}
	
	@Inject(
		method = "neighborUpdate",
		at = @At("HEAD")
	)
	private void whenNeighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify, CallbackInfo ci) {
		if(!world.isClient && world instanceof ServerWorld sworld && state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
			GatewayPersistentState.getFor(sworld).helloDoor(sworld, pos.toImmutable());
		}
	}
	
	@Override
	//@SoftOverride (mixin blows up if i have this annotation, but it's true)
	public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onBlockAdded(state, world, pos, oldState, notify);
		
		if(!world.isClient && world instanceof ServerWorld sworld && state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
			GatewayPersistentState.getFor(sworld).helloDoor(sworld, pos.toImmutable());
		}
	}
}
