package aplini.ipacwhitelist;

import aplini.ipacwhitelist.util.SQL;
import aplini.ipacwhitelist.util.wlType;
import aplini.ipacwhitelist.visit.Visit;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static aplini.ipacwhitelist.util.SQL.getVisitPlayerUUIDFromName;
import static org.bukkit.Bukkit.getLogger;

public class IpacWhitelist extends JavaPlugin implements Listener {
    private static IpacWhitelist plugin;
    boolean allowJoin = false;

    @Override // 插件加载
    public void onLoad() {
        plugin = this;
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        plugin.getConfig();

        // 连接数据库
        SQL.connection();
        // 初始化数据库
        SQL.initialize();
    }

    // 插件启动
    public void onEnable() {
        // 注册监听器
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(plugin.getCommand("wl")).setExecutor(this);

        // 参观账户
        if(plugin.getConfig().getBoolean("visit.enable", false)){
            // 注册监听器
            getServer().getPluginManager().registerEvents(new Visit(this), this);
        }
    }

    // 插件禁用
    public void onDisable() {
        // 关闭数据库连接
        SQL.closeConnection();
    }


    @EventHandler // 服务器启动完成
    public void onServerLoad(ServerLoadEvent event) {
        // 异步
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            // 等待时间
            try {
                TimeUnit.MILLISECONDS.sleep(plugin.getConfig().getInt("whitelist.late-join-time", 4000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            getLogger().info("[IpacWhitelist] 启动等待结束");
            // 允许加入
            allowJoin = true;
        });
        executor.shutdown();
    }


    @EventHandler // 玩家加入
    public void onPlayerLogin(PlayerLoginEvent event) {

        // 服务器启动等待
        if(!allowJoin){
            event.setKickMessage(plugin.getConfig().getString("message.join.starting", ""));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        // ip 黑名单
        // 原始ip格式: ipv4: /127.0.0.1, ipv6: /0:0:0:0:0:0:0:1 没有方括号
        boolean inBlacklist = false;
        String playerIP = event.getRealAddress().toString();
        playerIP = playerIP.substring(playerIP.lastIndexOf("/") +1);
        for(String li : plugin.getConfig().getStringList("whitelist.ip-blacklist")){
            if(Pattern.matches(li, playerIP)){
                inBlacklist = true;
                break;
            }
        }
        if(inBlacklist){
            getLogger().info("[IpacWhitelist] %s 在IP黑名单中: %s".formatted(event.getPlayer().getName(), playerIP));
            event.setKickMessage(plugin.getConfig().getString("message.join.black", "").replace("%player%", event.getPlayer().getName()));
            event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
            return;
        }

        // 白名单逻辑
        switch(SQL.isWhitelisted(event.getPlayer())){

            case NOT -> { // 不在白名单中
                // 是否启用参观账户
                if(plugin.getConfig().getBoolean("visit.enable", false)){
                    Visit.onNewVisitPlayerLoginEvent(event);
                }else{
                    getLogger().info("[IpacWhitelist] %s 不在白名单中".formatted(event.getPlayer().getName()));
                    event.setKickMessage(plugin.getConfig().getString("message.join.not", "").replace("%player%", event.getPlayer().getName()));
                    event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
                }
            }

            case BLACK -> { // 黑名单
                getLogger().info("[IpacWhitelist] %s 在黑名单中".formatted(event.getPlayer().getName()));
                event.setKickMessage(plugin.getConfig().getString("message.join.black", "").replace("%player%", event.getPlayer().getName()));
                event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
            }

            case VISIT_DEL_DATA -> { // 已删除数据的参观账户
                Visit.onNewVisitPlayerLoginEvent(event);
            }

            case VISIT -> { // 参观账户
                Visit.onVisitPlayerLoginEvent(event);
            }

            case ERROR -> { // 内部错误
                getLogger().warning("[IpacWhitelist] %s 触发内部错误".formatted(event.getPlayer().getName()));
                event.setKickMessage(plugin.getConfig().getString("message.join.err", ""));
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            }
        }

        // 允许玩家登录
        event.setResult(PlayerLoginEvent.Result.ALLOWED);
    }


    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        switch(args[0]){
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(plugin.getConfig().getString("message.command.reload", ""));
            }

            // 重新连接数据库
            case "reconnect_database" -> {
                allowJoin = false;
                SQL.closeConnection();
                SQL.connection();
                SQL.initialize();
                allowJoin = true;
                sender.sendMessage(plugin.getConfig().getString("message.command.reconnect-database", ""));
            }

            // 添加一个账户
            case "add" -> {
                if(args.length == 2){
                    // 检查参数
                    if(args[1].length() > 16){
                        sender.sendMessage(plugin.getConfig().getString("message.command.err-name-length", ""));
                        return true;
                    }

                    // VISIT=是参观账户, 重置数据. DEFAULT=是普通账户, 重置数据. NOT=是新账户, 创建数据
                    switch(SQL.addPlayer(args[1])){
                        case VISIT -> {
                            // 运行 wl-add
                            String UUID = getVisitPlayerUUIDFromName(args[1]);
                            for(String li : plugin.getConfig().getStringList("visit.wl-add.command")){
                                Bukkit.getScheduler().callSyncMethod(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                        li
                                                .replace("%playerName%", args[1])
                                                .replace("%playerUUID%", UUID)));
                            }
                            // DEFAULT ->
                            sender.sendMessage(plugin.getConfig().getString("message.command.add-reset", "")
                                    .replace("%player%", args[1]));
                        }
                        case DEFAULT ->
                                sender.sendMessage(plugin.getConfig().getString("message.command.add-reset", "")
                                        .replace("%player%", args[1]));
                        case NOT ->
                                sender.sendMessage(plugin.getConfig().getString("message.command.add", "")
                                        .replace("%player%", args[1]));
                        default ->
                                sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
                    }
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

                    // VISIT=是参观账户, 重置数据. DEFAULT=是普通账户, 重置数据. NOT=是新账户, 创建数据
                    switch(SQL.addPlayer(args[1], args[2])){
                        case VISIT -> {
                            // 运行 wl-add
                            for(String li : plugin.getConfig().getStringList("visit.wl-add.command")){
                                Bukkit.getScheduler().callSyncMethod(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                        li
                                                .replace("%playerName%", args[1])
                                                .replace("%playerUUID%", args[2])));
                            }
                            // DEFAULT ->
                            sender.sendMessage(plugin.getConfig().getString("message.command.add-reset", "")
                                    .replace("%player%", args[1]));
                        }
                        case DEFAULT ->
                                sender.sendMessage(plugin.getConfig().getString("message.command.add-reset", "")
                                        .replace("%player%", args[1]));
                        case NOT ->
                                sender.sendMessage(plugin.getConfig().getString("message.command.add", "")
                                        .replace("%player%", args[1]));
                        default ->
                                sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
                    }
                }else{
                    sender.sendMessage("/wl add <Name> [UUID]");
                }
                return true;
            }

