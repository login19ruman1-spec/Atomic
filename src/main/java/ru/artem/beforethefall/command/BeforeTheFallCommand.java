package ru.artem.beforethefall.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import ru.artem.beforethefall.radiation.RadiationManager;
import ru.artem.beforethefall.disaster.NuclearBlastManager;
import org.bukkit.Location;
import ru.artem.beforethefall.weather.AshStormManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BeforeTheFallCommand implements CommandExecutor, TabCompleter {
    private final BeforeTheFallPlugin plugin;
    private final RadiationManager radiation;
    private final AshStormManager ash;
    private final NuclearBlastManager nuclearBlast;

    public BeforeTheFallCommand(BeforeTheFallPlugin plugin, RadiationManager radiation, AshStormManager ash, NuclearBlastManager nuclearBlast) {
        this.plugin = plugin;
        this.radiation = radiation;
        this.ash = ash;
        this.nuclearBlast = nuclearBlast;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(color("&eBeforeTheFall &7| &fPaper 1.21.4"));
                sender.sendMessage(color("&7Use /beforethefall status in-game."));
                return true;
            }
            sender.sendMessage(color("&8&m---------&r &6BeforeTheFall &8&m---------"));
            sender.sendMessage(color("&7Радиация: &e" + String.format(Locale.US, "%.1f", radiation.get(player)) + "/100"));
            sender.sendMessage(color("&7Пепельная буря: " + (ash.isActive() ? "&cактивна" : "&aнеактивна")));
            boolean maskOn = plugin.getGasMaskManager().isProtected(player);
            sender.sendMessage(color("&7Противогаз: " + (maskOn ? "&aзащищает" : "&cне защищает (надень маску / проверь фильтр)")));
            return true;
        }
        if (args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) { sender.sendMessage(color("&cКоманда доступна только игроку.")); return true; }
            if (!sender.hasPermission("beforethefall.admin")) { sender.sendMessage(message("no-permission", "&cНедостаточно прав.")); return true; }
            plugin.openStormSettings(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("gasmask")) {
            if (!(sender instanceof Player player)) { sender.sendMessage(color("&cКоманда доступна только игроку.")); return true; }
            player.getInventory().addItem(plugin.getGasMaskManager().createMask()).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(color("&aТы получил противогаз. Надень его в слот шлема; фильтр работает 15 минут."));
            return true;
        }
        if (!sender.hasPermission("beforethefall.admin")) {
            sender.sendMessage(message("no-permission", "&cНедостаточно прав."));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "ashstorm" -> {
                boolean enable = args.length < 2 ? !ash.isActive() : args[1].equalsIgnoreCase("on");
                ash.setActive(enable);
                sender.sendMessage(message(enable ? "ash-started" : "ash-stopped", enable ? "&eПепельная буря началась." : "&aПепельная буря закончилась."));
            }
            case "nuke" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(color("&cКоманда доступна только игроку."));
                    return true;
                }
                float power = (float) plugin.getConfig().getDouble("nuclear-blast.default-power", 18.0);
                if (args.length >= 2) {
                    try { power = Float.parseFloat(args[1]); }
                    catch (NumberFormatException ex) { sender.sendMessage(color("&cМощность должна быть числом.")); return true; }
                }
                nuclearBlast.detonate(player.getLocation(), power);
                sender.sendMessage(color("&c☢ Ядерный удар инициирован."));
            }
            case "radiation" -> {
                if (args.length < 3) {
                    sender.sendMessage(color("&eИспользование: /bft radiation <игрок> <0-100>"));
                    return true;
                }
                Player target = plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(color("&cИгрок не найден или не в сети."));
                    return true;
                }
                try {
                    double value = Math.max(0, Math.min(100, Double.parseDouble(args[2])));
                    radiation.set(target, value);
                    sender.sendMessage(color("&aРадиация игрока " + target.getName() + " установлена: " + value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage(color("&cУровень должен быть числом."));
                }
            }
            case "reload" -> {
                plugin.reloadPluginConfig();
                sender.sendMessage(color("&aКонфигурация BeforeTheFall перезагружена."));
            }
            case "zone" -> sender.sendMessage(color("&eЗоны радиации задаются в config.yml: radiation.zones."));
            default -> sender.sendMessage(color("&eКоманды: /bft status, /bft menu, /bft gasmask, /bft ashstorm [on|off], /bft nuke [мощность], /bft radiation <игрок> <0-100>, /bft zone, /bft reload"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String option : List.of("status", "menu", "gasmask", "ashstorm", "nuke", "radiation", "zone", "reload")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) result.add(option);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("ashstorm")) {
            result.add("on"); result.add("off");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("radiation")) {
            plugin.getServer().getOnlinePlayers().forEach(p -> result.add(p.getName()));
        }
        return result;
    }

    private String message(String key, String fallback) {
        return color(plugin.getConfig().getString("messages.prefix", "&8[&6BeforeTheFall&8] &r") + plugin.getConfig().getString("messages." + key, fallback));
    }

    private String color(String value) { return ChatColor.translateAlternateColorCodes('&', value); }
}
