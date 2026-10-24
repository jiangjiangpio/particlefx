package dev.particlefx.network;

import dev.particlefx.storage.EffectValidator;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Large effects use bounded ASCII frames in either direction. */
public final class EditorTransfer {
    public static final int MAX_FRAME_TEXT = 30000;
    public static final int FRAME_BYTES = 18000;
    private static final int MAX_FRAMES = (EffectValidator.MAX_JSON_BYTES + FRAME_BYTES - 1) / FRAME_BYTES;
    private String id, name;
    private int next, total;
    private long started;
    private ByteArrayOutputStream bytes;

    public static List<String> split(String json) {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        EffectValidator.require(data.length <= EffectValidator.MAX_JSON_BYTES, "JSON exceeds "+EffectValidator.MAX_JSON_BYTES+" bytes");
        int count = Math.max(1, (data.length + FRAME_BYTES - 1) / FRAME_BYTES);
        String id = UUID.randomUUID().toString();
        List<String> frames = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int start = i * FRAME_BYTES, end = Math.min(data.length, start + FRAME_BYTES);
            frames.add(id+":"+i+":"+count+":"+Base64.getEncoder().encodeToString(java.util.Arrays.copyOfRange(data, start, end)));
        }
        return frames;
    }

    public boolean active(long tick) { return bytes != null && tick - started <= 600 && tick >= started; }

    /** Ordered delivery is guaranteed by the play connection. Returns null until complete. */
    public String accept(String effectName, String frame, long tick) {
        try {
            EffectValidator.name(effectName);
            EffectValidator.require(frame.length() <= MAX_FRAME_TEXT, "Upload frame too large");
            String[] fields = frame.split(":", 4);
            EffectValidator.require(fields.length == 4, "Invalid upload frame");
            UUID.fromString(fields[0]);
            int index = Integer.parseInt(fields[1]), count = Integer.parseInt(fields[2]);
            EffectValidator.require(count >= 1 && count <= MAX_FRAMES, "Invalid upload frame count");
            if (!active(tick)) {
                clear();
                EffectValidator.require(index == 0, "Upload expired or missing first frame");
                id = fields[0]; name = effectName; total = count; started = tick;
                bytes = new ByteArrayOutputStream();
            }
            EffectValidator.require(id.equals(fields[0]) && name.equals(effectName) && total == count && next == index,
                "Upload frames out of order");
            byte[] part = Base64.getDecoder().decode(fields[3]);
            EffectValidator.require(part.length <= FRAME_BYTES && (index == count - 1 || part.length == FRAME_BYTES),
                "Invalid upload frame size");
            EffectValidator.require(bytes.size() + part.length <= EffectValidator.MAX_JSON_BYTES, "Upload JSON too large");
            bytes.writeBytes(part);
            if (++next != total) return null;
            String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
            clear();
            return json;
        } catch (CharacterCodingException ex) {
            clear(); throw new IllegalArgumentException("Invalid upload UTF-8", ex);
        } catch (RuntimeException ex) {
            clear(); throw ex;
        }
    }

    public void clear() { bytes = null; id = null; name = null; next = total = 0; }
}
