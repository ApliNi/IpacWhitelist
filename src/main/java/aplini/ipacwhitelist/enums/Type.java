package aplini.ipacwhitelist.enums;

public enum Type {

    // SQL
    // | ID | Type | Ban | UUID | Name | Time | Config |

    // 账户类型
    NOT(0, "NOT"), // 未定义 or 不在白名单中
    VISIT(1, "VISIT"), // 参观账户
    WHITE(2, "WHITE"), // 白名单
    VISIT_CONVERT(4, "VISIT_CONVERT"), // 参观账户需要转为白名单
    // Ban
    BAN(1, "BAN"),

    ;

    public final int num;      // 数据值
    public final String name;  // 显示名称
    Type(int num, String name){
        this.num = num;
        this.name = name;
    }

    // 获取 Type 列的对应枚举
    public static Type getType(int num){
        return (switch(num){
            case 0 -> Type.NOT;
            case 1 -> Type.VISIT;
            case 2 -> Type.WHITE;
            case 4 -> Type.VISIT_CONVERT;
            default -> throw new IllegalStateException("Unexpected value: " + num);
        });
    }

    // 获取显示名称对应的枚举
    public static Type getType(String test){
        return (switch(test.toUpperCase()){
            case "NOT" -> Type.NOT;
            case "VISIT" -> Type.VISIT;
            case "WHITE" -> Type.WHITE;
            case "VISIT_CONVERT" -> Type.VISIT_CONVERT;
            case "BAN" -> Type.BAN;
            default -> null;
        });
    }

    // 获取 Ban 列的对应枚举
    public static Type getBan(int num){
        return (switch(num){
            case 0 -> Type.NOT;
            case 1 -> Type.BAN;
            default -> throw new IllegalStateException("Unexpected value: " + num);
        });
    }
}
