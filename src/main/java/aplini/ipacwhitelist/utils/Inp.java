package aplini.ipacwhitelist.utils;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.IpacWhitelist.server;

public class Inp {
    public String _inpString = null;
    public String inp = null;

    public String forName = null;
    public String forUUID = null;
    public UUID uuidObj = null;

    public Player onlinePlayer = null;

    public PlayerData pd = new PlayerData();
    public List<PlayerData> pds = new ArrayList<>();

    // 从用户输入中得到玩家数据
    public Inp fromInp(String _inp, boolean allowDel){

        // 如果包含反斜杠, 则表示这是来自控制台的消息, 需要进行预处理
        int index = _inp.indexOf("\\");
        if(index == -1){
            this._inpString = _inp;
        }else{
            this._inpString = _inp.substring(0, index);
        }
        this.inp = util.setUUID36(this._inpString);

        // 根据用户输入的数据类型填充数据
        if(this.inp.length() == 36){
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
        this.pds = sql.getPlayerDataList(this.forUUID, this.forName, allowDel, true);
        if(this.pds.isEmpty()){
            this.pd.setPlayerInfo(this.forUUID, this.forName);
        }else{
            this.pd = this.pds.get(0);
        }
        return this;
    }
    public Inp fromInp(String _inp){
        return this.fromInp(_inp, true);
    }
}
