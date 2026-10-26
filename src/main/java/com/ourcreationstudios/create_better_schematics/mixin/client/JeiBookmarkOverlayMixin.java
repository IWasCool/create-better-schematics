package com.ourcreationstudios.create_better_schematics.mixin.client;

import com.ourcreationstudios.create_better_schematics.client.jei.JeiBookmarkVisibility;
import mezz.jei.common.config.IClientToggleState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.BookmarkOverlay", remap = false)
abstract class JeiBookmarkOverlayMixin implements JeiBookmarkVisibility {
    @Shadow @Final private IClientToggleState toggleState;

    @Override public void betterSchematics$showMaterialBookmarks() {
        if (!toggleState.isOverlayEnabled())
            toggleState.toggleOverlayEnabled();
        toggleState.setBookmarkEnabled(true);
    }
}
