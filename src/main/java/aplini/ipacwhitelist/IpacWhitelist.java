package aplini.ipacwhitelist;

import aplini.ipacwhitelist.util.SQL;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
            event.setKickMessage(plugin.getConfig().getString("message.late-join-time", ""));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        // 白名单逻辑
        switch(SQL.isWhitelisted(event.getPlayer())){

            case NOT -> { // 不在白名单中
                // 非白名单玩家加入服务器 -> 交给参观账户
                if(plugin.getConfig().getBoolean("visit.enable", false)){
                    Visit.onNewVisitPlayerLoginEvent(event);
                }else{
                    getLogger().warning("[IpacWhitelist] %s 不在白名单中".formatted(event.getPlayer().getName()));
                    event.setKickMessage(plugin.getConfig().getString("message.not", "").replace("%player%", event.getPlayer().getName()));
                    event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
                }
            }

            case VISIT_DEL_DATA -> { // 已删除数据的参观账户
                Visit.onNewVisitPlayerLoginEvent(event);
            }

            case VISIT -> { // 参观账户
                Visit.onVisitPlayerLoginEvent(event);
            }

            case ERROR -> { // 内部错误
                getLogger().warning("[IpacWhitelist] %s 触发内部错误".formatted(event.getPlayer().getName()));
                event.setKickMessage(plugin.getConfig().getString("message.err-sql-player-join", ""));
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
                sender.sendMessage(plugin.getConfig().getString("message.reload", ""));
            }

            // 重新连接数据库
            case "reconnect_database" -> {
                allowJoin = false;
                SQL.closeConnection();
                SQL.connection();
                SQL.initialize();
                allowJoin = true;
                sender.sendMessage(plugin.getConfig().getString("message.reconnect-database", ""));
            }

            // 添加一个账户
            case "add" -> {
                if(args.length == 2){
                    // 检查参数
                    if(args[1].length() > 16){
                        sender.sendMessage(plugin.getConfig().getString("message.err-name-length", ""));
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
                            sender.sendMessage(plugin.getConfig().getString("message.add-ok-reset", "")
                                    .replace("%player%", args[1]));
                        }
                        case DEFAULT ->
                                sender.sendMessage(plugin.getConfig().getString("message.add-ok-reset", "")
                                        .replace("%player%", args[1]));
                        case NOT ->
                                sender.sendMessage(plugin.getConfig().getString("message.add-ok", "")
                                        .replace("%player%", args[1]));
                        default ->
                                sender.sendMessage(plugin.getConfig().getString("message.err-sql", ""));
                    }
                }else if(args.length >= 3){
                    // 检查参数
                    if(args[1].length() > 16){
                        sender.sendMessage(plugin.getConfig().getString("message.err-name-length", ""));
                        return true;
                    }
                    if(args[2].length() != 36){
                        sender.sendMessage(plugin.getConfig().getString("message.err-uuid-length", ""));
                        return true;
                    }

                    // VISIT=是参观账户, 重置数据. DEFAULT=是普通账户, 重置数据. NOT=是新账户, 创建数据
                    switch(SQL.addPlayer(args[1])){
                        case VISIT -> {
                            // 运行 wl-add
                            for(String li : plugin.getConfig().getStringList("visit.wl-add.command")){
                                Bukkit.getScheduler().callSyncMethod(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                        li
                                                .replace("%playerName%", args[1])
                                                .replace("%playerUUID%", args[2])));
                            }
                            // DEFAULT ->
                            sender.sendMessage(plugin.getConfig().getString("message.add-ok-reset", "")
                                    .replace("%player%", args[1]));
                        }
                        case DEFAULT ->
                                sender.sendMessage(plugin.getConfig().getString("message.add-ok-reset", "")
                                        .replace("%player%", args[1]));
                        case NOT ->
                                sender.sendMessage(plugin.getConfig().getString("message.add-ok", "")
                                        .replace("%player%", args[1]));
                        default ->
                                sender.sendMessage(plugin.getConfig().getString("message.err-sql", ""));
                    }
                }else{
                    sender.sendMessage("/wl add <Name> [UUID]");
                }
                return true;
            }

            // 删除一个账户
            case "del" -> {
                if(args.length != 2){
                    sender.sendMessage("/wl del <Name|UUID>");
                    return true;
                }

                boolean b;

                // uuid
                if(args[1].length() == 36){
                    b = SQL.delPlayerUUID(args[1]);
                }

                // name
                else if(args[1].length() <= 16){
                    b = SQL.delPlayerName(args[1]);
                }

                else{
                    sender.sendMessage(plugin.getConfig().getString("message.err-name-length", ""));
                    return true;
                }

                if(b){
                    sender.sendMessage(plugin.getConfig().getString("message.del-ok", "").replace("%player%", args[1]));
                }else{
                    sender.sendMessage(plugin.getConfig().getString("message.err-sql", ""));
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
            list.add("reconnect_database");
            return list;
        }
        return null;
    }

    public static IpacWhitelist getPlugin() {
        return plugin;
    }
}
