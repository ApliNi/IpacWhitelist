package aplini.ipacwhitelist.enums;

public enum ph {

    ip("%ip%"),
    var("%var%"),
    playerUUID("%playerUUID%"),
    playerName("%playerName%"),
    id("%id%"),
    type("%type%"),
    ban("%ban%"),
    time("%time%"),
    worldPath("%worldPath%"),
    worldName("%worldName%"),

    ;

    public final String ph;
    ph(String ph){
        this.ph = ph;
    }
}