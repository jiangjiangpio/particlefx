package dev.particlefx.network;
import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.Identifier;
public record EditorRequest(String action,String name,String json) {
    public static final Identifier ID=new Identifier("particlefx","editor_request_v1");
    public static PacketByteBuf write(EditorRequest request) {
        PacketByteBuf buf=new PacketByteBuf(Unpooled.buffer());
        buf.writeString(request.action());
        buf.writeString(request.name());
        buf.writeString(request.json());
        return buf;
    }
    public static EditorRequest read(PacketByteBuf buf) {
        return new EditorRequest(buf.readString(),buf.readString(),buf.readString());
    }
}
