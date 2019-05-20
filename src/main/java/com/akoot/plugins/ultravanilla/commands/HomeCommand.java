package com.akoot.plugins.ultravanilla.commands;

import com.akoot.plugins.ultravanilla.UltraVanilla;
import com.akoot.plugins.ultravanilla.serializable.Position;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class HomeCommand extends UltraCommand implements CommandExecutor, TabCompleter {

    public static final String HOME_PATH = "data.homes";
    public static final ChatColor COLOR = ChatColor.GRAY;

    public HomeCommand(UltraVanilla plugin) {
        super(plugin);
        this.color = COLOR;
    }

    /**
     * Creates a home if one doesn't exist, replaces a home if it already exits
     *
     * @param player   The user
     * @param name     The name of the home
     */
    public static void setHome(Player player, String name) {

        // Create a new home
        Position home = new Position(player.getLocation());
        home.setName(name);

        // Get home list from config
        List<Position> homes = getHomes(player);

        // Check if there is a valid list, if not create one
        if (homes == null) {
            homes = new ArrayList<>();
        }

        homes.forEach(e -> player.sendMessage(e.getName()));

        // Loop through the home list
        for (Position h : new ArrayList<>(homes)) {

            // Check if a home in the list already has that name
            if (h.getName().equals(name)) {

                // Remove the home if it exists, as it will be re-created later with a new Location
                homes.remove(h);
            }
        }

        // Add the home to the home list
        homes.add(home);
        homes.forEach(e -> player.sendMessage(e.getName()));

        // Save the new home list to config
        UltraVanilla.set(player, HOME_PATH, homes);
    }

    /**
     * Get a user's list of homes
     *
     * @param player The user
     * @return A list of homes a user has. Returns null if they do not have any homes
     */
    public static List<Position> getHomes(Player player) {
        @SuppressWarnings({"unchecked"})
        List<Position> homes = (List<Position>) UltraVanilla.getConfig(player.getUniqueId()).getList(HOME_PATH);
        return homes;
    }

    /**
     * Removes a user's home
     *
     * @param player The user
     * @param name   The name of the home to remove
     * @return Whether or not the home was deleted
     */
    public static boolean removeHome(Player player, String name) {

        // Set a found variable to initially be false
        boolean found = false;

        // Get home list from config
        List<Position> homes = getHomes(player);

        // Check if there is a valid list, if not return null
        if (homes != null) {

            // Loop through the home list
            for (Position h : homes) {

                // Removes the home if it exists & stop looping
                if (h.getName().equals(name)) {
                    homes.remove(h);
                    found = true;
                    break;
                }
            }

            // Check if a home with the specified name was found earlier
            if (found) {

                // Save the new home list to config
                UltraVanilla.set(player, HOME_PATH, homes);
            }
        }
        return found;
    }

    /**
     * Gets the location of a home
     *
     * @param player The user
     * @param name   The name of the home
     * @return The location of the specified home. Returns null of not found
     */
    public static Location getHomeLocation(Player player, String name) {

        // Get home list from config
        List<Position> homes = getHomes(player);

        // Check if there is a valid list, if not return null
        if (homes != null) {

            // Loop through the home list
            for (Position h : homes) {

                // Return the location of the home with the name that matches the one specified
                // If one is not found, return null
                if (h.getName().equals(name)) {
                    return h.getLocation();
                }
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Check if a player executed this command
        if (sender instanceof Player) {

            // Set player & user variables
            Player player = (Player) sender;

            // Get home list from config
            List<Position> homes = getHomes(player);

            // Create an empty home list if one doesn't exist
            if (homes == null) {
                homes = new ArrayList<>();
            }

            // /home
            if (args.length == 0) {

                // Check if the user had homes
                if (!homes.isEmpty()) {

                    // Loop through player homes
                    for (Position home : homes) {

                        // If a player has a "default" home called "home", teleport to it
                        if (home.getName().equals("home")) {
                            player.teleport(home.getLocation());
                            sender.sendMessage(format(command, "message.teleport.home"));
                            return true;
                        }
                    }

                    // Send specify home message
                    sender.sendMessage(format(command, "error.not-found.home"));
                } else {
                    sender.sendMessage(format(command, "error.not-found.any"));
                    return true;
                }
            }

            // /home [name]|set|remove|list
            else if (args.length == 1) {

                // /home set
                if (args[0].equalsIgnoreCase("set")) {

                    // Create a new "default" home with the name "home"
                    setHome(player, "home");
                    sender.sendMessage(format(command, "message.set.home"));
                }

                // /home remove
                else if (args[0].equalsIgnoreCase("remove")) {

                    // Set a default variable "removed" to false
                    boolean removed = false;

                    // Loop through homes
                    for (Position home : homes) {

                        // Check if a home has the "default" name "home"
                        if (home.getName().equals("home")) {

                            // Remove the home if it exists
                            homes.remove(home);
                            removed = true;
                            break;
                        }
                    }

                    // Check if a home named "home" was found earlier
                    if (removed) {
                        sender.sendMessage(format(command, "message.remove.home"));
                    } else {
                        sender.sendMessage(format(command, "error.not-found.any"));
                        return true;
                    }
                }

                // /home remove-all
                else if (args[0].equalsIgnoreCase("remove-all")) {

                    // Check if there are any homes
                    if (!homes.isEmpty()) {

                        // Setting the homes to null will remove the path from config entirely
                        homes = null;
                        sender.sendMessage(format(command, "message.remove.all"));
                    } else {
                        sender.sendMessage(format(command, "error.not-found.any"));
                        return true;
                    }
                }

                // /home list
                else if (args[0].equalsIgnoreCase("list")) {

                    // Check if the user has homes
                    if (!homes.isEmpty()) {

                        // Print out all of the home names
                        sender.sendMessage(plugin.getTitle(format(command, "format.list.title"), color));
                        if (((Player) sender).getBedSpawnLocation() != null) {
                            sender.sendMessage(format(command, "format.list.item", "{name}", "bed"));
                        }
                        for (Position home : homes) {
                            sender.sendMessage(format(command, "format.list.item", "{name}", home.getName()));
                        }
                    } else {
                        sender.sendMessage(format(command, "error.not-found.any"));
                    }
                    return true;
                }

                // /home [name]
                else {

                    // Set the home name
                    String homeName = args[0];

                    if (homeName.equals("bed")) {
                        Location bed = player.getBedSpawnLocation();
                        if (bed != null) {
                            player.teleport(bed);
                            sender.sendMessage(format(command, "message.teleport.bed"));
                            return true;
                        }
                    }

                    // Loop through all of the homes
                    for (Position home : homes) {

                        // Check if the home's name is the one specified
                        if (home.getName().equals(homeName)) {

                            // Teleport to the home
                            player.teleport(home.getLocation());
                            sender.sendMessage(format(command, "message.teleport.misc", "{name}", home.getName()));
                            return true;
                        }
                    }
                    sender.sendMessage(format(command, "message.error.not-found.home", "{name}", args[0]));
                }
            }
            // /home set|remove <name>
            else if (args.length == 2) {

                // Set the home name
                String homeName = args[1];

                // /home set <name>
                if (args[0].equalsIgnoreCase("set")) {

                    if (homes.size() > 0 && !player.hasPermission("ultravanilla.command.home.unlimited")) {
                        sender.sendMessage(format(command, "message.error.limited-homes"));
                        return true;
                    }

                    // Use the static method as it's easier
                    setHome(player, homeName);
                    sender.sendMessage(format(command, "message.set.misc", "{name}", homeName));
                    return true;
                }

                // /home remove <name>
                else if (args[0].equalsIgnoreCase("remove")) {

                    // Use the static method as it's easier. If succeeded, send message
                    if (removeHome(player, homeName)) {
                        sender.sendMessage(format(command, "message.remove.misc", "{name}", homeName));
                        return true;
                    } else {
                        sender.sendMessage(format(command, "message.error.not-found.misc", "{name}", homeName));
                        return true;
                    }
                } else {
                    return false;
                }
            }

            // If the command didn't return yet, the homes list has been updated above, so it's time to save it
            UltraVanilla.set(player, HOME_PATH, homes);
        } else {
            sender.sendMessage(plugin.getString("player-only", "{action}", "use homes"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // Initialize the list as empty
        List<String> list = new ArrayList<>();

        // Check if sender is a player
        if (sender instanceof Player) {

            // Set player & user variables
            Player player = (Player) sender;

            List<Position> homes = getHomes(player);

            // /home [name]|set|remove
            if (args.length == 1) {
                if (homes != null) {

                    if (args[0].isEmpty()) {
                        for (Position home : homes) {
                            list.add(home.getName());
                        }
                        list.add("list");
                        list.add("remove");
                        list.add("remove-all");
                        list.add("set");
                    } else {
                        for (Position home : homes) {
                            if (home.getName().startsWith(args[0])) {
                                list.add(home.getName());
                            }
                        }
                        if (player.getBedSpawnLocation() != null) {
                            list.add("bed");
                        }
                        list.add("list");
                        list.add("remove");
                        list.add("remove-all");
                        list.add("set");
                    }
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove")) {
                    if (homes != null) {
                        if (args[1].isEmpty()) {
                            for (Position home : homes) {
                                list.add(home.getName());
                            }
                        } else {
                            for (Position home : homes) {
                                if (home.getName().startsWith(args[1])) {
                                    list.add(home.getName());
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }
}

