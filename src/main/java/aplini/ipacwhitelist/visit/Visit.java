package aplini.ipacwhitelist.visit;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.util.SQL;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static aplini.ipacwhitelist.util.Type.*;
import static aplini.ipacwhitelist.visit.EventFunc.startAsyncEventFunc;
import static org.bukkit.Bukkit.getLogger;

public class Visit implements Listener {
    private static IpacWhitelist plugin;
    // AuthmeAPI
    private static AuthMeApi AuthmeAPI;
    // 当前在线的参观账户uuid列表
    private static final List<UUID> visitList = new ArrayList<>();
    public Visit(IpacWhitelist plugin){
        Visit.plugin = plugin;
        if(plugin.getConfig().getBoolean("visit.auto-register-AuthMe")){
            Visit.AuthmeAPI = AuthMeApi.getInstance();
        }
    }

    // 新参观账户第一次登录服务器
    public static void onNewVisitPlayerLoginEvent(PlayerLoginEvent event) {
        if(ifForbiddenJoin(event)){return;}

        // 将这个玩家以参观账户的身份添加到数据库中
        SQL.addPlayer(event.getPlayer(), VISIT);
        getLogger().info("[IpacWhitelist] 为新的参观账户创建数据: %s ".formatted(event.getPlayer().getName()));

        // 自动注册
        if(plugin.getConfig().getBoolean("visit.auto-register-AuthMe")){
            // 如果已注册则跳过
            if(!AuthmeAPI.isRegistered(event.getPlayer().getName())){
                AuthmeAPI.forceRegister(event.getPlayer(), plugin.getConfig().getString("visit.auto-register-AuthMe-password"));
            }
        }

        // 参观账户事件程序
        startAsyncEventFunc("onNewVisitPlayerLoginEvent", plugin, event.getPlayer());

        // 触发 参观账户加入服务器 事件
        onVisitPlayerLoginEvent(event);
    }

    // 参观账户登录服务器
    public static void onVisitPlayerLoginEvent(PlayerLoginEvent event) {
        if(ifForbiddenJoin(event)){return;}

        UUID playerUUID = event.getPlayer().getUniqueId();

        // 添加到 visitList
        visitList.add(playerUUID);

        // 自动登录
        if(plugin.getConfig().getBoolean("visit.auto-login-AuthMe")){
            AuthmeAPI.forceLogin(event.getPlayer());
        }

        // 参观账户事件程序
        startAsyncEventFunc("onVisitPlayerLoginEvent", plugin, event.getPlayer());

        getLogger().info("[IpacWhitelist] %s 以参观模式加入服务器".formatted(event.getPlayer().getName()));
    }

    // 参观账户登录服务器时进行验证
    public static boolean ifForbiddenJoin(PlayerLoginEvent event) {
        // 限定主机名
        if(plugin.getConfig().getBoolean("visit.limit-hostname.enable")){
            String Hostname = event.getHostname();
            if(!plugin.getConfig().getStringList("visit.limit-hostname.list").contains(Hostname)){
                getLogger().info("[IpacWhitelist] %s 参观账户未使用专用地址: %s".formatted(event.getPlayer().getName(), Hostname));
                event.setKickMessage(plugin.getConfig().getString("message.join.not", "").replace("%player%", event.getPlayer().getName()));
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

    @EventHandler // 玩家加入
    public void onVisitPlayerJoinEvent(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        // 是参观账户
        if(visitList.contains(playerUUID)){
            // 参观账户事件程序
            startAsyncEventFunc("onVisitPlayerJoinEvent", plugin, event.getPlayer());
        }
    }

    @EventHandler // 玩家退出
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
