package aplini.ipacwhitelist.listener.cmd;

import aplini.ipacwhitelist.enums.Type;
import aplini.ipacwhitelist.utils.PlayerData;
import aplini.ipacwhitelist.utils.sql;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.IpacWhitelist.plugin;
import static aplini.ipacwhitelist.utils.util.*;

public class importData {

    public static void cmd(CommandSender sender, String[] args){

        if(config.getBoolean("command.importData.enable", false)){
            sender.sendMessage(config.getString("message.noEnable", ""));
            return;
        }

        if(!sender.hasPermission("IpacWhitelist.cmd.importData")){
            sender.sendMessage(config.getString("message.noPermission", ""));
            return;
        }

        if(args.length < 1){
            sender.sendMessage("/wl import");
            return;
        }

        String filePath = new File(plugin.getDataFolder(), config.getString("command.importData.file", "")).getPath();
        if(!new File(filePath).exists()){
            sender.sendMessage("文件不存在");
            return;
        }

        try(BufferedReader br = new BufferedReader(new FileReader(filePath))){
            String li;
            while((li = br.readLine()) != null){
                sender.sendMessage("读取到行: "+ li);
                Matcher matcher = Pattern.compile(config.getString("command.importData.regExp", "")).matcher(li);
                if(matcher.find()){
                    PlayerData pd = new PlayerData();
                    pd.type  = Type.getType(getMatcherData(matcher, "TYPE"));
                    pd.ban   = Type.getType(getMatcherData(matcher, "BAN"));
                    pd.uuid  = getMatcherData(matcher, "UUID");
                    pd.name  = getMatcherData(matcher, "NAME");
                    pd.time  = Long.parseLong(getMatcherData(matcher, "TIME"));
                    // 检查 null
                    if(pd.type == null || pd.ban == null || pd.uuid == null || pd.name == null || pd.time == 0){
                        sender.sendMessage("  - [失败] 存在未成功匹配的数据");
                        continue;
                    }
                    // 检查名称和 UUID
                    pd.name = setUUID36(pd.name);
                    if(!Pattern.matches(config.getString("whitelist.playerNameRule", ".*"), pd.name)){
                        sender.sendMessage("  - [失败] 参数不可识别或未通过检查: NAME");
                        continue;
                    }
                    // 检查是否存在现有的数据
                    pd.uuid = setUUID36(pd.uuid);
                    PlayerData pdForDB = sql.getPlayerData(pd.uuid, pd.name, true);
                    if(!pdForDB.isNull()){
                        sender.sendMessage("  - 存在重复数据");
                        continue;
                    }
                    pd.updateTime = false;
                    pd.save();
                    sender.sendMessage("  - 导入成功 "+ pd.name +"["+ pd.uuid +"]: TYPE: "+ pd.type.name +", BAN: "+ pd.ban.name +", TIME: "+ getDisplayTime(pd.time)+ ". ");
                }
            }
            sender.sendMessage("数据导入结束");
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    public static String getMatcherData(Matcher matcher, String type){
        return matcher.group(config.getInt("command.importData."+ type));
    }
}
