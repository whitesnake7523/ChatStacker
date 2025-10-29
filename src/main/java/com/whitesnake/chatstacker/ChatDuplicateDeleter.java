package com.whitesnake.chatstacker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

@Mod(
        modid = ChatDuplicateDeleter.MODID,
        name = ChatDuplicateDeleter.NAME,
        version = ChatDuplicateDeleter.VERSION,
        clientSideOnly = true,
        guiFactory = "com.whitesnake.chatstacker.ChatStackerGuiFactory"
)
public class ChatDuplicateDeleter {
    public static final String MODID = "ChatStacker";
    public static final String NAME = "ChatStacker";
    public static final String VERSION = "1.3";
    public static final String CATEGORY_GENERAL = "general";

    public static Handler handlerInstance;
    private static Configuration config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configFile = new File(event.getModConfigurationDirectory(), MODID + ".cfg");
        config = new Configuration(configFile);
        config.load();

        int defaultTime = config.getInt(
                "expireSeconds",
                CATEGORY_GENERAL,
                30,
                1,
                3600,
                "チャットメッセージを削除対象から外すまでの秒数（1〜3600）"
        );

        if (handlerInstance == null) {
            handlerInstance = new Handler();
        }
        handlerInstance.setExpireSeconds(defaultTime);
        config.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(handlerInstance);
        ClientCommandHandler.instance.registerCommand(new ExpireTimeCommand());
        // Forge 1.8.9 では FML バス（bus()）へ登録
        FMLCommonHandler.instance().bus().register(new ConfigEventHandler());
    }

    public static class Handler {
        private final Map<String, Integer> counts = new HashMap<String, Integer>();
        private final Map<String, Integer> messageIds = new HashMap<String, Integer>();
        private final Map<String, TimerTask> expireTasks = new HashMap<String, TimerTask>();
        private final Timer timer = new Timer(true);
        private int expireSeconds = 30;

        // 同一msでもユニークにするための連番
        private static final AtomicInteger SEQ = new AtomicInteger(1);

        @SubscribeEvent
        public void onChat(ClientChatReceivedEvent event) {
            if (event.type == 2 || event.message == null) return;

            String baseText = event.message.getUnformattedText();
            if (baseText == null) return;
            baseText = baseText.trim();
            if (baseText.length() == 0) return;

            // $api で始まるメッセージは除外（標準表示に任せる）
            if (baseText.startsWith("$api")) {
                return;
            }

            // カウント更新（Java 8でも安全な書き方）
            int count = 0;
            if (counts.containsKey(baseText)) {
                count = counts.get(baseText);
            }
            count++;
            counts.put(baseText, count);

            // 削除置換ID（expire後は新規発行）
            Integer idObj = messageIds.get(baseText);
            int id;
            if (idObj == null) {
                // 時刻(ms)・連番・本文ハッシュを混ぜて衝突を極小化
                long t = System.currentTimeMillis();
                int next = SEQ.getAndIncrement();
                id = (int) ((t << 12) ^ next ^ baseText.hashCode());
                if (id == 0) id = next; // 念のため0回避
                messageIds.put(baseText, id);
            } else {
                id = idObj.intValue();
            }

            // 既定描画をキャンセルし、自前で置換描画
            event.setCanceled(true);

            IChatComponent newComponent = event.message.createCopy();
            if (count > 1) {
                IChatComponent countComponent = new ChatComponentText(" (" + count + ")");
                newComponent.appendSibling(countComponent);
            }

            GuiNewChat chatGUI = Minecraft.getMinecraft().ingameGUI.getChatGUI();
            chatGUI.printChatMessageWithOptionalDeletion(newComponent, id);

            // expire を最新受信時刻基準にリセット
            scheduleExpire(baseText);
        }

        private void scheduleExpire(final String baseText) {
            // 古いタスクがある場合はキャンセルして差し替え
            TimerTask oldTask = expireTasks.remove(baseText);
            if (oldTask != null) {
                oldTask.cancel();
            }

            TimerTask newTask = new TimerTask() {
                @Override
                public void run() {
                    counts.remove(baseText);
                    messageIds.remove(baseText);
                    expireTasks.remove(baseText);
                }
            };

            expireTasks.put(baseText, newTask);
            timer.schedule(newTask, expireSeconds * 1000L);
        }

        public void setExpireSeconds(int seconds) {
            this.expireSeconds = seconds;
        }

        public int getExpireSeconds() {
            return expireSeconds;
        }
    }

    // FML バスに登録するイベントハンドラは static/別クラスに
    public static class ConfigEventHandler {

        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent e) {
            if (!e.modID.equals(ChatDuplicateDeleter.MODID)) return;

            Configuration cfg = ChatDuplicateDeleter.getConfig();
            // まず保存（GUI側の変更を反映）、その後ロード
            if (cfg.hasChanged()) {
                cfg.save();
            }
            cfg.load();

            int newExpire = cfg.getInt(
                    "expireSeconds",
                    ChatDuplicateDeleter.CATEGORY_GENERAL,
                    30,
                    1,
                    3600,
                    "チャットメッセージを削除対象から外すまでの秒数（1〜3600）"
            );

            ChatDuplicateDeleter.handlerInstance.setExpireSeconds(newExpire);
            System.out.println("[ChatStacker] ConfigChangedEvent expireSeconds: " + newExpire);
        }
    }

    public static class ExpireTimeCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "chatstacker";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/chatstacker settime <秒数>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length == 2 && args[0].equalsIgnoreCase("settime")) {
                try {
                    int sec = Integer.parseInt(args[1]);
                    if (sec < 1) {
                        sender.addChatMessage(new ChatComponentText("§c1秒以上を指定してください"));
                        return;
                    }
                    handlerInstance.setExpireSeconds(sec);
                    sender.addChatMessage(new ChatComponentText("§a削除対象から外す時間を " + sec + " 秒に設定しました。"));

                    // Configにも保存
                    config.get(CATEGORY_GENERAL, "expireSeconds", 30).set(sec);
                    config.save();

                } catch (NumberFormatException e) {
                    sender.addChatMessage(new ChatComponentText("§c数字を入力してください"));
                }
            } else {
                sender.addChatMessage(new ChatComponentText("§e使用方法: /chatstacker settime <秒数>"));
                sender.addChatMessage(new ChatComponentText("§7現在の設定: " + handlerInstance.getExpireSeconds() + " 秒"));
            }
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }
    }

    public static Configuration getConfig() {
        return config;
    }
}
