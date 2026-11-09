package ru.artem.beforethefall.equipment;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import ru.artem.beforethefall.BeforeTheFallPlugin;
import java.util.List;

public final class GasMaskManager {
    private final BeforeTheFallPlugin plugin;
    private final NamespacedKey marker;
    private final NamespacedKey filter;
    private BukkitTask task;
    public GasMaskManager(BeforeTheFallPlugin plugin) {
        this.plugin = plugin;
        marker = new NamespacedKey(plugin, "gas_mask");
        filter = new NamespacedKey(plugin, "filter_ticks");
    }
    public ItemStack createMask() {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_GREEN + "Противогаз");
        meta.setLore(List.of(ChatColor.GRAY + "Фильтрует пепел и радиоактивную пыль",
                ChatColor.GRAY + "Фильтр: " + plugin.getConfig().getInt("gas-mask.filter-duration-seconds", 900) + " сек.",
                ChatColor.YELLOW + "Надень в слот шлема"));
        meta.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte)1);
        meta.getPersistentDataContainer().set(filter, PersistentDataType.INTEGER,
                plugin.getConfig().getInt("gas-mask.filter-duration-seconds", 900) * 2);
        item.setItemMeta(meta);
        return item;
    }
    public boolean isMask(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(marker, PersistentDataType.BYTE);
        return value != null && value == (byte)1;
    }
    public boolean isProtected(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        return isMask(helmet) && filterTicks(helmet) > 0;
    }
    private int filterTicks(ItemStack item) {
        if (!isMask(item)) return 0;
        Integer value = item.getItemMeta().getPersistentDataContainer().get(filter, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }
    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack helmet = player.getInventory().getHelmet();
                if (!isMask(helmet)) continue;
                int remaining = filterTicks(helmet);
                if (remaining <= 0) {
                    continue;
                }
                ItemMeta meta = helmet.getItemMeta();
                int next = remaining - 1;
                meta.getPersistentDataContainer().set(filter, PersistentDataType.INTEGER, next);
                meta.setLore(List.of(ChatColor.GRAY + "Фильтрует пепел и радиоактивную пыль",
                        ChatColor.GRAY + "Осталось: " + next / 2 + " сек.",
                        next > 0 ? ChatColor.GREEN + "Фильтр работает" : ChatColor.RED + "Фильтр исчерпан"));
                helmet.setItemMeta(meta);
                if (next == 0) player.sendMessage(ChatColor.RED + "Фильтр противогаза исчерпан. Замени противогаз!");
            }
        }, 10L, 10L);
    }
    public void stop() { if (task != null) task.cancel(); }
}
