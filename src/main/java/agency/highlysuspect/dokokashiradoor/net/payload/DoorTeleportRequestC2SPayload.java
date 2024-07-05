package agency.highlysuspect.dokokashiradoor.net.payload;

import agency.highlysuspect.dokokashiradoor.Init;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record DoorTeleportRequestC2SPayload(BlockPos leftFrom, BlockPos destination) implements CustomPayload {
	public static final PacketCodec<PacketByteBuf, DoorTeleportRequestC2SPayload> CODEC = PacketCodec.tuple(
		BlockPos.PACKET_CODEC, DoorTeleportRequestC2SPayload::leftFrom,
		BlockPos.PACKET_CODEC, DoorTeleportRequestC2SPayload::destination,
		DoorTeleportRequestC2SPayload::new
	);

	public static final CustomPayload.Id<DoorTeleportRequestC2SPayload> ID = new CustomPayload.Id<>(Init.id("door_teleport_request"));

	@Override
	public Id<? extends DoorTeleportRequestC2SPayload> getId() {
		return ID;
	}
}
