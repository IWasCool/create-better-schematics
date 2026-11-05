package com.ourcreationstudios.create_better_schematics.client;

import net.minecraft.client.gui.GuiGraphics;

/** Marks a table control for an open-bottom frame that joins the menu's top edge. */
public interface SchematicTableTab {
    void betterSchematics$attachTab();

    void betterSchematics$renderBehind(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);
}
