package ru.artem.beforethefall.equipment;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.artem.beforethefall.BeforeTheFallPlugin;

import java.util.List;
import java.util.Locale;

public final class DosimeterManager implements Listener {
    private final BeforeTheFallPlugin plugin;
    private final NamespacedKey marker;

    public DosimeterManager(BeforeTheFallPlugin plugin) {
        this.plugin = plugin;
        this.marker = new NamespacedKey(plugin, "dosimeter");
    }

    public ItemStack createDosimeter() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Дозиметр ДП-5");
        meta.setCustomModelData(30);
        meta.setLore(List.of(
                ChatColor.GRAY + "Измеряет местный радиационный фон",
                ChatColor.YELLOW + "ПКМ — снять показания"
        ));
        meta.getPersistentDataContainer().set(marker, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isDosimeter(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK || !item.hasItemMeta()) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(marker, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isDosimeter(event.getItem())) return;

        Player player = event.getPlayer();
        double ambient = plugin.getRadiationManager().getAmbientLevel(player);
        double dose = plugin.getRadiationManager().get(player);
        ChatColor levelColor = ambient >= 8.0 ? ChatColor.DARK_RED
                : ambient >= 4.0 ? ChatColor.RED
                : ambient >= 1.0 ? ChatColor.YELLOW : ChatColor.GREEN;
        String danger = ambient >= 8.0 ? "СМЕРТЕЛЬНЫЙ ФОН"
                : ambient >= 4.0 ? "ОПАСНО"
                : ambient >= 1.0 ? "ПОВЫШЕННЫЙ ФОН"
                : "НОРМА";
        boolean contaminatedWater = plugin.getRadiationManager().isInContaminatedWater(player);
        player.sendMessage(ChatColor.DARK_GRAY + "──── " + ChatColor.GOLD + "ДОЗИМЕТР" + ChatColor.DARK_GRAY + " ────");
        player.sendMessage(ChatColor.GRAY + "Мощность дозы: " + levelColor
                + String.format(Locale.US, "%.2f", ambient) + ChatColor.DARK_GRAY + " мкЗв/ч  " + levelColor + danger);
        player.sendMessage(ChatColor.GRAY + "Накопленная доза: " + ChatColor.YELLOW
                + String.format(Locale.US, "%.1f", dose) + ChatColor.GRAY + " / 100 усл. ед.");
        player.sendMessage(ChatColor.GRAY + "Среда: " + (contaminatedWater
                ? ChatColor.RED + "РАДИОАКТИВНАЯ ВОДА"
                : ChatColor.GREEN + "вода не загрязнена в этой точке"));
        player.sendMessage(ChatColor.GRAY + "Защита: " + (plugin.getGasMaskManager().isProtected(player)
                ? ChatColor.GREEN + "фильтр работает (пыль)" : ChatColor.RED + "дыхание не защищено")
                + ChatColor.DARK_GRAY + " | " + (plugin.getProtectiveSuitManager() != null
                && plugin.getProtectiveSuitManager().hasFullSuit(player)
                ? ChatColor.GREEN + "костюм" : ChatColor.GRAY + "без полного костюма"));
        player.playSound(player.getLocation(), "beforethefall:dosimeter_measure", SoundCategory.PLAYERS, 0.75f, 1.0f);
        event.setCancelled(true);
    }
}
