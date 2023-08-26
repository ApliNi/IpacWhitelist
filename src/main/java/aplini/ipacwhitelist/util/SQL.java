package aplini.ipacwhitelist.util;

import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;

import static aplini.ipacwhitelist.IpacWhitelist.getPlugin;
import static aplini.ipacwhitelist.util.Type.*;
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
    // 重连
    public static synchronized void reconnect() {
        closeConnection();
        connection();
    }

    // 初始化数据库
    public static synchronized void initialize() {
        try {

//            String db = getPlugin().getConfig().getString("sql.db", "sqlite");
            String db = "sqlite";

            // SQLITE
            if(db.equalsIgnoreCase("sqlite")){

                // 使用 WAL 模式. 自动复用碎片空间
                connection.prepareStatement("""
                        PRAGMA journal_mode = WAL;
                        PRAGMA auto_vacuum = 2;
                        """
                ).execute();

                // 重新连接数据库
                reconnect();

                // 是否启用大小写不敏感
                String Name_COLLATE_NOCASE =
                        getPlugin().getConfig().getBoolean("sql.Name_COLLATE_NOCASE", true)
                                ? "COLLATE NOCASE" : "";

                // 加载数据表
                connection.prepareStatement("""
                        CREATE TABLE IF NOT EXISTS "player" (
                            "ID" INTEGER NOT NULL,
                            "Type" INTEGER NOT NULL,
                            "UUID" TEXT NOT NULL,
                            "Name" TEXT NOT NULL %s,
                            "Time" INTEGER NOT NULL,
                            PRIMARY KEY("ID" AUTOINCREMENT)
                        );
                        CREATE INDEX IF NOT EXISTS IDX_Type ON "player" (Type);
                        CREATE INDEX IF NOT EXISTS IDX_UUID ON "player" (UUID);
                        CREATE INDEX IF NOT EXISTS IDX_Name ON "player" (Name);
                        """.formatted(Name_COLLATE_NOCASE)
                ).execute();
            }else{
//                connection.prepareStatement("""
//                    CREATE TABLE IF NOT EXISTS `%s` (
//                        `ID` bigint(7) NOT NULL,
//                        `Type` bigint(2) NOT NULL,
//                        `UUID` char(36) NOT NULL,
//                        `Name` varchar(16) NOT NULL,
//                        `Time` bigint(11) NOT NULL,
//
//                        INDEX `IDX_UUID` (`UUID`) USING BTREE,
//                        INDEX `IDX_Name` (`Name`) USING BTREE,
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
     * @param name 玩家名称, 为 null 时不修改
     * @param UUID 玩家 UUID, 为 null 时不修改 (不可同时与名称为空
     * @param time 时间戳. -1=始终有效, -2=不修改, -3=更新为当前时间
     * @param type 白名单类型, 为 null 时不修改
     * @return 账户类型枚举 or ERROR
     */
    public static Type setPlayerData(String name, String UUID, long time, Type type){
        Type out;
        try {
            PreparedStatement sql;
            ResultSet results;

            // 检查是否有 name 和 uuid 匹配的记录
            if(name != null && UUID != null){
                sql = connection.prepareStatement("SELECT * FROM `player` WHERE `Name` = ? AND `UUID` = ? ORDER BY ROWID DESC LIMIT 1;");
                sql.setString(1, name);
                sql.setString(2, UUID);
            }else if(name != null){
                sql = connection.prepareStatement("SELECT * FROM `player` WHERE `Name` = ? ORDER BY ROWID DESC LIMIT 1;");
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
                out = Type.getType(results.getInt("Type"));
                // 处理缺省值
                if(time == -2){
                    time = results.getLong("Time");
                }else if(time == -3){
                    time = System.currentTimeMillis() / 1000;
                }
                int TypeID = type != null ? type.getID() : results.getInt("Type");

                // 已添加, 重置这个ID的 Time Type
                sql = connection.prepareStatement("UPDATE `player` SET `Time` = ?, `Type` = ? WHERE `ID` = ?;");
                sql.setLong(1, time);
                sql.setInt(2, TypeID);
                sql.setInt(3, results.getInt("ID"));
            }else{
                // 输出账户类型
                out = NOT;
                // 处理缺省值
                if(time == -2){
                    time = -1;
                }else if(time == -3){
                    time = System.currentTimeMillis() / 1000;
                }
                int TypeID = type != null ? type.getID() : NOT.getID();

                // 未添加, 创建记录
                sql = connection.prepareStatement("REPLACE INTO `player` (`UUID`, `Name`, `Time`, `Type`) VALUES (?, ?, ?, ?);");
                sql.setString(1, UUID);
                sql.setString(2, name);
                sql.setLong(3, time);
                sql.setInt(4, TypeID);
            }
            sql.execute();
            sql.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }
    // 设置或修改数据
    public static void addPlayer(Player player, long time, Type type){
        setPlayerData(player.getName(), String.valueOf(player.getUniqueId()), time, type);
    }
    // 添加玩家
    public static Type addPlayer(String name, String UUID){
        return setPlayerData(name, UUID, -1, WHITE);
    }
    public static void addPlayer(String name, String UUID, Type type){
        setPlayerData(name, UUID, -1, type);
    }
    public static void addPlayer(Player player, Type type){
        addPlayer(player, -1, type);
    }
    // 移除玩家
    public static Type delPlayerName(String name){
        return setPlayerData(name, null, -2, NOT);
    }
    public static Type delPlayerUUID(String UUID){
        return setPlayerData(null, UUID, -2, NOT);
    }
    // 封禁玩家
    public static Type banPlayerName(String name){
        return setPlayerData(name, null, -2, BLACK);
    }
    public static Type banPlayerUUID(String UUID){
        return setPlayerData(null, UUID, -2, BLACK);
    }


    // 是否在白名单中
    // 除基础数据外, 还会输出 WHITE_EXPIRED
    public static Type isWhitelisted(String playerName, String playerUUID){
        try {
            PreparedStatement sql;
            ResultSet results;

            // 如果UUID匹配
            sql = connection.prepareStatement("SELECT * FROM `player` WHERE `UUID` = ?;");
            sql.setString(1, playerUUID);
            results = sql.executeQuery();
            if(results.next()){

                // 白名单
                if(results.getInt("Type") == WHITE.getID()){
                    // 白名单上的玩家是否超时
                    if(Util.isWhitelistedTimeout(results.getLong("Time"))){return WHITE_EXPIRED;}

                    // 更新名称和最后加入时间
                    PreparedStatement update = connection.prepareStatement("UPDATE `player` SET `Name` = ?, `Time` = ? WHERE `ID` = ?;");
                    update.setString(1, playerName);
                    update.setLong(2, (System.currentTimeMillis() / 1000));
                    update.setInt(3, results.getInt("ID"));
                    update.executeUpdate();
                    update.close();
                    return WHITE;
                }

                return Type.getType(results.getInt("Type"));
            }

            // 如果名称匹配
            sql = connection.prepareStatement("SELECT * FROM `player` WHERE `Name` = ?;");
            sql.setString(1, playerName);
            results = sql.executeQuery();
            if(results.next()){

                // 如果uuid不为空: 名称相同但uuid不同
                if(!results.getString("UUID").isEmpty()){return NOT;}

                // 白名单
                if(results.getInt("Type") == WHITE.getID()){
                    // 白名单上的玩家是否超时
                    if(Util.isWhitelistedTimeout(results.getLong("Time"))){return WHITE_EXPIRED;}

                    // 更新UUID/名称和最后加入时间
                    PreparedStatement update = connection.prepareStatement("UPDATE `player` SET `UUID` = ?, `Name` = ?, `Time` = ? WHERE `ID` = ?;");
                    update.setString(1, playerUUID);
                    update.setString(2, playerName); // 在第一次加入时处理名称大小写不匹配
                    update.setLong(3, (System.currentTimeMillis() / 1000));
                    update.setInt(4, results.getInt("ID"));
                    update.executeUpdate();
                    update.close();
                    return WHITE;
                }

                return Type.getType(results.getInt("Type"));
            }

            return NOT;
        } catch (Exception e) {
            return ERROR;
        }
    }
    public static Type isWhitelisted(Player player){
        return isWhitelisted(player.getName(), player.getUniqueId().toString());
    }


    // 获取一个玩家的所有数据
    public static ResultSet getPlayerData(Type inpDataType, String inpData){
        String query;

        switch(inpDataType){
            case UUID -> query = "SELECT * FROM `player` WHERE `UUID` = ? ORDER BY ROWID DESC LIMIT 1;";
            case NAME -> query = "SELECT * FROM `player` WHERE `Name` = ? ORDER BY ROWID DESC LIMIT 1;";
            default -> {return null;}
        }

        // 查询
        try {
            PreparedStatement sql = connection.prepareStatement(query);
            sql.setString(1, inpData);
            ResultSet results = sql.executeQuery();
            if(results.next()){
                return results;
            }
            sql.close();
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
        return null;
    }
    // 获取玩家 Type
    public static Type getPlayerType(Type inpDataType, String inpData){
        ResultSet results = getPlayerData(inpDataType, inpData);
        if(results == null){return NOT;}
        try {
            return Type.getType(results.getInt("Type"));
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
        return NOT;
    }
    // 获取玩家 NAME
    public static String getPlayerName(String UUID){
        ResultSet results = getPlayerData(Type.UUID, UUID);
        if(results == null){return null;}
        try {
            return results.getString("Name");
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
        return null;
    }
    // 获取玩家 UUID
    public static String getPlayerUUID(String Name){
        ResultSet results = getPlayerData(Type.NAME, Name);
        if(results == null){return null;}
        try {
            return results.getString("UUID");
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
        return null;
    }

}
