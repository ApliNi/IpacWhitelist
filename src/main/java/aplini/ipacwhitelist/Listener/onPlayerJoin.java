package aplini.ipacwhitelist.Listener;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.util.PlayerData;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static aplini.ipacwhitelist.Func.EventFunc.startVisitConvertFunc;
import static aplini.ipacwhitelist.IpacWhitelist.allowJoin;
import static org.bukkit.Bukkit.getLogger;

public class onPlayerJoin implements Listener {
    private static IpacWhitelist plugin;
    private static final List<UUID> playerDisconnectList = new ArrayList<>();
    public onPlayerJoin(IpacWhitelist plugin){
        onPlayerJoin.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST) // 玩家登录服务器
    public void onPlayerLogin(PlayerLoginEvent event) {

        // 服务器启动等待
        if(!allowJoin){
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    plugin.getConfig().getString("message.join.starting", ""));
            return;
        }

        // 玩家 UUID
        UUID playerUUID = event.getPlayer().getUniqueId();
        // 玩家是否在断开连接的列表中
        if(playerDisconnectList.contains(playerUUID)){
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    plugin.getConfig().getString("message.join.limiter-reconnection-time", ""));
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
            getLogger().info("[IpacWhitelist] %s 在IP黑名单中: %s".formatted(playerName, playerIP));
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                    plugin.getConfig().getString("message.join.ban-ip", "")
                            .replace("%player%",playerName)
                            .replace("%ip%", playerIP));
            return;
        }

        // 白名单逻辑
        PlayerData pd = SQL.isInWhitelisted(event.getPlayer());
        switch(pd.__whitelistedState){

            case NOT, VISIT -> { // 不存在 / 参观账户
                // 检查用户名
                if(!Pattern.matches(plugin.getConfig().getString("whitelist.name-rule-visit", ".*"), playerName)){
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                            plugin.getConfig().getString("message.join.err-name-visit", "")
                                    .replace("%player%", playerName));
                    return;
                }
                // 是否启用参观账户
                if(plugin.getConfig().getBoolean("visit.enable", false)){
                    // 如果是新账户, 则需要运行 onNewVisitPlayerLoginEvent
                    if(pd.__whitelistedState == Type.NOT){
                        // 参观账户的数据在这里创建...
                        onVisitPlayerJoin.onNewVisitPlayerLoginEvent(event, pd);
                    }else{
                        onVisitPlayerJoin.onVisitPlayerLoginEvent(event);
                    }
                }else{
                    getLogger().info("[IpacWhitelist] %s 不在白名单中".formatted(event.getPlayer().getName()));
                    event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST,
                            plugin.getConfig().getString("message.join.not", "")
                                    .replace("%player%", playerName));
                }
            }

            case WHITE, VISIT_CONVERT -> { // 白名单 / 正在将 VISIT 转换为 WHITE
                // 检查用户名
                if(!Pattern.matches(plugin.getConfig().getString("whitelist.name-rule", ".*"), playerName)){
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                            plugin.getConfig().getString("message.join.err-name", "")
                                    .replace("%player%", playerName));
                    return;
                }
                // 如果是待转换的参观账户
                if(pd.__whitelistedState == Type.VISIT_CONVERT){
                    // 运行 wl-add-convert
                    Player player = event.getPlayer();
                    startVisitConvertFunc(plugin, player, "visit.wl-add-convert.command");
                    // 修改 Type 为 WHITE
                    pd.Type = Type.WHITE;
                    pd.save();
                }
                // 通过白名单, 允许登录
                event.allow();
            }

            case WHITE_EXPIRED -> { // 白名单已过期
                getLogger().info("[IpacWhitelist] %s 白名单已过期".formatted(playerName));
                event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST,
                        plugin.getConfig().getString("message.join.expired", "")
                                .replace("%player%", playerName));
            }

            case BAN -> { // 黑名单
                getLogger().info("[IpacWhitelist] %s 在黑名单中".formatted(playerName));
                event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                        plugin.getConfig().getString("message.join.ban", "")
                                .replace("%player%", playerName));
            }

            default -> { // 内部错误
                getLogger().warning("[IpacWhitelist] %s 触发内部错误".formatted(playerName));
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                        plugin.getConfig().getString("message.join.err", ""));
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR) // 玩家退出
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        // 更新玩家的最后连接时间
        PlayerData pd = SQL.getPlayerData(Type.DATA_UUID, playerUUID.toString());
        pd.Time = -3;
        pd.save();

        // 玩家退出后等待指定时间才能重新连接
        playerDisconnectList.add(playerUUID);
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(plugin.getConfig().getInt("whitelist.playerDisconnectToReconnectMinTime", 1000));
            } catch (InterruptedException ignored) {}
            playerDisconnectList.remove(playerUUID);
        });
    }
}
