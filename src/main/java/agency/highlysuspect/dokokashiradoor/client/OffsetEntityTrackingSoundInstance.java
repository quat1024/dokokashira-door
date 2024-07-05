package agency.highlysuspect.dokokashiradoor.client;

import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.util.DoorUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public class OffsetEntityTrackingSoundInstance extends MovingSoundInstance {
	public OffsetEntityTrackingSoundInstance(SoundEvent soundEvent, SoundCategory soundCategory, Random random, float volume, float pitch, Entity entity, double dx, double dy, double dz) {
		super(soundEvent, soundCategory, random);
		this.volume = volume;
		this.pitch = pitch;
		
		this.entity = entity;
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		
		updatePos();
	}
	
	public static OffsetEntityTrackingSoundInstance doorOpen(PlayerEntity opener, BlockPos doorPos, BlockState doorState, Vec3d offset, Random random) {
		Vec3d offset2 = Vec3d.ofCenter(doorPos).subtract(opener.getPos());
		
		return new OffsetEntityTrackingSoundInstance(
			DoorUtil.getOpenSound(doorState),
			SoundCategory.BLOCKS,
			random,
			1f, //volume
			random.nextFloat() * 0.1f + 0.9f, //pitch
			opener,
			offset2.x,
			offset2.y,
			offset2.z
		);
	}
	
	private final Entity entity;
	private final double dx, dy, dz;
	
	@Override
	public boolean canPlay() {
		return !entity.isSilent();
	}
	
	@Override
	public void tick() {
		if(entity.isRemoved()) {
			setDone();
		} else {
			updatePos();
		}
	}
	
	private void updatePos() {
		x = dx + entity.getX();
		y = dy + entity.getY();
		z = dz + entity.getZ();
	}
}
