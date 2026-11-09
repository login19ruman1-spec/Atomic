package ru.artem.beforethefall.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.artem.beforethefall.BeforeTheFallPlugin;

import java.util.List;

public final class StormSettingsMenu implements Listener {
    private final BeforeTheFallPlugin plugin;
    public StormSettingsMenu(BeforeTheFallPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        MenuHolder holder = new MenuHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_GRAY + "Настройки бури и холода");
        holder.inventory = inv;
        inv.setItem(10, item(Material.SNOWBALL, "&bПлотность снегопада", "&7Частиц за цикл: &f" + plugin.getConfig().getInt("ash-storm.particle-count", 450), "&eНажми: изменить"));
        inv.setItem(12, item(Material.SNOW_BLOCK, "&bНакопление снега", "&7Блоков за цикл: &f" + plugin.getConfig().getInt("ash-storm.deposits-per-player", 14), "&eНажми: изменить"));
        inv.setItem(14, item(Material.IRON_SWORD, "&cУрон от бури", "&7Урон: &f" + plugin.getConfig().getDouble("ash-storm.damage", 0.0), "&eНажми: изменить"));
        inv.setItem(16, item(Material.POWDER_SNOW_BUCKET, "&bУрон от мороза", "&7Урон: &f" + plugin.getConfig().getDouble("frost.damage", 1.0), "&7Заморозка включена: &f" + plugin.getConfig().getBoolean("frost.enabled", true), "&eНажми: изменить"));
        inv.setItem(22, item(Material.BARRIER, "&cЗакрыть"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getView().getTopInventory()) return;
        switch (event.getSlot()) {
            case 10 -> cycleInt("ash-storm.particle-count", new int[]{220, 450, 700}, 450);
            case 12 -> cycleInt("ash-storm.deposits-per-player", new int[]{5, 14, 28}, 14);
            case 14 -> cycleDouble("ash-storm.damage", new double[]{0.0, 1.0, 2.0, 4.0}, 0.0);
            case 16 -> {
                double current = plugin.getConfig().getDouble("frost.damage", 1.0);
                cycleDouble("frost.damage", new double[]{0.0, 1.0, 2.0, 4.0}, 1.0);
                plugin.getConfig().set("frost.enabled", plugin.getConfig().getDouble("frost.damage", 1.0) > 0.0);
            }
            case 22 -> { player.closeInventory(); return; }
            default -> { return; }
        }
        plugin.saveConfig();
        open(player);
    }

    private void cycleInt(String path, int[] values, int fallback) {
        int current = plugin.getConfig().getInt(path, fallback), next = values[0];
        for (int i = 0; i < values.length; i++) if (values[i] == current) { next = values[(i + 1) % values.length]; break; }
        plugin.getConfig().set(path, next);
    }
    private void cycleDouble(String path, double[] values, double fallback) {
        double current = plugin.getConfig().getDouble(path, fallback), next = values[0];
        for (int i = 0; i < values.length; i++) if (Double.compare(values[i], current) == 0) { next = values[(i + 1) % values.length]; break; }
        plugin.getConfig().set(path, next);
    }
    private ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(java.util.Arrays.stream(lore).map(this::color).toList());
        stack.setItemMeta(meta);
        return stack;
    }
    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text); }

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
    }
}
