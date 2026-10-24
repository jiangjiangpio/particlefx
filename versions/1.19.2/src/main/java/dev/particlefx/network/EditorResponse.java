package dev.particlefx.network;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
/** Legacy (1.19.2) play-channel payload: a plain record plus manual buffer helpers. */
public record EditorResponse(String action,String content) {
    public static final Identifier ID=new Identifier("particlefx","editor_response_v1");
    public static EditorResponse read(PacketByteBuf buf) {
        return new EditorResponse(buf.readString(16),buf.readString(30000));
    }
    public void write(PacketByteBuf buf) {
        buf.writeString(action==null?"":action,16);
        buf.writeString(content==null?"":content,30000);
    }
}
