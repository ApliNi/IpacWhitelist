package aplini.ipacwhitelist.Listener;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.util.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static aplini.ipacwhitelist.hook.hookAuthMe.autoLogin;
import static aplini.ipacwhitelist.hook.hookAuthMe.autoRegister;
import static aplini.ipacwhitelist.util.EventFunc.startAsyncEventFunc;
import static aplini.ipacwhitelist.util.Type.VISIT;
import static org.bukkit.Bukkit.getLogger;

public class onVisitPlayerJoin implements Listener {
    private static IpacWhitelist plugin;
    // 当前在线的参观账户uuid列表
    static final List<UUID> visitList = new ArrayList<>();

    public onVisitPlayerJoin(IpacWhitelist plugin){
        onVisitPlayerJoin.plugin = plugin;
    }

    // 参观账户第一次登录服务器
    public static void onNewVisitPlayerLoginEvent(PlayerLoginEvent event, PlayerData pd) {
        if(ifForbiddenJoin(event)){return;}

        // 创建参观账户
        pd.setPlayerInfo(event.getPlayer());
        pd.Type = VISIT;
        pd.Time = -3;
        pd.save();
        getLogger().info("[IpacWhitelist] 为新的参观账户创建数据: %s ".formatted(event.getPlayer().getName()));

        // 参观账户事件程序
        startAsyncEventFunc("onNewVisitPlayerLoginEvent", plugin, event.getPlayer());

        // 自动注册
        if(plugin.getConfig().getBoolean("visit.AuthMe.autoRegister")){
            autoRegister(event.getPlayer(), plugin.getConfig().getString("visit.AuthMe.autoRegisterPassword"));
        }

        // 触发 参观账户加入服务器 事件
        onVisitPlayerLoginEvent(event);
    }

    // 参观账户登录服务器
    public static void onVisitPlayerLoginEvent(PlayerLoginEvent event) {
        if(ifForbiddenJoin(event)){return;}

        UUID playerUUID = event.getPlayer().getUniqueId();

        // 添加到 visitList
        visitList.add(playerUUID);

        // 参观账户事件程序
        startAsyncEventFunc("onVisitPlayerLoginEvent", plugin, event.getPlayer());

        // 自动登录
        if(plugin.getConfig().getBoolean("visit.AuthMe.autoLogin")){
            autoLogin(event.getPlayer());
        }

        getLogger().info("[IpacWhitelist] %s 以参观模式加入服务器".formatted(event.getPlayer().getName()));
    }

    // 参观账户登录服务器时进行验证
    public static boolean ifForbiddenJoin(PlayerLoginEvent event) {
        // 限定主机名
        if(plugin.getConfig().getBoolean("visit.limit-hostname.enable")){
            String Hostname = event.getHostname();
            if(!plugin.getConfig().getStringList("visit.limit-hostname.list").contains(Hostname)){
                getLogger().info("[IpacWhitelist] %s 参观账户使用非法主机名: %s".formatted(event.getPlayer().getName(), Hostname));
                event.setKickMessage(plugin.getConfig().getString("message.visit.illegal-hostname", "")
                        .replace("%player%", event.getPlayer().getName()));
                event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
                return true;
            }
        }

        // 参观账户队列已满
        if(visitList.size() == plugin.getConfig().getInt("visit.max-visit-player")){
            event.setKickMessage(plugin.getConfig().getString("message.visit.full", ""));
            event.setResult(PlayerLoginEvent.Result.KICK_FULL);
            return true;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST) // 玩家加入
    public void onVisitPlayerJoinEvent(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        // 是参观账户
        if(visitList.contains(playerUUID)){
            // 运行 玩家加入消息显示 :: 参观账户加入服务器
            if(plugin.getConfig().getBoolean("playerJoinMessage.enable")){
                PlayerJoinMessage.onVisitPlayerJoin(event.getPlayer());
            }
            // 参观账户事件程序
            startAsyncEventFunc("onVisitPlayerJoinEvent", plugin, event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // 玩家退出
    public void onVisitPlayerQuitEvent(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        // 是参观账户
        if(visitList.contains(playerUUID)){
            // 参观账户事件程序
            startAsyncEventFunc("onVisitPlayerQuitEvent", plugin, event.getPlayer());

            getLogger().info("[IpacWhitelist] %s 以参观模式离开服务器".formatted(event.getPlayer().getName()));

            // 移出 visitList
            visitList.remove(playerUUID);
        }
    }
}
