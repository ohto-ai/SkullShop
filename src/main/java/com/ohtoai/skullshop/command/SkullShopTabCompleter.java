package com.ohtoai.skullshop.command;

import com.ohtoai.skullshop.SkullShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SkullShopTabCompleter implements TabCompleter {

    private final SkullShopPlugin plugin;

    private static final List<String> SUB_COMMANDS = Arrays.asList("buy", "price", "currency", "allowother", "setitem", "reload", "help");
    private static final List<String> ECONOMY_KEYS = Arrays.asList("vault", "player_points", "item");
    private static final List<String> ENABLE_DISABLE = Arrays.asList("enable", "disable");
    private static final List<String> QUANTITIES = Arrays.asList("1", "8", "64");

    public SkullShopTabCompleter(SkullShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(partial)) {
                    result.add(sub);
                }
            }
            if (!sender.hasPermission("skullshop.admin")) {
                result.remove("reload");
                result.remove("setitem");
                result.remove("currency");
                result.remove("allowother");
            }
            return result;
        }

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "buy" -> completeBuy(sender, args);
            case "price" -> completePrice(args);
            case "currency" -> completeCurrency(args);
            case "allowother" -> completeAllowOther(args);
            default -> new ArrayList<>();
        };
    }

    private List<String> completeBuy(CommandSender sender, String[] args) {
        // /ss buy [id] [via <currency>] [1|8|64]
        String partial = args[args.length - 1].toLowerCase();

        // Find the "via" keyword position
        int viaPos = -1;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("via")) {
                viaPos = i;
                break;
            }
        }

        if (viaPos < 0) {
            // No "via" yet — suggest player names + "via"
            List<String> result = new ArrayList<>();
            if ("via".startsWith(partial)) result.add("via");
            result.addAll(getOnlinePlayerNames(partial));
            return result;
        }

        // "via" found at viaPos. What comes after?
        if (args.length == viaPos + 2) {
            // Cursor is just after "via" — suggest economy keys (only available ones)
            return ECONOMY_KEYS.stream()
                    .filter(k -> k.startsWith(partial) && plugin.getEconomyProvider(k) != null
                            && plugin.getEconomyProvider(k).isAvailable())
                    .collect(Collectors.toList());
        }

        // Economy key already typed — suggest quantities
        if (args.length >= viaPos + 3) {
            return QUANTITIES.stream()
                    .filter(q -> q.startsWith(partial))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private List<String> completePrice(String[] args) {
        String partial = args[args.length - 1].toLowerCase();
        if (args.length == 2) {
            return ECONOMY_KEYS.stream()
                    .filter(k -> k.startsWith(partial)
                            && plugin.getEconomyProvider(k) != null
                            && plugin.getEconomyProvider(k).isAvailable())
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> completeCurrency(String[] args) {
        String partial = args[args.length - 1].toLowerCase();
        if (args.length == 2) {
            return ECONOMY_KEYS.stream()
                    .filter(k -> k.startsWith(partial)
                            && plugin.getEconomyProvider(k) != null)
                    .collect(Collectors.toList());
        }
        if (args.length == 3) {
            return ENABLE_DISABLE.stream()
                    .filter(a -> a.startsWith(partial))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> completeAllowOther(String[] args) {
        String partial = args[args.length - 1].toLowerCase();
        if (args.length == 2) {
            return ENABLE_DISABLE.stream()
                    .filter(a -> a.startsWith(partial))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<String> getOnlinePlayerNames(String partial) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
    }
}
