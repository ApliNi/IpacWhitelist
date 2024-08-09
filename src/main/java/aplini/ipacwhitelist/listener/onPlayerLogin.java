package aplini.ipacwhitelist.listener;

import aplini.ipacwhitelist.IpacWhitelist;
import aplini.ipacwhitelist.enums.Key;
import aplini.ipacwhitelist.enums.Type;
import aplini.ipacwhitelist.enums.ph;
import aplini.ipacwhitelist.utils.PlayerData;
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
import static aplini.ipacwhitelist.enums.Type.getType;
import static aplini.ipacwhitelist.func.eventFunc.runEventFunc;
import static aplini.ipacwhitelist.hook.authMe.forceLoginPlayer;
import static aplini.ipacwhitelist.hook.authMe.registeredPlayerName;
import static aplini.ipacwhitelist.hook.geyser.isGeyserPlayer;
import static aplini.ipacwhitelist.utils.netReq.isPremiumPlayer;
import static aplini.ipacwhitelist.utils.sql.getPlayerData;
import static aplini.ipacwhitelist.utils.util.*;
import static org.bukkit.event.player.PlayerLoginEvent.Result.KICK_OTHER;

public class onPlayerLogin implements Listener {
    // 退出之后需要等待一段时间才能再次加入
    private static final List<String> playerQuitIng = new ArrayList<>();
    // 参观账户列表
    public static final List<String> visitPlayerList = new ArrayList<>();
    // 白名单玩家列表
    public static final List<String> playerList = new ArrayList<>();


