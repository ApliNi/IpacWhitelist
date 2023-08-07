package aplini.ipacwhitelist;

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
    }


    // 插件启动
    public void onEnable() {
//        SQL.openConnection();

        Bukkit.getPluginManager().registerEvents(this, this);

        if(!getConfig().getString("sql.jdbc_driver", "").isEmpty()){
            try {
                Class.forName(getConfig().getString("sql.jdbc_driver"));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        SQL.initialize();

        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(plugin.getCommand("wl")).setExecutor(this);
    }


    // 插件禁用
    public void onDisable() {
//        SQL.closeConnection();
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
            // 允许加入
            allowJoin = true;

            getLogger().info("[IpacWhitelist] 启动等待结束");
        });
        executor.shutdown();
    }


    @EventHandler // 玩家加入
    public void onPlayerLogin(PlayerLoginEvent event) {

        // 白名单逻辑
        // 0 = 不在, 1 = 存在, 2 = 出错
        switch(SQL.isWhitelisted(event.getPlayer())){
            case 0 -> {
                getLogger().warning("[IpacWhitelist] "+ event.getPlayer().getName() +" 不在白名单中");
                event.setKickMessage(plugin.getConfig().getString("message.not", "").replace("%player%", event.getPlayer().getName()));
                event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
                return;
            }
            case 2 -> {
                getLogger().warning("[IpacWhitelist] "+ event.getPlayer().getName() +" 触发内部错误");
                event.setKickMessage(plugin.getConfig().getString("message.err-sql-player-join", ""));
                event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
                return;
            }
        }

        // 服务器启动等待
        if(!allowJoin){
            getLogger().info("[IpacWhitelist] "+ event.getPlayer().getName() +" 因启动等待被拒绝加入");
            event.setKickMessage(plugin.getConfig().getString("message.late-join-time", ""));
            event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
            return;
        }
    }


    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        switch (args[0]) {
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(plugin.getConfig().getString("message.reload", ""));
            }
            case "add" -> {
                if (args.length == 2) {
                    // 检查参数
                    if (args[1].length() > 16) {
                        sender.sendMessage(plugin.getConfig().getString("message.err-name-length", ""));
                        return true;
                    }

                    // 0=错误, 1=已添加, 重置白名单, 2=未添加, 创建记录
                    switch (SQL.addPlayer(args[1])) {
                        case 0 ->
                            sender.sendMessage(plugin.getConfig().getString("message.err-sql", ""));
                        case 1 ->
                                sender.sendMessage(plugin.getConfig().getString("message.add-ok-reset", "")
                                        .replace("%player%", args[1]));
                        case 2 ->
                                sender.sendMessage(plugin.getConfig().getString("message.add-ok", "")
                                        .replace("%player%", args[1]));
                    }
                    return true;
                } else if (args.length >= 3) {
                    // 检查参数
                    if (args[1].length() > 16) {
                        sender.sendMessage(plugin.getConfig().getString("message.err-name-length", ""));
                        return true;
                    }
                    if (args[2].length() != 36) {
                        sender.sendMessage(plugin.getConfig().getString("message.err-uuid-length", ""));
                        return true;
                    }

                    // 0=错误, 1=已添加, 重置白名单, 2=未添加, 创建记录
                    switch (SQL.addPlayer(args[1], args[2])) {
                        case 0 ->
                                sender.sendMessage(plugin.getConfig().getString("message.err-sql", ""));
                        case 1 ->
                                sender.sendMessage(plugin.getConfig().getString("message.add-ok-reset", "")
                                        .replace("%player%", args[1]));
                        case 2 ->
                                sender.sendMessage(plugin.getConfig().getString("message.add-ok", "")
                                        .replace("%player%", args[1]));
                    }
                    return true;
                }
                sender.sendMessage("/wl add <Name> [UUID]");
            }
            case "del" -> {
                if (args.length == 2) {
                    if (args[1].length() == 36) {
                        // uuid
                        boolean s = SQL.delPlayerUUID(args[1]);
                        if (s) {
                            sender.sendMessage(plugin.getConfig().getString("message.add-ok", "")
                                    .replace("%player%", args[1]));
                        } else {
                            sender.sendMessage(plugin.getConfig().getString("message.err-sql", ""));
                        }
                        return true;
                    } else if (args[1].length() <= 16) {
                        // name
                        boolean s = SQL.delPlayerName(args[1]);
                        if (s) {
                            sender.sendMessage(plugin.getConfig().getString("message.del-ok", "")
                                    .replace("%player%", args[1]));
                        } else {
                            sender.sendMessage(plugin.getConfig().getString("message.err-sql", ""));
                        }
                        return true;
                    } else {
                        sender.sendMessage(plugin.getConfig().getString("message.err-name-length", ""));
                        return true;
                    }
                }
                sender.sendMessage("/wl del <Name|UUID>");
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
            return list;
        }
        return null;
    }

    public static IpacWhitelist getPlugin() {
        return plugin;
    }
}
