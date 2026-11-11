package ru.artem.beforethefall.resourcepack;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.scheduler.BukkitRunnable;
import ru.artem.beforethefall.BeforeTheFallPlugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ResourcePackManager implements Listener {
    private static final String DEFAULT_URL =
            "https://github.com/login19ruman1-spec/Atomic/releases/download/resourcepack-latest/resourcepack.zip";

    private final BeforeTheFallPlugin plugin;
    private final String sha1;

    public ResourcePackManager(BeforeTheFallPlugin plugin) {
        this.plugin = plugin;
        this.sha1 = extractAndValidatePack();
    }

    private String extractAndValidatePack() {
        try {
            Path directory = plugin.getDataFolder().toPath();
            Files.createDirectories(directory);
            Path packFile = directory.resolve("resourcepack.zip");
            try (InputStream input = plugin.getResource("resourcepack.zip")) {
                if (input == null) throw new IllegalStateException("resourcepack.zip is missing from the plugin JAR");
                Files.copy(input, packFile, StandardCopyOption.REPLACE_EXISTING);
            }

            try (ZipFile zip = new ZipFile(packFile.toFile())) {
                requireEntry(zip, "pack.mcmeta");
                requireEntry(zip, "assets/beforethefall/items/gas_mask.json");
                requireEntry(zip, "assets/beforethefall/items/gas_filter.json");
                requireEntry(zip, "assets/beforethefall/items/dosimeter.json");
                requireEntry(zip, "assets/beforethefall/items/nuclear_bomb.json");
                requireEntry(zip, "assets/beforethefall/items/nuclear_explosion.json");
                requireEntry(zip, "assets/beforethefall/models/item/gas_mask.json");
                requireEntry(zip, "assets/beforethefall/textures/gui/gasmask_overlay.png");
                requireEntry(zip, "assets/beforethefall/sounds.json");
                requireEntry(zip, "assets/beforethefall/sounds/blast/close.ogg");
                requireEntry(zip, "assets/beforethefall/sounds/blast/wave.ogg");
                requireEntry(zip, "assets/beforethefall/sounds/blast/distant.ogg");
                requireEntry(zip, "assets/beforethefall/sounds/gas-mask/equip.ogg");
                requireEntry(zip, "assets/beforethefall/sounds/gas-mask/filter.ogg");
                requireEntry(zip, "assets/beforethefall/sounds/gas-mask/breath.ogg");
                requireEntry(zip, "assets/beforethefall/sounds/gas-mask/empty.ogg");
                requireEntry(zip, "assets/beforethefall/sounds/dosimeter/measure.ogg");
            }

            byte[] digest = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(packFile));
            String hex = HexFormat.of().formatHex(digest);
            plugin.getLogger().info("Resource pack extracted and validated: " + packFile
                    + " (" + Files.size(packFile) + " bytes, SHA-1 " + hex + ")");
            return hex;
        } catch (Exception exception) {
            plugin.getLogger().severe("Resource pack preparation failed: " + exception.getMessage());
            return null;
        }
    }

    private void requireEntry(ZipFile zip, String name) {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null || entry.isDirectory()) throw new IllegalStateException("resourcepack.zip is missing " + name);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (sha1 == null) {
            event.getPlayer().sendMessage(Component.text("Atomic: ресурс-пак не собран внутри плагина. Сообщите администрации."));
            return;
        }
        String url = plugin.getConfig().getString("resource-pack.url", DEFAULT_URL);
        if (url == null || url.isBlank()) url = DEFAULT_URL;
        final String packUrl = url.trim();

        new BukkitRunnable() {
            @Override public void run() {
                Player player = event.getPlayer();
                if (!player.isOnline()) return;
                try {
                    player.setResourcePack(packUrl, sha1, true,
                            Component.text("BeforeTheFall — модели, текстуры и звуки атомной станции"));
                    plugin.getLogger().info("Resource pack requested for " + player.getName()
                            + " from " + packUrl + " (SHA-1 " + sha1 + ")");
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().severe("Invalid resource-pack URL for " + player.getName() + ": " + exception.getMessage());
                    player.kick(Component.text("Не удалось запросить обязательный ресурспак Atomic."));
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        PlayerResourcePackStatusEvent.Status status = event.getStatus();
        switch (status) {
            case SUCCESSFULLY_LOADED -> plugin.getLogger().info("Resource pack loaded by " + event.getPlayer().getName());
            case DECLINED -> plugin.getLogger().warning("Resource pack declined by " + event.getPlayer().getName());
            case FAILED_DOWNLOAD -> plugin.getLogger().severe("Resource pack download failed for " + event.getPlayer().getName());
            case ACCEPTED -> plugin.getLogger().info("Resource pack accepted by " + event.getPlayer().getName() + "; waiting for download and validation");
            default -> plugin.getLogger().warning("Resource pack status for " + event.getPlayer().getName() + ": " + status);
        }
    }
}