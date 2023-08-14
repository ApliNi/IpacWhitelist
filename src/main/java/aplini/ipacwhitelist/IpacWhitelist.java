package aplini.ipacwhitelist;

import aplini.ipacwhitelist.Listener.CommandHandler;
import aplini.ipacwhitelist.Listener.onPlayerJoin;
import aplini.ipacwhitelist.util.SQL;
import aplini.ipacwhitelist.Listener.onVisitPlayerJoin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IpacWhitelist extends JavaPlugin implements Listener {
    private static IpacWhitelist plugin;
    public static boolean allowJoin = false;

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
        getServer().getPluginManager().registerEvents(new onPlayerJoin(this), this);
        Objects.requireNonNull(Bukkit.getPluginCommand("wl")).setExecutor(new CommandHandler(this));

        if(plugin.getConfig().getBoolean("visit.enable", false)){
            getServer().getPluginManager().registerEvents(new onVisitPlayerJoin(this), this);
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

            getLogger().info("启动等待结束");
            // 允许加入
            allowJoin = true;
        });
        executor.shutdown();
    }


    public static IpacWhitelist getPlugin() {
        return plugin;
    }
}
