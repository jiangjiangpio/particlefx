package dev.particlefx.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ClientNetworking {
    public static boolean canSend(CustomPacketPayload.Type<?> type) {
        var connection=Minecraft.getInstance().getConnection();
        return connection!=null&&connection.hasChannel(type.id());
    }
    public static void send(CustomPacketPayload payload) {
        if(canSend(payload.type()))ClientPacketDistributor.sendToServer(payload);
    }
    private ClientNetworking() {}
}

