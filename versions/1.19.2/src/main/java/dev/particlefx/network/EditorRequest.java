package dev.particlefx.network;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
/** Legacy (1.19.2) play-channel payload: a plain record plus manual buffer helpers. */
public record EditorRequest(String action,String name,String json) {
    public static final Identifier ID=new Identifier("particlefx","editor_request_v1");
    public static EditorRequest read(PacketByteBuf buf) {
        return new EditorRequest(buf.readString(16),buf.readString(64),buf.readString(30000));
    }
    public void write(PacketByteBuf buf) {
        buf.writeString(action==null?"":action,16);
        buf.writeString(name==null?"":name,64);
        buf.writeString(json==null?"":json,30000);
    }
}
