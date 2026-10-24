package dev.particlefx.client.gui;

import dev.particlefx.storage.EffectValidator;

/** Client view of server editor limits, refreshed after joining a server. */
public final class EditorLimits {
    private static int maxCustomPoints=EffectValidator.MAX_CUSTOM_POINTS;
    private static boolean canEditServerConfig;

    public static int maxCustomPoints(){return maxCustomPoints;}
    public static boolean canEditServerConfig(){return canEditServerConfig;}
    public static void update(int maxPoints,boolean canEdit){
        maxCustomPoints=Math.max(1,Math.min(EffectValidator.MAX_CUSTOM_POINTS_LIMIT,maxPoints));
        canEditServerConfig=canEdit;
    }
    public static void reset(){
        maxCustomPoints=EffectValidator.MAX_CUSTOM_POINTS;
        canEditServerConfig=false;
    }
    private EditorLimits(){}
}


