package dev.particlefx.network;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
public record EditorResponse(String action,String content) implements CustomPayload {
    public static final Id<EditorResponse> ID=new Id<>(Identifier.of("particlefx","editor_response_v1"));
    public static final PacketCodec<RegistryByteBuf,EditorResponse> CODEC=PacketCodec.tuple(
        PacketCodecs.string(16),EditorResponse::action,PacketCodecs.string(30000),EditorResponse::content,EditorResponse::new);
    public Id<? extends CustomPayload> getId(){return ID;}
}
