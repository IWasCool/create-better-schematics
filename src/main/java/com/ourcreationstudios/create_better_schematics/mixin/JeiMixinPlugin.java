package com.ourcreationstudios.create_better_schematics.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Avoids resolving absent optional addon targets and older JEI APIs. */
public final class JeiMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".TransmissionJointBlockEntityMixin")
                || mixinClassName.endsWith(".TransmissionToolgunNoticeMixin")) {
            var mods = FMLLoader.getLoadingModList();
            return mods != null && mods.getModFileById("aeronautics_utility_objects") != null;
        }
        if (!mixinClassName.endsWith(".JeiBookmarkElementMixin") && !mixinClassName.endsWith(".JeiBookmarkOverlayMixin"))
            return true;
        var mods = FMLLoader.getLoadingModList();
        return mods != null && mods.getMods().stream().anyMatch(mod -> mod.getModId().equals("jei")
                && mod.getVersion().compareTo(new DefaultArtifactVersion("19.53.0")) >= 0);
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
