package com.example.advancedModerationSystem;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.ChatColor;

public class ChatListener implements Listener {

    private final AdvancedModerationSystem plugin;

    public ChatListener(AdvancedModerationSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.isPlayerMuted(player.getName())) { // Usa il metodo isPlayerMuted della classe principale
            event.setCancelled(true); // Cancella il messaggio del giocatore mutato
            player.sendMessage(ChatColor.RED + "Non puoi parlare in chat perché sei mutato.");
        }
    }
}