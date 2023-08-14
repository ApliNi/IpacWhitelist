package aplini.ipacwhitelist.Listener;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.util.SQL_io;
import aplini.ipacwhitelist.util.Type;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.regex.Pattern;

import static aplini.ipacwhitelist.IpacWhitelist.allowJoin;
import static aplini.ipacwhitelist.util.Type.NOT;
import static org.bukkit.Bukkit.getLogger;

public class onPlayerJoin implements Listener {
    private static IpacWhitelist plugin;
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
        Type state = SQL_io.isWhitelisted(event.getPlayer());
        switch(state){

            case WHITE -> // 白名单
                    event.setResult(PlayerLoginEvent.Result.ALLOWED);

            // 不存在/已移出白名单, 参观账户
            case NOT, VISIT -> { // 不在白名单中
                // 是否启用参观账户
                if(plugin.getConfig().getBoolean("visit.enable", false)){
                    // 如果是新账户, 则需要运行 onNewVisitPlayerLoginEvent
                    if(state == NOT){
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

            case WHITE_EXPIRED -> { // 白名单已过期
                getLogger().info("[IpacWhitelist] %s 白名单已过期".formatted(event.getPlayer().getName()));
                event.setKickMessage(plugin.getConfig().getString("message.join.expired", "").replace("%player%", event.getPlayer().getName()));
                event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
            }

            case BLACK -> { // 黑名单
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
}