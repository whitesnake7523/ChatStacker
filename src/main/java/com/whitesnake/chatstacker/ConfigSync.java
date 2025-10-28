package com.whitesnake.chatstacker;

import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.config.Configuration;

public class ConfigSync {

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent e) {
        // このMOD以外の変更は無視
        if (!e.modID.equals(ChatDuplicateDeleter.MODID)) return;

        Configuration cfg = ChatDuplicateDeleter.getConfig();
        cfg.load();

        int newExpire = cfg.getInt(
                "expireSeconds",
                ChatDuplicateDeleter.CATEGORY_GENERAL,
                30,
                1,
                3600,
                "チャットメッセージを削除対象から外すまでの秒数（1〜3600）"
        );

        // handlerに反映
        ChatDuplicateDeleter.handlerInstance.setExpireSeconds(newExpire);

        // 念のため保存
        if (cfg.hasChanged()) cfg.save();
    }
}
