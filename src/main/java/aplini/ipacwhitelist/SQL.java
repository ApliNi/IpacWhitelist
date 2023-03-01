package aplini.ipacwhitelist;

import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static aplini.ipacwhitelist.IpacWhitelist.getPlugin;

public class SQL {

    public static Connection connection;

    // 连接
    public static synchronized void openConnection() {
        String db = getPlugin().getConfig().getString("sql.db", "sqlite");
        String jdbc;

        if(db.equalsIgnoreCase("sqlite") || db.equalsIgnoreCase("h2")){
            jdbc = "jdbc:"+ db +":"+ new File(getPlugin().getDataFolder(), "database.db").getAbsolutePath();
        }else{
            jdbc = "jdbc:"+ db +"://" +
                    getPlugin().getConfig().getString("sql.host") + ":" +
                    getPlugin().getConfig().getString("sql.port") + "/" +
                    getPlugin().getConfig().getString("sql.database");
        }

        try {
            connection = DriverManager.getConnection(jdbc,
                    getPlugin().getConfig().getString("sql.user"),
                    getPlugin().getConfig().getString("sql.password"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 断开
    public static synchronized void closeConnection() {
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 初始化数据库
    public static synchronized void initialize() {
        openConnection();
        try {
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS `" + getPlugin().getConfig().getString("sql.table") + "` (" +
                            "`UUID` char(36) NOT NULL UNIQUE, " +
                            "`NAME` varchar(16) NOT NULL UNIQUE, " +
                            "`TIME` bigint(11) NOT NULL, " +
                            "`WHITE` boolean NOT NULL" +
                            ");").execute();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    // 添加玩家
    public static boolean addPlayer(String name, String UUID){
        openConnection();
        try {
            PreparedStatement sql = connection.prepareStatement(
                    "REPLACE INTO `"+ getPlugin().getConfig().getString("sql.table") +"` (`UUID`, `NAME`, `TIME`, `WHITE`) VALUES (?, ?, ?, ?);");
            sql.setString(1, UUID);
            sql.setString(2, name);
            sql.setInt(3, 0);
            sql.setBoolean(4, true);
            sql.execute();
            sql.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
        return true;
    }
    public static boolean addPlayer(String name){
        return addPlayer(name, "");
    }

    // 删除玩家, 通过名称
    public static boolean delPlayerName(String name){
        openConnection();
        try {
            PreparedStatement sql = connection.prepareStatement(
                    "UPDATE `"+ getPlugin().getConfig().getString("sql.table") +"` SET `WHITE` = ? WHERE `NAME` = ?;");
            sql.setBoolean(1, false);
            sql.setString(2, name);
            sql.executeUpdate();
            sql.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
        return true;
    }

    // 删除玩家, 通过UUID
    public static boolean delPlayerUUID(String UUID){
        openConnection();
        try {
            PreparedStatement sql = connection.prepareStatement(
                    "UPDATE `"+ getPlugin().getConfig().getString("sql.table") +"` SET `WHITE` = ? WHERE `UUID` = ?;");
            sql.setBoolean(1, false);
            sql.setString(2, UUID);
            sql.executeUpdate();
            sql.close();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            closeConnection();
        }
        return true;
    }

    // 是否在白名单中
    public static int isWhitelisted(Player player){
        openConnection();
        try {
            PreparedStatement sql;
            ResultSet results;

            // 如果UUID匹配
            sql = connection.prepareStatement(
                    "SELECT * FROM `" + getPlugin().getConfig().getString("sql.table") + "` WHERE `WHITE` = true AND `UUID` = ?;");
            sql.setString(1, player.getUniqueId().toString());
            results = sql.executeQuery();
            if(results.next()){
                // 更新名称和最后加入时间
                try {
                    PreparedStatement sql2 = connection.prepareStatement(
                            "UPDATE `"+ getPlugin().getConfig().getString("sql.table") +"` SET `NAME` = ?, `TIME` = ? WHERE `UUID` = ?;");
                    sql2.setString(1, player.getName());
                    sql2.setLong(2, System.currentTimeMillis() / 1000);
                    sql2.setString(3, player.getUniqueId().toString());
                    sql2.executeUpdate();
                    sql2.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 1;
            }

            // 如果名称匹配
            sql = connection.prepareStatement(
                    "SELECT * FROM `" + getPlugin().getConfig().getString("sql.table") + "` WHERE `WHITE` = true AND `NAME` = ?;");
            sql.setString(1, player.getName());
            results = sql.executeQuery();
            if(results.next()){
                // 更新UUID/名称和最后加入时间
                try {
                    PreparedStatement sql2 = connection.prepareStatement(
                            "UPDATE `"+ getPlugin().getConfig().getString("sql.table") +"` SET `UUID` = ?, `NAME` = ?, `TIME` = ? WHERE `NAME` = ?;");
                    sql2.setString(1, player.getUniqueId().toString());
                    sql2.setString(2, player.getName()); // 在第一次加入时处理名称大小写不匹配
                    sql2.setLong(3, System.currentTimeMillis() / 1000);
                    sql2.setString(4, player.getName());
                    sql2.executeUpdate();
                    sql2.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 1;
            }

            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 2;
        } finally {
            closeConnection();
        }
    }
}
