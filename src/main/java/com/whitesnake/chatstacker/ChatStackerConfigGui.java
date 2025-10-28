package com.whitesnake.chatstacker;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatStackerConfigGui extends GuiConfig {

    public ChatStackerConfigGui(GuiScreen parent) {
        super(
                parent,
                new ConfigElement(ChatDuplicateDeleter.getConfig()
                        .getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
                ChatDuplicateDeleter.MODID,
                false,
                false,
                "ChatStacker 設定"
        );
    }
}
