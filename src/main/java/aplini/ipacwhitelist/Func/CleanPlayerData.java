package aplini.ipacwhitelist.Func;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.util.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;

public class CleanPlayerData {

    // 清理一个参观账户数据
    static public void deletePlayerDataAll(IpacWhitelist plugin, AtomicInteger i, PlayerData pd) {

        // 生成用于输出日志的玩家信息
        String logPlayerInfo = i.toString() +" -> "+ pd.ID +"."+ pd.Name;

        // 执行指令
        for(String li : plugin.getConfig().getStringList("dev.deletePlayerDataAll.playerDataCommand")){
            String command = li
                    .replace("%playerUUID%", pd.UUID)
                    .replace("%playerName%", pd.Name);

            getLogger().info("[IpacWhitelist] [del.Command] ["+ logPlayerInfo +"]: /"+ command);

            Bukkit.getScheduler().callSyncMethod(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
        }

        try {
            TimeUnit.MILLISECONDS.sleep(plugin.getConfig().getInt("dev.deletePlayerDataAll.intervalTime2", 150));
        } catch (InterruptedException ignored) {}

        // 删除文件
        for(String li : plugin.getConfig().getStringList("dev.deletePlayerDataAll.playerDataFilePlugin")){
            String filePath = li
                    .replace("%playerUUID%", pd.UUID)
                    .replace("%playerName%", pd.Name);

            File file = new File(filePath);
            if(file.delete()){
                getLogger().info("[IpacWhitelist] [del.File] ["+ logPlayerInfo +"]: "+ filePath);
            }
        }

        // 删除世界文件
        for(World world : getServer().getWorlds()){
            // 获取世界根目录
            String worldRoot = world.getWorldFolder().getPath();
            // 遍历配置
            for(String li : plugin.getConfig().getStringList("dev.deletePlayerDataAll.playerDataFile")){
                String filePath = li
                        .replace("%worldRoot%", worldRoot)
                        .replace("%playerUUID%", pd.UUID)
                        .replace("%playerName%", pd.Name);

                File file = new File(filePath);
                if(file.delete()){
                    getLogger().info("[IpacWhitelist] [del.File] ["+ logPlayerInfo +"]: "+ filePath);
                }
            }
        }
    }
}
