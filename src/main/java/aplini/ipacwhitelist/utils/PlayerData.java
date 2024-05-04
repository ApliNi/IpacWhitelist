package aplini.ipacwhitelist.utils;

import aplini.ipacwhitelist.enums.Type;
import org.bukkit.entity.Player;

import java.sql.ResultSet;

public class PlayerData {

    // SQL
    // | ID | Type | Ban | UUID | Name | Time | Config |

    // 数据库中的数据
    public int id       = -1;
    public Type type    = Type.NOT;
    public Type ban     = Type.NOT;
    public String uuid  = null;
    public String name  = null;
    public long time    = 0;    // 秒级时间戳
    public PlayerConfig config  = new PlayerConfig();

    // 保存数据时是否更新时间
    public boolean updateTime = true;

    // 数据是否为空
    public boolean isNull(){
        return this.id == -1;
    }

    // 数据是否存在: 不为空且 type 不为 NULL (0)
    public boolean isExist(){
        return this.id != -1 && this.type != Type.NOT;
    }

    // 从数据库返回数据中生成 PlayerData
    public PlayerData fromDB(ResultSet results){
        try {
            this.id = results.getInt("ID");
            this.type = Type.getType(results.getInt("Type"));
            this.ban = Type.getBan(results.getInt("Ban"));
            this.uuid = results.getString("UUID");
            this.name = results.getString("Name");
            this.time = results.getLong("Time");
            this.config = new PlayerConfig().setYamlStr(results.getString("Config"));
        } catch (Exception e) {
            throw new RuntimeException("[IpacWhitelist] [错误] 无法解析数据库中的玩家数据");
        }
        return this;
    }

    // 添加玩家信息
    public void setPlayerInfo(Player player){
        this.uuid = player.getUniqueId().toString();
        this.name = player.getName();
    }
    public void setPlayerInfo(String uuid, String name){
        this.uuid = uuid;
        this.name = name;
    }

    // 与一个数据进行对比, 取其中权重较高的项进行更新
    public void compareAndConvert(PlayerData pd2){
        if(pd2.type.weights > this.type.weights){
            this.type = pd2.type;
        }
        if(pd2.ban.weights > this.ban.weights){
            this.ban = pd2.ban;
        }
    }

    // 保存数据
    public void save(){
        sql.savePlayerData(this);
    }

    // 删除数据
    public void delete(){
        sql.delPlayerData(this.id);
    }
}
