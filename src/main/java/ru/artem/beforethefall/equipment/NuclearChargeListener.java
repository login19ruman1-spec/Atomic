package ru.artem.beforethefall.equipment;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
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
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.artem.beforethefall.BeforeTheFallPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class NuclearChargeListener implements Listener {
    private final BeforeTheFallPlugin plugin;
    private final NamespacedKey chargeKey;
    private final Set<String> placedCharges = new HashSet<>();
    private final Set<String> detonatingCharges = new HashSet<>();
    private final java.util.Map<String, UUID> bombDisplays = new java.util.HashMap<>();

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

    // LIGHT is an invisible, non-solid marker. The visible bomb is the ItemDisplay,
    // so the vanilla TNT cube can no longer cover the custom model.
    private boolean isPlacedCharge(Block block) {
        return block.getType() == Material.LIGHT && placedCharges.contains(key(block));
    }

    private void save() {
        plugin.getConfig().set("nuclear-blast.placed-charges", placedCharges.stream().toList());
        plugin.saveConfig();
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!isChargeItem(event.getItemInHand())) return;
        Block block = event.getBlockPlaced();
        String id = key(block);
        placedCharges.add(id);

        ItemStack displayItem = event.getItemInHand().clone();
        displayItem.setAmount(1);
        ItemDisplay display = block.getWorld().spawn(
                block.getLocation().add(0.5, 0.5, 0.5), ItemDisplay.class);
        display.setItemStack(displayItem);
        display.setBillboard(Display.Billboard.FIXED);
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                new Vector3f(1.0f, 1.0f, 1.0f), new Quaternionf()));
        display.setViewRange(2.5f);
        bombDisplays.put(id, display.getUniqueId());

        // Replace TNT one tick later, after vanilla block placement has completed.
        new BukkitRunnable() {
            @Override public void run() {
                if (!placedCharges.contains(id) || !block.getType().isAir() && block.getType() != Material.TNT) return;
                block.setType(Material.LIGHT, false);
                block.getBlockData().getAsString();
            }
        }.runTask(plugin);

        save();
        event.getPlayer().sendMessage(ChatColor.RED
                + "☢ Ядерный заряд установлен. Подожги его рядом огнивом или подай редстоун-сигнал — таймер 10 секунд.");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        String id = key(event.getBlock());
        if (!placedCharges.contains(id)) return;
        event.setDropItems(false);
        placedCharges.remove(id);
        detonatingCharges.remove(id);
        removeDisplay(id);
        save();
    }

    private void removeDisplay(String id) {
        UUID uuid = bombDisplays.remove(id);
        if (uuid == null) return;
        var entity = plugin.getServer().getEntity(uuid);
        if (entity != null) entity.remove();
    }

    private Block findAdjacentCharge(Block clicked) {
        if (clicked == null) return null;
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            Block candidate = clicked.getRelative(x, y, z);
            if (isPlacedCharge(candidate)) return candidate;
        }
        return null;
    }

    @EventHandler
    public void onIgnite(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) return;
        Material held = event.getItem() == null ? Material.AIR : event.getItem().getType();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && (held == Material.FLINT_AND_STEEL || held == Material.FIRE_CHARGE)) {
            Block target = findAdjacentCharge(event.getClickedBlock());
            if (target != null) {
                event.setCancelled(true);
                trigger(target);
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block target = findAdjacentCharge(event.getClickedBlock());
            if (target != null && !detonatingCharges.contains(key(target))) {
                placedCharges.remove(key(target));
                removeDisplay(key(target));
                save();
                target.setType(Material.AIR, false);
                event.getPlayer().sendMessage(ChatColor.YELLOW + "Ядерный заряд снят.");
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
            new BukkitRunnable() {
                @Override public void run() {
                    if (isPlacedCharge(candidate)) trigger(candidate);
                }
            }.runTask(plugin);
            return;
        }
    }

    private void trigger(Block block) {
        if (!isPlacedCharge(block)) return;
        String id = key(block);
        if (!detonatingCharges.add(id)) return;

        new BukkitRunnable() {
            private int secondsRemaining = 10;

            @Override public void run() {
                if (!isPlacedCharge(block) || !block.getChunk().isLoaded()) {
                    detonatingCharges.remove(id);
                    cancel();
                    return;
                }

                Location center = block.getLocation().add(0.5, 0.5, 0.5);
                World world = block.getWorld();
                if (secondsRemaining <= 0) {
                    detonatingCharges.remove(id);
                    placedCharges.remove(id);
                    removeDisplay(id);
                    save();
                    block.setType(Material.AIR, false);
                    plugin.getNuclearBlastManager().detonate(center,
                            (float) plugin.getConfig().getDouble("nuclear-blast.default-power", 18.0));
                    cancel();
                    return;
                }

                world.spawnParticle(Particle.ELECTRIC_SPARK, center, 8, 0.22, 0.22, 0.22, 0.02);
                world.spawnParticle(Particle.SMOKE, center, 2, 0.18, 0.18, 0.18, 0.005);
                float pitch = 0.65f + (10 - secondsRemaining) * 0.075f;
                world.playSound(center, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.BLOCKS, 0.9f, pitch);
                for (Player player : world.getPlayers()) {
                    if (player.getLocation().distanceSquared(center) <= 64.0 * 64.0) {
                        player.sendTitle(ChatColor.DARK_RED + "☢ " + secondsRemaining,
                                ChatColor.RED + "ЯДЕРНЫЙ ЗАРЯД АКТИВИРОВАН", 0, 20, 0);
                    }
                }
                secondsRemaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
