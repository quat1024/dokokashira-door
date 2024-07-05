package agency.highlysuspect.dokokashiradoor.net.payload;

import agency.highlysuspect.dokokashiradoor.Init;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record AcknowledgeRandomSeedsPayloadC2S(int checksum) implements CustomPayload {
	public static final PacketCodec<ByteBuf, AcknowledgeRandomSeedsPayloadC2S> CODEC = PacketCodecs.INTEGER.xmap(AcknowledgeRandomSeedsPayloadC2S::new, AcknowledgeRandomSeedsPayloadC2S::checksum);

	public static final CustomPayload.Id<AcknowledgeRandomSeedsPayloadC2S> ID = new CustomPayload.Id<>(Init.id("random_seeds_ack"));

	@Override
	public Id<? extends AcknowledgeRandomSeedsPayloadC2S> getId() {
		return ID;
	}
}
