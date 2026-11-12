package ru.artem.beforethefall.resourcepack;

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

public final class ResourcePackManager implements Listener {
    private static final String RELEASE_URL =
            "https://github.com/login19ruman1-spec/Atomic/releases/download/resourcepack-latest/resourcepack.zip";

    private final BeforeTheFallPlugin plugin;
    private byte[] hash;

    public ResourcePackManager(BeforeTheFallPlugin plugin) {
        this.plugin = plugin;
        try {
            Path directory = plugin.getDataFolder().toPath();
            Files.createDirectories(directory);
            Path packFile = directory.resolve("resourcepack.zip");

            try (InputStream input = plugin.getResource("resourcepack.zip")) {
                if (input == null) {
                    throw new IllegalStateException(
                            "resourcepack.zip is missing from the plugin JAR. Rebuild the plugin with the resource-pack task.");
                }
                Files.copy(input, packFile, StandardCopyOption.REPLACE_EXISTING);
            }

            byte[] bytes = Files.readAllBytes(packFile);
            hash = MessageDigest.getInstance("SHA-1").digest(bytes);
            plugin.getLogger().info("Embedded resource pack extracted (" + bytes.length
                    + " bytes, SHA-1 " + HexFormat.of().formatHex(hash) + ").");
        } catch (Exception exception) {
            hash = null;
            plugin.getLogger().severe("Cannot prepare embedded resource pack: " + exception.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (hash == null) {
            plugin.getLogger().severe("Resource pack will not be sent: embedded ZIP or its SHA-1 is unavailable.");
            return;
        }

        String configuredUrl = plugin.getConfig().getString("resource-pack.url", "").trim();
        String url = configuredUrl.isEmpty() ? RELEASE_URL : configuredUrl;

        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = event.getPlayer();
                if (!player.isOnline()) return;

                try {
                    player.setResourcePack(url, hash);
                    plugin.getLogger().info("Sent resource pack request to " + player.getName()
                            + " from " + url);
                } catch (IllegalArgumentException exception) {
                    plugin.getLogger().warning("Invalid resource-pack URL for " + player.getName()
                            + ": " + exception.getMessage());
                    player.sendMessage("§cНе удалось отправить ресурспак Atomic. Проверьте URL в config.yml.");
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        String status = event.getStatus().name();
        Player player = event.getPlayer();

        if ("SUCCESSFULLY_LOADED".equals(status)) {
            plugin.getLogger().info("Resource pack successfully loaded by " + player.getName() + ".");
            return;
        }

        plugin.getLogger().warning("Resource pack status for " + player.getName() + ": " + status);
        if ("DECLINED".equals(status) || "FAILED_DOWNLOAD".equals(status)
                || "INVALID_URL".equals(status)) {
            player.sendMessage("§cРесурспак Atomic не загружен (" + status
                    + "). Модели, текстуры и звуки не будут отображаться.");
        }
    }
}
