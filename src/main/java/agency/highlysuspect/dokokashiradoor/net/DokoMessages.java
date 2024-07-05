package agency.highlysuspect.dokokashiradoor.net;

import agency.highlysuspect.dokokashiradoor.net.payload.FullGatewayUpdateS2CPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.AcknowledgeDeltaGatewayC2SPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.AcknowledgeRandomSeedsPayloadC2S;
import agency.highlysuspect.dokokashiradoor.net.payload.AddRandomSeedsPayloadS2C;
import agency.highlysuspect.dokokashiradoor.net.payload.DeltaGatewayUpdateS2CPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.DoorTeleportRequestC2SPayload;
import agency.highlysuspect.dokokashiradoor.net.payload.SetRandomSeedsPayloadS2C;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class DokoMessages {
	public static void onInitialize() {
		PayloadTypeRegistry.playS2C().register(FullGatewayUpdateS2CPayload.ID, FullGatewayUpdateS2CPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(DeltaGatewayUpdateS2CPayload.ID, DeltaGatewayUpdateS2CPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(AcknowledgeDeltaGatewayC2SPayload.ID, AcknowledgeDeltaGatewayC2SPayload.CODEC);

		PayloadTypeRegistry.playS2C().register(AddRandomSeedsPayloadS2C.ID, AddRandomSeedsPayloadS2C.CODEC);
		PayloadTypeRegistry.playS2C().register(SetRandomSeedsPayloadS2C.ID, SetRandomSeedsPayloadS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(AcknowledgeRandomSeedsPayloadC2S.ID, AcknowledgeRandomSeedsPayloadC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(DoorTeleportRequestC2SPayload.ID, DoorTeleportRequestC2SPayload.CODEC);
	}
}
