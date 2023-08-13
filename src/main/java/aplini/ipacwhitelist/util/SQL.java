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
                            "WHITE" BOOLEAN NOT NULL,
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

    // 添加玩家
    public static wlType addPlayer(String name, String UUID, wlType Type){
        wlType out;
        try {
            PreparedStatement sql;
            ResultSet results;
            // 检查是否有相同的记录
            if(!UUID.isEmpty()){
                sql = connection.prepareStatement("SELECT * FROM `player` WHERE `NAME` = ? AND `UUID` = ? ORDER BY ROWID DESC LIMIT 1;");
                sql.setString(1, name);
                sql.setString(2, UUID);
            }else{
                sql = connection.prepareStatement("SELECT * FROM `player` WHERE `NAME` = ? ORDER BY ROWID DESC LIMIT 1;");
                sql.setString(1, name);
            }
            results = sql.executeQuery();
            if(results.next()){
                // 检查是否为参观账户
                if(results.getLong("Type") == 1){
                    out = VISIT;
                }else{
                    out = DEFAULT;
                }
                // 已添加, 重置这个ID的 TIME WHITE Type
                sql = connection.prepareStatement("UPDATE `player` SET `TIME` = ?, `WHITE` = ?, `Type` = ? WHERE `ID` = ?;");
                sql.setInt(1, -1);
                sql.setBoolean(2, true);
                sql.setInt(3, Type.getID());
                sql.setLong(4, results.getLong("ID"));
            }else{
                out = NOT;
                // 未添加, 创建记录
                sql = connection.prepareStatement("REPLACE INTO `player` (`UUID`, `NAME`, `TIME`, `WHITE`, `Type`) VALUES (?, ?, ?, ?, ?);");
                sql.setString(1, UUID);
                sql.setString(2, name);
                sql.setInt(3, -1);
                sql.setBoolean(4, true);
                sql.setInt(5, Type.getID());
            }
            sql.execute();
            sql.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }
    public static wlType addPlayer(String name){
        return addPlayer(name, "", wlType.DEFAULT);
    }
    public static wlType addPlayer(String name, String UUID){
        return addPlayer(name, UUID, wlType.DEFAULT);
    }
    public static wlType addPlayer(Player player, wlType type){
        return addPlayer(player.getName(), String.valueOf(player.getUniqueId()), type);
    }

    // 删除玩家, 通过名称
    public static boolean delPlayerName(String name){
        try {
            PreparedStatement sql = connection.prepareStatement("UPDATE `player` SET `WHITE` = ? WHERE `NAME` = ? ORDER BY ROWID DESC LIMIT 1;");
            sql.setBoolean(1, false);
            sql.setString(2, name);
            sql.executeUpdate();
            sql.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // 删除玩家, 通过UUID
    public static boolean delPlayerUUID(String UUID){
        try {
            PreparedStatement sql = connection.prepareStatement("UPDATE `player` SET `WHITE` = ? WHERE `UUID` = ? ORDER BY ROWID DESC LIMIT 1;");
            sql.setBoolean(1, false);
            sql.setString(2, UUID);
            sql.executeUpdate();
            sql.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // 是否在白名单中
    // NOT = 不在, DEFAULT = 存在, ERROR = 出错, VISIT = 存在但是参观账号, VISIT_DEL_DATA = 已删除数据的参观账户
    public static wlType isWhitelisted(Player player){
        try {
            PreparedStatement sql;
            ResultSet results;

            // 如果UUID匹配
            sql = connection.prepareStatement("SELECT * FROM `player` WHERE `WHITE` = true AND `UUID` = ?;");
            sql.setString(1, player.getUniqueId().toString());
            results = sql.executeQuery();
            if(results.next()){

                // 是否为参观账户
                if(results.getLong("Type") == VISIT.getID()){return VISIT;}
                if(results.getLong("Type") == VISIT_DEL_DATA.getID()){return VISIT_DEL_DATA;}
                // 白名单上的玩家是否超时
                if(Util.isWhitelistedTimeout(results.getLong("TIME"))){return NOT;}

                // 更新名称和最后加入时间
                PreparedStatement sql2 = connection.prepareStatement("UPDATE `player` SET `NAME` = ?, `TIME` = ? WHERE `ID` = ?;");
                sql2.setString(1, player.getName());
                sql2.setLong(2, System.currentTimeMillis() / 1000);
                sql2.setLong(3, results.getLong("ID"));
                sql2.executeUpdate();
                sql2.close();
                return DEFAULT;
            }

            // 如果名称匹配
            sql = connection.prepareStatement("SELECT * FROM `player` WHERE `WHITE` = true AND `NAME` = ?;");
            sql.setString(1, player.getName());
            results = sql.executeQuery();
            if(results.next()){

                // 是否为参观账户
                if(results.getLong("Type") == VISIT.getID()){return VISIT;}
                if(results.getLong("Type") == VISIT_DEL_DATA.getID()){return VISIT_DEL_DATA;}
                // 白名单上的玩家是否超时
                if(Util.isWhitelistedTimeout(results.getLong("TIME"))){return NOT;}
                // 如果uuid不为空: 名称相同但uuid不同
                if(!results.getString("UUID").isEmpty()){return NOT;}

                // 更新UUID/名称和最后加入时间
                PreparedStatement sql2 = connection.prepareStatement("UPDATE `player` SET `UUID` = ?, `NAME` = ?, `TIME` = ? WHERE `ID` = ?;");
                sql2.setString(1, player.getUniqueId().toString());
                sql2.setString(2, player.getName()); // 在第一次加入时处理名称大小写不匹配
                sql2.setLong(3, System.currentTimeMillis() / 1000);
                sql2.setLong(4, results.getLong("ID"));
                sql2.executeUpdate();
                sql2.close();
                return DEFAULT;
            }

            return NOT;
        } catch (Exception e) {
            return ERROR;
        }
    }

    // 获取数据
    public static String getVisitPlayerUUIDFromName(String name){
        String out = "";
        try {
            PreparedStatement sql = connection.prepareStatement("SELECT * FROM `player` WHERE `NAME` = ? AND `Type` = ? ORDER BY ROWID DESC LIMIT 1;");
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
