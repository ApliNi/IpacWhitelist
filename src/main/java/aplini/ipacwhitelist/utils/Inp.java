package aplini.ipacwhitelist.utils;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.IpacWhitelist.server;

public class Inp {
    public String inpString = null;
    public String inp = null;

    public String forName = null;
    public String forUUID = null;
    public UUID uuidObj = null;

    public Player onlinePlayer = null;

    public final PlayerData pd = new PlayerData();
    public List<PlayerData> pds = new ArrayList<>();

    // 从用户输入中得到玩家数据
    public Inp fromInp(String _inp, boolean allowDel){
        // 如果包含反斜杠, 则表示这是来自控制台的消息, 需要进行预处理
        int index = _inp.indexOf("\\");
        if(index != -1){
            this.inpString = _inp.substring(0, index);
        }else{
            this.inpString = _inp;
        }
        this.inp = util.setUUID36(this.inpString);
        // 根据用户输入的数据类型填充数据
        if(inp.length() == 36){
            this.forUUID = this.inp.toLowerCase();
            this.uuidObj = UUID.fromString(this.inp);
            this.onlinePlayer = server.getPlayer(this.uuidObj);
        }else{
            // 如果无法与名称正则匹配, 则返回 null
            if(!Pattern.matches(config.getString("whitelist.playerNameRule", ".*"), this.inp)){
                return null;
            }
            this.forName = this.inp;
            this.onlinePlayer = server.getPlayer(this.inp);
        }
        // 在数据库中查询数据
        this.pd.setPlayerInfo(forUUID, forName);
        this.pds = sql.getPlayerDataList(this.pd.uuid, this.pd.name, allowDel, true);
        return this;
    }
    public Inp fromInp(String _inp){
        return this.fromInp(_inp, true);
    }
}
