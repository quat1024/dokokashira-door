package agency.highlysuspect.dokokashiradoor.net.payload;

import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.gateway.GatewayMap;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DeltaGatewayUpdateS2CPayload(Identifier worldKeyId, GatewayMap additions, GatewayMap removals) implements CustomPayload {
	public static final PacketCodec<RegistryByteBuf, DeltaGatewayUpdateS2CPayload> CODEC = PacketCodec.tuple(
		Identifier.PACKET_CODEC, DeltaGatewayUpdateS2CPayload::worldKeyId,
		GatewayMap.PACKET_CODEC, DeltaGatewayUpdateS2CPayload::additions,
		GatewayMap.PACKET_CODEC, DeltaGatewayUpdateS2CPayload::removals,
		DeltaGatewayUpdateS2CPayload::new
	);

	public static final CustomPayload.Id<DeltaGatewayUpdateS2CPayload> ID = new CustomPayload.Id<>(Init.id("delta_gateway_update"));

	@Override
	public Id<? extends DeltaGatewayUpdateS2CPayload> getId() {
		return ID;
	}
}
