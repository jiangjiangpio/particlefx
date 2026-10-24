package dev.particlefx.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record EditorRequest(String action,String name,String json) implements CustomPacketPayload {
    public static final Type<EditorRequest> ID=new Type<>(Identifier.fromNamespaceAndPath("particlefx","editor_request_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf,EditorRequest> CODEC=StreamCodec.composite(
        ByteBufCodecs.stringUtf8(16),EditorRequest::action,ByteBufCodecs.stringUtf8(64),EditorRequest::name,
        ByteBufCodecs.stringUtf8(EditorTransfer.MAX_FRAME_TEXT),EditorRequest::json,EditorRequest::new);
    public Type<? extends CustomPacketPayload> type(){return ID;}
}
