package com.example.advancedModerationSystem;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.sql.SQLException;
import java.util.HashMap; // Import aggiunto
import java.util.Map; // Import aggiunto

public class CapsDetectorListener implements Listener {

    private final AdvancedModerationSystem plugin;
    private final Map<String, Integer> capsCountMap = new HashMap<>(); // Mappa per conteggiare i messaggi in maiuscolo

    public CapsDetectorListener(AdvancedModerationSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Controlla se il messaggio è completamente in maiuscolo
        if (message.equals(message.toUpperCase())) {
            // Incrementa il contatore di messaggi in maiuscolo per il giocatore
            String playerName = player.getName().toLowerCase();
            capsCountMap.put(playerName, capsCountMap.getOrDefault(playerName, 0) + 1);

            int capsCount = capsCountMap.get(playerName);

            // Avvisa automaticamente il giocatore dopo 3 messaggi consecutivi in maiuscolo
            if (capsCount >= 3) {
                event.setCancelled(true); // Cancella il messaggio dalla chat pubblica
                try {
                    plugin.handleAutoWarn(player.getName(), "Uso eccessivo di CAPS"); // Chiama il metodo handleAutoWarn
                } catch (SQLException e) {
                    e.printStackTrace();
                    player.sendMessage(ChatColor.RED + "Si è verificato un errore durante l'avviso automatico.");
                }
                player.sendMessage(ChatColor.RED + "Sei stato avvisato automaticamente per uso eccessivo di CAPS!");
                capsCountMap.put(playerName, 0); // Resetta il contatore
            } else {
                player.sendMessage(ChatColor.YELLOW + "Attenzione: evita di scrivere troppo spesso in maiuscolo.");
            }
        } else {
            // Resetta il contatore se il messaggio non è in maiuscolo
            capsCountMap.put(player.getName().toLowerCase(), 0);
        }
    }
}