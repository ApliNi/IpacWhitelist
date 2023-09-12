package aplini.ipacwhitelist.Listener;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.util.PlayerData;
import aplini.ipacwhitelist.util.SQL;
import aplini.ipacwhitelist.util.Type;
import aplini.ipacwhitelist.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static aplini.ipacwhitelist.Func.CleanPlayerData.deletePlayerDataAll;
import static aplini.ipacwhitelist.Func.EventFunc.startVisitConvertFunc;
import static aplini.ipacwhitelist.IpacWhitelist.allowJoin;
import static aplini.ipacwhitelist.util.SQL.whileDataForList;
import static aplini.ipacwhitelist.util.Util.getTime;
import static aplini.ipacwhitelist.util.Util.ifIsUUID32toUUID36;
import static org.bukkit.Bukkit.getLogger;

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
                "list",
                "clean"
        );
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if(args.length == 0){
            sender.sendMessage("/wl ... "+ String.join(", ", commandList));
            return true;
        }

        switch(args[0].toLowerCase()){
            case "reload" -> {
                if(!sender.hasPermission("IpacWhitelist.command.reload")){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-permission", ""));
                    return true;
                }

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
                if(!sender.hasPermission("IpacWhitelist.command.add")){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-permission", ""));
                    return true;
                }

                if(args.length != 2){
                    sender.sendMessage("/wl add <playerName|playerUUID>");
                    return true;
                }

                // 获取玩家数据
                String inpData = ifIsUUID32toUUID36(args[1]);
                PlayerData pd = Util.getPlayerData(inpData);
                // 输入数据错误
                if(pd == null){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                // 如果已被封禁
                if(pd.Ban == Type.BAN){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-ban", "").replace("%player%", pd.Name));
                    return true;
                }

                // 根据添加前的 Type
                switch(pd.Type){
                    // 如果为这些 Type, 数据库中一定有名称和 UUID
                    case VISIT, VISIT_CONVERT -> { // 参观账户/需要转换的参观账户
                        // 运行 wl-add
                        startVisitConvertFunc(plugin, pd.Name, pd.UUID, "visit.wl-add.command");
                        // 玩家在线
                        Player player = Bukkit.getPlayer(pd.Name);
                        if(player != null){
                            // 是否需要踢出玩家
                            if(plugin.getConfig().getBoolean("whitelist.kick-on-add-visit")){
                                player.kickPlayer(plugin.getConfig().getString("message.join.add"));
                            }else{
                                // 运行 wl-add-convert
                                startVisitConvertFunc(plugin, pd.Name, pd.UUID, "visit.wl-add-convert.command");
                            }
                        }else{
                            // 标记为 VISIT_CONVERT, 等待下一次加入时处理 (在 onPlayerJoin 中)
                            pd.Type = Type.VISIT_CONVERT;
                            getLogger().info("[IpacWhitelist] "+ pd.Name +" 不在线, wl-add-convert 将推迟到玩家重新上线时运行");
                        }
                        sender.sendMessage(plugin.getConfig().getString("message.command.add-reset-visit", "").replace("%player%", pd.Name));
                    }

                    // 如果为这些 Type, 数据库中可能缺失名称或 UUID

                    case WHITE -> // 白名单, 只需要更新即可
                            sender.sendMessage(plugin.getConfig().getString("message.command.add-reset", "").replace("%player%", inpData));
                    case NOT -> { // 没有账户, 添加到白名单
                        pd.Type = Type.WHITE;
                        sender.sendMessage(plugin.getConfig().getString("message.command.add", "").replace("%player%", inpData));
                    }
                    default -> {
                        sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
                        return true;
                    }
                }

                // 更新时间, 设置玩家数据
                pd.Time = -3;
                pd.addPlayerAuto(inpData);
                pd.save();

                return true;
            }

            // 删除一个账户
            case "del" -> {
                if(!sender.hasPermission("IpacWhitelist.command.del")){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-permission", ""));
                    return true;
                }

                if(args.length != 2){
                    sender.sendMessage("/wl del <playerName|playerUUID>");
                    return true;
                }

                // 获取玩家数据
                String inpData = ifIsUUID32toUUID36(args[1]);
                PlayerData pd = Util.getPlayerData(inpData);
                // 输入数据错误
                if(pd == null){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                // 是否被封禁
                if(pd.Ban == Type.BAN){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-ban", ""));
                    return true;
                }

                // 检查这个玩家是否存在
                if(pd.Type == Type.NOT){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-note-exist", "").replace("%player%", inpData));
                    return true;
                }

                // 删除
                pd.Type = Type.NOT;
                pd.save();

                // 踢出玩家
                Player player = Bukkit.getPlayer(pd.Name);
                if(plugin.getConfig().getBoolean("whitelist.kick-on-del") && player != null){
                    player.kickPlayer(plugin.getConfig().getString("message.join.not", "").replace("%player%", player.getName()));
                }
                sender.sendMessage(plugin.getConfig().getString("message.command.del", "").replace("%player%", inpData));
                return true;
            }

            // 封禁一个账户
            case "ban" -> {
                if(!sender.hasPermission("IpacWhitelist.command.ban")){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-permission", ""));
                    return true;
                }

                if(args.length != 2){
                    sender.sendMessage("/wl ban <playerName|playerUUID>");
                    return true;
                }

                // 获取玩家数据
                String inpData = ifIsUUID32toUUID36(args[1]);
                PlayerData pd = Util.getPlayerData(inpData);
                // 输入数据错误
                if(pd == null){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                // 如果已被封禁
                if(pd.Ban == Type.BAN){
                    sender.sendMessage(plugin.getConfig().getString("message.command.ban-exist", "").replace("%player%", pd.Name));
                    return true;
                }

                // ban
                pd.Ban = Type.BAN;
                pd.save();

                // 踢出玩家
                Player player = Bukkit.getPlayer(pd.Name);
                if(plugin.getConfig().getBoolean("whitelist.kick-on-ban") && player != null){
                    player.kickPlayer(plugin.getConfig().getString("message.join.ban", "").replace("%player%", player.getName()));
                }
                sender.sendMessage(plugin.getConfig().getString("message.command.ban", "").replace("%player%", inpData));
                return true;
            }

            // 解封一个账户
            case "unban" -> {
                if(!sender.hasPermission("IpacWhitelist.command.unban")){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-permission", ""));
                    return true;
                }

                if(args.length != 2){
                    sender.sendMessage("/wl unban <playerName|playerUUID>");
                    return true;
                }

                // 获取玩家数据
                String inpData = ifIsUUID32toUUID36(args[1]);
                PlayerData pd = Util.getPlayerData(inpData);
                // 输入数据错误
                if(pd == null){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                // 检查这个玩家是否存在
                if(pd.isNull()){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-note-exist", "").replace("%player%", inpData));
                    return true;
                }

                // 如果账号没有被封
                if(pd.Ban == Type.NOT_BAN){
                    sender.sendMessage(plugin.getConfig().getString("message.command.unban-exist", "").replace("%player%", pd.Name));
                    return true;
                }

                // unban
                pd.Ban = Type.NOT_BAN;
                pd.save();

                sender.sendMessage(plugin.getConfig().getString("message.command.unban", "").replace("%player%", inpData));
                return true;
            }

            // 显示一个玩家的数据
            case "info" -> {
                if(!sender.hasPermission("IpacWhitelist.command.info")){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-permission", ""));
                    return true;
                }

                if(args.length != 2){
                    sender.sendMessage("/wl info <playerName|playerUUID>");
                    return true;
                }

                // 获取玩家数据
                String inpData = ifIsUUID32toUUID36(args[1]);
                PlayerData pd = Util.getPlayerData(inpData);
                // 输入数据错误
                if(pd == null){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                sender.sendMessage(plugin.getConfig().getString("message.command.info", "")
                        .replace("%player%", inpData)
                        .replace("%ID%", String.valueOf(pd.ID))
                        .replace("%Type%", pd.Type.getName())
                        .replace("%Ban%", pd.Ban.getName())
                        .replace("%UUID%", pd.UUID)
                        .replace("%Name%", pd.Name)
                        .replace("%Time%", String.valueOf(pd.Time)));

                return true;
            }

            // 列出匹配的玩家的数据
            case "list" -> {
                if(!sender.hasPermission("IpacWhitelist.command.list")){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-permission", ""));
                    return true;
                }

                if (args.length != 3) {
                    sender.sendMessage("/wl list <NOT|VISIT|WHITE|BLACK|VISIT_CONVERT|VISIT_BLACK|*> <num|ALL>");
                    return true;
                }

                // 获取选择的 Type
                String inp1 = args[1].toUpperCase();
                if(inp1.equals("*")){inp1 = "ALL";}
                if(!Pattern.compile("^(?:NOT|VISIT|WHITE|BLACK|VISIT_CONVERT|VISIT_BLACK|ALL)$").matcher(inp1).matches()){
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

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> whileDataForList(type, maxLine, (pd) -> {
                    i.getAndIncrement();
                    sender.sendMessage(plugin.getConfig().getString("message.command.list", "")
                            .replace("%num%", i.toString())
                            .replace("%ID%", String.valueOf(pd.ID))
                            .replace("%Type%", pd.Type.getName())
                            .replace("%Ban%", pd.Ban.getName())
                            .replace("%UUID%", pd.UUID)
                            .replace("%Name%", pd.Name)
                            .replace("%Time%", String.valueOf(pd.Time)));
                }));
                executor.shutdown();

                return true;
            }


            // 清理玩家数据
            case "clean" -> {
                if(!sender.hasPermission("IpacWhitelist.command.clean")){
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-permission", ""));
                    return true;
                }

                if(args.length < 2){
                    sender.sendMessage("/wl clean <VISIT|NOT> | /wl clean PLAYER <playerName|playerUUID>");
                    return true;
                }

                String args_1 = args[1].toUpperCase();

                // 子命令
                switch(args_1){
                    default -> {
                        sender.sendMessage("/wl clean <VISIT|NOT>");
                        return true;
                    }

                    // 删除指定类型的玩家数据
                    case "VISIT", "NOT" -> {

                        if(args.length != 2){
                            sender.sendMessage("/wl clean <VISIT|NOT>");
                            return true;
                        }

                        sender.sendMessage(plugin.getConfig().getString("message.command.clean", ""));

                        // 是否禁用参观账户
                        if(!plugin.getConfig().getBoolean("dev.deletePlayerDataAll.deletingLockPlayer", true)){
                            onVisitPlayerJoin.disabledVisit = true;
                        }

                        // 获取参观账户过期的时间位置
                        long visitExpiredTime = getTime() - plugin.getConfig().getLong("dev.deletePlayerDataAll.deleteDataTimeout", 43200000);

                        Type type = Type.valueOf(args_1);

                        // 匹配 达到可删除时间且没有被封禁的参观账户
                        PreparedStatement sql;
                        try {
                            sql = SQL.connection.prepareStatement("SELECT * FROM `player` WHERE `Type` = ? AND `Ban` = ? AND `UUID` != '' AND `Time` < ?;");
                            sql.setInt(1, type.getID());
                            sql.setInt(2, Type.NOT_BAN.getID());
                            sql.setLong(3, visitExpiredTime);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                        // 遍历所有数据
                        AtomicInteger i = new AtomicInteger();

                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            whileDataForList(sql, (pd) -> {
                                // 跳过数据不完整和玩家在线
                                if(pd.UUID.isEmpty() || pd.Name.isEmpty() || Bukkit.getPlayer(pd.Name) != null){
                                    return;
                                }

                                getLogger().info("[IpacWhitelist.clean] <<< ----------");

                                // 锁定这个参观账户
                                onVisitPlayerJoin.cleanVisitList.add(pd.UUID);

                                // 开始清理数据 //
                                i.getAndIncrement();
                                // 删除文件和运行指令
                                deletePlayerDataAll(plugin, i, pd);
                                // 删除账户
                                pd.Type = Type.NOT;
                                pd.save();

                                // 清理结束, 等待指定时间
                                try {
                                    TimeUnit.MILLISECONDS.sleep(plugin.getConfig().getInt("dev.deletePlayerDataAll.intervalTime", 100));
                                } catch (InterruptedException ignored) {}

                                // 取消锁定
                                onVisitPlayerJoin.cleanVisitList.remove(pd.UUID);

                            });

                            // 恢复参观账户
                            if (!plugin.getConfig().getBoolean("dev.deletePlayerDataAll.deletingLockPlayer", true)) {
                                onVisitPlayerJoin.disabledVisit = false;
                            }

                            getLogger().info("[IpacWhitelist.clean] <<< ----------");
                            sender.sendMessage(plugin.getConfig().getString("message.command.clean-ok", "")
                                    .replace("%num%", i.toString()));
                        });
                        executor.shutdown();
                    } // <- VISIT.. END

                    // 删除任意玩家数据
                    case "PLAYER" -> {

                        if(args.length != 3){
                            sender.sendMessage("/wl clean PLAYER <playerName|playerUUID>");
                            return true;
                        }

                        // 获取账户数据
                        String inpData = ifIsUUID32toUUID36(args[2]);
                        PlayerData pd = Util.getPlayerData(inpData);
                        // 输入数据错误
                        if(pd == null){
                            sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                            return true;
                        }

                        // 是否存在
                        if(pd.isNull() || pd.Type == Type.NOT){
                            sender.sendMessage(plugin.getConfig().getString("message.command.err-note-exist", "")
                                    .replace("%player%", inpData));
                            return true;
                        }

                        // 是否被封禁
                        if(pd.Ban == Type.BAN){
                            sender.sendMessage(plugin.getConfig().getString("message.command.err-ban", ""));
                            return true;
                        }

                        // 数据是否完整
                        if(pd.UUID.isEmpty() || pd.Name.isEmpty()){
                            sender.sendMessage(plugin.getConfig().getString("message.command.err-clean-incomplete", "")
                                    .replace("%player%", inpData));
                            return true;
                        }

                        // 玩家在线
                        if(Bukkit.getPlayer(pd.Name) != null){
                            sender.sendMessage(plugin.getConfig().getString("message.command.err-clean-online", ""));
                            return true;
                        }

                        // 未达到可以删除的时间
                        long visitExpiredTime = getTime() - plugin.getConfig().getLong("dev.deletePlayerDataAll.deleteDataTimeout", 43200000);
                        if(pd.Time > visitExpiredTime){
                            sender.sendMessage(plugin.getConfig().getString("message.command.err-clean-deleteDataTimeout", "")
                                    .replace("%player%", inpData));
                            return true;
                        }

                        // 删除数据
                        sender.sendMessage(plugin.getConfig().getString("message.command.clean", ""));

                        // 是否全局禁用连接
                        if(!plugin.getConfig().getBoolean("dev.deletePlayerDataAll.deletingLockPlayer", true)){
                            onPlayerJoin.disabled = true;
                        }
                        // 锁定这个账户
                        onPlayerJoin.cleanList.add(pd.UUID);

                        CompletableFuture.runAsync(() -> {
                            getLogger().info("[IpacWhitelist.clean] <<< ----------");
                            // 删除文件和运行指令
                            deletePlayerDataAll(plugin, new AtomicInteger(), pd);
                            // 删除账户
                            pd.Type = Type.NOT;
                            pd.save();

                            // 清理结束, 等待指定时间
                            try {
                                TimeUnit.MILLISECONDS.sleep(plugin.getConfig().getInt("dev.deletePlayerDataAll.intervalTime", 100));
                            } catch (InterruptedException ignored) {}

                            // 取消锁定
                            onPlayerJoin.cleanList.remove(pd.UUID);
                            if(!plugin.getConfig().getBoolean("dev.deletePlayerDataAll.deletingLockPlayer", true)){
                                onPlayerJoin.disabled = false;
                            }

                            getLogger().info("[IpacWhitelist.clean] <<< ----------");
                            sender.sendMessage(plugin.getConfig().getString("message.command.clean-ok", "")
                                    .replace("%num%", "1"));
                        });
                    } // <- PLAYER END
                }
                return true;
            }

            case "cat" -> {
                sender.sendMessage("§6喵§a喵§b喵§f~");
                return true;
            }
        }

        sender.sendMessage("/wl ... "+ String.join(", ", commandList));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, String[] args) {

        // 返回子命令列表
        if(args.length == 1){
            return commandList;
        }

        // 根据子命令返回不同数据
        switch(args[0].toLowerCase()){

            case "list" -> {
                if(args.length == 2){
                    return List.of(
                            "NOT",
                            "VISIT",
                            "WHITE",
                            "BLACK",
                            "VISIT_CONVERT",
                            "VISIT_BLACK",
                            "*"
                    );

                }else if(args.length == 3){
                    return List.of("5", "10", "ALL");
                }
            }

            case "clean" -> {
                if(args.length == 2){
                    return List.of(
                            "VISIT",
                            "NOT",
                            "PLAYER"
                    );
                }
            }
        }
        return null;
    }

}
