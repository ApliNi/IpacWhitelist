package aplini.ipacwhitelist.Listener;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.util.SQL;
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
import static aplini.ipacwhitelist.util.EventFunc.startVisitConvertFunc;
import static aplini.ipacwhitelist.util.SQL.getPlayerInfo;

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
                SQL.reconnect();
                SQL.initialize();
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
                    playerUUID = getPlayerInfo(Type.UUID, playerName); // 如果不存在则输出 null
                    state = SQL.addPlayer(playerName);

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
                    state = SQL.addPlayer(playerName, playerUUID);

                }else{
                    sender.sendMessage("/wl add <playerName> [playerUUID]");
                    return true;
                }

                // 获取玩家对象, 如果不在线则为 null
                Player player = Bukkit.getPlayer(playerName);

                // 根据添加前的 Type
                switch(state){
                    case VISIT, VISIT_CONVERT, VISIT_BLACK -> { // 参观账户/需要转换的参观账户/被封禁的参观账户
                        // 运行 wl-add
                        startVisitConvertFunc(plugin, playerName, playerUUID, "visit.wl-add.command");
                        // 玩家在线或不在线
                        if(player != null){
                            // 修改 Type 为 WHITE, 同时更新时间
                            SQL.addPlayer(player, -3, Type.WHITE);
                            // 运行 wl-add-convert
                            startVisitConvertFunc(plugin, playerName, playerUUID, "visit.wl-add-convert.command");
                        }else{
                            // 标记为 VISIT_CONVERT, 等待下一次加入时处理 (在 onPlayerJoin 中)
                            SQL.addPlayer(playerName, playerUUID, Type.VISIT_CONVERT);
                        }
                        // WHITE, BLACK ->
                        sender.sendMessage(plugin.getConfig().getString("message.command.add-reset-visit", "").replace("%player%", playerName));
                    }
                    case WHITE, BLACK -> // 在 白名单/黑名单 中
                            sender.sendMessage(plugin.getConfig().getString("message.command.add-reset", "").replace("%player%",playerName));
                    case NOT -> // 没有账户
                            sender.sendMessage(plugin.getConfig().getString("message.command.add", "").replace("%player%", playerName));
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
                String playerName;
                Player player;

                // uuid
                if(args[1].length() == 36){
                    playerName = SQL.getPlayerInfo(Type.NAME, args[1]);
                    state = SQL.delPlayerUUID(args[1]);
                    player = Bukkit.getPlayer(UUID.fromString(args[1]));
                }
                // name
                else if(args[1].length() <= 16){
                    playerName = args[1];
                    state = SQL.delPlayerName(playerName);
                    player = Bukkit.getPlayer(playerName);
                }

                else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                if(state != Type.ERROR){
                    // 踢出玩家
                    if(plugin.getConfig().getBoolean("whitelist.kick-out-on-del") && player != null){
                        player.kickPlayer(plugin.getConfig().getString("message.join.not", "").replace("%player%", playerName));
                    }
                    sender.sendMessage(plugin.getConfig().getString("message.command.del", "").replace("%player%", playerName));
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
                String playerUUID;
                Player player;

                // uuid
                if(args[1].length() == 36){
                    playerUUID = args[1];
                    state = SQL.banPlayerUUID(args[1]);
                    player = Bukkit.getPlayer(UUID.fromString(args[1]));
                }
                // name
                else if(args[1].length() <= 16){
                    playerUUID = SQL.getPlayerInfo(Type.UUID, args[1]);
                    state = SQL.banPlayerName(args[1]);
                    player = Bukkit.getPlayer(args[1]);
                }

                else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                if(state != Type.ERROR){
                    sender.sendMessage(plugin.getConfig().getString("message.command.ban", "").replace("%player%", args[1]));
                    // 如果玩家被封禁前是参观账户
                    if(state == Type.VISIT){
                        // 修改 Type 为 VISIT_BLACK
                        SQL.setPlayerData(null, playerUUID, -2, Type.VISIT_BLACK);
                    }
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
