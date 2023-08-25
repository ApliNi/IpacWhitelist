package aplini.ipacwhitelist.Listener;

import aplini.ipacwhitelist.IpacWhitelist;
import fr.xephi.authme.events.FailedLoginEvent;
import fr.xephi.authme.events.LoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PlayerJoinMessage implements Listener {
    private static IpacWhitelist plugin;
    private static final List<UUID> joinLock = new ArrayList<>();
    private static final List<UUID> quitLock = new ArrayList<>();

    public PlayerJoinMessage(IpacWhitelist plugin){
        PlayerJoinMessage.plugin = plugin;
    }

    // 待办: 实现输出玩家断开连接的原因, 比如 连接超时或主动断开...
    // 想法: 捕获控制台输出, 匹配断开连接的消息


    // 加入和退出事件处理
    public static void playerJoinMessage(String cp, Player player, boolean isJoin){
        // 事件是否禁用
        if(plugin.getConfig().getString(cp + ".message", "").isEmpty()) {
            return;
        }
        List<UUID> Lock = isJoin ? joinLock : quitLock;
        // 是否需要 Lock
        UUID playerUUID = player.getUniqueId();
        if(plugin.getConfig().getBoolean(cp +".terminate", true)){
            // 是否被添加到 Lock
            if(Lock.contains(playerUUID)){
                return;
            }
            Lock.add(playerUUID);
        }

        // 广播消息
        Bukkit.getServer().broadcastMessage(
                plugin.getConfig().getString(cp +".message", "")
                        .replace("%player%", player.getName()));

        // 如果是加入, 则立即释放退出的lock, 反之亦然
        if(isJoin){
            quitLock.remove(playerUUID);
        }else{
            joinLock.remove(playerUUID);

            // 我们可以一直持有加入的lock, 直到玩家退出后再慢慢处理. 因为退出事件在瞬间完成, 而加入事件会有几秒的延迟
            // 异步等待并释放锁
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(plugin.getConfig().getInt("playerJoinMessage.quitLockFreedTime", 2000));
                } catch (InterruptedException ignored) {}
                Lock.remove(playerUUID);
            });
            executor.shutdown();
        }
    }


    // ------ //
    // ------ //


    // 参观账户登录服务器
    public static void onVisitPlayerJoin(Player player) {
        playerJoinMessage("playerJoinMessage.playerJoin.onVisitPlayerJoin", player, true);
    }

    // AuthMe 玩家登录事件
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAuthMeLoginEvent(LoginEvent event) {
        playerJoinMessage("playerJoinMessage.playerJoin.onAuthMeLoginEvent", event.getPlayer(), true);
    }

    // 玩家加入事件
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        playerJoinMessage("playerJoinMessage.playerJoin.onPlayerJoinEvent", event.getPlayer(), true);
    }


    // ------ //
    // ------ //


    // AuthMe 玩家输入错误密码
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAuthMeFailedLoginEvent(FailedLoginEvent event) {
        playerJoinMessage("playerJoinMessage.playerQuit.onAuthMeFailedLoginEvent", event.getPlayer(), false);
    }

    // 玩家退出事件
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        playerJoinMessage("playerJoinMessage.playerQuit.onPlayerQuitEvent", event.getPlayer(), false);
    }

}
