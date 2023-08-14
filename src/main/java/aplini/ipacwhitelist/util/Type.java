package aplini.ipacwhitelist.util;

import java.util.HashMap;
import java.util.Map;

public enum Type {

    // 账户类型数值
    NOT(0), // 未定义 or 不在白名单中
    VISIT(1), // 参观账户
    WHITE(2), // 白名单
    BLACK(3), // 黑名单
    VISIT_CONVERT(4), // 参观账户需要转为白名单
    VISIT_BLACK(5), // 参观账户在黑名单中

    // 表示方法
    WHITE_EXPIRED(-1), // 白名单过期
    ERROR(-5), // 错误

    // 数据列名称
    UUID(-2), // UUID
    NAME(-2), // UUID

    ;

    private final int key;
    Type(int key){this.key = key;}

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

    // 获取id对应的枚举
    public static Type getType(int id){
        return TypeMap.get(id);
    }
}
