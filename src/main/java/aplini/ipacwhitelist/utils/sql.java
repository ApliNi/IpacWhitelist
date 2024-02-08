package aplini.ipacwhitelist.utils;

import aplini.ipacwhitelist.enums.Key;
import aplini.ipacwhitelist.enums.Type;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.IpacWhitelist.plugin;
import static aplini.ipacwhitelist.utils.util.getTime;

public class sql {

    // 数据库连接
    public static Connection conn = null;

    // 连接数据库
    public static void runConn() {
        try {
            conn = DriverManager.getConnection(
                    "jdbc:%s:%s".formatted(
                            "sqlite",
                            new File(plugin.getDataFolder(), "Data.sqlite3").getAbsolutePath()));
        } catch (SQLException e) {throw new RuntimeException(e);}
    }

    // 断开连接
    public static void closeConn(){
        if(conn != null){
            try {
                conn.close();
            } catch (Exception e) {throw new RuntimeException(e);}
        }
    }

    // 重新连接
    public static synchronized void reconnect() {
        closeConn();
        runConn();
    }

    // 初始化数据库
    public static void initialize(){
        try {
            final Statement statement = conn.createStatement();
            statement.execute("PRAGMA auto_vacuum = FULL;");
            statement.execute("PRAGMA journal_mode = WAL;");
            statement.close();

            // 重新连接数据库
            reconnect();

            // 加载数据表
            conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS "player" (
                        "ID"        INTEGER NOT NULL,
                        "Type"      INTEGER NOT NULL,
                        "Ban"       INTEGER NOT NULL,
                        "UUID"      TEXT    NOT NULL,
                        "Name"      TEXT    NOT NULL %s,     -- 大小写敏感控制
                        "Time"      INTEGER NOT NULL,
                        "Config"    TEXT    NOT NULL,
                        PRIMARY KEY("ID" AUTOINCREMENT)
                    );
                    CREATE INDEX IF NOT EXISTS IDX_Type ON "player" (Type);
                    CREATE INDEX IF NOT EXISTS IDX_Ban  ON "player" (Ban );
                    CREATE INDEX IF NOT EXISTS IDX_UUID ON "player" (UUID);
                    CREATE INDEX IF NOT EXISTS IDX_Name ON "player" (Name);
                    CREATE INDEX IF NOT EXISTS IDX_Time ON "player" (Time);
                    """.formatted(config.getBoolean("sqlite.nameCaseSensitive", false) ? "" : "COLLATE NOCASE")
            ).execute();

        } catch (Exception e) {throw new RuntimeException(e);}
    }


    /**
     * 获取一个玩家数据
     * @param uuid 带连字符的 UUID 字符串
     * @param name 用户名, 填入数据时区分大小写, 允许用户控制大小写敏感
     * @param allowDel 是否查询 Type = NOT 的数据
     * @return PlayerData 数据, 如果不存在则为空
     */
    public static List<PlayerData> getPlayerDataList(String uuid, String name, boolean allowDel, boolean getArray){
        // 均为空
        if(uuid == null && name == null){
            throw new RuntimeException("[IpacWhitelist] sql.getPlayerData 传入空数据");
        }
        // 是否允许查询已删除的数据
        String additional = " "+
                (allowDel ? "" : " AND `Type` != %s ".formatted(Type.NOT.num)) +
                (getArray ? " LIMIT 999999 " : " LIMIT 1 ");
        try {
            PreparedStatement sql;
            // 名称为空, 查询 UUID
            if(name == null){
                sql = conn.prepareStatement("SELECT * FROM `player` WHERE `UUID` = ? "+ additional +";");
                sql.setString(1, uuid);
            }
            // UUID为空, 查询名称
            else if(uuid == null){
                sql = conn.prepareStatement("SELECT * FROM `player` WHERE `Name` = ? "+ additional +";");
                sql.setString(1, name);
            }
            // 均不为空
            else{
                sql = conn.prepareStatement("SELECT * FROM `player` WHERE `UUID` = ? AND `Name` = ? "+ additional +";");
                sql.setString(1, uuid);
                sql.setString(2, name);
            }
            ResultSet results = sql.executeQuery();
            // 返回一条数据还是返回所有匹配项
            if(getArray){
                List<PlayerData> pd = new ArrayList<>();
                while(results.next()){
                    pd.add(new PlayerData().fromDB(results));
                }
                return pd;
            }else{
                if(results.next()){
                    return List.of(new PlayerData().fromDB(results));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<>();
    }
    public static PlayerData getPlayerData(String uuid, String name, boolean allowDel){
        List<PlayerData> pds = getPlayerDataList(uuid, name, allowDel, false);
        return pds.get(0);
    }
    public static PlayerData getPlayerData(Player player, boolean allowDel){
        return getPlayerData(player.getUniqueId().toString(), player.getName(), allowDel);
    }


    // 模糊查找匹配的数据
    public static List<PlayerData> findPlayerDataList(String inp, Key mode){
        // 为空
        if(inp == null){
            throw new RuntimeException("[IpacWhitelist] sql.getPlayerData 传入空数据");
        }
        // 是否允许查询已删除的数据
        String additional = (switch(mode){

            case GET_VISIT_OR_NOT ->
                    " AND (`Type` = %s OR `Type` = %s) ".formatted(Type.VISIT.num, Type.NOT.num);

            case GET_WHITE_OR_VISIT_NOT_BAN ->
                    " AND (`Type` = %s OR `Type` = %s) AND `Ban` = %s ".formatted(Type.WHITE.num, Type.VISIT.num, Type.NOT.num);

            case GET_NOT ->
                    " AND `Type` = %s ".formatted(Type.NOT.num);

            case GET_WHITE ->
                    " AND `Type` = %s ".formatted(Type.WHITE.num);

            case GET_VISIT ->
                    " AND `Ban` = %s ".formatted(Type.VISIT.num);

            case GET_VISIT_CONVERT ->
                    " AND `Ban` = %s ".formatted(Type.VISIT_CONVERT.num);

            case GET_BAN ->
                    " AND `Ban` = %s ".formatted(Type.BAN.num);

            default -> "";
        });
        try {
            PreparedStatement sql = conn.prepareStatement("SELECT * FROM `player` WHERE `Name` LIKE ? OR `UUID` LIKE ? "+ additional +" LIMIT 999999;");
            sql.setString(1, inp +"%");
            sql.setString(2, inp +"%");
            ResultSet results = sql.executeQuery();

            List<PlayerData> pd = new ArrayList<>();
            while(results.next()){
                pd.add(new PlayerData().fromDB(results));
            }
            return pd;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static List<PlayerData> findPlayerDataList(String inp){
        return findPlayerDataList(inp, Key.NULL);
    }


    // 保存玩家数据
    public static void savePlayerData(PlayerData pd){
        // 更新数据修改时间
        if(pd.updateTime){
            pd.time = getTime();
        }
        // 将 null 转换为空字符串
        pd.uuid = pd.uuid == null ? "" : pd.uuid;
        pd.name = pd.name == null ? "" : pd.name;
        try {
            PreparedStatement sql;
            int i = 0;
            // 如果id不存在则创建数据, 否则更新数据
            if (pd.isNull()) {
                sql = conn.prepareStatement("INSERT INTO `player` (`Type`, `Ban`, `UUID`, `Name`, `Time`, `Config`) VALUES (?, ?, ?, ?, ?, ?);");
                sql.setInt(++i, pd.type.num);
                sql.setInt(++i, pd.ban.num);
                sql.setString(++i, pd.uuid);
                sql.setString(++i, pd.name);
                sql.setLong(++i, pd.time);
                sql.setString(++i, pd.config.getString());
            } else {
                sql = conn.prepareStatement("UPDATE `player` SET `Type` = ?, `Ban` = ?, `UUID` = ?, `Name` = ?, `Time` = ?, `Config` = ? WHERE `ID` = ?;");
                sql.setInt(++i, pd.type.num);
                sql.setInt(++i, pd.ban.num);
                sql.setString(++i, pd.uuid);
                sql.setString(++i, pd.name);
                sql.setLong(++i, pd.time);
                sql.setString(++i, pd.config.getString());
                sql.setInt(++i, pd.id);
            }
            sql.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // 完全删除一条数据
    public static void delPlayerData(int id){
        try {
            PreparedStatement sql = conn.prepareStatement("DELETE FROM `player` WHERE `ID` = ?;");
            sql.setInt(1, id);
            sql.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
