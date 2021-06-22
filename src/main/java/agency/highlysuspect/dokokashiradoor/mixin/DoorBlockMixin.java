package agency.highlysuspect.dokokashiradoor.mixin;

import agency.highlysuspect.dokokashiradoor.GatewayPersistentState;
import agency.highlysuspect.dokokashiradoor.client.ClientPlayerGatewayData;
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
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

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
			state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER &&
			state.get(DoorBlock.OPEN) && 
			playerIsClose(world, pos, state, player)
		) {
			if(!world.isClient() && world instanceof ServerWorld sworld && player instanceof ServerPlayerEntity splayer) {
				//TODO: use this bool to suppress the sound of the door
				boolean todo = GatewayPersistentState.getFor(sworld).playerUseDoor(sworld, pos, splayer);
			}
			
			if(world.isClient()) {
				Optional<ClientPlayerGatewayData> cpgd = ClientPlayerGatewayData.get();
				if(cpgd.isPresent()) {
					boolean todo = cpgd.get().predictDoorClient(world, pos, player);
				}
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
	
	private boolean playerIsClose(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		//TODO: Do this for real
		if(true) return true;
		
		//Standing in the same block as the door. No player-look check, because if you're able to stand this close and
		//activate the door in the first place, your entire screen is probably taken up by the door frame.
		Box interiorBox = Box.of(Vec3d.ofBottomCenter(pos), 0.3d, 0.3d, 0.3d);
		if(world.getOtherEntities(null, interiorBox).contains(player)) return true;
		
		//The direction from the door's model towards the middle of the block.
		Direction toInside = state.get(DoorBlock.FACING);
		
		//This box pokes outside the front of the door by a tiny bit.
		//If you're standing in this box, you need to be looking vaguely at the door.
		Box exteriorBox = Box.of(Vec3d.ofBottomCenter(pos), 0.1d, 0.1d, 0.1d)
			.offset(Vec3d.of(toInside.getVector()).multiply(-0.5d));
		
		if(world.getOtherEntities(null, exteriorBox).contains(player)) {
			//Player needs to be looking vaguely at the door, so it fills their screen.
			//Numbers here determined through vague trial and error
			
			//Can't be looking too far down or up.
			if(MathHelper.abs(player.getPitch()) > 15) return false;
			
			//Need to be facing the door.
			//Some nastiness here wrt. "player facing 359 degrees, door at 0 degrees"
			float playerYaw = MathHelper.wrapDegrees(player.getYaw());
			float expectYaw = MathHelper.wrapDegrees(toInside.asRotation());
			float diff = MathHelper.abs(playerYaw - expectYaw);
			if(diff > 10 && diff < 350) return false;
			
			return true;
			
//			HitResult result = player.raycast(0.5d, 1f, true);
//			Vec3d hit = result.getPos();
//			if(hit == null) return false;
//			
//			//World space -> block space
//			hit = hit.subtract(Vec3d.of(pos));
//			
//			//One of X/Z should be one of 0 or 1 - this means you clicked somewhere on the outer flat side of the door.
//			//I could determine *which* of x/z should be *which* of 0 or 1 using the facing, but it doesn't really matter.
//			boolean xFace = MathHelper.approximatelyEquals(hit.x, 0) ^ MathHelper.approximatelyEquals(hit.x, 1);
//			boolean zFace = MathHelper.approximatelyEquals(hit.z, 0) ^ MathHelper.approximatelyEquals(hit.z, 1);
//			if(xFace == zFace) return false; //xor
//			
//			//Y, and the other of X/Z, should be about 0.5 - you need to click on the "center" of the door.
//			double y = hit.y;
//			double o = xFace ? hit.z : hit.x;
//			
//			double WINDOW_HEIGHT = 0.1;
//			double WINDOW_WIDTH = 0.1;
//			
//			return o > .5 - WINDOW_WIDTH && o < .5 + WINDOW_WIDTH && y > .5 - WINDOW_HEIGHT && y < .5 + WINDOW_HEIGHT;
		}
		
		return false;
	}
}
