package dev.particlefx.network;
import dev.particlefx.storage.EffectValidator;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
public record EditorResponse(String action,String content) implements CustomPayload {
    public static final Id<EditorResponse> ID=new Id<>(Identifier.of("particlefx","editor_response_v2"));
    public static final PacketCodec<RegistryByteBuf,EditorResponse> CODEC=PacketCodec.tuple(
        PacketCodecs.string(16),EditorResponse::action,PacketCodecs.string(EffectValidator.MAX_JSON_BYTES),EditorResponse::content,EditorResponse::new);
    public Id<? extends CustomPayload> getId(){return ID;}
}
