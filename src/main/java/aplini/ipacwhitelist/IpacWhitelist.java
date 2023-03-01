package aplini.ipacwhitelist;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IpacWhitelist extends JavaPlugin implements Listener {
    private static IpacWhitelist plugin;

    @Override
    public void onLoad() {
        plugin = this;
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        plugin.getConfig();
    }

    public void onEnable() {
//        SQL.openConnection();

        if(!getConfig().getString("sql.jdbc_driver", "").equals("")){
            try {
                Class.forName(getConfig().getString("sql.jdbc_driver"));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        SQL.initialize();

        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(plugin.getCommand("wl")).setExecutor(this);
    }

    public void onDisable() {
//        SQL.closeConnection();
    }


    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (args[0]) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(plugin.getConfig().getString("message.reload", "message.reload"));

                break;
            case "add":
                if (args.length == 2) {
                    if (args[1].length() > 16) {
                        sender.sendMessage(plugin.getConfig().getString("message.err-name-length", "message.err-name-length"));
                        return true;
                    }

                    if (SQL.addPlayer(args[1])) {
                        sender.sendMessage(plugin.getConfig().getString("message.add-ok", "message.add-ok")
                                .replace("%player%", args[1]));
                    } else {
                        sender.sendMessage(plugin.getConfig().getString("message.err-sql", "message.err-sql"));
                    }

                    return true;
                }

                else if (args.length >= 3) {
                    if (args[1].length() > 16) {
                        sender.sendMessage(plugin.getConfig().getString("message.err-name-length", "message.err-name-length"));
                        return true;
                    }
                    if (args[2].length() != 36) {
                        sender.sendMessage(plugin.getConfig().getString("message.err-uuid-length", "message.err-uuid-length"));
                        return true;
                    }

                    if (SQL.addPlayer(args[1], args[2])) {
                        sender.sendMessage(plugin.getConfig().getString("message.add-ok", "message.add-ok")
                                .replace("%player%", args[1]));
                    } else {
                        sender.sendMessage(plugin.getConfig().getString("message.err-sql", "message.err-sql"));
                    }
                    return true;
                }

                sender.sendMessage("/wl add <Name> [UUID]");
                break;
            case "del":
                if (args[1].length() == 36) {
                    if (SQL.delPlayerUUID(args[1])) {
                        sender.sendMessage(plugin.getConfig().getString("message.add-ok", "message.add-ok")
                                .replace("%player%", args[1]));
                    } else {
                        sender.sendMessage(plugin.getConfig().getString("message.err-sql", "message.err-sql"));
                    }
                } else {
                    if (args[1].length() > 16) {
                        sender.sendMessage(plugin.getConfig().getString("message.err-name-length", "message.err-name-length"));
                        return true;
                    }
                    if (SQL.delPlayerName(args[1])) {
                        sender.sendMessage(plugin.getConfig().getString("message.del-ok", "message.del-ok")
                                .replace("%player%", args[1]));
                    } else {
                        sender.sendMessage(plugin.getConfig().getString("message.err-sql", "message.err-sql"));
                    }
                }

                sender.sendMessage("/wl del <Name|UUID>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sendermm, Command command, String label, String[] args) {
        if(args.length == 1){
            List<String> list = new ArrayList<>();
            list.add("reload");
            list.add("add");
            list.add("del");
            return list;
        }
        return null;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        int success = SQL.isWhitelisted(event.getPlayer());
        if (success == 0) {
            event.setKickMessage(plugin.getConfig().getString("message.not", "message.not")
                    .replace("%player%", event.getPlayer().getName()));
            event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
        }else if(success == 2){
            event.setKickMessage(plugin.getConfig().getString("message.err-sql-player-join", "message.err-sql-player-join"));
            event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
        }

        event.getPlayer().setDisplayName("aaa");
    }

    public static IpacWhitelist getPlugin() {
        return plugin;
    }
}
