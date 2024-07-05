package agency.highlysuspect.dokokashiradoor.net.payload;

import agency.highlysuspect.dokokashiradoor.Init;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SetRandomSeedsPayloadS2C(IntList newSeeds) implements CustomPayload {
	public static final PacketCodec<PacketByteBuf, SetRandomSeedsPayloadS2C> CODEC = CustomPayload.codecOf(SetRandomSeedsPayloadS2C::write, SetRandomSeedsPayloadS2C::new);

	public static final CustomPayload.Id<SetRandomSeedsPayloadS2C> ID = new CustomPayload.Id<>(Init.id("set_random_seeds"));

	private SetRandomSeedsPayloadS2C(PacketByteBuf buf) {
		this(buf.readIntList());
	}

	private void write(PacketByteBuf buf) {
		buf.writeIntList(this.newSeeds);
	}

	@Override
	public Id<? extends SetRandomSeedsPayloadS2C> getId() {
		return ID;
	}
}
