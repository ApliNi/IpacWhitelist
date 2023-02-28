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
        SQL.openConnection();
        SQL.initialize();

        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(plugin.getCommand("wl")).setExecutor(this);
    }

    public void onDisable() {
        SQL.closeConnection();
    }


    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 2) {
            if (args[0].equals("add")) {
                if(args[1].length() > 16){
                    sender.sendMessage(plugin.getConfig().getString("message.err-name-length", "message.err-name-length"));
                    return true;
                }
                SQL.addPlayer(args[1]);
                sender.sendMessage(plugin.getConfig().getString("message.add-ok", "message.add-ok")
                        .replace("%player%", args[1]));
                return true;
            }
            else if (args[0].equals("del")) {
                if(args[1].length() == 36){
                    SQL.delPlayerUUID(args[1]);
                }else{
                    if(args[1].length() > 16){
                        sender.sendMessage(plugin.getConfig().getString("message.err-name-length", "message.err-name-length"));
                        return true;
                    }
                    SQL.delPlayerName(args[1]);
                }
                sender.sendMessage(plugin.getConfig().getString("message.del-ok", "message.del-ok")
                        .replace("%player%", args[1]));
                return true;
            }
        }

        else if (args.length == 3){
            if (args[0].equals("add")) {
                if(args[1].length() > 16){
                    sender.sendMessage(plugin.getConfig().getString("message.err-name-length", "message.err-name-length"));
                    return true;
                }
                if(args[2].length() != 36){
                    sender.sendMessage(plugin.getConfig().getString("message.err-uuid-length", "message.err-uuid-length"));
                    return true;
                }
                SQL.addPlayer(args[1], args[2]);
                sender.sendMessage(plugin.getConfig().getString("message.add-ok", "message.add-ok")
                        .replace("%player%", args[1]));
                return true;
            }
        }

        sender.sendMessage("/wl add <Name> [UUID]");
        sender.sendMessage("/wl del <Name|UUID>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sendermm, Command command, String label, String[] args) {
        if(args.length == 1){
            List<String> list = new ArrayList<>();
            list.add("add");
            list.add("del");
            return list;
        }
        return null;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!SQL.isWhitelisted(event.getPlayer())) {
            event.setKickMessage(plugin.getConfig().getString("message.not", "message.not")
                    .replace("%player%", event.getPlayer().getName()));
            event.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
        }
        SQL.setPlayerTime(event.getPlayer().getUniqueId().toString(), System.currentTimeMillis() / 1000);
    }

    public static IpacWhitelist getPlugin() {
        return plugin;
    }
}
