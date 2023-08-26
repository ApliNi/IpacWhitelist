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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static aplini.ipacwhitelist.IpacWhitelist.allowJoin;
import static aplini.ipacwhitelist.util.EventFunc.startVisitConvertFunc;
import static aplini.ipacwhitelist.util.SQL.*;
import static aplini.ipacwhitelist.util.Util.ifIsUUID32toUUID36;

public class CommandHandler implements Listener, CommandExecutor, TabCompleter {
    private static IpacWhitelist plugin;
    private static List<String> commandList;
    public CommandHandler(IpacWhitelist plugin){
        CommandHandler.plugin = plugin;
        commandList = List.of(
                "reload",
                "add",
                "del",
                "ban",
                "unban",
                "info",
                "list"
        );
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if(args.length == 0){
            sender.sendMessage("/wl ... "+ String.join(", ", commandList));
            return true;
        }

        switch(args[0]){
            case "reload" -> {
                allowJoin = false;
                // 重载配置
                plugin.reloadConfig();
                // 重载数据库
                SQL.reconnect();
                SQL.initialize();
                sender.sendMessage(plugin.getConfig().getString("message.command.reload", ""));
                allowJoin = true;
                return true;
            }

            // 添加一个账户
            case "add" -> {
                if(args.length != 2){
                    sender.sendMessage("/wl add <playerName|playerUUID>");
                    return true;
                }

                String inpData = ifIsUUID32toUUID36(args[1]);
                String playerName;
                String playerUUID;
                Type state;


                if(inpData.length() == 36){ // uuid
                    playerUUID = inpData;
                    playerName = getPlayerName(playerUUID); // 如果不存在则输出 null
                    state = SQL.addPlayer(null, playerUUID);

                }else if(inpData.length() <= 16){ // name
                    playerName = inpData;
                    playerUUID = getPlayerUUID(playerName); // 如果不存在则输出 null
                    state = SQL.addPlayer(playerName, null);

                }else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                // 根据添加前的 Type
                switch(state){
                    // 如果为这些 Type, 数据库中一定有名称和 UUID
                    case VISIT, VISIT_CONVERT, VISIT_BLACK -> { // 参观账户/需要转换的参观账户/被封禁的参观账户
                        // 运行 wl-add
                        startVisitConvertFunc(plugin, playerName, playerUUID, "visit.wl-add.command");
                        // 获取玩家对象, 如果不在线则为 null
                        Player player = playerName != null ? Bukkit.getPlayer(playerName) : null;
                        // 是否需要踢出玩家
                        if(plugin.getConfig().getBoolean("whitelist.kick-on-add-visit") && player != null){
                            player.kickPlayer(plugin.getConfig().getString("message.join.add"));
                            player = null;
                        }
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
                        sender.sendMessage(plugin.getConfig().getString("message.command.add-reset-visit", "").replace("%player%", inpData));
                    }

                    // 如果为这些 Type, 数据库中可能缺失名称或 UUID
                    case WHITE, BLACK -> // 在 白名单/黑名单 中
                            sender.sendMessage(plugin.getConfig().getString("message.command.add-reset", "").replace("%player%", inpData));
                    case NOT -> // 没有账户
                            sender.sendMessage(plugin.getConfig().getString("message.command.add", "").replace("%player%", inpData));
                    default ->
                            sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
                }

                return true;
            }

            // 删除一个账户
            case "del", "unban" -> {
                if(args.length != 2){
                    sender.sendMessage("/wl "+ args[0] +" <playerName|playerUUID>");
                    return true;
                }

                String inpData = ifIsUUID32toUUID36(args[1]);
                Player player;
                Type state;


                if(args[1].length() == 36){ // uuid
                    // 检查这个玩家是否存在
                    if(getPlayerType(Type.UUID, inpData) == Type.NOT){
                        sender.sendMessage(plugin.getConfig().getString("message.command.err-note-exist", "")
                                .replace("%player%", inpData));
                        return true;
                    }
                    state = SQL.delPlayerUUID(inpData);
                    player = Bukkit.getPlayer(UUID.fromString(inpData));

                }else if(inpData.length() <= 16){ // name
                    // 检查这个玩家是否存在
                    if(getPlayerType(Type.NAME, inpData) == Type.NOT){
                        sender.sendMessage(plugin.getConfig().getString("message.command.err-note-exist", "")
                                .replace("%player%", inpData));
                        return true;
                    }
                    state = SQL.delPlayerName(inpData);
                    player = Bukkit.getPlayer(inpData);

                }else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                if(state != Type.ERROR){
                    // 踢出玩家
                    if(plugin.getConfig().getBoolean("whitelist.kick-on-del") && player != null){
                        player.kickPlayer(plugin.getConfig().getString("message.join.not", "").replace("%player%", player.getName()));
                    }
                    sender.sendMessage(plugin.getConfig().getString("message.command.del", "").replace("%player%", inpData));
                }else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
                }
                return true;
            }

            // 封禁一个账户
            case "ban" -> {
                if(args.length != 2){
                    sender.sendMessage("/wl ban <playerName|playerUUID>");
                    return true;
                }

                String inpData = ifIsUUID32toUUID36(args[1]);
                String playerUUID;
                Player player;
                Type state;


                if(inpData.length() == 36){ // uuid
                    playerUUID = inpData;
                    state = SQL.banPlayerUUID(inpData);
                    player = Bukkit.getPlayer(UUID.fromString(inpData));

                }else if(inpData.length() <= 16){ // name
                    playerUUID = SQL.getPlayerUUID(inpData);
                    state = SQL.banPlayerName(inpData);
                    player = Bukkit.getPlayer(inpData);

                }else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                if(state != Type.ERROR){
                    sender.sendMessage(plugin.getConfig().getString("message.command.ban", "").replace("%player%", inpData));
                    // 如果玩家被封禁前是参观账户
                    if(state == Type.VISIT){
                        // 修改 Type 为 VISIT_BLACK
                        SQL.setPlayerData(null, playerUUID, -2, Type.VISIT_BLACK);
                    }
                    // 踢出玩家
                    if(plugin.getConfig().getBoolean("whitelist.kick-on-ban") && player != null){
                        player.kickPlayer(plugin.getConfig().getString("message.join.black", "").replace("%player%", player.getName()));
                    }
                }else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
                }

                return true;
            }

