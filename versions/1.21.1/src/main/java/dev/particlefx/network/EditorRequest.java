package dev.particlefx.network;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
public record EditorRequest(String action,String name,String json) implements CustomPayload {
    public static final Id<EditorRequest> ID=new Id<>(Identifier.of("particlefx","editor_request_v1"));
    public static final PacketCodec<RegistryByteBuf,EditorRequest> CODEC=PacketCodec.tuple(
        PacketCodecs.string(16),EditorRequest::action,PacketCodecs.string(64),EditorRequest::name,
        PacketCodecs.string(30000),EditorRequest::json,EditorRequest::new);
    public Id<? extends CustomPayload> getId(){return ID;}
}
