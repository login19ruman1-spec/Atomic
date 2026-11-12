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
                    throw new IllegalStateException("resourcepack.zip is missing inside the plugin JAR");
                }
                Files.copy(input, packFile, StandardCopyOption.REPLACE_EXISTING);
            }

            byte[] bytes = Files.readAllBytes(packFile);
            hash = MessageDigest.getInstance("SHA-1").digest(bytes);
            plugin.getLogger().info("Embedded resource pack extracted (" + bytes.length
                    + " bytes, SHA-1 " + HexFormat.of().formatHex(hash) + ")");
        } catch (Exception exception) {
            hash = null;
            plugin.getLogger().severe("Resource pack preparation failed: " + exception.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (hash == null) {
            event.getPlayer().sendMessage("§cBeforeTheFall: ресурс-пак не подготовлен. Проверь latest.log.");
            return;
        }

        String configured = plugin.getConfig().getString("resource-pack.url", "").trim();
        String baseUrl = configured.isEmpty()
                || configured.contains("raw.githubusercontent.com/login19ruman1-spec/Atomic/main/src/main/resources/resourcepack.zip")
                ? RELEASE_URL : configured;

        // Pin the release download to the exact embedded pack hash. The query string
        // busts CDN caches after a new release replaces the stable resourcepack-latest asset.
        String hashHex = HexFormat.of().formatHex(hash);
        String separator = baseUrl.contains("?") ? "&" : "?";
        String url = baseUrl + separator + "v=" + hashHex;

        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = event.getPlayer();
                if (!player.isOnline()) return;
                player.setResourcePack(
                        url,
                        hashHex,
                        false,
                        Component.text("BeforeTheFall: модели, текстуры, HUD и звуки")
                );
                plugin.getLogger().info("Requested resource pack for " + player.getName()
                        + " (SHA-1 " + hashHex + ")");
            }
        }.runTaskLater(plugin, 40L);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        switch (event.getStatus()) {
            case ACCEPTED -> plugin.getLogger().info(event.getPlayer().getName() + " accepted the resource pack download");
            case DOWNLOADED -> plugin.getLogger().info(event.getPlayer().getName() + " downloaded the resource pack; applying it");
            case SUCCESSFULLY_LOADED -> {
                plugin.getLogger().info(event.getPlayer().getName() + " successfully loaded BeforeTheFall resource pack");
                event.getPlayer().sendMessage("§aBeforeTheFall: модели, текстуры и звуки загружены.");
            }
            case DECLINED -> plugin.getLogger().warning(event.getPlayer().getName() + " declined the BeforeTheFall resource pack");
            case FAILED_DOWNLOAD -> {
                plugin.getLogger().severe(event.getPlayer().getName() + " failed to download the BeforeTheFall resource pack");
                event.getPlayer().sendMessage("§cНе удалось скачать ресурспак BeforeTheFall. Проверь доступ к GitHub Releases.");
            }
            case FAILED_RELOAD -> {
                plugin.getLogger().severe(event.getPlayer().getName() + " downloaded the pack but Minecraft could not apply it");
                event.getPlayer().sendMessage("§cMinecraft скачал ресурспак, но не смог его применить. Проверь pack.mcmeta и assets.");
            }
            case INVALID_URL -> plugin.getLogger().severe("Minecraft rejected the resource pack URL for " + event.getPlayer().getName());
            case DISCARDED -> plugin.getLogger().warning(event.getPlayer().getName() + " discarded the resource pack");
        }
    }
}
