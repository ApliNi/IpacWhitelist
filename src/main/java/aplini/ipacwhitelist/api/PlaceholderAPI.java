package aplini.ipacwhitelist.api;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.enums.Type;
import aplini.ipacwhitelist.utils.Inp;
import aplini.ipacwhitelist.utils.PlayerData;
import aplini.ipacwhitelist.utils.sql;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static aplini.ipacwhitelist.IpacWhitelist.config;

public class PlaceholderAPI extends PlaceholderExpansion {

    private final IpacWhitelist plugin;

    public PlaceholderAPI(IpacWhitelist plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "ApliNi";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "iwl";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public boolean persist() {
        return true;
    }

    static Pattern pattern = Pattern.compile("^(.+)\\((.+)\\)$");

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params){

        // 如果这是一个带参数的变量
        Matcher matcher = pattern.matcher(params);
        String cmd = null, playerInp = null;
        if(matcher.find()){
            cmd = matcher.group(1);
            playerInp = matcher.group(2);
        }

        cmd = (cmd == null)? params.toLowerCase() : cmd.toLowerCase();
        playerInp = (playerInp == null)? player.getUniqueId().toString() : playerInp;

        Inp inp = new Inp().fromInp(playerInp, false);

        switch(cmd){
            case "player_type" -> {
                Type type = (inp.pd.ban == Type.BAN)? Type.BAN : inp.pd.type;
                return type.name;
            }
            case "player_type_name" -> {
                Type type = (inp.pd.ban == Type.BAN)? Type.BAN : inp.pd.type;
                return config.getString("api.PlaceholderAPI.iwl_player_type_name." + type.name, "未定义变量名 " + type.name);
            }
        }

        return null;
    }
}