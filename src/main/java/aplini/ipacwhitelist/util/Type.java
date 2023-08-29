package aplini.ipacwhitelist.util;

import java.util.HashMap;
import java.util.Map;

public enum Type {

    // 账户类型数值
    NOT(0, "NOT"), // 未定义 or 不在白名单中
    VISIT(1, "VISIT"), // 参观账户
    WHITE(2, "WHITE"), // 白名单
//    BLACK(3, "BLACK"), // 黑名单
    VISIT_CONVERT(4, "VISIT_CONVERT"), // 参观账户需要转为白名单
//    VISIT_BLACK(5, "VISIT_BLACK"), // 参观账户在黑名单中

    // Ban 列
    NOT_BAN(0, "NOT"),
    BAN(1, "BAN"),

    // 表示方法
    WHITE_EXPIRED(-1, "WHITE_EXPIRED"), // 白名单过期
    ERROR(-5, "ERROR"), // 错误
    ALL(-1, "ALL"),

    // 数据列名称
    DATA_UUID(-2, "UUID"), // UUID
    DATA_NAME(-2, "NAME"), // NAME

    ;

    private final int key;
    private final String name;
    Type(int key, String name){
        this.key = key;
        this.name = name;
    }

    // 将枚举存入 HashMap
    private static final Map<Integer, Type> TypeMap = new HashMap<>();
    static {
        for(Type value : Type.values()){
            TypeMap.put(value.getID(), value);
        }
    }

    // 获取枚举对应的id
    public int getID() {
        return key;
    }

    // 获取名称
    public String getName() {
        return name;
    }

    // 获取id对应的枚举
    public static Type getType(int id){
        return TypeMap.get(id);
    }

    // 获取 Ban 列的对应枚举
    public static Type getBan(int id){
        if(id == NOT_BAN.key){
            return NOT_BAN;
        }
        return BAN;
    }

    // 是否为参观账户
    public static boolean isVisit(Type type){
        return type == VISIT || type == VISIT_CONVERT;
    }
}
