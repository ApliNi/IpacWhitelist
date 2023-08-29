package aplini.ipacwhitelist.util;

import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static aplini.ipacwhitelist.IpacWhitelist.getPlugin;
import static aplini.ipacwhitelist.util.Type.*;
import static aplini.ipacwhitelist.util.Util.getTime;
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
                            "ID"   INTEGER NOT NULL,
                            "Type" INTEGER NOT NULL,
                            "Ban"  INTEGER NOT NULL,
                            "UUID" TEXT    NOT NULL,
                            "Name" TEXT    NOT NULL %s,
                            "Time" INTEGER NOT NULL,
                            PRIMARY KEY("ID" AUTOINCREMENT)
                        );
                        CREATE INDEX IF NOT EXISTS IDX_Type ON "player" (Type);
                        CREATE INDEX IF NOT EXISTS IDX_Ban  ON "player" (Ban );
                        CREATE INDEX IF NOT EXISTS IDX_UUID ON "player" (UUID);
                        CREATE INDEX IF NOT EXISTS IDX_Name ON "player" (Name);
                        CREATE INDEX IF NOT EXISTS IDX_Time ON "player" (Time);
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
     * @param ban  是否被封禁, 为 null 时不修改
     * @return 修改前的账户类型枚举 or ERROR
     */
    public static Type setPlayerData(String name, String UUID, long time, Type type, Type ban){
        Type out;
        try {
            PreparedStatement sql;
            ResultSet results;

            // 检查是否有 name 和 uuid 匹配的记录
            if(UUID != null){
                sql = connection.prepareStatement("SELECT * FROM `player` WHERE `UUID` = ? LIMIT 1;");
                sql.setString(1, UUID);
                if(name == null){
                    name = "";
                }
            }else if(name != null){
                sql = connection.prepareStatement("SELECT * FROM `player` WHERE `Name` = ? LIMIT 1;");
                sql.setString(1, name);
                UUID = "";
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
                int typeID = type != null ? type.getID() : results.getInt("Type");
                int banID = ban != null ? ban.getID() : results.getInt("Ban");

                // 已添加, 重置这个ID的 Time Type
                sql = connection.prepareStatement("UPDATE `player` SET `Time` = ?, `Type` = ?, `Ban` = ? WHERE `ID` = ?;");
                sql.setLong(1, time);
                sql.setInt(2, typeID);
                sql.setInt(3, banID);
                sql.setInt(4, results.getInt("ID"));
            }else{
                // 输出账户类型
                out = NOT;
                // 处理缺省值
                if(time == -2){
                    time = -1;
                }else if(time == -3){
                    time = System.currentTimeMillis() / 1000;
                }
                int typeID = type != null ? type.getID() : NOT.getID();
                int banID = ban != null ? ban.getID() : NOT_BAN.getID();

                // 未添加, 创建记录
                sql = connection.prepareStatement("REPLACE INTO `player` (`UUID`, `Name`, `Time`, `Type`, `Ban`) VALUES (?, ?, ?, ?, ?);");
                sql.setString(1, UUID);
                sql.setString(2, name);
                sql.setLong(3, time);
                sql.setInt(4, typeID);
                sql.setInt(5, banID);
            }
            sql.execute();
            sql.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    // 添加玩家
    public static void addPlayer(String name, String UUID, Type type){
        setPlayerData(name, UUID, -3, type, null);
    }
    public static void addPlayer(Player player, Type type){
        addPlayer(player.getName(), String.valueOf(player.getUniqueId()), type);
    }

    // 获取一个玩家的所有数据
    public static PlayerData getPlayerData(Type inpDataType, String inpData){
        String query;
        PlayerData pd = new PlayerData();

        switch(inpDataType){
            case DATA_UUID -> query = "SELECT * FROM `player` WHERE `UUID` = ? LIMIT 1;";
            case DATA_NAME -> query = "SELECT * FROM `player` WHERE `Name` = ? LIMIT 1;";
            default -> {return pd;}
        }

        // 查询
        try {
            PreparedStatement sql = connection.prepareStatement(query);
            sql.setString(1, inpData);
            ResultSet results = sql.executeQuery();
            if(results.next()){
                pd.ID = results.getInt("ID");
                pd.Type = getType(results.getInt("Type"));
                pd.Ban = getBan(results.getInt("Ban"));
                pd.UUID = results.getString("UUID");
                pd.Name = results.getString("Name");
                pd.Time = results.getLong("Time");
            }
            sql.close();
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
        return pd;
    }

    // 是否在白名单中
    public static Type isInWhitelisted(String playerName, String playerUUID){

        PlayerData pd;

        // 如果 UUID 匹配
        pd = getPlayerData(DATA_UUID, playerUUID);
        if(!pd.isNull()){
            // 黑名单
            if(pd.Ban == BAN){return BAN;}
            // 白名单
            if(pd.Type == WHITE){
                // 不是参观账户 && 白名单上的玩家超时
                if(!isVisit(pd.Type) && Util.isWhitelistedExpired(pd.Time)){return WHITE_EXPIRED;}
                // 更新数据
                pd.Name = playerName;
                pd.Time = getTime();
                pd.save();
            }
            return pd.Type;
        }

        // 如果 Name 匹配
        pd = getPlayerData(DATA_NAME, playerName);
        if(!pd.isNull()){
            // 如果UUID不为空: 是同名的其他玩家
            if(!pd.UUID.isEmpty()){return NOT;}
            // 黑名单
            if(pd.Ban == BAN){return BAN;}
            // 白名单
            if(pd.Type == WHITE){
                // 不是参观账户 && 白名单上的玩家超时
                if(!isVisit(pd.Type) && Util.isWhitelistedExpired(pd.Time)){return WHITE_EXPIRED;}
                // 更新数据
                pd.UUID = playerUUID;
                pd.Time = getTime();
                pd.save();
            }
            return pd.Type;
        }

        return NOT;
    }
    public static Type isInWhitelisted(Player player){
        return isInWhitelisted(player.getName(), player.getUniqueId().toString());
    }


    // 遍历数据
    public interface whileDataForListInterface {
        void test(ResultSet results);
    }
    public static void whileDataForList(Type type, int maxLine, whileDataForListInterface whileDataForListInterface){
        String query;
        String limit = maxLine != -1 ? ("LIMIT "+ maxLine) : "";

        if(type == ALL){
            query = "SELECT * FROM `player` %s;".formatted(limit);
        }else{
            query = "SELECT * FROM `player` WHERE (`Type` = %s) %s;".formatted(type.getID(), limit);
        }

        // 查询
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                PreparedStatement sql = connection.prepareStatement(query);
                ResultSet results = sql.executeQuery();
                while(results.next()){
                    whileDataForListInterface.test(results);
                }
                sql.close();
            } catch (Exception e) {
                getLogger().warning(e.getMessage());
            }
        });
        executor.shutdown();
    }

}
