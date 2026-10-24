package dev.particlefx.network;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.Identifier;
public record EditorResponse(String action,String content) {
    public static final Identifier ID=new Identifier("particlefx","editor_response_v1");
    public static PacketByteBuf write(EditorResponse response) {
        PacketByteBuf buf=new PacketByteBuf(Unpooled.buffer());
        buf.writeString(response.action());
        buf.writeString(response.content());
        return buf;
    }
    public static EditorResponse read(PacketByteBuf buf) {
        return new EditorResponse(buf.readString(),buf.readString());
    }
}
