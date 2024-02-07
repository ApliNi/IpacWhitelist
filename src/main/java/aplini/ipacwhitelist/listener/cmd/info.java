package aplini.ipacwhitelist.listener.cmd;

import aplini.ipacwhitelist.utils.Inp;
import aplini.ipacwhitelist.utils.PlayerData;
import aplini.ipacwhitelist.enums.ph;
import aplini.ipacwhitelist.utils.sql;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.IpacWhitelist.server;
import static aplini.ipacwhitelist.listener.onPlayerLogin.visitPlayerList;
import static aplini.ipacwhitelist.utils.util.*;

public class info {

    public static void cmd(CommandSender sender, String[] args){

        if(args.length < 2){
            sender.sendMessage("/wl info <playerName|playerUUID>");
            return;
        }

        // 获取指定玩家的数据
        Inp inp = new Inp().fromInp(args[1], false);
        if(inp == null){
            sender.sendMessage(config.getString("message.parameterErr", "")
                    .replace(ph.var.ph, args[1]));
            return;
        }

        sender.sendMessage(config.getString("command.info.title", ""));

        // 不存在匹配项
        if(inp.pds.isEmpty()){
            sender.sendMessage(msg(config.getString("command.info.isEmpty", ""), inp.forUUID, inp.forName));
            return;
        }

        // 遍历数据库中的匹配项
        for(PlayerData li : inp.pds){
            sender.sendMessage(msg(config.getString("command.info.finish", ""), li.uuid, li.name)
                    .replace(ph.id.ph, ""+ li.id)
                    .replace(ph.type.ph, li.type.name)
                    .replace(ph.ban.ph, li.ban.name)
                    .replace(ph.time.ph, getDisplayTime(li.time))
            );
        }
    }


    public static List<String> tab(String[] args){
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
