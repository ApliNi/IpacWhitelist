package aplini.ipacwhitelist.listener;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.utils.PlayerData;
import aplini.ipacwhitelist.enums.Type;
import aplini.ipacwhitelist.enums.ph;
import aplini.ipacwhitelist.utils.sql;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static aplini.ipacwhitelist.IpacWhitelist.config;
import static aplini.ipacwhitelist.IpacWhitelist.plugin;
import static aplini.ipacwhitelist.func.eventFunc.runEventFunc;
import static aplini.ipacwhitelist.hook.hookAuthMe.authmeLoginPlayer;
import static aplini.ipacwhitelist.hook.hookAuthMe.authmeRegisterPlayer;
import static aplini.ipacwhitelist.utils.sql.getPlayerData;
import static aplini.ipacwhitelist.utils.util.msg;
import static org.bukkit.event.player.PlayerLoginEvent.Result.*;

public class onPlayerLogin implements Listener {
    // 退出之后需要等待一段时间才能再次加入
    private static final List<String> playerQuitIng = new ArrayList<>();
    // 参观账户列表
    public static final List<String> visitPlayerList = new ArrayList<>();
    // 白名单玩家列表
    public static final List<String> playerList = new ArrayList<>();


    @EventHandler(priority = EventPriority.LOWEST) // 玩家登录服务器
    public void onPlayerLoginEvent(PlayerLoginEvent event) {

        // 设置不允许登录的结果, 在允许登录的分支取消它. 防止出错造成意外的玩家登录
        event.disallow(KICK_OTHER, config.getString("message.playerLoginErr", ""));

        Player player = event.getPlayer();
        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();
        String playerIP = event.getRealAddress().toString();
        playerIP = playerIP.substring(playerIP.lastIndexOf("/") +1);


        // 玩家 ip 地址检查
        // 原始ip格式: ipv4: "/127.0.0.1", ipv6: "/0:0:0:0:0:0:0:1" 没有方括号
        for(String li : config.getStringList("whitelist.ipBlacklist")){
            if(Pattern.matches(li, playerIP)){
                plugin.getLogger().info("%s 在IP黑名单中: %s".formatted(playerName, playerIP));
                event.disallow(KICK_BANNED, msg(config.getString("whitelist.ipBlacklistMsg", ""), playerUUID, playerName)
                        .replace(ph.ip.ph, playerIP));
                return;
            }
        }

        // 玩家名称字符检查
        if(!Pattern.matches(config.getString("whitelist.playerNameRule", ".*"), playerName)){
            event.disallow(KICK_BANNED, msg(config.getString("whitelist.playerNameRuleMsg", ""), playerUUID, playerName));
            return;
        }

        // 等待服务器启动完毕
        if(!IpacWhitelist.allowJoin){
            event.disallow(KICK_OTHER, config.getString("whitelist.lateJoinTimeMsg", ""));
            return;
        }

        // 退出之后需要等待一段时间才能再次加入
        if(playerQuitIng.contains(playerUUID)){
            event.disallow(KICK_OTHER, config.getString("whitelist.repeatJoinIntervalMsg", ""));
            return;
        }

        // 最大人数限制
        if(config.getBoolean("whitelist.maxPlayers", false)){
            // 当前玩家数量来自 visitPlayerList 和 playerList
            int nowPlayers = playerList.size() + (config.getBoolean("whitelist.maxPlayersIncludesVisit", true) ? visitPlayerList.size() : 0);
            if(nowPlayers >= Bukkit.getMaxPlayers() && !player.hasPermission("IpacWhitelist.maxPlayer.bypass")){
                event.disallow(KICK_FULL, config.getString("whitelist.maxPlayersMsg", ""));
                return;
            }
        }

        PlayerData pd = null;

        // 处理重复的数据
        // 1. 检查是否有 UUID 匹配的数据, 存在则合并内容. 理论上不会产生 UUID 相同的记录, 以防万一
        // 2.1. 如果数据库中没有 UUID 匹配的记录, 则填充玩家信息
        // 2.2. 更新玩家名称. 正版用户改名
        // 3. 检查是否有(与新)名称匹配但 UUID 为空的数据, 存在则使用数据库中的记录. 否则会使用第二步产生的数据

        // 如果存在多个 UUID, 则转换数据, 并删除另一个
        List<PlayerData> pdsForUUID = sql.getPlayerDataList(playerUUID, null, true, true);
        for(PlayerData li : pdsForUUID){
            // 初始化第一个数据
            if(pd == null){
                pd = li;
                continue;
            }
            // 对比其他数据与第一个, 取其中数值最大的一项进行转换
            if(li.type.num > pd.type.num){
                pd.type = li.type;
            }
            if(li.ban.num > pd.ban.num){
                pd.ban = li.ban;
            }
            // 删除多余记录
            li.delete();
        }

        // 如果不存在 uuid 相同的数据, 则新建记录并填充玩家信息
        if(pd == null){
            pd = new PlayerData();
            pd.setPlayerInfo(playerUUID, playerName);
        }else{
            // 更新玩家名称
            pd.name = playerName;
        }

        // 检查所有匹配的名称, 如果 id 不同则代表不同的数据, 进行转换并删除另一个
        List<PlayerData> pdsForName = sql.getPlayerDataList("", playerName, true, true);
        for(PlayerData li : pdsForName){
            // 如果 pd 不在数据库中, 则使用数据库中匹配的记录
            if(pd.isNull()){
                pd = li;
                continue;
            }
            // 排除 id 相同的记录
            if(li.id != pd.id){continue;}
            // 对比其他数据与第一个, 取其中数值最大的一项进行转换
            if(li.type.num > pd.type.num){
                pd.type = li.type;
            }
            if(li.ban.num > pd.ban.num){
                pd.ban = li.ban;
            }
            // 删除多余记录
            li.delete();
        }

        // 被封禁的账户
        if(pd.ban == Type.BAN){
            event.disallow(KICK_BANNED, msg(config.getString("whitelist.BAN.kickMsg", ""), playerUUID, playerName));
            return;
        }
        // 白名单逻辑
        switch(pd.type){
            // 参观账户不在白名单中
            case VISIT, NOT -> {
                // 未启用参观账户
                if(!config.getBoolean("whitelist.visitEnable", true)){
                    event.disallow(KICK_WHITELIST, msg(config.getString("whitelist.NOT.notMsg", ""), playerUUID, playerName));
                    return;
                }

                // 玩家名称字符检查
                if(!Pattern.matches(config.getString("whitelist.VISIT.playerNameRule", ".*"), playerName)){
                    event.disallow(KICK_BANNED, msg(config.getString("whitelist.VISIT.playerNameRuleMsg", ""), playerName, playerUUID));
                    return;
                }

                // 参观账户人数限制
                if(visitPlayerList.size() >= config.getInt("whitelist.VISIT.maxPlayers", 0)){
                    event.disallow(KICK_FULL, config.getString("whitelist.VISIT.maxPlayersMsg", ""));
                    return;
                }

                // 参观账户第一次加入服务器, 创建参观账户数据
                if(pd.type == Type.NOT){
                    pd.type = Type.VISIT;
                    pd.setPlayerInfo(playerUUID, playerName);
                    pd.save();
                    // 通过 AuthMe 注册账户
                    if(config.getBoolean("whitelist.VISIT.AuthMePlugin.autoRegisterAndLogin", true)){
                        authmeRegisterPlayer(player, config.getString("whitelist.VISIT.AuthMePlugin.autoRegisterPassword", ""));
                    }
                    // 参观账户第一次登录服务器
                    runEventFunc("whitelist.VISIT.onNewPlayerLoginEvent", player, playerUUID, playerName);

                    plugin.getLogger().info("为新的参观账户创建数据: "+ playerName);
                }

                // 参观账户每次登录服务器
                runEventFunc("whitelist.VISIT.onPlayerLoginEvent", player, playerUUID, playerName);

                event.allow();
            }

            // 需要进行转换的参观账户和白名单
            case VISIT_CONVERT, WHITE -> {
                // 如果需要进行账户转换
                if(pd.type == Type.VISIT_CONVERT){
                    runEventFunc("whitelist.VISIT_CONVERT.onPlayerLoginEvent", player, playerUUID, playerName);
                }

                event.allow();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // 玩家加入服务器
    public void onPlayerJoinEvent(PlayerJoinEvent event){
        Player player = event.getPlayer();
        PlayerData pd = getPlayerData(player, true);
        switch(pd.type){
            case VISIT -> {
                // 自动登录
                if(config.getBoolean("whitelist.VISIT.AuthMePlugin.autoRegisterAndLogin", true)){
                    authmeLoginPlayer(player);
                }
                // 记录在线的参观账户
                visitPlayerList.add(pd.uuid);
                // 参观账户加入事件
                runEventFunc("whitelist.VISIT.onPlayerJoinEvent", player);
                plugin.getLogger().info(pd.name +" 以参观模式加入服务器");
            }
            case VISIT_CONVERT, WHITE -> {
                // 如果需要进行账户转换
                if(pd.type == Type.VISIT_CONVERT){
                    runEventFunc("whitelist.VISIT_CONVERT.onPlayerJoinEvent", player);
                }
                // 记录在线玩家
                playerList.add(pd.uuid);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // 玩家退出
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        PlayerData pd = getPlayerData(player, true);
        switch(pd.type){
            case VISIT -> {
                // 记录在线的参观账户
                visitPlayerList.remove(pd.uuid);
                // 参观账户退出事件
                runEventFunc("whitelist.VISIT.onPlayerQuitEvent", player, pd.uuid, pd.name);
            }
            case WHITE -> {
                // 记录在线玩家
                playerList.remove(pd.uuid);
            }
        }

        // 玩家退出后等待指定时间才能重新连接
        playerQuitIng.add(pd.uuid);
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(plugin.getConfig().getInt("whitelist.repeatJoinInterval", 1200));
            } catch (InterruptedException ignored) {}
            playerQuitIng.remove(pd.uuid);
        });
    }
}