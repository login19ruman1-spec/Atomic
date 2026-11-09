package ru.artem.beforethefall.equipment;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.artem.beforethefall.BeforeTheFallPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class NuclearChargeListener implements Listener {
    private final BeforeTheFallPlugin plugin;
    private final NamespacedKey chargeKey;
    private final Set<String> placedCharges = new HashSet<>();

    public NuclearChargeListener(BeforeTheFallPlugin plugin) {
        this.plugin = plugin;
        chargeKey = new NamespacedKey(plugin, "atomic_charge");
        placedCharges.addAll(plugin.getConfig().getStringList("nuclear-blast.placed-charges"));
    }

    private String key(Block block) {
        return block.getWorld().getUID() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
    }
    private boolean isChargeItem(ItemStack item) {
        if (item == null || item.getType() != Material.TNT || !item.hasItemMeta()) return false;
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(chargeKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
    private boolean isPlacedCharge(Block block) {
        return block.getType() == Material.TNT && placedCharges.contains(key(block));
    }
    private void save() {
        plugin.getConfig().set("nuclear-blast.placed-charges", placedCharges.stream().toList());
        plugin.saveConfig();
    }
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!isChargeItem(event.getItemInHand())) return;
        placedCharges.add(key(event.getBlockPlaced()));
        save();
        event.getPlayer().sendMessage(ChatColor.RED + "☢ Заряд установлен. Подожги его огнивом или подай редстоун-сигнал.");
    }
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (placedCharges.remove(key(event.getBlock()))) save();
    }
    @EventHandler
    public void onIgnite(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Material held = event.getItem() == null ? Material.AIR : event.getItem().getType();
        if (held != Material.FLINT_AND_STEEL && held != Material.FIRE_CHARGE) return;
        Block target = event.getClickedBlock();
        if (!isPlacedCharge(target)) return;
        event.setCancelled(true);
        trigger(target);
        if (held == Material.FLINT_AND_STEEL && event.getItem() != null) {
            ItemStack tool = event.getItem();
            if (tool.getType() == Material.FLINT_AND_STEEL) {
                var meta = tool.getItemMeta();
                if (meta != null && meta.hasDamage()) {
                    meta.setDamage(Math.min(meta.getDamage() + 1, tool.getType().getMaxDurability()));
                    tool.setItemMeta(meta);
                }
            }
        }
    }
    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        if (event.getOldCurrent() > 0 || event.getNewCurrent() <= 0) return;
        Block source = event.getBlock();
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            Block candidate = source.getRelative(x, y, z);
            if (!isPlacedCharge(candidate)) continue;
            event.setNewCurrent(0);
            new BukkitRunnable() { @Override public void run() { if (isPlacedCharge(candidate)) trigger(candidate); } }.runTask(plugin);
            return;
        }
    }
    private void trigger(Block block) {
        if (!isPlacedCharge(block)) return;
        placedCharges.remove(key(block));
        save();
        var location = block.getLocation().add(.5, .5, .5);
        block.setType(Material.AIR, false);
        plugin.getNuclearBlastManager().detonate(location, (float) plugin.getConfig().getDouble("nuclear-blast.default-power", 18.0));
    }
}
