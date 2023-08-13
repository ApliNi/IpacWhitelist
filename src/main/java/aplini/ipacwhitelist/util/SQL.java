package aplini.ipacwhitelist.util;

import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;

import static aplini.ipacwhitelist.IpacWhitelist.getPlugin;
import static aplini.ipacwhitelist.util.wlType.*;
import static org.bukkit.Bukkit.getLogger;

public class SQL {

    public static Connection connection;

    // 连接
    public static synchronized void connection() {
        String db = getPlugin().getConfig().getString("sql.db", "sqlite");
        String jdbc;

        if(db.equalsIgnoreCase("sqlite")){
            jdbc = "jdbc:%s:%s".formatted(db, new File(getPlugin().getDataFolder(), "database.db").getAbsolutePath());
        }else{
            jdbc = "jdbc:%s://%s:%s/%s".formatted(db,
                    getPlugin().getConfig().getString("sql.host"),
                    getPlugin().getConfig().getString("sql.port"),
                    getPlugin().getConfig().getString("sql.database"));
        }

        try {
            connection = DriverManager.getConnection(jdbc,
                    getPlugin().getConfig().getString("sql.user", ""),
                    getPlugin().getConfig().getString("sql.password", ""));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    // 断开
    public static synchronized void closeConnection() {
        try {
            connection.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // 初始化数据库
    public static synchronized void initialize() {
        try {
            // uuid 不能唯一, 因为需要留空等待玩家加入时填充

            String db = getPlugin().getConfig().getString("sql.db", "sqlite");
            db = "sqlite";
            String table = getPlugin().getConfig().getString("sql.table");

            // SQLITE
            if(db.equalsIgnoreCase("sqlite")){
                connection.prepareStatement("""
                        CREATE TABLE IF NOT EXISTS "%s" (
                            "ID" INTEGER NOT NULL,
                            "UUID" TEXT NOT NULL,
                            "NAME" TEXT NOT NULL,
                            "TIME" INTEGER NOT NULL,
                            "WHITE" INTEGER NOT NULL,
                            "Type" INTEGER NOT NULL,
                            PRIMARY KEY("ID" AUTOINCREMENT)
                        );
                        CREATE INDEX IF NOT EXISTS IDX_UUID ON %s (UUID);
                        CREATE INDEX IF NOT EXISTS IDX_NAME ON %s (NAME);
                        CREATE INDEX IF NOT EXISTS IDX_Type ON %s (Type);
                        """.formatted(table, table, table, table)
                ).execute();
            }else{
//                connection.prepareStatement("""
//                    CREATE TABLE IF NOT EXISTS `%s` (
//                        `ID` bigint(7) NOT NULL,
//                        `UUID` char(36) NOT NULL,
//                        `NAME` varchar(16) NOT NULL,
//                        `TIME` bigint(11) NOT NULL,
//                        `WHITE` boolean NOT NULL,
//                        `Type` bigint(2) NOT NULL,
//
//                        INDEX `IDX_UUID` (`UUID`) USING BTREE,
//                        INDEX `IDX_NAME` (`NAME`) USING BTREE,
//                        INDEX `IDX_Type` (`Type`) USING BTREE
//                    );
//                    """.formatted(table)
//                ).execute();
                getLogger().warning("[IpacWhitelist] 暂时移除了非 SQLite 数据库的支持...");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 修改或创建玩家数据
     * @param name 玩家名称, 可使用 null
     * @param UUID 玩家 UUID, 可使用 null
     * @param Time 时间戳. -1=始终有效, -2=使用默认值
     * @param White 白名单状态
     * @param Type 白名单类型, 可使用 null
     * @return 账户类型 VISIT, VISIT_DEL_DATA, DEFAULT, NOT
     */
    public static wlType setPlayerData(String name, String UUID, int Time, wlType White, wlType Type){
        wlType out;
        try {
            PreparedStatement sql;
            ResultSet results;

            // 检查是否有 name 和 uuid 匹配的记录
            if(name != null && UUID != null){
                sql = connection.prepareStatement("SELECT * FROM `player` WHERE `NAME` = ? AND `UUID` = ? ORDER BY ROWID DESC LIMIT 1;");
                sql.setString(1, name);
                sql.setString(2, UUID);
            }else if(name != null){
                sql = connection.prepareStatement("SELECT * FROM `player` WHERE `NAME` = ? ORDER BY ROWID DESC LIMIT 1;");
                sql.setString(1, name);
                UUID = "";
            }else if(UUID != null){
                sql = connection.prepareStatement("SELECT * FROM `player` WHERE `UUID` = ? ORDER BY ROWID DESC LIMIT 1;");
                sql.setString(1, UUID);
                name = "";
            }else{
                return ERROR;
            }

            results = sql.executeQuery();
            if(results.next()){
                // 输出账户类型
                switch(results.getInt("Type")){
                    case 1 -> out = VISIT;
                    case 2 -> out = VISIT_DATA_DELETE;
                    default -> out = DEFAULT;
                }
                // 处理缺省值
                Time = Time == -2 ? results.getInt("TIME") : Time;
                int TypeID = Type != null ? Type.getID() : results.getInt("Type");

                // 已添加, 重置这个ID的 TIME WHITE Type
                sql = connection.prepareStatement("UPDATE `player` SET `TIME` = ?, `WHITE` = ?, `Type` = ? WHERE `ID` = ?;");
                sql.setInt(1, Time);
                sql.setInt(2, White.getID());
                sql.setInt(3, TypeID);
                sql.setInt(4, results.getInt("ID"));
            }else{
                // 输出账户类型
                out = NOT;
                // 处理缺省值
                Time = Time == -2 ? -1 : Time;
                int TypeID = Type != null ? Type.getID() : DEFAULT.getID();

                // 未添加, 创建记录
                sql = connection.prepareStatement("REPLACE INTO `player` (`UUID`, `NAME`, `TIME`, `WHITE`, `Type`) VALUES (?, ?, ?, ?, ?);");
                sql.setString(1, UUID);
                sql.setString(2, name);
                sql.setLong(3, Time);
                sql.setInt(4, White.getID());
                sql.setInt(5, TypeID);
            }
            sql.execute();
            sql.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }
    // 添加玩家
    public static wlType addPlayer(String name){
        return setPlayerData(name, null, -1, WHITE, DEFAULT);
    }
    public static wlType addPlayer(String name, String UUID){
        return setPlayerData(name, UUID, -1, WHITE, DEFAULT);
    }
    public static void addPlayer(Player player, wlType type){
        setPlayerData(player.getName(), String.valueOf(player.getUniqueId()), -1, WHITE, type);
    }
    // 移除玩家
    public static wlType delPlayerName(String name){
        return setPlayerData(name, null, -2, NOT_WHITE, null);
    }
    public static wlType delPlayerUUID(String UUID){
        return setPlayerData(null, UUID, -2, NOT_WHITE, null);
    }
    // 封禁玩家
    public static wlType banPlayerName(String name){
        return setPlayerData(name, null, -2, BLACK, null);
    }
    public static wlType banPlayerUUID(String UUID){
        return setPlayerData(null, UUID, -2, BLACK, null);
    }


    // 是否在白名单中
    // NOT = 不存在记录, NOT_WHITE = 已移出白名单, EXPIRED = 过期, WHITE = 存在, ERROR = 出错, VISIT = 存在参观账号, VISIT_DEL_DATA = 已删除数据的参观账户
    public static wlType isWhitelisted(Player player){
        try {
            PreparedStatement sql;
            ResultSet results;

            // 如果UUID匹配
            sql = connection.prepareStatement("SELECT * FROM `player` WHERE `WHITE` = true AND `UUID` = ?;");
            sql.setString(1, player.getUniqueId().toString());
            results = sql.executeQuery();
            if(results.next()){

                // 黑名单
                if(results.getLong("WHITE") == BLACK.getID()){return BLACK;}
                // 未定义
                if(results.getLong("WHITE") == NOT_WHITE.getID()){return NOT_WHITE;}
                // 是否为参观账户
                if(results.getLong("Type") == VISIT.getID()){return VISIT;}
                if(results.getLong("Type") == VISIT_DATA_DELETE.getID()){return VISIT_DATA_DELETE;}
                // 白名单上的玩家是否超时
                if(Util.isWhitelistedTimeout(results.getLong("TIME"))){return EXPIRED;}

                // 白名单
                if(results.getLong("WHITE") == WHITE.getID()){
                    // 更新名称和最后加入时间
                    PreparedStatement update = connection.prepareStatement("UPDATE `player` SET `NAME` = ?, `TIME` = ? WHERE `ID` = ?;");
                    update.setString(1, player.getName());
                    update.setLong(2, System.currentTimeMillis() / 1000);
                    update.setLong(3, results.getLong("ID"));
                    update.executeUpdate();
                    update.close();
                    return WHITE;
                }
            }

            // 如果名称匹配
            sql = connection.prepareStatement("SELECT * FROM `player` WHERE `WHITE` = true AND `NAME` = ?;");
            sql.setString(1, player.getName());
            results = sql.executeQuery();
            if(results.next()){

                // 如果uuid不为空: 名称相同但uuid不同
                if(!results.getString("UUID").isEmpty()){return NOT;}

                // 黑名单
                if(results.getLong("WHITE") == BLACK.getID()){return BLACK;}
                // 未定义
                if(results.getLong("WHITE") == NOT_WHITE.getID()){return NOT_WHITE;}
                // 是否为参观账户
                if(results.getLong("Type") == VISIT.getID()){return VISIT;}
                if(results.getLong("Type") == VISIT_DATA_DELETE.getID()){return VISIT_DATA_DELETE;}
                // 白名单上的玩家是否超时
                if(Util.isWhitelistedTimeout(results.getLong("TIME"))){return EXPIRED;}

                // 白名单
                if(results.getLong("WHITE") == WHITE.getID()){
                    // 更新UUID/名称和最后加入时间
                    PreparedStatement update = connection.prepareStatement("UPDATE `player` SET `UUID` = ?, `NAME` = ?, `TIME` = ? WHERE `ID` = ?;");
                    update.setString(1, player.getUniqueId().toString());
                    update.setString(2, player.getName()); // 在第一次加入时处理名称大小写不匹配
                    update.setLong(3, System.currentTimeMillis() / 1000);
                    update.setLong(4, results.getLong("ID"));
                    update.executeUpdate();
                    update.close();
                    return WHITE;
                }
            }

            return NOT;
        } catch (Exception e) {
            return ERROR;
        }
    }

    // 获取参观账户的UUID, 通过名称
    public static String getVisitPlayerUUIDFromName(String name){
        String out = "";
        try {
            // 查找指定参观账户一条有uuid的记录
            PreparedStatement sql = connection.prepareStatement("SELECT * FROM `player` WHERE `NAME` = ? AND `Type` = ? AND UUID != '' ORDER BY ROWID DESC LIMIT 1;");
            sql.setString(1, name);
            sql.setInt(2, VISIT.getID());
            ResultSet results = sql.executeQuery();
            if(results.next()){
                out = results.getString("UUID");
            }
            sql.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }
}
