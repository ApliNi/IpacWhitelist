package aplini.ipacwhitelist.Listener;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.util.SQL_io;
import aplini.ipacwhitelist.util.Type;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

import static aplini.ipacwhitelist.IpacWhitelist.allowJoin;
import static aplini.ipacwhitelist.util.SQL_io.getVisitPlayerUUIDFromName;

public class CommandHandler implements Listener, CommandExecutor, TabCompleter {
    private static IpacWhitelist plugin;
    public CommandHandler(IpacWhitelist plugin){
        CommandHandler.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        switch(args[0]){
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(plugin.getConfig().getString("message.command.reload", ""));
            }

            // 重新连接数据库
            case "reconnect_database" -> {
                allowJoin = false;
                SQL_io.reconnect();
                SQL_io.initialize();
                sender.sendMessage(plugin.getConfig().getString("message.command.reconnect-database", ""));
                allowJoin = true;
            }

            // 添加一个账户
            case "add" -> {

                String playerName;
                String playerUUID;
                Type state;

                if(args.length == 2){
                    // 检查参数
                    if(args[1].length() > 16){
                        sender.sendMessage(plugin.getConfig().getString("message.command.err-name-length", ""));
                        return true;
                    }

                    playerName = args[1];
                    playerUUID = getVisitPlayerUUIDFromName(args[1]);
                    state = SQL_io.addPlayer(playerName);

                }else if(args.length >= 3){
                    // 检查参数
                    if(args[1].length() > 16){
                        sender.sendMessage(plugin.getConfig().getString("message.command.err-name-length", ""));
                        return true;
                    }
                    if(args[2].length() != 36){
                        sender.sendMessage(plugin.getConfig().getString("message.command.err-uuid-length", ""));
                        return true;
                    }

                    playerName = args[1];
                    playerUUID = args[2];
                    state = SQL_io.addPlayer(playerName, playerUUID);

                }else{
                    sender.sendMessage("/wl add <Name> [UUID]");
                    return true;
                }

                switch(state){
                    case VISIT -> { // 参观账户
                        // 运行 wl-add
                        for(String li : plugin.getConfig().getStringList("visit.wl-add.command")){
                            Bukkit.getScheduler().callSyncMethod(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                    li
                                            .replace("%playerName%", playerName)
                                            .replace("%playerUUID%", playerUUID)));
                        }
                        // WHITE, BLACK ->
                        sender.sendMessage(plugin.getConfig().getString("message.command.add-reset", "").replace("%player%", args[1]));
                    }
                    case WHITE, BLACK -> // 普通账户/黑名单
                            sender.sendMessage(plugin.getConfig().getString("message.command.add-reset", "").replace("%player%", args[1]));
                    case NOT -> // 没有账户
                            sender.sendMessage(plugin.getConfig().getString("message.command.add", "").replace("%player%", args[1]));
                    default ->
                            sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
                }

                return true;
            }

            // 删除一个账户
            case "del", "unban" -> {
                if(args.length != 2){
                    sender.sendMessage("/wl del <Name|UUID>");
                    return true;
                }

                Type state;
                Player player;

                // uuid
                if(args[1].length() == 36){
                    state = SQL_io.delPlayerUUID(args[1]);
                    player = Bukkit.getPlayer(UUID.fromString(args[1]));
                }
                // name
                else if(args[1].length() <= 16){
                    state = SQL_io.delPlayerName(args[1]);
                    player = Bukkit.getPlayer(args[1]);
                }

                else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                if(state != Type.ERROR){
                    sender.sendMessage(plugin.getConfig().getString("message.command.del", "").replace("%player%", args[1]));
                    // 踢出玩家
                    if(plugin.getConfig().getBoolean("whitelist.kick-out-on-del") && player != null){
                        player.kickPlayer(plugin.getConfig().getString("message.join.not", "").replace("%player%", player.getName()));
                    }
                }else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
                }
                return true;
            }

            // 封禁一个账户
            case "ban" -> {
                if(args.length != 2){
                    sender.sendMessage("/wl del <Name|UUID>");
                    return true;
                }

                Type state;
                Player player;

                // uuid
                if(args[1].length() == 36){
                    state = SQL_io.banPlayerUUID(args[1]);
                    player = Bukkit.getPlayer(UUID.fromString(args[1]));
                }
                // name
                else if(args[1].length() <= 16){
                    state = SQL_io.banPlayerName(args[1]);
                    player = Bukkit.getPlayer(args[1]);
                }

                else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                if(state != Type.ERROR){
                    sender.sendMessage(plugin.getConfig().getString("message.command.ban", "").replace("%player%", args[1]));
                    // 踢出玩家
                    if(plugin.getConfig().getBoolean("whitelist.kick-out-on-ban") && player != null){
                        player.kickPlayer(plugin.getConfig().getString("message.join.black", "").replace("%player%", player.getName()));
                    }
                }else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
                }

                return true;
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, String[] args) {
        if(args.length == 1){
            return List.of(
                    "reload",
                    "add",
                    "del",
                    "ban",
                    "unban",
                    "reconnect_database"
            );
        }
        return null;
    }

}
