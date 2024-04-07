package aplini.ipacwhitelist.hook;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.FailedLoginEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.LogoutEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.func.eventFunc.runEventFunc;
import static aplini.ipacwhitelist.listener.onPlayerLogin.playerList;

public class authMe implements Listener {
    private static AuthMeApi AuthmeAPI = null;
    // 加载 API
    public authMe(){
        AuthmeAPI = AuthMeApi.getInstance();
    }

    // 注册这个名称, true = 注册完成, false = 无需注册
    public static boolean registeredPlayerName(String playerName){
        // 此方法会自动检查是否已注册
        return AuthmeAPI.registerPlayer(playerName, config.getString("whitelist.VISIT.AuthMePlugin.autoRegisterPassword", ""));
    }

    // 强制登录账户
    public static void forceLoginPlayer(Player player){
        AuthmeAPI.forceLogin(player);
    }

    // AuthMe 玩家登录成功事件
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAuthMeLoginEvent(LoginEvent event) {
        Player player = event.getPlayer();
        // 只处理白名单玩家产生的事件
        if(playerList.contains(player.getUniqueId().toString())){
            runEventFunc("whitelist.WHITE.onAuthMeLoginEvent", player);
        }
    }

    // AuthMe 玩家输入错误密码
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAuthMeFailedLoginEvent(FailedLoginEvent event) {
        Player player = event.getPlayer();
        if(playerList.contains(player.getUniqueId().toString())){
            runEventFunc("whitelist.WHITE.onAuthMeFailedLoginEvent", player);
        }
    }

    // AuthMe 玩家注销
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogoutEvent(LogoutEvent event) {
        Player player = event.getPlayer();
        if(playerList.contains(player.getUniqueId().toString())){
            runEventFunc("whitelist.WHITE.onAuthMeLogoutEvent", player);
        }
    }
}
