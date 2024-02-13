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

public class hookAuthMe implements Listener {
    private static AuthMeApi AuthmeAPI = null;
    // 加载 API
    public hookAuthMe(){
        AuthmeAPI = AuthMeApi.getInstance();
    }

    // 需要传入 player 对象的 AuthMe 操作应发生在玩家加入服务器时

    // 自动注册和登录
    public static void AuthMeAutoRegisteredAndLogin(Player player){
        if(AuthmeAPI == null) return;
        // 检查玩家是否已注册
        if(AuthmeAPI.isRegistered(player.getName())){
            // 自动登录
            if(!AuthmeAPI.isAuthenticated(player)){
                AuthmeAPI.forceLogin(player);
            }
        }else{
            // 自动注册并登录
            AuthmeAPI.forceRegister(player, config.getString("whitelist.VISIT.AuthMePlugin.autoRegisterPassword", ""), true);
        }
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
