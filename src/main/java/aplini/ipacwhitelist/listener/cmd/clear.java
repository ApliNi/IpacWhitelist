package aplini.ipacwhitelist.listener.cmd;

import aplini.ipacwhitelist.enums.Key;
import aplini.ipacwhitelist.enums.Type;
import aplini.ipacwhitelist.enums.ph;
import aplini.ipacwhitelist.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static aplini.ipacwhitelist.IpacWhitelist.*;
import static aplini.ipacwhitelist.utils.util.*;
import static org.bukkit.Bukkit.getServer;

public class clear {

    public static void cmd(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.clear")){
            sender.sendMessage(config.getString("message.noPermission", ""));
            return;
        }

        if(args.length < 3){
            sender.sendMessage("/wl clear PLAYER|TYPE <playerName|playerUUID|Type>");
            return;
        }

        String mode = args[1].toUpperCase();

        // 异步运行
        CompletableFuture.runAsync(() -> {

            sender.sendMessage(config.getString("command.clear.title", "")
                    .replace(ph.type.ph, mode)
                    .replace(ph.var.ph, args[2].replace("\\", "")));

            // 清理一个玩家的数据
            if(mode.equals("PLAYER")){
                // 获取指定玩家的数据
                Inp inp = new Inp().fromInp(args[2], true);
                if(inp == null){
                    sender.sendMessage(config.getString("message.parameterErr", "")
                            .replace(ph.var.ph, args[2]));
                    return;
                }

                // 如果没有 UUID 和 NAME 则不运行清理
                if(inp.pd.uuid == null || inp.pd.name == null){
                    sender.sendMessage(msg(config.getString("command.clear.isMiss", ""), inp.pd.uuid, inp.pd.name));
                    return;
                }

                // 如果在线则不处理
                if(inp.onlinePlayer != null){
                    sender.sendMessage(msg(config.getString("command.clear.online", ""), inp.pd.uuid, inp.pd.name));
                    return;
                }

                // 未达到可删除的时间则不处理
                if(!isTimedOut(inp.pd.time, config.getLong("command.clear.delTime", 43200))){
                    sender.sendMessage(msg(config.getString("command.clear.delTimeMsg", ""), inp.pd.uuid, inp.pd.name));
                    return;
                }

                // 运行数据清理
                clearPlayerData(inp.pd);
                sender.sendMessage(msg(config.getString("command.clear.ing", ""), inp.pd.uuid, inp.pd.name)
                        .replace(ph.var.ph, "1")
                        .replace(ph.id.ph, ""+ inp.pd.id));
            }

            // 清理一个标签下的所有数据
            else if(mode.equals("TYPE")){

                String typeName = args[2].toUpperCase();

                List<PlayerData> pds = (switch (typeName) {
                    case "NOT" -> sql.findPlayerDataList("", Key.GET_NOT);
                    case "VISIT" -> sql.findPlayerDataList("", Key.GET_VISIT);
                    case "BAN" -> sql.findPlayerDataList("", Key.GET_BAN);
                    default -> null;
                });

                if (pds == null) {
                    sender.sendMessage("/wl clear PLAYER|TYPE <playerName|playerUUID|Type>");
                    return;
                }

                // 运行数据清理
                int i = 0;
                for(PlayerData li : pds){
                    i++;

                    // 如果没有 UUID 和 NAME 则不运行清理
                    if(li.uuid == null || li.name == null){
                        sender.sendMessage(msg(config.getString("command.clear.isMiss", ""), li.uuid, li.name));
                        continue;
                    }

                    // 如果在线则不处理
                    if(server.getPlayer(li.name) != null){
                        sender.sendMessage(msg(config.getString("command.clear.online", ""), li.uuid, li.name));
                        continue;
                    }

                    // 未达到可删除的时间则不处理
                    if(!isTimedOut(li.time, config.getLong("command.clear.delTime", 43200))){
                        sender.sendMessage(msg(config.getString("command.clear.delTimeMsg", ""), li.uuid, li.name));
                        continue;
                    }

                    clearPlayerData(li);
                    sender.sendMessage(msg(config.getString("command.clear.ing", ""), li.uuid, li.name)
                            .replace(ph.var.ph, "" + i)
                            .replace(ph.id.ph, "" + li.id));
                    try {
                        TimeUnit.MILLISECONDS.sleep(config.getInt("command.clear.delayLoop", 727));
                    } catch (Exception ignored) {}
                }
            }

            // 啥啥啥, 写的这是啥
            else {
                sender.sendMessage("/wl clear PLAYER|TYPE <playerName|playerUUID|Type>");
                return;
            }

            // 运行完毕
            sender.sendMessage(config.getString("command.clear.finish", ""));
        });
    }


    static void clearPlayerData(PlayerData pd){

        // 将清理完的 Type 设置为 NOT
        // 不处理 Ban 账户的数据
        if(pd.ban == Type.NOT){
            pd.type = Type.NOT;
            pd.save();
        }

        // 执行数据清理指令
        for(String li : config.getStringList("command.clear.runCommand")){
            // 获取命令
            String cmd = li
                    .replace(ph.playerUUID.ph, pd.uuid)
                    .replace(ph.playerName.ph, pd.name);
            // 运行命令
            Bukkit.getScheduler().callSyncMethod(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        }

        try {
            TimeUnit.MILLISECONDS.sleep(plugin.getConfig().getInt("command.clear.delayStep", 270));
        } catch (Exception ignored) {}

        // 清理文件
        for(String li : config.getStringList("command.clear.clearFile")){

            String filePath = li
                    .replace("%playerUUID%", pd.uuid)
                    .replace("%playerName%", pd.name);

            // 如果包含 %worldPath% 则遍历所有地图
            if(li.contains(ph.worldPath.ph) || li.contains(ph.worldName.ph)){
                // 遍历所有 world
                for(World world : getServer().getWorlds()){
                    String liFilePath = filePath
                            .replace(ph.worldPath.ph, world.getWorldFolder().getPath())
                            .replace(ph.worldName.ph, world.getName());
                    File file = new File(liFilePath);
                    if(file.delete()){
                        plugin.getLogger().info("[delFile]: "+ liFilePath);
                    }
                }
            }else{
                // 直接删除文件
                File file = new File(filePath);
                if(file.delete()){
                    plugin.getLogger().info("[delFile]: "+ filePath);
                }
            }
        }
    }


    public static List<String> tab(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.clear")){
            return List.of("");
        }

        if(args.length == 2){
            return List.of(
                    "PLAYER",
                    "TYPE"
            );
        }
        else if(args.length == 3){
            if(args[1].equalsIgnoreCase("PLAYER")){
                List<String> list = new ArrayList<>();
                for(PlayerData li : sql.findPlayerDataList(setUUID36(args[2]))){
                    list.add(li.uuid +" - "+ li.name +" [TYPE: "+ li.type.name +", BAN: "+ li.ban.name +"] ");
                }
                return list;
            }
            else if(args[1].equalsIgnoreCase("TYPE")){
                return List.of(
                        "NOT",
                        "VISIT",
                        "BAN"
                );
            }
        }
        return List.of("");
    }
}