    @EventHandler(priority = EventPriority.LOWEST) // 玩家登录服务器
    public void onPlayerLoginEvent(PlayerLoginEvent event) {

        // 如果结果已被设置, 我们可以忽略这个玩家
        // 同时防止这个事件循环回来 (触发两次)
        if(event.getResult() != PlayerLoginEvent.Result.ALLOWED){
            return;
        }

        // 设置不允许登录的结果, 在允许登录的分支取消它. 防止出错造成意外的玩家登录
        event.disallow(KICK_OTHER, config.getString("message.playerLoginErr", ""));

        Player player = event.getPlayer();
        String playerName = player.getName();
        String playerUUID = player.getUniqueId().toString();
        String playerIP = event.getRealAddress().toString();
        String playerAddressHost = event.getHostname();
        plugin.getLogger().info("%s[%s] -> Server[%s]".formatted(playerName, playerIP, playerAddressHost));


        // 玩家 ip 地址检查
        // 原始ip格式: ipv4: "/127.0.0.1", ipv6: "/0:0:0:0:0:0:0:1" 没有方括号
        if(config.getStringList("whitelist.ipBlacklist").stream().anyMatch(li -> Pattern.matches(li, playerIP))){
            plugin.getLogger().info("%s 在IP黑名单中: %s".formatted(playerName, playerIP));
            event.disallow(KICK_OTHER, msg(config.getString("whitelist.ipBlacklistMsg", ""), playerUUID, playerName)
                    .replace(ph.ip.ph, playerIP));
            return;
        }

        // 限定玩家只能通过以下地址加入服务器
        if(config.getBoolean("whitelist.addressConfig.enable", false)){
            if(config.getStringList("whitelist.addressConfig.list").stream().noneMatch(li -> Pattern.matches(li, playerAddressHost))){
                event.disallow(KICK_OTHER, msg(config.getString("whitelist.addressConfig.kickMsg", ""), playerUUID, playerName));
                return;
            }
        }

        // 玩家名称字符检查
        if(!Pattern.matches(config.getString("whitelist.playerNameRule", ".*"), playerName)){
            event.disallow(KICK_OTHER, msg(config.getString("whitelist.playerNameRuleMsg", ""), playerUUID, playerName));
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
        if(config.getBoolean("whitelist.maxPlayers", true)){
            // 当前玩家数量来自 visitPlayerList 和 playerList
            int nowPlayers = playerList.size();
            if(config.getBoolean("whitelist.maxPlayersIncludesVisit", true)){
                nowPlayers += visitPlayerList.size();
            }
            if(nowPlayers >= Bukkit.getMaxPlayers() && !player.hasPermission("IpacWhitelist.maxPlayer.bypass")){
                event.disallow(KICK_OTHER, config.getString("whitelist.maxPlayersMsg", ""));
                return;
            }
        }

        PlayerData pd = null;

        // 处理重复的数据
        // [!] 这些注释可能和实际的代码不匹配
        // 1.1. 检查是否有 UUID 匹配的数据, 存在则合并内容. 理论上不会产生 UUID 相同的记录, 以防万一
        // 1.2.   如果数据库中没有 UUID 匹配的记录, 则填充玩家信息
        // 1.3.   否则更新玩家名称
        // 2.1. 检查是否有名称匹配的数据
        // 2.2.   检查是否存在名称相同, 但 UUID 不同的数据
        // 2.3.   存在则合并内容
        // 3. 检查是否有(与新)名称匹配但 UUID 为空的数据, 存在则使用数据库中的记录. 否则会使用第二步产生的数据

        // 如果存在多个 UUID, 则转换数据, 并删除另一个
        List<PlayerData> pdsForUUID = sql.getPlayerDataList(playerUUID, null, true, true);
        for(PlayerData li : pdsForUUID){
            // 初始化第一个数据
            if(pd == null){
                pd = li;
                continue;
            }
            // 对比和转换数据
            pd.compareAndConvert(li);
            // 删除多余记录
            li.delete();
        }

        // 如果不存在 uuid 相同的数据, 则新建记录并填充玩家信息
        if(pd == null){
            pd = new PlayerData();
            pd.setPlayerInfo(playerUUID, playerName);
        }else{
            // 我们始终需要更新玩家名称
            pd.name = playerName;
        }

        // 检查所有匹配的名称, 如果 id 不同则代表不同的数据, 进行转换并删除另一个
        List<PlayerData> pdsForName = sql.getPlayerDataList(null, playerName, true, true);
        for(PlayerData li : pdsForName){
            // 排除 id 相同的记录
            if(li.id == pd.id){continue;}

            // 如果 Type 和 Ban 均为 NOT 则将这条记录删除, 然后这不属于名称重复
            // 处理名称重复, 但被标记为已删除的数据. 通过 /wl del 产生
            if(li.type == Type.NOT && li.ban == Type.NOT){
                // 可以选择删除或仅忽略, 我们准备好了对于这些数据的"保护", 应避免任何可能有用的数据被删除
                // 如果遇到出乎意料的错误, 可以尝试开启自动删除
//                li.delete();
                continue;
            }
            // 如果 UUID 为空, 则可以合并
            if(li.uuid.isEmpty()){
                // 如果 pd 不在数据库中, 则使用数据库中匹配的记录
                if(pd.isNull()){
                    pd = li;
                    pd.setPlayerInfo(playerUUID, playerName);
                    continue;
                }
                // 对比和转换数据
                pd.compareAndConvert(li);
                // 删除多余记录
                li.delete();
            }
            // 检查是否存在名称相同, 但 UUID 不同的数据
            // 主要用于解决部分同时支持正版和离线账户服务器上有时没有正确应用正版 UUID 的错误
            else if(!li.uuid.equals(playerUUID)){
                if(config.getBoolean("whitelist.preventNameDuplication", true)){
                    event.disallow(KICK_OTHER, msg(config.getString("whitelist.preventNameDuplicationMsg", ""), playerUUID, playerName));
                    return;
                }
            }
        }

        // 根据玩家登录来源自动添加到白名单
        if(config.getBoolean("whitelist.AutoWL.enable", false) && pd.type == Type.NOT){

            Type onGeyserPlayer = (Type) SEL(getType(config.getString("whitelist.AutoWL.onGeyserPlayer", "NOT")), Type.NOT);
            Type onPremiumPlayer = (Type) SEL(getType(config.getString("whitelist.AutoWL.onPremiumPlayer", "NOT")), Type.NOT);
            Type onOtherPlayer = (Type) SEL(getType(config.getString("whitelist.AutoWL.onOtherPlayer", "NOT")), Type.NOT);

            // 检查基岩版玩家
            if(onGeyserPlayer != Type.NOT && isGeyserPlayer(player.getUniqueId())){
                pd.type = onGeyserPlayer;
            }
            // 检查正版账户
            else if(onPremiumPlayer != Type.NOT){
                Key data = isPremiumPlayer(playerName, playerUUID);
                if(data == Key.ERR){
                    event.disallow(KICK_OTHER, msg(config.getString("whitelist.AutoWL.onPremiumPlayerErrMsg", ""), playerUUID, playerName));
                }else if(data == Key.TRUE){
                    pd.type = onPremiumPlayer;
                }
            }
            // 其他的
            else if(onOtherPlayer != Type.NOT){
                pd.type = onOtherPlayer;
            }

            if(pd.type != Type.NOT) {
                plugin.getLogger().info("[AutoWL] 玩家 " + playerName + " 已设置白名单 "+ pd.type.name);
            }

            // 处理 Ban 属性
            if(pd.type == Type.BAN){
                pd.type = Type.NOT;
                pd.ban = Type.BAN;
            }
        }


        // 被封禁的账户
        if(pd.ban == Type.BAN){
            event.disallow(KICK_OTHER, msg(config.getString("whitelist.BAN.kickMsg", ""), playerUUID, playerName));
            return;
        }
        // 白名单逻辑
        switch(pd.type){
            // 参观账户不在白名单中
            case VISIT, NOT -> {
                // 未启用参观账户
                if(!config.getBoolean("whitelist.visitEnable", true)){
                    event.disallow(KICK_OTHER, msg(config.getString("whitelist.NOT.notMsg", ""), playerUUID, playerName));
                    return;
                }

                // 限定参观模式可使用的地址
                if(config.getBoolean("whitelist.VISIT.addressConfig.enable", false)){
                    if(config.getStringList("whitelist.VISIT.addressConfig.list").stream().noneMatch(li -> Pattern.matches(li, playerAddressHost))){
                        event.disallow(KICK_OTHER, msg(config.getString("whitelist.VISIT.addressConfig.kickMsg", ""), playerUUID, playerName));
                        return;
                    }
                }

                // 玩家名称字符检查
                if(!Pattern.matches(config.getString("whitelist.VISIT.playerNameRule", ".*"), playerName)){
                    event.disallow(KICK_OTHER, msg(config.getString("whitelist.VISIT.playerNameRuleMsg", ""), playerName, playerUUID));
                    return;
                }

                // 参观账户人数限制
                if(visitPlayerList.size() >= config.getInt("whitelist.VISIT.maxPlayers", 0)){
                    event.disallow(KICK_OTHER, config.getString("whitelist.VISIT.maxPlayersMsg", ""));
                    return;
                }

                // 参观账户第一次加入服务器, 创建参观账户数据
                if(pd.type == Type.NOT){
                    // 参观账户第一次登录服务器
                    runEventFunc("whitelist.VISIT.onNewPlayerLoginEvent", player, playerUUID, playerName);
                    pd.type = Type.VISIT;
                    plugin.getLogger().info("为新的参观账户创建数据: "+ playerName);
                }

                // 参观账户每次登录服务器
                runEventFunc("whitelist.VISIT.onPlayerLoginEvent", player, playerUUID, playerName);

                event.allow();
            }

            // 需要进行转换的参观账户
            case VISIT_CONVERT -> {
                // Type = WHITE 会在玩家加入时进行
                // 在登录过程中转换参观账户
                runEventFunc("whitelist.VISIT_CONVERT.onPlayerLoginEvent", player, playerUUID, playerName);
                event.allow();
            }

            // 白名单
            case WHITE -> {
                // 白名单超时
                if(pd.time < getTime() - config.getLong("whitelist.WHITE.timeOut", 18394560)){
                    event.disallow(KICK_OTHER, config.getString("whitelist.WHITE.timeOutMsg", ""));
                    return;
                }
                event.allow();
            }

            default -> {
                plugin.getLogger().warning("出现未知的错误: 不存在有效数据的玩家登录服务器: "+ player.getUniqueId());
                event.disallow(KICK_OTHER, config.getString("message.playerLoginErr", ""));
                return;
            }
        }

        // 保存玩家数据
        // 以上代码通过 return 来绕过这里的保存操作, 只有成功加入游戏才需要保存
        pd.save();
    }

    @EventHandler(priority = EventPriority.LOWEST) // 玩家加入服务器
    public void onPlayerJoinEvent(PlayerJoinEvent event){
        Player player = event.getPlayer();
        PlayerData pd = getPlayerData(player, true);
        switch(pd.type){
            case VISIT -> {
                // 记录在线的参观账户
                visitPlayerList.add(pd.uuid);
                // 为参观账户注册账号
                if(config.getBoolean("whitelist.VISIT.AuthMePlugin.autoRegisterAndLogin", true)){
                    if(registeredPlayerName(pd.name)){
                        plugin.getLogger().info("为参观账户 "+ player.getName() +" 注册账号");
                    }
                    // by games647
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                            forceLoginPlayer(player);
                        });
                        // delay the login process to let auth plugins initialize the player
                        // Magic number however as there is no direct event from those plugins
                    }, 10);
                }
                // 参观账户加入事件
                runEventFunc("whitelist.VISIT.onPlayerJoinEvent", player);
                plugin.getLogger().info(pd.name +" 以参观模式加入服务器");
            }
            case VISIT_CONVERT -> {
                // 在加入过程中转换参观账户
                runEventFunc("whitelist.VISIT_CONVERT.onPlayerJoinEvent", player);
                // 在这里设置为白名单
                pd.type = Type.WHITE;
                pd.save();
                // 记录在线玩家
                playerList.add(pd.uuid);
            }
            case WHITE -> {
                // 记录在线玩家
                playerList.add(pd.uuid);
            }
            default -> {
                plugin.getLogger().warning("出现未知的错误: 不存在有效数据的玩家加入服务器: "+ pd.id +": "+ pd.type.name +": "+ player.getUniqueId());
                player.kickPlayer(config.getString("message.playerLoginErr", ""));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // 玩家退出
    public void onPlayerQuit(PlayerQuitEvent event){
        CompletableFuture.runAsync(() -> {
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
                    // 玩家退出事件
                    runEventFunc("whitelist.WHITE.onPlayerQuitEvent", player, pd.uuid, pd.name);
                }
                // 玩家可能因为 del / ban 等操作被退出服务器, 但不需要在这里进行处理
                // default -> {}
            }

            // 玩家退出后等待指定时间才能重新连接
            playerQuitIng.add(pd.uuid);
            try {
                TimeUnit.MILLISECONDS.sleep(plugin.getConfig().getInt("whitelist.repeatJoinInterval", 1200));
            } catch (InterruptedException ignored) {}
            playerQuitIng.remove(pd.uuid);
        });
    }
}
