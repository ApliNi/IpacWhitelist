package aplini.ipacwhitelist.enums;

public enum ph {

    ip("%ip%"),
    var("%var%"),
    key("%key%"),
    playerUUID("%playerUUID%"),
    playerUUID32("%playerUUID32%"),
    playerName("%playerName%"),
    id("%id%"),
    type("%type%"),
    ban("%ban%"),
    time("%time%"),
    timeLong("%timeLong%"),
    worldPath("%worldPath%"),
    worldName("%worldName%"),

    ;

    public final String ph;
    ph(String ph){
        this.ph = ph;
    }
}
