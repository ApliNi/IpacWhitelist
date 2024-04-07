package aplini.ipacwhitelist.hook;

import org.geysermc.geyser.api.GeyserApi;

import java.util.UUID;

public class geyser {

    public static boolean isGeyserPlayer(UUID uuid){
        return GeyserApi.api().connectionByUuid(uuid) != null;
    }
}
