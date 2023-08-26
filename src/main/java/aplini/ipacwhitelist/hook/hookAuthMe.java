package aplini.ipacwhitelist.hook;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.FailedLoginEvent;
import fr.xephi.authme.events.LoginEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import static aplini.ipacwhitelist.Listener.PlayerJoinMessage.onAuthMeFailedLoginEvent;
import static aplini.ipacwhitelist.Listener.PlayerJoinMessage.onAuthMeLoginEvent;

public class hookAuthMe implements Listener {
    private static AuthMeApi AuthmeAPI = null;

    // 加载 API
    public hookAuthMe(){
        AuthmeAPI = AuthMeApi.getInstance();
    }

    // 注册玩家
    public static void autoRegister(Player player, String password){
        if(AuthmeAPI == null) return;
        // 如果已注册则跳过
        if(!AuthmeAPI.isRegistered(player.getName())){
            AuthmeAPI.forceRegister(player, password);
        }
    }

    // 登录玩家
    public static void autoLogin(Player player){
        if(AuthmeAPI == null) return;
        AuthmeAPI.forceLogin(player);
    }


    // AuthMe 玩家登录事件
    @EventHandler(priority = EventPriority.MONITOR)
    public void _onAuthMeLoginEvent(LoginEvent event) {
        onAuthMeLoginEvent(event.getPlayer());
    }

    // AuthMe 玩家输入错误密码
    @EventHandler(priority = EventPriority.MONITOR)
    static public void _onAuthMeFailedLoginEvent(FailedLoginEvent event) {
        onAuthMeFailedLoginEvent(event.getPlayer());
    }

}
