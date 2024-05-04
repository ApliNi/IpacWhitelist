package aplini.ipacwhitelist.api;

import aplini.ipacwhitelist.utils.PlayerData;
import aplini.ipacwhitelist.utils.sql;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import static aplini.ipacwhitelist.IpacWhitelist.config;

public class PlaceholderAPI extends PlaceholderExpansion {

    @Override
    public String getAuthor() {
        return "ApliNi";
    }

    @Override
    public String getIdentifier() {
        return "IpacWhitelist";
    }

    @Override
    public String getVersion() {
        return "0.0.1";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params){

        if(params.equalsIgnoreCase("iwl_player_type")){
            PlayerData pd = sql.getPlayerData(player.getUniqueId().toString(), player.getName(), false);
            return pd.type.name;
        }

        if(params.equalsIgnoreCase("iwl_player_type_name")){
            PlayerData pd = sql.getPlayerData(player.getUniqueId().toString(), player.getName(), false);
            return config.getString("api.PlaceholderAPI.iwl_player_type_name."+ pd.type.name, "未定义变量名 "+ pd.type.name);
        }

        return null;
    }
}