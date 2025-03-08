package com.example.advancedModerationSystem;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player; // Importa Player
import org.bukkit.Bukkit; // Importa Bukkit
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class StaffChatListener implements Listener {

    private final AdvancedModerationSystem plugin;

    public StaffChatListener(AdvancedModerationSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer(); // Ottieni il giocatore mittente del messaggio
        String message = event.getMessage();

        // Controlla se il messaggio inizia con "/sc"
        if (message.startsWith("/sc ")) {
            event.setCancelled(true); // Cancella il messaggio dalla chat pubblica

            String staffMessage = message.replaceFirst("/sc ", ""); // Rimuovi "/sc " dal messaggio

            // Leggi il prefisso della chat staff dal config.yml
            String staffChatPrefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("settings.staff-chat-prefix", "&9[StaffChat] &5%s&9: &f%s"));

            // Invia il messaggio solo agli utenti con il permesso appropriato
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) { // Usa Bukkit.getOnlinePlayers()
                if (onlinePlayer.hasPermission("advancedmoderation.staffchat")) {
                    onlinePlayer.sendMessage(String.format(staffChatPrefix, player.getName(), staffMessage));
                }
            }
        }
    }
}