package aplini.ipacwhitelist.listener.cmd;

import aplini.ipacwhitelist.enums.Key;
import aplini.ipacwhitelist.enums.ph;
import aplini.ipacwhitelist.utils.PlayerData;
import aplini.ipacwhitelist.utils.sql;
import org.bukkit.command.CommandSender;

import java.util.List;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.utils.util.getDisplayTime;
import static aplini.ipacwhitelist.utils.util.msg;

public class list {

    public static void cmd(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.list")){
            sender.sendMessage(config.getString("message.noPermission", ""));
            return;
        }

        if(args.length < 2){
            sender.sendMessage("/wl list <NOT | VISIT | WHITE | VISIT_CONVERT | BAN | ALL | NAME_CONFLICT | WHITE_TIMEOUT>");
            return;
        }

        // 获取对应的标签
        String typeName = args[1].toUpperCase();

        sender.sendMessage(config.getString("command.list.title", "")
                .replace(ph.type.ph, typeName));

        // 查询指定的数据
        switch(typeName){
            case "NOT" -> sendListMsg(sender, sql.findPlayerDataList("", Key.GET_NOT));
            case "VISIT" -> sendListMsg(sender, sql.findPlayerDataList("", Key.GET_VISIT));
            case "WHITE" -> sendListMsg(sender, sql.findPlayerDataList("", Key.GET_WHITE));
            case "VISIT_CONVERT" -> sendListMsg(sender, sql.findPlayerDataList("", Key.GET_VISIT_CONVERT));
            case "BAN" -> sendListMsg(sender, sql.findPlayerDataList("", Key.GET_BAN));
            case "ALL" -> sendListMsg(sender, sql.findPlayerDataList("", Key.GET_ALL));
            case "NAME_CONFLICT" -> sendListMsg(sender, sql.findPlayerDataList("", Key.GET_NAME_CONFLICT));
            case "WHITE_TIMEOUT" -> sendListMsg(sender, sql.findPlayerDataList("", Key.GET_WHITE_TIMEOUT));
            default -> sender.sendMessage("/wl list <Type>");
        }
    }


    static void sendListMsg(CommandSender sender, List<PlayerData> list){

        if(list.isEmpty()){
            sender.sendMessage(config.getString("command.list.noData", ""));
            return;
        }

        for(PlayerData li : list){
            sender.sendMessage(msg(config.getString("command.list.finish", ""), li.uuid, li.name)
                    .replace(ph.id.ph, ""+ li.id)
                    .replace(ph.type.ph, li.type.name)
                    .replace(ph.ban.ph, li.ban.name)
                    .replace(ph.timeLong.ph, ""+ li.time)
                    .replace(ph.time.ph, getDisplayTime(li.time)));
        }
    }


    public static List<String> tab(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.list")){
            return List.of("");
        }

        if(args.length < 3){
            return List.of(
                    "NOT",
                    "VISIT",
                    "WHITE",
                    "VISIT_CONVERT",
                    "BAN",
                    "ALL",
                    "NAME_CONFLICT",
                    "WHITE_TIMEOUT"
            );
        }

        return null;
    }
}
