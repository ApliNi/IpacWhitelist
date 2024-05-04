package aplini.ipacwhitelist.listener.cmd;

import aplini.ipacwhitelist.enums.Key;
import aplini.ipacwhitelist.enums.Type;
import aplini.ipacwhitelist.enums.pc;
import aplini.ipacwhitelist.enums.ph;
import aplini.ipacwhitelist.utils.Inp;
import aplini.ipacwhitelist.utils.PlayerData;
import aplini.ipacwhitelist.utils.sql;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Supplier;

import static aplini.ipacwhitelist.IpacWhitelist.*;
import static aplini.ipacwhitelist.func.eventFunc.runEventFunc;
import static aplini.ipacwhitelist.listener.onPlayerLogin.visitPlayerList;
import static aplini.ipacwhitelist.utils.util.msg;
import static aplini.ipacwhitelist.utils.util.setUUID36;

public class add {

    public static void cmd(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.add")){
            sender.sendMessage(config.getString("message.noPermission", ""));
            return;
        }

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

        // 操作记录
        Map<String, Object> addData = new HashMap<>();
        if(config.getBoolean("command.add.logger_sender.enable", false)){
            if(sender instanceof Player){
                Player player = (Player) sender;
                addData.put(pc.CmdAddLogSender.key,
                        config.getString("command.add.logger_sender.isPlayer", "")
                                .replace(ph.playerName.ph, player.getName())
                                .replace(ph.playerUUID.ph, player.getUniqueId().toString()));
            }else{
                addData.put(pc.CmdAddLogSender.key, config.getString("command.add.logger_sender.isOther", "[Other]"));
            }
        }

        sender.sendMessage(config.getString("command.add.title", "")
                .replace(ph.var.ph, inp.inp));

        // 遍历数据库中的匹配项
        for(PlayerData li : inp.pds){
            // 已被封禁
            if(li.ban == Type.BAN){
                sender.sendMessage(msg(config.getString("command.add.isBan", ""), li.uuid, li.name));
                return;
            }
            // 重置参观账户
            if(li.type == Type.VISIT){
                // 运行参观账户转换程序
                runEventFunc("whitelist.VISIT.onWhitelistAddEvent", inp.onlinePlayer, li.uuid, li.name);
                li.type = Type.VISIT_CONVERT;
                li.config.putAll(addData);
                li.save();
                sender.sendMessage(msg(config.getString("command.add.isVisit", ""), li.uuid, li.name));
                return;
            }
            // 非 NOT 类型
            if(li.isExist()){
                li.save();  // 更新时间记录
                sender.sendMessage(msg(config.getString("command.add.isExist", ""), li.uuid, li.name));
                return;
            }
            // 从已删除的数据中恢复, 如果 UUID 相同则恢复, 否则只能添加一条新数据
            else if(Objects.equals(li.uuid, inp.forUUID)){
                li.type = Type.WHITE;
                li.config.putAll(addData);
                li.save();
                sender.sendMessage(msg(config.getString("command.add.finish", ""), li.uuid, li.name));
                return;
            }
            // 需要创建单独的数据
            // else {}
        }

        // 创建新数据
        inp.pd = new PlayerData();
        inp.pd.type = Type.WHITE;
        inp.pd.config.putAll(addData);
        inp.pd.setPlayerInfo(inp.forUUID, inp.forName);
        inp.pd.save();
        sender.sendMessage(msg(config.getString("command.add.finish", ""), inp.forUUID, inp.forName));
    }


    public static List<String> tab(CommandSender sender, String[] args){

        if(!sender.hasPermission("IpacWhitelist.cmd.add")){
            return List.of("");
        }

        if(args[1].isEmpty()){
            // 返回在线的参观账户列表
            List<String> list = new ArrayList<>();
            for(Player player : server.getOnlinePlayers()){
                String uuid = player.getUniqueId().toString();
                if(visitPlayerList.contains(uuid)){
                    list.add(uuid +" - "+ player.getName() +" [TYPE: VISIT] ");
                }
            }
            list.add("....");
            return list;
        }

        // 查询匹配的输入
        List<String> list = new ArrayList<>();
        for(PlayerData li : sql.findPlayerDataList(setUUID36(args[1]), Key.GET_ALLOW_ADD)){
            list.add(li.uuid +" - "+ li.name +" [TYPE: "+ li.type.name +", BAN: "+ li.ban.name +"] ");
        }
        return list;
    }
}
