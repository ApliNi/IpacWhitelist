package aplini.ipacwhitelist.util;

public enum wlType {

    // 混合使用, 最初为了填充数据表中的 Type

    DEFAULT(0), // Type 默认
    NOT(0), // 不存在的
    VISIT(1), // Type 参观账户
    VISIT_DEL_DATA(2), // Type 已删除数据的参观账户
    ERROR(-1), // 出错
    ;

    private final int key;

    private wlType(int key) {
        this.key = key;
    }

    public int getID() {
        return key;
    }

}
