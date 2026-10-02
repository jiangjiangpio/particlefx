package dev.particlefx.storage;

import net.minecraft.util.math.BlockPos;

/** Persistent rising-edge trigger bound to a block in one dimension. */
public final class RedstoneTrigger {
    public String dimension = "minecraft:overworld";
    public int x;
    public int y;
    public int z;
    public String targetType = "effect";
    public String target = "new_effect";
    public transient boolean powered;

    public RedstoneTrigger() {}

    public RedstoneTrigger(String dimension, BlockPos pos, String targetType, String target) {
        this.dimension = dimension;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.targetType = targetType;
        this.target = target;
    }

    public BlockPos pos() { return new BlockPos(x, y, z); }
    public String key() { return dimension + "|" + x + "|" + y + "|" + z; }
}
