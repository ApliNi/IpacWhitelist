package aplini.ipacwhitelist.util;

import static aplini.ipacwhitelist.util.Type.NOT;
import static aplini.ipacwhitelist.util.Type.NOT_BAN;
import static aplini.ipacwhitelist.util.Util.ifIsUUID32toUUID36;

// 与数据库中的数据结构相同
// 默认数据表示此数据不可用
public class PlayerData {
    public boolean available = false;
    public int ID = -1;
    public Type Type = NOT;
    public Type Ban = NOT_BAN;
    public String UUID = null;
    public String Name = null;
    public long Time = -1;


    // 自动识别并添加玩家数据
    public void addPlayerAuto(String inpData){
        inpData = ifIsUUID32toUUID36(inpData);
        if(inpData.length() == 36){ // uuid
            UUID = inpData;
        }else if(inpData.length() <= 16){ // name
            Name = inpData;
        }
    }

    // 保存数据到数据库
    public Type save(){
        return SQL.setPlayerData(Name, UUID, Time, Type, Ban);
    }
}
