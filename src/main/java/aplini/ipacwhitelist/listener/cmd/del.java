package aplini.ipacwhitelist.listener.cmd;

import aplini.ipacwhitelist.enums.Type;
import aplini.ipacwhitelist.enums.ph;
import aplini.ipacwhitelist.utils.Inp;
import aplini.ipacwhitelist.utils.PlayerData;
import aplini.ipacwhitelist.utils.sql;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.func.eventFunc.runEventFunc;
import static aplini.ipacwhitelist.utils.util.msg;
import static aplini.ipacwhitelist.utils.util.setUUID36;

public class del {

    public static void cmd(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.del")){
            sender.sendMessage(config.getString("message.noPermission", ""));
            return;
        }

        if(args.length < 2){
            sender.sendMessage("/wl del <playerName|playerUUID>");
            return;
        }

        // 获取指定玩家的数据
        Inp inp = new Inp().fromInp(args[1], false);
        if(inp == null){
            sender.sendMessage(config.getString("message.parameterErr", "")
                    .replace(ph.var.ph, args[1]));
            return;
        }

        sender.sendMessage(config.getString("command.del.title", "")
                .replace(ph.var.ph, inp.inp));

        // 不存在匹配项
        if(inp.pds.isEmpty()){
            sender.sendMessage(msg(config.getString("command.del.isEmpty", ""), inp.forUUID, inp.forName));
            return;
        }

        // 存在多个
        if(inp.pds.size() >= 2){
            for(PlayerData li : inp.pds){
                sender.sendMessage(msg(config.getString("command.del.isMulti", ""), li.uuid, li.name));
            }
            return;
        }

        // 遍历数据库中的匹配项
        for(PlayerData li : inp.pds){
            // 已被封禁
            if(li.ban == Type.BAN){
                sender.sendMessage(msg(config.getString("command.del.isBan", ""), li.uuid, li.name));
                return;
            }
            runEventFunc("whitelist.WHITE.onWhitelistDelEvent", inp.onlinePlayer, li.uuid, li.name);
            li.type = Type.NOT;
            li.save();
            sender.sendMessage(msg(config.getString("command.del.finish", ""), li.uuid, li.name));
            return;
        }
    }


    public static List<String> tab(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.del")){
            return List.of("");
        }

        if(args[1].isEmpty()){
            return List.of("....");
        }

        // 查询匹配的输入
        List<String> list = new ArrayList<>();
        for(PlayerData li : sql.findPlayerDataList(setUUID36(args[1]))){
            list.add(li.uuid +" - "+ li.name +" [TYPE: "+ li.type.name +", BAN: "+ li.ban.name +"] ");
        }
        return list;
    }
}
