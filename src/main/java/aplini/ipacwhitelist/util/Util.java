package aplini.ipacwhitelist.util;

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
        return (System.currentTimeMillis() / 1000) - dbPlayerTime > configTime;
    }

    // 将 32 位 UUID 转换为 36 位
    public static String ifIsUUID32toUUID36(String UUID){
        if(UUID.length() != 32){
            return UUID;
        }

        // 添加横杠
        StringBuilder UUID36 = new StringBuilder(UUID);
        UUID36.insert(8,"-");
        UUID36.insert(12,"-");
        UUID36.insert(16,"-");
        UUID36.insert(20,"-");

        return UUID36.toString();
    }
}
