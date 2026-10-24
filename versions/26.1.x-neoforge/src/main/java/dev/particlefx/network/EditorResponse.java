package dev.particlefx.network;
import dev.particlefx.storage.EffectValidator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record EditorResponse(String action,String content) implements CustomPacketPayload {
    public static final Type<EditorResponse> ID=new Type<>(Identifier.fromNamespaceAndPath("particlefx","editor_response_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf,EditorResponse> CODEC=StreamCodec.composite(
        ByteBufCodecs.stringUtf8(16),EditorResponse::action,ByteBufCodecs.stringUtf8(EffectValidator.MAX_JSON_BYTES),EditorResponse::content,EditorResponse::new);
    public Type<? extends CustomPacketPayload> type(){return ID;}
}
