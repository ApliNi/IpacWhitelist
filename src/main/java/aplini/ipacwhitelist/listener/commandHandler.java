package aplini.ipacwhitelist.listener;

import aplini.ipacwhitelist.listener.cmd.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static aplini.ipacwhitelist.IpacWhitelist.config;

public class commandHandler implements Listener, CommandExecutor, TabCompleter {
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args){

        // 如果没有主权限, 则什么都不能运行
        if(!sender.hasPermission("IpacWhitelist.cmd")){
            return List.of("");
        }

        // 返回子命令列表
        if(args.length == 1){
            return List.of(
                    "reload",
                    "add",
                    "del",
                    "ban",
                    "unban",
                    "info",
                    "list",
                    "clear",
                    "importData"
            );
        }

        return (switch(args[0].toLowerCase()){

            case "add" -> add.tab(sender, args);
            case "del" -> del.tab(sender, args);
            case "ban" -> ban.tab(sender, args);
            case "unban" -> unban.tab(sender, args);
            case "info" -> info.tab(sender, args);
            case "list" -> list.tab(sender, args);
            case "clear" -> clear.tab(sender, args);

            default -> null;
        });
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args){

        // 对 RCON 发送者使用同步执行
        if(sender instanceof RemoteConsoleCommandSender){
            // 同步执行命令处理
            executeCommand(sender, args);
        }else{
            // 异步处理指令
            CompletableFuture.runAsync(() -> executeCommand(sender, args));
        }
        return true;
    }

    // 提取命令执行逻辑到单独的方法
    private void executeCommand(CommandSender sender, String[] args){
        // 如果没有主权限, 则什么都不能运行
        if(!sender.hasPermission("IpacWhitelist.cmd")){
            sender.sendMessage(config.getString("message.noPermission", ""));
            return;
        }

        if(args.length == 0){
            sender.sendMessage("""
                    IpacEL > IpacWhitelist 白名单
                      指令:
                        - /wl reload    - 重载插件
                        - /wl add <Name|UUID>   - 添加到白名单
                        - /wl del <Name|UUID>   - 从白名单移出
                        - /wl ban <Name|UUID>   - 封禁一个玩家
                        - /wl unban <Name|UUID> - 解除封禁玩家
                        - /wl info <Name|UUID>  - 显示玩家信息
                        - /wl list <Type>       - 查询玩家数据
                        - /wl clear PLAYER|TYPE <Name|UUID|Type>  - 清除数据
                        - /wl importData        - 导入数据
                    """);
            return;
        }

        switch(args[0].toLowerCase()){

            case "reload" -> reload.cmd(sender);
            case "add" -> add.cmd(sender, args);
            case "del" -> del.cmd(sender, args);
            case "ban" -> ban.cmd(sender, args);
            case "unban" -> unban.cmd(sender, args);
            case "info" -> info.cmd(sender, args);
            case "list" -> list.cmd(sender, args);
            case "clear" -> clear.cmd(sender, args);
            case "importdata" -> importData.cmd(sender, args);
        }
    }
}