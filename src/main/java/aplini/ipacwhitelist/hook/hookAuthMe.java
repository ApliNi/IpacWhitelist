package aplini.ipacwhitelist.hook;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.FailedLoginEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.LogoutEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import static aplini.ipacwhitelist.func.eventFunc.runEventFunc;
import static aplini.ipacwhitelist.listener.onPlayerLogin.playerList;

public class hookAuthMe implements Listener {
    private static AuthMeApi AuthmeAPI = null;
    // 加载 API
    public hookAuthMe(){
        AuthmeAPI = AuthMeApi.getInstance();
    }

    // AuthMe 操作应发生在玩家加入服务器时
    // @EventHandler(priority = EventPriority.LOWEST)

    // 注册账户
    public static void authmeRegisterPlayer(Player player, String password){
        if(AuthmeAPI == null) return;
        // 如果已注册则跳过
        if(!AuthmeAPI.isRegistered(player.getName())){
            AuthmeAPI.forceRegister(player, password);
        }
    }

    // 登录账户
    public static void authmeLoginPlayer(Player player){
        if(AuthmeAPI == null) return;
        // 如果已经登录则跳过
        if(!AuthmeAPI.isAuthenticated(player)){
            AuthmeAPI.forceLogin(player);
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
    static public void onAuthMeFailedLoginEvent(FailedLoginEvent event) {
        Player player = event.getPlayer();
        if(playerList.contains(player.getUniqueId().toString())){
            runEventFunc("whitelist.WHITE.onAuthMeFailedLoginEvent", player);
        }
    }

    // AuthMe 玩家注销
    @EventHandler(priority = EventPriority.MONITOR)
    static public void onLogoutEvent(LogoutEvent event) {
        Player player = event.getPlayer();
        if(playerList.contains(player.getUniqueId().toString())){
            runEventFunc("whitelist.WHITE.onAuthMeLogoutEvent", player);
        }
    }
}
