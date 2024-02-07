package aplini.ipacwhitelist;

import aplini.ipacwhitelist.hook.hookAuthMe;
import aplini.ipacwhitelist.listener.commandHandler;
import aplini.ipacwhitelist.listener.onPlayerLogin;
import aplini.ipacwhitelist.utils.Metrics;
import aplini.ipacwhitelist.utils.sql;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class IpacWhitelist extends JavaPlugin implements Listener {
    public static Server server = Bukkit.getServer();
    public static IpacWhitelist plugin;
    public static FileConfiguration config;
    public static boolean allowJoin = false;

    @Override // 插件加载
    public void onLoad() {
        loadConfig();
    }

    // 加载配置文件
    public void loadConfig(){
        plugin = this;
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();
        sql.reconnect();
        sql.initialize();
    }

    // 插件启动
    public void onEnable(){
        // 连接到插件
        if(config.getBoolean("hook.AuthMe", true)){
            getServer().getPluginManager().registerEvents(new hookAuthMe(), plugin);
        }

        // 注册指令
        Objects.requireNonNull(getCommand("wl")).setExecutor(new commandHandler());

        // 注册监听器
        getServer().getPluginManager().registerEvents(this, plugin);
        getServer().getPluginManager().registerEvents(new onPlayerLogin(), plugin);

        // 统计
        Metrics metrics = new Metrics(this, 20926);
    }

    @EventHandler // 服务器启动完成
    public void onServerLoad(ServerLoadEvent event){
        // 异步
        CompletableFuture.runAsync(() -> {
            // 等待时间
            try {
                TimeUnit.MILLISECONDS.sleep(config.getInt("whitelist.lateJoinTime", 4000));
            } catch (InterruptedException ignored) {}

            plugin.getLogger().info("启动等待结束");
            // 允许加入
            allowJoin = true;
        });
    }
}
