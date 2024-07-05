package agency.highlysuspect.dokokashiradoor.net.payload;

import agency.highlysuspect.dokokashiradoor.Init;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AcknowledgeDeltaGatewayC2SPayload(Identifier worldKeyId, int checksum) implements CustomPayload {
	public static final PacketCodec<PacketByteBuf, AcknowledgeDeltaGatewayC2SPayload> CODEC = PacketCodec.tuple(
		Identifier.PACKET_CODEC, AcknowledgeDeltaGatewayC2SPayload::worldKeyId,
		PacketCodecs.INTEGER, AcknowledgeDeltaGatewayC2SPayload::checksum,
		AcknowledgeDeltaGatewayC2SPayload::new
	);

	public static final CustomPayload.Id<AcknowledgeDeltaGatewayC2SPayload> ID = new CustomPayload.Id<>(Init.id("delta_gateway_ack"));

	@Override
	public Id<? extends AcknowledgeDeltaGatewayC2SPayload> getId() {
		return ID;
	}
}
