package agency.highlysuspect.dokokashiradoor.net.payload;

import agency.highlysuspect.dokokashiradoor.Init;
import agency.highlysuspect.dokokashiradoor.gateway.GatewayMap;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FullGatewayUpdateS2CPayload(Identifier worldKeyId, GatewayMap gateways) implements CustomPayload {
	public static final PacketCodec<RegistryByteBuf, FullGatewayUpdateS2CPayload> CODEC = PacketCodec.tuple(
		Identifier.PACKET_CODEC, FullGatewayUpdateS2CPayload::worldKeyId,
		GatewayMap.PACKET_CODEC, FullGatewayUpdateS2CPayload::gateways,
		FullGatewayUpdateS2CPayload::new
	);

	public static final CustomPayload.Id<FullGatewayUpdateS2CPayload> ID = new CustomPayload.Id<>(Init.id("full_gateway_update"));

	@Override
	public Id<? extends FullGatewayUpdateS2CPayload> getId() {
		return ID;
	}
}
