package dev.particlefx.network;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
public record EditorResponse(String action,String content) {
    public static final Identifier ID=new Identifier("particlefx","editor_response_v1");
    public PacketByteBuf write() {
        PacketByteBuf buf=new PacketByteBuf(Unpooled.buffer());
        buf.writeString(action==null?"":action,16);
        buf.writeString(content==null?"":content,30000);
        return buf;
    }
    public static EditorResponse read(PacketByteBuf buf) {
        return new EditorResponse(buf.readString(16),buf.readString(30000));
    }
}
