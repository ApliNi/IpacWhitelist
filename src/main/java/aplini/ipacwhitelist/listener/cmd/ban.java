package aplini.ipacwhitelist.listener.cmd;

import aplini.ipacwhitelist.enums.Key;
import aplini.ipacwhitelist.enums.Type;
import aplini.ipacwhitelist.enums.ph;
import aplini.ipacwhitelist.utils.Inp;
import aplini.ipacwhitelist.utils.PlayerData;
import aplini.ipacwhitelist.utils.sql;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.IpacWhitelist.server;
import static aplini.ipacwhitelist.func.eventFunc.runEventFunc;
import static aplini.ipacwhitelist.listener.onPlayerLogin.visitPlayerList;
import static aplini.ipacwhitelist.utils.util.msg;
import static aplini.ipacwhitelist.utils.util.setUUID36;

public class ban {

    public static void cmd(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.ban")){
            sender.sendMessage(config.getString("message.noPermission", ""));
            return;
        }

        if(args.length < 2){
            sender.sendMessage("/wl ban <playerName|playerUUID>");
            return;
        }

        // 获取指定玩家的数据
        Inp inp = new Inp().fromInp(args[1], false);
        if(inp == null){
            sender.sendMessage(config.getString("message.parameterErr", "")
                    .replace(ph.var.ph, args[1]));
            return;
        }

        sender.sendMessage(config.getString("command.ban.title", "")
                .replace(ph.var.ph, inp.inp));

        // 不存在匹配项, 添加到黑名单
        if(inp.pds.isEmpty()){
            inp.pd.ban = Type.BAN;
            inp.pd.save();
            sender.sendMessage(msg(config.getString("command.ban.finish", ""), inp.forUUID, inp.forName));
            return;
        }

        // 存在多个
        if(inp.pds.size() >= 2){
            for(PlayerData li : inp.pds){
                sender.sendMessage(msg(config.getString("command.ban.isMulti", ""), li.uuid, li.name));
            }
            return;
        }

        // 遍历数据库中的匹配项
        for(PlayerData li : inp.pds){
            // 已被封禁
            if(li.ban == Type.BAN){
                sender.sendMessage(msg(config.getString("command.ban.isBan", ""), li.uuid, li.name));
                return;
            }
            // 添加到黑名单
            li.ban = Type.BAN;
            li.save();
            runEventFunc("whitelist.BAN.onWhitelistBanEvent", inp.onlinePlayer, li.uuid, li.name);
            sender.sendMessage(msg(config.getString("command.ban.finish", ""), li.uuid, li.name));
            return;
        }
    }


    public static List<String> tab(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.ban")){
            return List.of("");
        }

        if(args[1].isEmpty()){
            // 返回在线的玩家列表
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
        for(PlayerData li : sql.findPlayerDataList(setUUID36(args[1]), Key.GET_ALLOW_BAN)){
            list.add(li.uuid +" - "+ li.name +" [TYPE: "+ li.type.name +", BAN: "+ li.ban.name +"] ");
        }
        return list;
    }
}
