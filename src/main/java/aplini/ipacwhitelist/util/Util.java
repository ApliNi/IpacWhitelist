package aplini.ipacwhitelist.util;

import static aplini.ipacwhitelist.IpacWhitelist.getPlugin;

public class Util {

    // 获取当前时间
    public static long getTime(){
        return System.currentTimeMillis() / 1000;
    }

    // 是否超时
    public static boolean isWhitelistedExpired(Long dbPlayerTime){
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

    // 获取玩家数据的封装, 支持直接输入 Name, UUID32, UUID36
    public static PlayerData getPlayerData(String inp){
        if(inp.isEmpty()){return null;}
        // 转换 UUID 格式
        inp = ifIsUUID32toUUID36(inp);
        // 检查数据
        if(inp.length() == 36){ // uuid
            return SQL.getPlayerData(Type.DATA_UUID, inp);
        }else if(inp.length() <= 16){ // name
            return SQL.getPlayerData(Type.DATA_NAME, inp);
        }
        return null;
    }
}
