package aplini.ipacwhitelist.listener.cmd;

import aplini.ipacwhitelist.utils.sql;
import org.bukkit.command.CommandSender;

import static aplini.ipacwhitelist.IpacWhitelist.*;

public class reload {

    public static void cmd(CommandSender sender){

        if(!sender.hasPermission("IpacWhitelist.cmd.reload")){
            sender.sendMessage(config.getString("message.noPermission", ""));
            return;
        }

        allowJoin = false;
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        sql.reconnect();
        sql.initialize();
        allowJoin = true;

        sender.sendMessage("重载完成");
    }
}
