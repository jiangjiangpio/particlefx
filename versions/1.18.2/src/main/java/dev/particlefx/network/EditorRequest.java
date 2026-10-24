package dev.particlefx.network;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
public record EditorRequest(String action,String name,String json) {
    public static final Identifier ID=new Identifier("particlefx","editor_request_v1");
    public PacketByteBuf write() {
        PacketByteBuf buf=new PacketByteBuf(Unpooled.buffer());
        buf.writeString(action==null?"":action,16);
        buf.writeString(name==null?"":name,64);
        buf.writeString(json==null?"":json,30000);
        return buf;
    }
    public static EditorRequest read(PacketByteBuf buf) {
        return new EditorRequest(buf.readString(16),buf.readString(64),buf.readString(30000));
    }
}
