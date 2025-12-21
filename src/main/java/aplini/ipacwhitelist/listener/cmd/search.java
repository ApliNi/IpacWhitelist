package aplini.ipacwhitelist.listener.cmd;

import aplini.ipacwhitelist.enums.ph;
import aplini.ipacwhitelist.utils.PlayerData;
import aplini.ipacwhitelist.utils.sql;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.IpacWhitelist.server;
import static aplini.ipacwhitelist.enums.Key.BY_NAME;
import static aplini.ipacwhitelist.enums.Key.BY_UUID;
import static aplini.ipacwhitelist.listener.onPlayerLogin.visitPlayerList;
import static aplini.ipacwhitelist.utils.util.*;

public class search {

    public static void cmd(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.search")){
            sender.sendMessage(config.getString("message.noPermission", ""));
            return;
        }

        if(args.length < 2){
            sender.sendMessage("/wl search <playerName|playerUUID> | /wl s <playerName|playerUUID>");
            return;
        }

        // 在名称和 uuid 上搜索这个玩家

        if(config.getLong("command.search.byName") > 0) {
            String searchName = args[1];
            List<PlayerData> list = sql.findPlayerDataList(searchName, BY_NAME, config.getLong("command.search.byName", 5));

            sender.sendMessage(config.getString("command.search.byNameTitle", "")
                    .replace(ph.var.ph, setUUID36(searchName)));

            // 不存在匹配项
            if (list.isEmpty()) {
                sender.sendMessage(config.getString("command.search.isEmpty", ""));
                return;
            }

            // 遍历数据库中的匹配项
            for (PlayerData li : list) {
                sender.sendMessage(msg(config.getString("command.search.finish", ""), li.uuid, li.name)
                        .replace(ph.id.ph, "" + li.id)
                        .replace(ph.type.ph, li.type.name)
                        .replace(ph.ban.ph, li.ban.name)
                        .replace(ph.time.ph, getDisplayTime(li.time))
                );
            }
        }

        if(config.getLong("command.search.byUUID") > 0){
            String searchUUID = setUUID36(args[1]);
            List<PlayerData> list = sql.findPlayerDataList(searchUUID, BY_UUID, config.getLong("command.search.byUUID", 3));

            sender.sendMessage(config.getString("command.search.byUUIDTitle", "")
                    .replace(ph.var.ph, searchUUID));

            // 不存在匹配项
            if (list.isEmpty()) {
                sender.sendMessage(config.getString("command.search.isEmpty", ""));
                return;
            }

            // 遍历数据库中的匹配项
            for (PlayerData li : list) {
                sender.sendMessage(msg(config.getString("command.search.finish", ""), li.uuid, li.name)
                        .replace(ph.id.ph, "" + li.id)
                        .replace(ph.type.ph, li.type.name)
                        .replace(ph.ban.ph, li.ban.name)
                        .replace(ph.time.ph, getDisplayTime(li.time))
                );
            }
        }
    }


    public static List<String> tab(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.search")){
            return List.of("");
        }

        if(args[1].isEmpty()){
            // 返回在线玩家列表
            List<String> list = new ArrayList<>();
            for(Player player : server.getOnlinePlayers()){
                String uuid = player.getUniqueId().toString();
                if(visitPlayerList.contains(uuid)){
                    list.add(uuid +" - "+ player.getName() +" [TYPE: VISIT] ");
                }else{
                    list.add(uuid +" - "+ player.getName() +" [TYPE: WHITE] ");
                }
            }
            list.add("....");
            return list;
        }

        // 查询匹配的输入
        List<String> list = new ArrayList<>();
        for(PlayerData li : sql.findPlayerDataList(setUUID36(args[1]))){
            list.add(li.uuid +" - "+ li.name +" [TYPE: "+ li.type.name +", BAN: "+ li.ban.name +"] ");
        }
        return list;
    }
}
