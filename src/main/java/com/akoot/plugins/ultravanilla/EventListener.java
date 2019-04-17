package com.akoot.plugins.ultravanilla;

import com.akoot.plugins.ultravanilla.commands.PingCommand;
import com.akoot.plugins.ultravanilla.reference.Palette;
import com.akoot.plugins.ultravanilla.reference.Users;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventListener implements Listener {

    private Ultravanilla plugin;

    public EventListener(Ultravanilla instance) {
        this.plugin = instance;
    }

    @EventHandler
    public void onListPing(ServerListPingEvent event) {
        OfflinePlayer[] offlinePlayers = plugin.getServer().getOfflinePlayers();
        OfflinePlayer offlinePlayer = offlinePlayers[plugin.getRandom().nextInt(offlinePlayers.length)];
        assert offlinePlayer.getName() != null;
        String name = offlinePlayer.getName();
        event.setMotd(Palette.translate(plugin.getConfig().getString("server-name")) + "\n" + ChatColor.RESET + plugin.getMOTD().replace("%player", name));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String nick = Ultravanilla.getConfig(player.getUniqueId()).getString(Users.NICKNAME);
        if (nick != null) {
            player.setDisplayName(nick + ChatColor.RESET);
            player.setPlayerListName(nick);
        }
        Ultravanilla.set(player, Users.LAST_LOGIN, System.currentTimeMillis());
        if (!player.hasPlayedBefore()) {
            Ultravanilla.set(player, Users.FIRST_LOGIN, System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player.hasPermission("ultravanilla.command.suicide")) {
            String message = event.getDeathMessage();
            if (message != null && message.endsWith(" died")) {
                List<String> messages = plugin.getConfig().getStringList("suicide-message");
                message = messages.get(plugin.getRandom().nextInt(messages.size()));
                event.setDeathMessage(String.format(message, player.getName()));
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();

        // Chat filter
        String message = event.getMessage();
        if (plugin.getConfig().getBoolean("enable-chat-filter") && !player.hasPermission("ultravanilla.chat.swearing")) {
            String newMessage = "";
            for (String word : message.split(" ")) {
                for (String swear : plugin.getSwearsRegex()) {
                    Pattern p = Pattern.compile(swear);
                    Matcher m = p.matcher(word.toLowerCase());
                    if (m.find()) {
                        List<String> replacements = plugin.getSwears().getStringList(plugin.getSwearsRaw().get(plugin.getSwearsRegex().indexOf(swear)));
                        word = word.toLowerCase().replace(m.group(0), replacements.get(plugin.getRandom().nextInt(replacements.size())));
                    }
                }
                newMessage += word + " ";
            }
            message = newMessage;
        }

        // Chat color
        if (player.hasPermission("ultravanilla.chat.color")) {
            message = Palette.translate(message);
        }

        // Pings
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            String username = p.getName().toLowerCase();
            String name = ChatColor.stripColor(p.getDisplayName()).toLowerCase();
            for (String word : message.split(" ")) {
                if (word.length() >= 3 && word.startsWith("@")) {
                    word = word.substring(1);
                    if (username.contains(word.toLowerCase()) || name.contains(word.toLowerCase())) {
                        if (Ultravanilla.getConfig(p.getUniqueId()).getBoolean(Users.PING_ENABLED, true) || Ultravanilla.isIgnored(player, p)) {
                            String at = PingCommand.COLOR + word + ChatColor.RESET;
                            plugin.ping(p);
                            message = message.replace("@" + word, at);
                        }
                    }
                }
            }
        }

        //ignored
        for (Player p : event.getRecipients()) {
            if (Ultravanilla.isIgnored(p, player)) {
                event.getRecipients().remove(p);
            }
        }

        event.setMessage(message);
    }
}
