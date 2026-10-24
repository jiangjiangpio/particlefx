package dev.particlefx.client.mixin;

import dev.particlefx.client.compat.RenderBootstrap;
import dev.particlefx.client.compat.RenderCompatibility;
import java.util.List;
import java.util.Set;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.objectweb.asm.tree.ClassNode;

/** Prevents world-camera injections from loading on FCL/Pojav legacy GL backends. */
public final class ParticleFXMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) { RenderBootstrap.log(); }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return RenderCompatibility.shouldApplyMixin(RenderBootstrap.SAFE,mixinClassName);
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return List.of(); }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}

