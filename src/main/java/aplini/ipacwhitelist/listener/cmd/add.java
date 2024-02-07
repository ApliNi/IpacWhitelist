package aplini.ipacwhitelist.listener.cmd;

import aplini.ipacwhitelist.enums.Key;
import aplini.ipacwhitelist.enums.Type;
import aplini.ipacwhitelist.enums.ph;
import aplini.ipacwhitelist.utils.*;
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

public class add {

    public static void cmd(CommandSender sender, String[] args){

        if(args.length < 2){
            sender.sendMessage("/wl add <playerName|playerUUID>");
            return;
        }

        // 获取指定玩家的数据
        Inp inp = new Inp().fromInp(args[1]);
        if(inp == null){
            sender.sendMessage(config.getString("message.parameterErr", "")
                    .replace(ph.var.ph, args[1]));
            return;
        }

        sender.sendMessage(config.getString("command.add.title", ""));

        // 遍历数据库中的匹配项
        for(PlayerData li : inp.pds){
            // 已被封禁
            if(li.ban == Type.BAN){
                sender.sendMessage(msg(config.getString("command.add.isBan", ""), li.uuid, li.name));
                return;
            }
            // 重置参观账户
            if(li.type == Type.VISIT){
                li.type = Type.VISIT_CONVERT;
                li.save();
                // 运行参观账户转换程序
                runEventFunc("whitelist.VISIT.onWhitelistAddEvent", inp.onlinePlayer, li.uuid, li.name);
                sender.sendMessage(msg(config.getString("command.add.isVisit", ""), li.uuid, li.name));
                return;
            }
            // 已添加到白名单中
            if(li.isExist()){
                sender.sendMessage(msg(config.getString("command.add.isExist", ""), li.uuid, li.name));
                return;
            }
            // 从已删除的数据中恢复, 如果 UUID 相同则恢复, 否则只能添加一条新数据
            else if(li.uuid.equals(inp.forUUID)){
                li.type = Type.WHITE;
                li.save();
                sender.sendMessage(msg(config.getString("command.add.finish", ""), li.uuid, li.name));
                return;
            }
        }

        // 创建一条新数据
        inp.pd.save();
        sender.sendMessage(msg(config.getString("command.add.finish", ""), inp.forUUID, inp.forName));
    }


    public static List<String> tab(String[] args){
        if(args[1].isEmpty()){
            // 返回在线的参观账户列表
            List<String> list = new ArrayList<>();
            for(Player player : server.getOnlinePlayers()){
                String uuid = player.getUniqueId().toString();
                if(visitPlayerList.contains(uuid)){
                    list.add(uuid +" - "+ player.getName() +" [TYPE: VISIT] ");
                }
            }
            return list;
        }

        // 查询匹配的输入
        List<String> list = new ArrayList<>();
        for(PlayerData li : sql.findPlayerDataList(setUUID36(args[1]), Key.GET_VISIT_OR_NOT)){
            list.add(li.uuid +" - "+ li.name +" [TYPE: "+ li.type.name +", BAN: "+ li.ban.name +"] ");
        }
        return list;
    }
}
