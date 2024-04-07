package aplini.ipacwhitelist.enums;

public enum pc {

    CmdAddLogSender("CmdAddLogSender", "最后操作"),

    ;

    public final String key;
    public final String name;
    pc(String key, String name){
        this.key = key;
        this.name = name;
    }

    static public pc get(String key){
        for(pc li : pc.values()){
            if(li.key.equals(key)){
                return li;
            }
        }
        throw new IllegalArgumentException("找不到指定的 PlayerConfig Key: "+ key);
    }
}
