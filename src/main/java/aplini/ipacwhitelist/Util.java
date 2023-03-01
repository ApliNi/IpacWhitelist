package aplini.ipacwhitelist;

import static aplini.ipacwhitelist.IpacWhitelist.getPlugin;

public class Util {
    // 是否超时
    public static boolean isWhitelistedTimeout(Long dbPlayerTime){
        long configTime = getPlugin().getConfig().getLong("whitelist.timeout", -1);
        if(configTime == -1){
            return false;
        }
        if(dbPlayerTime == -1){
            return false;
        }
        if((System.currentTimeMillis() / 1000) - dbPlayerTime > configTime){
            return true;
        }
        return false;
    }
}
