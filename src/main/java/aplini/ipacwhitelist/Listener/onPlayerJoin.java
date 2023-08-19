package aplini.ipacwhitelist.Listener;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.util.SQL;
import aplini.ipacwhitelist.util.Type;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static aplini.ipacwhitelist.IpacWhitelist.allowJoin;
import static aplini.ipacwhitelist.util.EventFunc.startVisitConvertFunc;
import static org.bukkit.Bukkit.getLogger;

public class onPlayerJoin implements Listener {
    private static IpacWhitelist plugin;
    private static final List<UUID> playerDisconnectList = new ArrayList<>();
    public onPlayerJoin(IpacWhitelist plugin){
        onPlayerJoin.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH) // 玩家加入
    public void onPlayerLogin(PlayerLoginEvent event) {

        // 服务器启动等待
        if(!allowJoin){
            event.setKickMessage(plugin.getConfig().getString("message.join.starting", ""));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        UUID playerUUID = event.getPlayer().getUniqueId();

        // 玩家是否在断开连接的列表中
        if(playerDisconnectList.contains(playerUUID)){
            event.setKickMessage(plugin.getConfig().getString("message.join.limiter-reconnection-time", ""));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        // 玩家名称
        String playerName = event.getPlayer().getName();
        // 玩家IP地址
        String playerIP = event.getRealAddress().toString();
        playerIP = playerIP.substring(playerIP.lastIndexOf("/") +1);

        // ip 黑名单
        // 原始ip格式: ipv4: /127.0.0.1, ipv6: /0:0:0:0:0:0:0:1 没有方括号
        boolean inBlacklist = false;
        for(String li : plugin.getConfig().getStringList("whitelist.ip-blacklist")){
            if(Pattern.matches(li, playerIP)){
                inBlacklist = true;
                break;
            }
        }
        if(inBlacklist){
            getLogger().info("[IpacWhitelist] %s 在IP黑名单中: %s".formatted(event.getPlayer().getName(), playerIP));
            event.setKickMessage(plugin.getConfig().getString("message.join.black-ip", "").replace("%player%", event.getPlayer().getName()));
            event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
            return;
        }

        // 白名单逻辑
        Type state = SQL.isWhitelisted(event.getPlayer());
        switch(state){

            case NOT, VISIT -> { // 不存在 / 参观账户
                // 检查用户名
                if(!Pattern.matches(plugin.getConfig().getString("whitelist.name-rule-visit", ".*"), playerName)){
                    event.setKickMessage(plugin.getConfig().getString("message.join.err-name", ""));
                    event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                    return;
                }
                // 是否启用参观账户
                if(plugin.getConfig().getBoolean("visit.enable", false)){
                    // 如果是新账户, 则需要运行 onNewVisitPlayerLoginEvent
                    if(state == Type.NOT){
                        onVisitPlayerJoin.onNewVisitPlayerLoginEvent(event);
                    }else{
                        onVisitPlayerJoin.onVisitPlayerLoginEvent(event);
                    }
                }else{
                    getLogger().info("[IpacWhitelist] %s 不在白名单中".formatted(event.getPlayer().getName()));
                    event.setKickMessage(plugin.getConfig().getString("message.join.not", "").replace("%player%", event.getPlayer().getName()));
                    event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
                }
            }

            case WHITE, VISIT_CONVERT -> { // 白名单 / 正在将 VISIT 转换为 WHITE
                // 检查用户名
                if(!Pattern.matches(plugin.getConfig().getString("whitelist.name-rule", ".*"), playerName)){
                    event.setKickMessage(plugin.getConfig().getString("message.join.err-name", ""));
                    event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                    return;
                }

                if(state == Type.VISIT_CONVERT){
                    // 运行 wl-add-convert
                    Player player = event.getPlayer();
                    startVisitConvertFunc(plugin, player, "visit.wl-add-convert.command");
                    // 修改 Type 为 WHITE, 同时更新时间
                    SQL.addPlayer(player, -3, Type.WHITE);
                }
//                else if(state == Type.WHITE){
//                    event.setResult(PlayerLoginEvent.Result.ALLOWED); // 可能其他插件需要拒绝玩家加入
//                }
            }

            case WHITE_EXPIRED -> { // 白名单已过期
                getLogger().info("[IpacWhitelist] %s 白名单已过期".formatted(event.getPlayer().getName()));
                event.setKickMessage(plugin.getConfig().getString("message.join.expired", "").replace("%player%", event.getPlayer().getName()));
                event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
            }

            case BLACK, VISIT_BLACK -> { // 黑名单 / 被封禁的参观账户
                getLogger().info("[IpacWhitelist] %s 在黑名单中".formatted(event.getPlayer().getName()));
                event.setKickMessage(plugin.getConfig().getString("message.join.black", "").replace("%player%", event.getPlayer().getName()));
                event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
            }

            default -> { // 内部错误
                getLogger().warning("[IpacWhitelist] %s 触发内部错误".formatted(event.getPlayer().getName()));
                event.setKickMessage(plugin.getConfig().getString("message.join.err", ""));
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR) // 玩家退出
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 保存断开连接的玩家的uuid
        UUID playerUUID = event.getPlayer().getUniqueId();
        playerDisconnectList.add(playerUUID);

        // 定时移除这个uuid
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(plugin.getConfig().getInt("whitelist.playerDisconnectToReconnectMinTime", 1500));
            } catch (InterruptedException ignored) {}
            playerDisconnectList.remove(playerUUID);
        });
        executor.shutdown();
    }
}
