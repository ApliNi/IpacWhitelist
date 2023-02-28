package aplini.ipacwhitelist;

import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static aplini.ipacwhitelist.IpacWhitelist.getPlugin;

public class SQL {

    public static Connection connection;

    // 连接
    public static synchronized void openConnection() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" +
                            getPlugin().getConfig().getString("sql.host") + ":" +
                            getPlugin().getConfig().getString("sql.port") + "/" +
                            getPlugin().getConfig().getString("sql.database"),
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
        try {
            connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS `" + getPlugin().getConfig().getString("sql.table") + "` (" +
                            "`UUID` char(36) NOT NULL UNIQUE, " +
                            "`NAME` varchar(16) NOT NULL UNIQUE, " +
                            "`TIME` bigint(11) NOT NULL UNIQUE, " +
                            "`WHITE` boolean NOT NULL UNIQUE" +
                            ");").execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 添加玩家
    public static void addPlayer(String name, String UUID){
        try {
            PreparedStatement sql = connection.prepareStatement(
                    "INSERT INTO `"+ getPlugin().getConfig().getString("sql.table") +"` (`UUID`, `NAME`, `TIME`, `WHITE`) VALUES (?, ?, ?, ?);");
            sql.setString(1, UUID);
            sql.setString(2, name);
            sql.setInt(3, 0);
            sql.setBoolean(4, true);
            sql.execute();
            sql.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void addPlayer(String name){
        addPlayer(name, "");
    }

    // 修改信息
    public static void setPlayerUUID(String name, String UUID){
        try {
            PreparedStatement sql = connection.prepareStatement(
                    "UPDATE `"+ getPlugin().getConfig().getString("sql.table") +"` SET `UUID` = ? WHERE `NAME` = ?;");
            sql.setString(1, UUID);
            sql.setString(2, name);
            sql.executeUpdate();
            sql.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void setPlayerName(String name, String UUID){
        try {
            PreparedStatement sql = connection.prepareStatement(
                    "UPDATE `"+ getPlugin().getConfig().getString("sql.table") +"` SET `NAME` = ? WHERE `UUID` = ?;");
            sql.setString(1, name);
            sql.setString(2, UUID);
            sql.executeUpdate();
            sql.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 更新时间戳
    public static void setPlayerTime(String UUID, Long Time){
        try {
            PreparedStatement sql = connection.prepareStatement(
                    "UPDATE `"+ getPlugin().getConfig().getString("sql.table") +"` SET `TIME` = ? WHERE `UUID` = ?;");
            sql.setLong(1, Time);
            sql.setString(2, UUID);
            sql.executeUpdate();
            sql.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 删除玩家, 通过名称
    public static void delPlayerName(String name){
        try {
            PreparedStatement sql = connection.prepareStatement(
                    "UPDATE `"+ getPlugin().getConfig().getString("sql.table") +"` SET `WHITE` = ? WHERE `NAME` = ?;");
            sql.setBoolean(1, false);
            sql.setString(2, name);
            sql.execute();
            sql.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 删除玩家, 通过UUID
    public static void delPlayerUUID(String UUID){
        try {
            PreparedStatement sql = connection.prepareStatement(
                    "UPDATE `"+ getPlugin().getConfig().getString("sql.table") +"` SET `WHITE` = ? WHERE `UUID` = ?;");
            sql.setBoolean(1, false);
            sql.setString(2, UUID);
            sql.execute();
            sql.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 是否在白名单中
    public static boolean isWhitelisted(Player player){
        try {
            PreparedStatement sql;
            ResultSet results;

            sql = connection.prepareStatement(
                    "SELECT * FROM `" + getPlugin().getConfig().getString("sql.table") + "` WHERE `WHITE` = true AND `UUID` = ?;");
            sql.setString(1, player.getUniqueId().toString());
            results = sql.executeQuery();
            if(results.next()){
                setPlayerName(player.getName(), player.getUniqueId().toString());
                return true;
            }

            sql = connection.prepareStatement(
                    "SELECT * FROM `" + getPlugin().getConfig().getString("sql.table") + "` WHERE `WHITE` = true AND `NAME` = ?;");
            sql.setString(1, player.getName());
            results = sql.executeQuery();
            if(results.next()){
                setPlayerUUID(player.getName(), player.getUniqueId().toString());
                return true;
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