            case "info" -> {
                if(args.length != 2){
                    sender.sendMessage("/wl info <playerName|playerUUID>");
                    return true;
                }

                String inpData = ifIsUUID32toUUID36(args[1]);
                ResultSet results;


                if(inpData.length() == 36){ // uuid
                    results = getPlayerData(Type.UUID, inpData);

                }else if(inpData.length() <= 16){ // name
                    results = getPlayerData(Type.NAME, inpData);

                }else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                if(results == null){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-note-exist", "")
                            .replace("%player%", inpData));
                    return true;
                }

                // {ID: %ID%, Type: "%Type%", UUID: "%UUID%", Name: "%Name%", Time: %Time%}
                try {
                    sender.sendMessage(plugin.getConfig().getString("message.command.info", "")
                            .replace("%player%", inpData)
                            .replace("%ID%", String.valueOf(results.getInt("ID")))
                            .replace("%Type%", Type.getType(results.getInt("Type")).getName())
                            .replace("%UUID%", results.getString("UUID"))
                            .replace("%Name%", results.getString("Name"))
                            .replace("%Time%", String.valueOf(results.getLong("Time"))));
                } catch (SQLException ignored) {}

                return true;
            }

            case "list" -> {
                if (args.length != 3) {
                    sender.sendMessage("/wl list <VISIT|WHITE|BLACK|VISIT_CONVERT|VISIT_BLACK|*> <num|ALL>");
                    return true;
                }

                // 获取选择的 Type
                String inp1 = args[1].toUpperCase();
                if(inp1.equals("*")){inp1 = "ALL";}
                if(!Pattern.compile("^(?:VISIT|WHITE|BLACK|VISIT_CONVERT|VISIT_BLACK|ALL)$").matcher(inp1).matches()){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-parameter", "")
                            .replace("%i%", inp1));
                    return true;
                }

                // 获取最大输出数量
                String inp2 = args[2].toUpperCase();
                if(!Pattern.compile("^[0-9]{1,8}$|^ALL$").matcher(inp2).matches()){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-parameter", "")
                            .replace("%i%", inp2));
                    return true;
                }
                if(inp2.equals("ALL")){inp2 = "-1";}

                // 准备查询
                Type type = Type.valueOf(inp1);
                int maxLine = Integer.parseInt(inp2);

                AtomicInteger i = new AtomicInteger();
                whileDataForList(type, maxLine, (results) -> {
                    try {
                        sender.sendMessage(plugin.getConfig().getString("message.command.list", "")
                                .replace("%num%", i.toString())
                                .replace("%ID%", String.valueOf(results.getInt("ID")))
                                .replace("%Type%", Type.getType(results.getInt("Type")).getName())
                                .replace("%UUID%", results.getString("UUID"))
                                .replace("%Name%", results.getString("Name"))
                                .replace("%Time%", String.valueOf(results.getLong("Time"))));
                    } catch (SQLException ignored) {}
                    i.getAndIncrement();
                });

                return true;
            }
        }

        sender.sendMessage("/wl ... "+ String.join(", ", commandList));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, String[] args) {
        switch(args.length){
            case 1 -> {
                return commandList;
            }
            case 2 -> {
                if(args[0].equalsIgnoreCase("list")){
                    // VISIT|WHITE|BLACK|VISIT_CONVERT|VISIT_BLACK|*
                    return List.of(
                            "VISIT",
                            "WHITE",
                            "BLACK",
                            "VISIT_CONVERT",
                            "VISIT_BLACK",
                            "*"
                    );
                }
            }
            case 3 -> {
                if(args[0].equalsIgnoreCase("list")){
                    // num|ALL
                    return List.of("10", "100", "ALL");
                }
            }
        }
        return null;
    }

}
