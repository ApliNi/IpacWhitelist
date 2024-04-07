package aplini.ipacwhitelist.utils;

import aplini.ipacwhitelist.enums.Key;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import static aplini.ipacwhitelist.IpacWhitelist.plugin;

public class netReq {

    static public String fetch(String url){
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        Request.Builder request = new Request.Builder()
                .url(url);

        try (Response response = client.build().newCall(request.build()).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            return response.body().string();
        } catch (IOException e) {
            plugin.getLogger().warning("[HTTP Client] 无法访问 URL: "+ url);
            plugin.getLogger().warning(e.getMessage());
        }
        return null;
    }

    static public Key isPremiumPlayer(String name, String uuid){
        String data = fetch("https://api.mojang.com/users/profiles/minecraft/"+ name);
        if(data == null){
            return Key.ERR;
        }

        String uuidProbe = "\"id\" : \""+ uuid.replaceAll("-", "") +"\",";
        String nameProbe = "\"name\" : \""+ name +"\"";

        if(data.contains(uuidProbe) && data.contains(nameProbe)){
            return Key.TRUE;
        }
        return Key.FALSE;
    }
}