            // 删除一个账户
            case "del", "unban" -> {
                if(args.length != 2){
                    sender.sendMessage("/wl del <Name|UUID>");
                    return true;
                }

                wlType b;

                // uuid
                if(args[1].length() == 36){
                    b = SQL.delPlayerUUID(args[1]);
                }
                // name
                else if(args[1].length() <= 16){
                    b = SQL.delPlayerName(args[1]);
                }

                else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                if(b != wlType.ERROR){
                    sender.sendMessage(plugin.getConfig().getString("message.command.del", "").replace("%player%", args[1]));
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

                wlType b;

                // uuid
                if(args[1].length() == 36){
                    b = SQL.banPlayerUUID(args[1]);
                }
                // name
                else if(args[1].length() <= 16){
                    b = SQL.banPlayerName(args[1]);
                }

                else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err-length", ""));
                    return true;
                }

                if(b != wlType.ERROR){
                    sender.sendMessage(plugin.getConfig().getString("message.command.ban", "").replace("%player%", args[1]));
                }else{
                    sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
                }

                return true;
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(args.length == 1){
            List<String> list = new ArrayList<>();
            list.add("reload");
            list.add("add");
            list.add("del");
            list.add("ban");
            list.add("unban");
            list.add("reconnect_database");
            return list;
        }
        return null;
    }

    public static IpacWhitelist getPlugin() {
        return plugin;
    }
}
