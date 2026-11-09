package ru.artem.beforethefall.bunker;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import ru.artem.beforethefall.BeforeTheFallPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BunkerManager implements Listener {
    private final BeforeTheFallPlugin plugin;
    private final NamespacedKey coreKey;
    private final List<Core> cores = new ArrayList<>();

    public BunkerManager(BeforeTheFallPlugin plugin) {
        this.plugin = plugin;
        coreKey = new NamespacedKey(plugin, "atomic_bunker_core");
        for (String raw : plugin.getConfig().getStringList("bunkers.cores")) {
            String[] a = raw.split(",");
            if (a.length != 4) continue;
            try { cores.add(new Core(UUID.fromString(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]))); }
            catch (IllegalArgumentException ignored) { }
        }
    }

    private boolean isCraftedCore(ItemStack item) {
        if (item == null || item.getType() != Material.LODESTONE || !item.hasItemMeta()) return false;
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(coreKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.LODESTONE || !isCraftedCore(event.getItemInHand())) return;
        cores.add(new Core(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ()));
        save();
        event.getPlayer().sendMessage(ChatColor.GREEN + "Крафтовое ядро бункера активировано. Радиус защиты: " + radius() + " блоков.");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.LODESTONE) return;
        if (cores.removeIf(core -> core.world.equals(block.getWorld().getUID()) && core.x == block.getX() && core.y == block.getY() && core.z == block.getZ())) {
            save();
            event.getPlayer().sendMessage(ChatColor.RED + "Ядро бункера разрушено. Защита отключена.");
        }
    }

    public boolean isProtected(Location location) {
        if (location.getWorld() == null) return false;
        double radius = radius();
        for (Core core : cores) {
            if (!core.world.equals(location.getWorld().getUID())) continue;
            double dx = location.getX() - (core.x + .5), dz = location.getZ() - (core.z + .5), dy = location.getY() - (core.y + .5);
            if (dx * dx + dz * dz <= radius * radius
                    && dy >= -plugin.getConfig().getInt("bunkers.depth", 12)
                    && dy <= plugin.getConfig().getInt("bunkers.height", 8)) return true;
        }
        return false;
    }

    private int radius() { return Math.max(4, plugin.getConfig().getInt("bunkers.radius", 12)); }
    private void save() {
        List<String> values = new ArrayList<>();
        for (Core core : cores) values.add(core.world + "," + core.x + "," + core.y + "," + core.z);
        plugin.getConfig().set("bunkers.cores", values);
        plugin.saveConfig();
    }
    private record Core(UUID world, int x, int y, int z) { }
}
