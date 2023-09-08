package aplini.ipacwhitelist.util;

import java.util.HashMap;
import java.util.Map;

// Type 的索引
enum Li {
    NOT,
    UUID,
    NAME,
    TYPE,
    BAN,
}

public enum Type {

    // 账户类型数值
    NOT(0, Li.TYPE, "NOT"), // 未定义 or 不在白名单中
    VISIT(1, Li.TYPE, "VISIT"), // 参观账户
    WHITE(2, Li.TYPE, "WHITE"), // 白名单
    VISIT_CONVERT(4, Li.TYPE, "VISIT_CONVERT"), // 参观账户需要转为白名单

    // Ban 列
    NOT_BAN(0, Li.BAN, "NOT"),
    BAN(1, Li.BAN, "BAN"),

    // 表示方法
    WHITE_EXPIRED(-1, Li.NOT, "WHITE_EXPIRED"), // 白名单过期
    ERROR(-2, Li.NOT, "ERROR"), // 错误
    ALL(-3, Li.NOT, "ALL"), // 遍历账户
    DATA_NAME_LIMIT_EMPTY_UUID(-4, Li.NOT, "DATA_NAME_LIMIT_EMPTY_UUID"), // 查询数据, 但限定 UUID 为空

    // 数据列名称
    DATA_UUID(-1, Li.UUID, "UUID"), // UUID
    DATA_NAME(-2, Li.NAME, "NAME"), // NAME

    ;

    // 枚举中的数据
    private final int key; // 数据值
    private final Li li; // 分类
    private final String name; // 显示名称
    Type(int key, Li li, String name){
        this.key = key;
        this.li = li;
        this.name = name;
    }

    // 创建索引
    private static final Map<Li, Map<Integer, Type>> TypeMap = new HashMap<>();
    static {
        for(Type aType : Type.values()){
            TypeMap.computeIfAbsent(aType.li, k -> new HashMap<>());
            TypeMap.get(aType.li).put(aType.key, aType);
        }
    }

    // 获取 Type 列的对应枚举
    public static Type getType(int id){
        return TypeMap.get(Li.TYPE).get(id);
    }

    // 获取 Ban 列的对应枚举
    public static Type getBan(int id){
        return TypeMap.get(Li.BAN).get(id);
    }

    // 获取枚举对应的id
    public int getID() {
        return key;
    }

    // 获取名称
    public String getName() {
        return name;
    }

    // 是否为参观账户
    public static boolean isVisit(Type type){
        return type == VISIT || type == VISIT_CONVERT;
    }
}
