package aplini.ipacwhitelist.util;

public enum wlType {

    // 混合使用, 最初为了填充数据表中的 Type

    // 数据表 Type -> ID
    DEFAULT(0), // 默认, 正常的账户
    VISIT(1), // 参观账户
    VISIT_DEL_DATA(2), // 已删除数据的参观账户

    // 数据表 White
    NOT_WHITE(0), // 不在白名单中
    WHITE(1), // 白名单
    BLACK(2), // 黑名单

    // 表示
    NOT(0), // 不存在的
    ERROR(-5), // 出错
    ;

    private final int key;

    private wlType(int key) {
        this.key = key;
    }

    public int getID() {
        return key;
    }

}
