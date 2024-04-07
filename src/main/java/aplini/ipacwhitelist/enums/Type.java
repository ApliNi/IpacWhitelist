package aplini.ipacwhitelist.enums;

public enum Type {

    // SQL
    // | ID | Type | Ban | UUID | Name | Time | Config |

    // 权重
    // 已添加完毕的记录始终大于未添加完毕的记录
    // 黑名单等记录始终大于其他
    // 标记为运行过程中的记录小于任何已被确定的记录

    // 账户类型

    // 未定义 or 不在白名单中
    NOT(
            0,
            0,
            "NOT"),

    // 参观账户
    VISIT(
            1,
            1,
            "VISIT"),

    // 白名单
    WHITE(
            2,
            3,
            "WHITE"),

    // 参观账户需要转为白名单
    VISIT_CONVERT(
            4,
            2,
            "VISIT_CONVERT"),

    // Ban, 这个数据存储在独立的列中
    BAN(
            1,
            999,
            "BAN"),

    ;

    public final int num;      // 数据值
    public final String name;  // 显示名称
    public final int weights;
    Type(int num, int weights, String name){
        this.num = num;
        this.weights = weights;
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
