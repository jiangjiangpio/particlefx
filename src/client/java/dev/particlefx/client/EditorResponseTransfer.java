package dev.particlefx.client;

import dev.particlefx.network.EditorTransfer;
import dev.particlefx.storage.EffectValidator;
import java.nio.charset.StandardCharsets;

/** Two bounded downloads; no Minecraft dependency so failure paths can be tested. */
public final class EditorResponseTransfer {
    public record Message(String action, String content) {}
    private final EditorTransfer effect = new EditorTransfer();
    private final EditorTransfer open = new EditorTransfer();
    private boolean receivingEffect, receivingOpen;

    public Message accept(String action, String content, long tick) {
        try {
            EffectValidator.require(action != null && content != null, "Missing editor response");
            EffectValidator.require(content.getBytes(StandardCharsets.UTF_8).length <= EffectValidator.MAX_JSON_BYTES,
                "Editor response too large");
            if (action.equals("effect_part") || action.equals("open_part")) {
                boolean isOpen = action.equals("open_part");
                String json = (isOpen ? open : effect).accept(isOpen ? "open" : "effect", content, tick);
                if (isOpen) receivingOpen = json == null;
                else receivingEffect = json == null;
                return json == null ? null : new Message(isOpen ? "open" : "effect", json);
            }
            if (action.equals("effect")) { effect.clear(); receivingEffect = false; }
            if (action.equals("open")) { open.clear(); receivingOpen = false; }
            return new Message(action, content);
        } catch (RuntimeException ex) {
            clear();
            throw ex;
        }
    }

    public boolean expire(long tick) {
        boolean expired = false;
        if (receivingEffect && !effect.active(tick)) { effect.clear(); receivingEffect = false; expired = true; }
        if (receivingOpen && !open.active(tick)) { open.clear(); receivingOpen = false; expired = true; }
        return expired;
    }

    public void clear() {
        effect.clear(); open.clear();
        receivingEffect = receivingOpen = false;
    }
}
