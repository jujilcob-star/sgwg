package fr.exemple.rppcall;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RppCall extends JavaPlugin implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 30_000;
    private static final int BOT_AMOUNT = 5;
    private static final long REMOVE_AFTER_TICKS = 20L * 60;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onHorn(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.GOAT_HORN) return;
        if (!(item.getItemMeta() instanceof MusicInstrumentMeta meta)) return;
        if (meta.getInstrument() == null) return;
        if (!meta.getInstrument().getKey().getKey().equals("call_goat_horn")) return;

        Player p = e.getPlayer();

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(p.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) {
            p.sendMessage("§cAttends encore " + ((COOLDOWN_MS - (now - last)) / 1000) + "s.");
            return;
        }
        cooldowns.put(p.getUniqueId(), now);

        effects(p.getLocation());
        p.performCommand("rpp spawn " + BOT_AMOUNT);

        getServer().getScheduler().runTaskLater(this, () -> {
            if (!p.isOnline()) return;
            p.performCommand("rpp follow");
            effects(p.getLocation());
        }, 20L);

        if (REMOVE_AFTER_TICKS > 0) {
            getServer().getScheduler().runTaskLater(this, () -> {
                if (p.isOnline()) p.performCommand("rpp remove");
            }, REMOVE_AFTER_TICKS);
        }
    }

    private void effects(Location loc) {
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 1, 0), 100, 0.8, 1, 0.8, 0.02);
        groundEffect(loc);
    }

    private void groundEffect(Location loc) {
        Block below = loc.clone().subtract(0, 0.5, 0).getBlock();
        int tries = 0;
        while (below.getType().isAir() && tries++ < 5) {
            below = below.getRelative(BlockFace.DOWN);
        }
        if (below.getType().isAir()) return;

        BlockData data = below.getBlockData();
        loc.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.2, 0), 60, 0.6, 0.1, 0.6, 0.1, data);

        for (int i = 0; i < 24; i++) {
            double a = 2 * Math.PI * i / 24;
            Location ring = loc.clone().add(Math.cos(a) * 1.5, 0.1, Math.sin(a) * 1.5);
            loc.getWorld().spawnParticle(Particle.BLOCK, ring, 4, 0.1, 0.1, 0.1, 0.05, data);
        }
    }
}
