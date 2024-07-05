package agency.highlysuspect.dokokashiradoor.net.payload;

import agency.highlysuspect.dokokashiradoor.Init;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record AddRandomSeedsPayloadS2C(IntList newSeeds) implements CustomPayload {
	public static final PacketCodec<PacketByteBuf, AddRandomSeedsPayloadS2C> CODEC = CustomPayload.codecOf(AddRandomSeedsPayloadS2C::write, AddRandomSeedsPayloadS2C::new);

	public static final CustomPayload.Id<AddRandomSeedsPayloadS2C> ID = new CustomPayload.Id<>(Init.id("add_random_seeds"));

	private AddRandomSeedsPayloadS2C(PacketByteBuf buf) {
		this(buf.readIntList());
	}

	private void write(PacketByteBuf buf) {
		buf.writeIntList(this.newSeeds);
	}

	@Override
	public Id<? extends AddRandomSeedsPayloadS2C> getId() {
		return ID;
	}
}
