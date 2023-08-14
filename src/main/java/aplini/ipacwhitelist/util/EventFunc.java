package aplini.ipacwhitelist.util;

import aplini.ipacwhitelist.IpacWhitelist;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventFunc {

    public static void startEventFunc(String eventName, IpacWhitelist plugin, Player player) {
        // 获取事件配置
        String configPath = "visit.event."+ eventName +".";

        // 处理 command 程序
        for(String li : plugin.getConfig().getStringList(configPath +"command")){

            // 获取命令
            String command = li
                    .replace("%playerName%", player.getName())
                    .replace("%playerUUID%", String.valueOf(player.getUniqueId()));
            // 运行命令
            Bukkit.getScheduler().callSyncMethod(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }

        // 处理 message 程序
        for(String li : plugin.getConfig().getStringList(configPath +"message")){
            // 获取消息文本
            String message = li
                    .replace("%playerName%", player.getName())
                    .replace("%playerUUID%", String.valueOf(player.getUniqueId()));
            // 发送消息
            player.sendMessage(message);
        }
    }

    // 异步运行
    public static void startAsyncEventFunc(String eventName, IpacWhitelist plugin, Player player) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> startEventFunc(eventName, plugin, player));
        executor.shutdown();
    }
}
