package aplini.ipacwhitelist.util;

import org.bukkit.entity.Player;

import static aplini.ipacwhitelist.util.Type.NOT;
import static aplini.ipacwhitelist.util.Type.NOT_BAN;
import static aplini.ipacwhitelist.util.Util.ifIsUUID32toUUID36;

// 与数据库中的数据结构相同
// 默认数据表示此数据不可用
public class PlayerData {
    public int ID = -1;
    public Type Type = NOT;
    public Type Ban = NOT_BAN;
    public String UUID = "";
    public String Name = "";
    public long Time = -1;

    public Type __whitelistedState = null;


    // 数据是否为空
    public boolean isNull(){
        return ID == -1;
    }

    // 自动识别并添加玩家数据
    public void addPlayerAuto(String inpData){
        inpData = ifIsUUID32toUUID36(inpData);
        if(inpData.length() == 36){ // uuid
            this.UUID = inpData;
        }else if(inpData.length() <= 16){ // name
            this.Name = inpData;
        }
    }

    // 设置玩家数据
    public void setPlayerInfo(Player player){
        this.UUID = player.getUniqueId().toString();
        this.Name = player.getName();
    }

    // 保存数据到数据库
    public void save(){
        SQL.savePlayerData(this);
    }

    // 设置白名单状态, 并返回 this
    public PlayerData whitelistedState(Type type){
        this.__whitelistedState = type;
        return this;
    }
}
