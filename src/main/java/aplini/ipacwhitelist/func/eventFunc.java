package aplini.ipacwhitelist.func;

import aplini.ipacwhitelist.enums.ph;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static aplini.ipacwhitelist.IpacWhitelist.*;

public class eventFunc {
    // 传入事件配置位置, 处理事件程序
    // 当 player 为 null 时, 不运行与玩家有关的功能
    public static void runEventFunc(String cp, Player player, String playerUUID, String playerName){

        if(config.get(cp) == null){
            throw new RuntimeException("[IpacWhitelist] runEventFunc 事件路径无效: "+ cp);
        }

        // kick
        if(player != null){
            // 将消息合并, 添加换行符
            List<String> list = config.getStringList(cp + ".kick");
            if(!list.isEmpty()){
                String msg = String.join("\\n", list)
                        .replace(ph.playerUUID.ph, playerUUID)
                        .replace(ph.playerName.ph, playerName);
                Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(msg));
            }
        }

        // 运行指令
        for(String li : config.getStringList(cp + ".cmd")){
            // 获取命令
            String cmd = li
                    .replace(ph.playerUUID.ph, playerUUID)
                    .replace(ph.playerName.ph, playerName);
            // 运行命令
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        }

        // 剩余部分都可以异步运行
        CompletableFuture.runAsync(() -> {
            // 发送消息
            if(player != null){
                for (String li : config.getStringList(cp + ".msg")) {
                    // 获取消息文本
                    String msg = li
                            .replace(ph.playerUUID.ph, playerUUID)
                            .replace(ph.playerName.ph, playerName);
                    // 发送消息
                    player.sendMessage(msg);
                }
            }

            // 广播消息
            for(String li : config.getStringList(cp + ".msgBroadcast")){
                // 获取消息文本
                String msg = li
                        .replace(ph.playerUUID.ph, playerUUID)
                        .replace(ph.playerName.ph, playerName);
                // 广播消息
                server.broadcastMessage(msg);
            }

            // 广播消息, 但排除这个玩家
            for(String li : config.getStringList(cp + ".msgExclude")){
                // 获取消息文本
                String msg = li
                        .replace(ph.playerUUID.ph, playerUUID)
                        .replace(ph.playerName.ph, playerName);
                // 发送消息
                if(player != null){
                    server.getConsoleSender().sendMessage(msg);
                    for(Player liPlayer : server.getOnlinePlayers()){
                        if(!liPlayer.getName().equals(playerName)){
                            liPlayer.sendMessage(msg);
                        }
                    }
                }else{
                    // 广播消息
                    server.broadcastMessage(msg);
                }
            }
        });
    }
    public static void runEventFunc(String cp, Player player) {
        runEventFunc(cp, player, player.getUniqueId().toString(), player.getName());
    }
}
