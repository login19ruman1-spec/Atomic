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
            "https://github.com/login19ruman1-spec/Atomic/releases/download/resourcepack9-latest/resourcepack.zip";
    private static final String PROJECT_RELEASE_PREFIX =
            "https://github.com/login19ruman1-spec/Atomic/releases/download/";

    private final BeforeTheFallPlugin plugin;
    private String sha1;

    public ResourcePackManager(BeforeTheFallPlugin plugin) {
        this.plugin = plugin;
        try {
            Path directory = plugin.getDataFolder().toPath();
            Files.createDirectories(directory);
            Path packFile = directory.resolve("resourcepack.zip");

            try (InputStream input = plugin.getResource("resourcepack.zip")) {
                if (input == null) {
                    throw new IllegalStateException("resourcepack.zip is missing from the plugin JAR.");
                }
                Files.copy(input, packFile, StandardCopyOption.REPLACE_EXISTING);
            }

            byte[] bytes = Files.readAllBytes(packFile);
            sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
            plugin.getLogger().info("Embedded resource pack extracted (" + bytes.length
                    + " bytes, SHA-1 " + sha1 + ").");
        } catch (Exception exception) {
            sha1 = null;
            plugin.getLogger().severe("Cannot prepare embedded resource pack: " + exception.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (sha1 == null) {
            plugin.getLogger().severe("Resource pack will not be sent: embedded ZIP is unavailable.");
            return;
        }

        String configuredUrl = plugin.getConfig().getString("resource-pack.url", "").trim();
        String url;
        if (configuredUrl.isEmpty() || configuredUrl.startsWith(PROJECT_RELEASE_PREFIX)) {
            url = RELEASE_URL;
            if (!url.equals(configuredUrl)) {
                plugin.getConfig().set("resource-pack.url", RELEASE_URL);
                plugin.saveConfig();
                plugin.getLogger().info("Updated the outdated Atomic resource-pack URL in config.yml.");
            }
        } else {
            url = configuredUrl;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = event.getPlayer();
                if (!player.isOnline()) return;

                try {
                    // Do not send a SHA-1 here: FAILED_DOWNLOAD was occurring after the
                    // client accepted the prompt. Omitting the optional hash avoids rejecting
                    // the downloaded release asset because of an archive/hash mismatch.
                    player.setResourcePack(url);
                    plugin.getLogger().info("Sent resource pack request to " + player.getName()
                            + " from " + url + " (optional SHA-1 check disabled; embedded SHA-1 "
                            + sha1 + ").");
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
