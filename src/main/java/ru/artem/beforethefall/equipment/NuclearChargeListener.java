package ru.artem.beforethefall.equipment;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
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
import org.bukkit.scheduler.BukkitTask;
import ru.artem.beforethefall.BeforeTheFallPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class NuclearChargeListener implements Listener {
    private final BeforeTheFallPlugin plugin;
    private final NamespacedKey chargeKey;
    private final Set<String> placedCharges = new HashSet<>();
    private final java.util.Map<String, UUID> bombDisplays = new java.util.HashMap<>();
    private final java.util.Map<String, BukkitTask> countdowns = new java.util.HashMap<>();

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
        ItemStack displayItem = event.getItemInHand().clone();
        displayItem.setAmount(1);
        ItemDisplay display = event.getBlockPlaced().getWorld().spawn(
                event.getBlockPlaced().getLocation().add(0.5, 0.15, 0.5), ItemDisplay.class);
        display.setItemStack(displayItem);
        display.setBillboard(Display.Billboard.FIXED);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                new Vector3f(0.72f, 0.72f, 0.72f), new Quaternionf()));
        display.setViewRange(1.5f);
        bombDisplays.put(key(event.getBlockPlaced()), display.getUniqueId());
        save();
        event.getPlayer().sendMessage(ChatColor.RED + "☢ Заряд установлен. Подожги его огнивом или подай редстоун-сигнал.");
    }
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        String id = key(event.getBlock());
        BukkitTask countdown = countdowns.remove(id);
        if (countdown != null) {
            countdown.cancel();
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Обратный отсчёт остановлен: заряд обезврежен.");
        }
        if (placedCharges.remove(id)) save();
        removeDisplay(id);
    }
    private void removeDisplay(String id) {
        UUID uuid = bombDisplays.remove(id);
        if (uuid == null) return;
        var entity = plugin.getServer().getEntity(uuid);
        if (entity != null) entity.remove();
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
        String id = key(block);
        if (countdowns.containsKey(id)) return;

        int seconds = Math.max(1, Math.min(60,
                plugin.getConfig().getInt("nuclear-blast.countdown-seconds", 10)));
        plugin.getLogger().info("Nuclear charge armed at " + id + "; countdown " + seconds + " seconds.");

        BukkitRunnable countdown = new BukkitRunnable() {
            private int remaining = seconds;

            @Override
            public void run() {
                if (block.getType() != Material.TNT || !placedCharges.contains(id)) {
                    countdowns.remove(id);
                    removeDisplay(id);
                    cancel();
                    return;
                }

                if (remaining <= 0) {
                    countdowns.remove(id);
                    placedCharges.remove(id);
                    removeDisplay(id);
                    save();

                    var location = block.getLocation().add(.5, .5, .5);
                    block.setType(Material.AIR, false);
                    plugin.getNuclearBlastManager().detonate(location,
                            (float) plugin.getConfig().getDouble("nuclear-blast.default-power", 18.0));
                    cancel();
                    return;
                }

                double soundRadius = plugin.getConfig().getDouble("nuclear-blast.distant-sound-radius", 500.0);
                double maxDistanceSquared = soundRadius * soundRadius;
                for (org.bukkit.entity.Player player : block.getWorld().getPlayers()) {
                    if (player.getLocation().distanceSquared(block.getLocation()) > maxDistanceSquared) continue;
                    player.sendTitle(ChatColor.DARK_RED + "☢ " + remaining,
                            ChatColor.YELLOW + "Ядерный заряд активирован", 0, 20, 0);
                    float pitch = (float) Math.min(1.8, 0.65 + (seconds - remaining) * 0.08);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,
                            SoundCategory.MASTER, 0.9f, pitch);
                }
                remaining--;
            }
        };

        countdowns.put(id, countdown.runTaskTimer(plugin, 0L, 20L));
    }
}
