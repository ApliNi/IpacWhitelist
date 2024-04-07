package aplini.ipacwhitelist.utils;

import aplini.ipacwhitelist.enums.ph;

import java.text.SimpleDateFormat;
import java.util.Date;

public class util {

    // 获取秒级时间戳
    public static long getTime(){
        return System.currentTimeMillis() / 1000;
    }

    // 从秒级时间戳中获取显示时间
    public static String getDisplayTime(long time){
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time * 1000));
    }

    // 是否达到超时时间
    public static boolean isTimedOut(long playerTime, long timeOut){
        if(playerTime == -1){
            return false;
        }
        return (System.currentTimeMillis() / 1000) - playerTime >= timeOut;
    }

    // 将 32 位 UUID 转换为 36 位
    // 用于处理用户输入的用户名和 UUID
    public static String setUUID36(String inp){
        if(inp.length() != 32){
            return inp;
        }
        // 添加连字符
        StringBuilder UUID36 = new StringBuilder(inp);
        UUID36.insert(8,"-");
        UUID36.insert(13,"-");
        UUID36.insert(18,"-");
        UUID36.insert(23,"-");
        return UUID36.toString();
    }

    // 在字符串填充玩家信息
    public static String msg(String msg, String playerUUID, String playerName){
        if(playerUUID == null || playerUUID.isEmpty()){playerUUID = "NULL";}
        if(playerName == null || playerName.isEmpty()){playerName = "NULL";}
        return msg
                .replace(ph.playerUUID.ph, playerUUID)
                .replace(ph.playerName.ph, playerName);
    }
    public static String msg(String msg, String var){
        return msg(msg, var, var);
    }

    public static Object SEL(Object obj1, Object obj2){
        return obj1 != null ? obj1 : obj2;
    }
}
