package aplini.ipacwhitelist.util;

import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
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


    // 打包一个玩家的数据， 从数据库到 PlayerData 类型
    public static PlayerData packPlayerData(ResultSet results){
        PlayerData pd = new PlayerData();
        // 查询
        try {
            pd.ID = results.getInt("ID");
            pd.Type = getType(results.getInt("Type"));
            pd.Ban = getBan(results.getInt("Ban"));
            pd.UUID = results.getString("UUID");
            pd.Name = results.getString("Name");
            pd.Time = results.getLong("Time");
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
        return pd;
    }


    // 获取一个玩家的所有数据
    public static PlayerData getPlayerData(Type inpDataType, String inpData){
        String query;
        PlayerData pd = new PlayerData();

        switch(inpDataType){
            case DATA_UUID -> query = "SELECT * FROM `player` WHERE `UUID` = ? LIMIT 1;";
            case DATA_NAME -> query = "SELECT * FROM `player` WHERE `Name` = ? LIMIT 1;";
            case DATA_NAME_LIMIT_EMPTY_UUID -> query = "SELECT * FROM `player` WHERE `UUID` = '' AND `Name` = ? LIMIT 1;";
            default -> {return pd;}
        }

        // 查询
        try {
            PreparedStatement sql = connection.prepareStatement(query);
            sql.setString(1, inpData);
            pd = packPlayerData(sql.executeQuery());
            sql.close();
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
        return pd;
    }

    // 删除指定 ID 的数据
    public static void delPlayerData(int id){
        try {
            PreparedStatement sql = connection.prepareStatement("DELETE FROM `player` WHERE `ID` = ?;");
            sql.setInt(1, id);
            sql.execute();
            sql.close();
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
    }

    // 是否在白名单中
    // 如果数据库中有数据, 才会更新 Time/UUID/Name. 否则返回 NOT
    public static PlayerData isInWhitelisted(String playerName, String playerUUID){

        PlayerData pd;

        // 如果 UUID 匹配
        pd = getPlayerData(DATA_UUID, playerUUID);
        if(!pd.isNull()){
            // 黑名单
            if(pd.Ban == BAN){return pd.whitelistedState(BAN);}
            // 非参观账户 && 白名单过期
            if(!isVisit(pd.Type) && Util.isWhitelistedExpired(pd.Time)){return pd.whitelistedState(WHITE_EXPIRED);}
            // 如果启用错误检查
            if(getPlugin().getConfig().getBoolean("whitelist.autoClean.enable", true)){
                // 检查是否存在一个 UUID 为空, 名称相同的数据
                PlayerData pd2 = getPlayerData(DATA_NAME_LIMIT_EMPTY_UUID, playerName);
                if(pd2.ID != -1){
                    // 如果启用按权重转移数据
                    if(getPlugin().getConfig().getBoolean("whitelist.autoClean.dataByWeight", true)){
                        // 比较这两条记录的关键数据, 保留 int 最大的一方
                        pd.Type = pd2.Type.getID() > pd.Type.getID() ? pd2.Type : pd.Type;
                        pd.Ban = pd2.Ban.getID() > pd.Ban.getID() ? pd2.Ban : pd.Ban;
                    }
                    delPlayerData(pd2.ID);
                }
            }
            // 更新数据
            pd.Name = playerName;
            pd.Time = getTime();
            pd.save();
            return pd.whitelistedState(pd.Type);
        }

        // 如果 Name 匹配, 且 UUID 为空
        pd = getPlayerData(DATA_NAME_LIMIT_EMPTY_UUID, playerName);
        if(!pd.isNull()){
            // 黑名单
            if(pd.Ban == BAN){return pd.whitelistedState(BAN);}
            // 非参观账户 && 白名单过期
            if(!isVisit(pd.Type) && Util.isWhitelistedExpired(pd.Time)){return pd.whitelistedState(WHITE_EXPIRED);}
            // 更新数据
            pd.UUID = playerUUID;
            pd.Time = getTime();
            pd.save();
            return pd.whitelistedState(pd.Type);
        }

        return pd.whitelistedState(NOT);
    }
    public static PlayerData isInWhitelisted(Player player){
        return isInWhitelisted(player.getName(), player.getUniqueId().toString());
    }


    // 遍历数据
    public interface whileDataForListInterface {
        void test(PlayerData pd);
    }
    public interface whileDataForListInterfaceEnd {
        void test();
    }
    public static void whileDataForList(PreparedStatement sql, whileDataForListInterface func, whileDataForListInterfaceEnd funcEnd){
        // 查询
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                ResultSet results = sql.executeQuery();
                while(results.next()){
                    PlayerData pd = packPlayerData(results);
                    func.test(pd);
                }
                sql.close();
            } catch (Exception e) {
                getLogger().warning(e.getMessage());
            }
        });
        future.join(); // 等待运行完毕

        // 运行结束
        funcEnd.test();
    }
    public static void whileDataForList(Type type, int maxLine, whileDataForListInterface func, whileDataForListInterfaceEnd funcEnd){
        String query;
        String limit = maxLine != -1 ? ("LIMIT "+ maxLine) : "";

        query = switch (type) {
            case ALL -> "SELECT * FROM `player` %s;".formatted(limit);
            default -> "SELECT * FROM `player` WHERE (`Type` = %s) %s;".formatted(type.getID(), limit);
        };

        try {
            PreparedStatement sql = connection.prepareStatement(query);
            whileDataForList(sql, func, funcEnd);

        } catch (SQLException e) {
            getLogger().warning(e.getMessage());
        }
    }


    // 保存玩家数据
    public static void savePlayerData(PlayerData pd){
        // 处理缺省数据
        pd.Time = pd.Time == -3 ? getTime() : pd.Time;

        try {
            PreparedStatement sql;
            int i = 0;
            // 如果id存在则更新数据, 否则创建新数据
            if(pd.ID != -1){
                sql = connection.prepareStatement("UPDATE `player` SET `Type` = ?, `Ban` = ?, `UUID` = ?, `Name` = ?, `Time` = ? WHERE `ID` = ?;");
                sql.setInt(++i, pd.Type.getID());
                sql.setInt(++i, pd.Ban.getID());
                sql.setString(++i, pd.UUID);
                sql.setString(++i, pd.Name);
                sql.setLong(++i, pd.Time);
                sql.setInt(++i, pd.ID);
            }else{
                sql = connection.prepareStatement("REPLACE INTO `player` (`Type`, `Ban`, `UUID`, `Name`, `Time`) VALUES (?, ?, ?, ?, ?);");
                sql.setInt(++i, pd.Type.getID());
                sql.setInt(++i, pd.Ban.getID());
                sql.setString(++i, pd.UUID);
                sql.setString(++i, pd.Name);
                sql.setLong(++i, pd.Time);
            }
            sql.execute();
            sql.close();
        } catch (Exception e) {
            getLogger().warning(e.getMessage());
        }
    }

}
