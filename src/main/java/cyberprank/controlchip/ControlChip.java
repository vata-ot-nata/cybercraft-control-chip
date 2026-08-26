package cyberprank.controlchip;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ControlChip extends JavaPlugin implements Listener {

    private static final ConcurrentHashMap<Player, String> chipState = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Player, String> chipControl = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

    }

    public void givePermission(Player player, String permission) {
        PermissionAttachment attachment = player.addAttachment(this);
        attachment.setPermission(permission, true);
        player.recalculatePermissions();
    }

    public void removePermission(Player player, String permission) {
        PermissionAttachment attachment = player.addAttachment(this);
        attachment.unsetPermission(permission);
        player.recalculatePermissions();
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEntityEvent event) {
        try {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getItemMeta().getDisplayName().equals("§4Control Chip")) {
                if (event.getRightClicked() instanceof Player) {
                    if (!player.hasCooldown(item.getType())) {
                        if (Objects.equals(chipState.get(player), "none")) {
                            player.setCooldown(item.getType(), 20);
                            chipState.put(player, "active");

                            String controledName = event.getRightClicked().getName();
                            Player controledPlayer = Bukkit.getPlayer(controledName);

                            player.sendMessage("Вы вставили чип в " + controledName + ", нажмите на чип ещё раз, чтобы начать управлять.");
                            chipControl.put(player, controledName);

                            if (controledPlayer.getInventory().getHelmet() == null) {
                                ItemStack chip = new ItemStack(Material.STONE_BUTTON);
                                ItemMeta meta = chip.getItemMeta();
                                meta.setDisplayName("§4Control Chip");
                                meta.setLore(List.of(new String[]{"§8Вами управляют"}));
                                chip.setItemMeta(meta);
                                controledPlayer.getInventory().setHelmet(chip);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onPlayerRC(PlayerInteractEvent event) {
        try {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            Player victim = Bukkit.getPlayer(chipControl.get(player));
            if (victim.isOnline()) {
                if (event.getAction().isRightClick()) {
                    if (item.getItemMeta().getDisplayName().equals("§4Control Chip")) {
                        if (!player.hasCooldown(item.getType())) {
                            if (Objects.equals(chipState.get(player), "active")) {
                                player.setCooldown(item.getType(), 100);
                                String name = victim.getName();

                                if (item.getAmount() == 1) {
                                    player.getInventory().setItemInMainHand(null);
                                } else {
                                    item.setAmount(item.getAmount() - 1);
                                }

                                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);

                                givePermission(player, "controlplayer.use");
                                player.performCommand("cp " + name);
                                removePermission(player, "controlplayer.use");
                                chipState.put(player, "use");

                                player.sendMessage("Вы управляете " + name + "\nЧтобы перестать управлять напишите \"/stopcontrol\".");
                            }
                        }
                    }
                }
            } else {
                player.sendMessage("Жертва оффлайн.");
                chipState.put(player, "none");
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equals("stopcontrol")) {
                if (Objects.equals(chipState.get(player), "use")) {
                    givePermission(player, "controlplayer.use");
                    player.performCommand("cp");
                    removePermission(player, "controlplayer.use");
                    chipState.put(player, "none");

                    player.sendMessage("Вы перестали управлять.");

                    String controledName = chipControl.get(player);
                    Player controledPlayer = Bukkit.getPlayer(controledName);

                    try {
                        if (controledPlayer.isOnline()) {
                            if (controledPlayer.getInventory().getHelmet() != null) {
                                if (controledPlayer.getInventory().getHelmet().getItemMeta().getDisplayName().equals("§4Control Chip")
                                && controledPlayer.getInventory().getHelmet().getItemMeta().getLore().get(0).equals("§8Вами управляют")) {
                                    controledPlayer.getInventory().setHelmet(null);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        return super.onCommand(sender, command, label, args);
    }

    @EventHandler
    public void onPlayerClick(InventoryClickEvent event) {
        try {
            ItemStack item = event.getCurrentItem();
            if (item.getItemMeta().getDisplayName().equals("§4Control Chip")
            && item.getItemMeta().getLore().contains("§8Вами управляют")) {
                event.setCancelled(true);
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        chipState.put(event.getPlayer(), "none");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerInventory inventory = event.getPlayer().getInventory();
        if (inventory.getHelmet() != null) {
            if (inventory.getHelmet().getItemMeta().getDisplayName().equals("§4Control Chip")) {
                inventory.setHelmet(null);
            }
        }
    }
}
