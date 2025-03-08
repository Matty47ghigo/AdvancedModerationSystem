package com.example.advancedModerationSystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap; // Import aggiunto
import java.util.Map; // Import aggiunto

public class AdvancedModerationSystem extends JavaPlugin {

    private Connection connection;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupDatabase();

        // Registra i listener
        Bukkit.getPluginManager().registerEvents(new StaffChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CapsDetectorListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);

        // Task scheduler per smutare automaticamente i giocatori dopo la durata specificata
        new BukkitRunnable() {
            @Override
            public void run() {
                checkExpiredMutes();
            }
        }.runTaskTimer(this, 0L, 20L * 60L); // Esegue ogni minuto

        getLogger().info("AdvancedModerationSystem è stato attivato!");
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        getLogger().info("AdvancedModerationSystem è stato disattivato!");
    }

    private void setupDatabase() {
        try {
            File dbFile = new File(getDataFolder(), "moderation.db");
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            DatabaseManager.createTable(connection, "MutedPlayers", "playerName TEXT PRIMARY KEY, reason TEXT, mutedBy TEXT, timestamp TEXT, duration TEXT");
            DatabaseManager.createTable(connection, "WarnedPlayers", "playerName TEXT PRIMARY KEY, warns INTEGER, reasons TEXT, warnedBy TEXT");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "warn":
                return handleWarnCommand(sender, args);
            case "resetwarns":
                return handleResetWarnsCommand(sender, args);
            case "warnlist":
                return handleWarnListCommand(sender, args);
            case "staffchat":
                return handleStaffChatCommand(sender, args);
            case "mute":
                return handleMuteCommand(sender, args);
            case "unmute":
                return handleUnmuteCommand(sender, args);
            case "tempmute":
                return handleTempMuteCommand(sender, args);
            default:
                return false;
        }
    }

    private boolean checkPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    private String getSenderName(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getName();
        } else {
            return "Console";
        }
    }

    // ==============================
    // Comando /warn
    // ==============================
    private boolean handleWarnCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /warn <player> [reason]");
            return true;
        }

        String targetPlayerName = args[0];
        String reason = String.join(" ", args).replaceFirst(args[0], "").trim();

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO WarnedPlayers (playerName, warns, reasons, warnedBy) VALUES (?, 1, ?, ?) ON CONFLICT(playerName) DO UPDATE SET warns = warns + 1, reasons = reasons || ' | ' || ?, warnedBy = warnedBy || ' | ' || ?"
        )) {
            stmt.setString(1, targetPlayerName.toLowerCase());
            stmt.setString(2, reason.isEmpty() ? "No reason" : reason);
            stmt.setString(3, getSenderName(sender));
            stmt.setString(4, reason.isEmpty() ? "No reason" : reason);
            stmt.setString(5, getSenderName(sender));
            stmt.executeUpdate();

            int currentWarns = getCurrentWarnCount(targetPlayerName);

            // Costruisci il messaggio formattato
            StringBuilder message = new StringBuilder();
            String separatorColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.separator", "&3"));
            String labelColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.label", "&e"));
            String valueColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.value", "&f"));
            String warnCountColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.warn-count-color", "&c"));

            message.append(separatorColor + "======================\n");
            message.append(labelColor + "Utente: " + valueColor + targetPlayerName + "\n");
            message.append(labelColor + "N. Warn: " + warnCountColor + currentWarns + "/3\n");
            message.append(labelColor + "Motivo: " + valueColor + (reason.isEmpty() ? "Non specificato" : reason) + "\n");
            message.append(labelColor + "Staff: " + valueColor + getSenderName(sender) + "\n");
            message.append(separatorColor + "======================");

            boolean broadcastWarnMessage = getConfig().getBoolean("settings.broadcast-warn-message", true);
            if (broadcastWarnMessage) {
                Bukkit.broadcastMessage(message.toString());
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("advancedmoderation.viewwarns")) {
                        player.sendMessage(message.toString());
                    }
                }
            }

            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
            if (targetPlayer != null) {
                targetPlayer.sendMessage(ChatColor.RED + "Hai ricevuto un avvertimento!" + (reason.isEmpty() ? "" : " Motivo: " + reason));
            }

            sender.sendMessage(ChatColor.GREEN + "Avvertimento assegnato: " + targetPlayerName);
            checkWarnCount(targetPlayerName);

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Si è verificato un errore durante l'avvertimento.");
        }
        return true;
    }

    private int getCurrentWarnCount(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT warns FROM WarnedPlayers WHERE playerName = ?")) {
            stmt.setString(1, playerName.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("warns");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0; // Restituisce 0 se il giocatore non ha warn
    }

    private void checkWarnCount(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT warns FROM WarnedPlayers WHERE playerName = ?")) {
            stmt.setString(1, playerName.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int warns = rs.getInt("warns");
                int maxWarnsBeforeBan = getConfig().getInt("settings.max-warns-before-ban", 3);
                if (warns >= maxWarnsBeforeBan) {
                    handleAutoBan(playerName);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleAutoBan(String playerName) {
        Plugin advancedBanSystem = Bukkit.getPluginManager().getPlugin("AdvancedBanSystem");
        if (advancedBanSystem == null || !(advancedBanSystem instanceof JavaPlugin)) {
            getLogger().warning("AdvancedBanSystem non è installato. Il ban automatico non verrà eseguito.");
            return;
        }

        CommandSender console = Bukkit.getConsoleSender();
        String reason = "Automatic Ban - " + getConfig().getInt("settings.max-warns-before-ban", 3) + " Warn Reached";
        Bukkit.dispatchCommand(console, "ban " + playerName + " " + reason);
        getLogger().info("Giocatore bannato automaticamente: " + playerName);
    }

    // ==============================
    // Comando /resetwarns
    // ==============================
    private boolean handleResetWarnsCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /resetwarns <player>");
            return true;
        }

        String targetPlayerName = args[0].toLowerCase();

        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM WarnedPlayers WHERE playerName = ?")) {
            stmt.setString(1, targetPlayerName);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.reset-warns-message", "&aGli avvertimenti sono stati resettati.")));
            } else {
                sender.sendMessage(ChatColor.RED + "Il giocatore non ha avvertimenti.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Si è verificato un errore durante il reset dei warn.");
        }
        return true;
    }

    // ==============================
    // Comando /warnlist
    // ==============================
    private boolean handleWarnListCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /warnlist <player>");
            return true;
        }

        String targetPlayerName = args[0].toLowerCase();

        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM WarnedPlayers WHERE playerName = ?")) {
            stmt.setString(1, targetPlayerName);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                sender.sendMessage(ChatColor.YELLOW + "Il giocatore non ha avvertimenti.");
                return true;
            }

            int totalWarns = rs.getInt("warns");
            String reasons = rs.getString("reasons");
            String warnedBy = rs.getString("warnedBy");

            String separatorColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.separator", "&3"));
            String labelColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.label", "&e"));
            String valueColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.value", "&f"));

            StringBuilder message = new StringBuilder();
            message.append(separatorColor + "======================\n");
            message.append(labelColor + "Lista degli avvertimenti per " + valueColor + targetPlayerName + "\n");
            message.append(labelColor + "Totale Warn: " + valueColor + totalWarns + "\n");
            message.append(separatorColor + "----------------------\n");

            String[] reasonArray = reasons.split(" \\| ");
            String[] staffArray = warnedBy.split(" \\| ");

            for (int i = 0; i < Math.min(reasonArray.length, staffArray.length); i++) {
                message.append(labelColor + "Warn #" + (i + 1) + ": " + valueColor + reasonArray[i] + "\n");
                message.append(labelColor + "Staff: " + valueColor + staffArray[i] + "\n");
                message.append(separatorColor + "----------------------\n");
            }

            message.append(separatorColor + "======================");
            sender.sendMessage(message.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Si è verificato un errore durante il recupero della lista degli avvertimenti.");
        }
        return true;
    }

    // ==============================
    // Comando /staffchat
    // ==============================
    private boolean handleStaffChatCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /staffchat <messaggio>");
            return true;
        }

        String message = String.join(" ", args);

        // Leggi il prefisso dalla config.yml
        String staffChatPrefix = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("settings.staff-chat-prefix", "&9[StaffChat] &5%s&9: &f%s"));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("advancedmoderation.staffchat")) {
                player.sendMessage(String.format(staffChatPrefix, sender.getName(), message));
            }
        }

        return true;
    }

    // ==============================
    // Comando /mute
    // ==============================
    private boolean handleMuteCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /mute <player> [reason]");
            return true;
        }

        String targetPlayerName = args[0];
        String reason = String.join(" ", args).replaceFirst(args[0], "").trim();

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO MutedPlayers (playerName, reason, mutedBy, timestamp, duration) VALUES (?, ?, ?, ?, ?)"
        )) {
            stmt.setString(1, targetPlayerName.toLowerCase());
            stmt.setString(2, reason.isEmpty() ? "No reason" : reason);
            stmt.setString(3, getSenderName(sender));
            stmt.setString(4, String.valueOf(System.currentTimeMillis()));
            stmt.setString(5, "PERMANENT");
            stmt.executeUpdate();

            // Costruisci il messaggio formattato
            StringBuilder message = new StringBuilder();
            String separatorColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.separator", "&3"));
            String labelColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.label", "&e"));
            String valueColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.value", "&f"));

            message.append(separatorColor + "======================\n");
            message.append(labelColor + "Utente: " + valueColor + targetPlayerName + "\n");
            message.append(labelColor + "Stato: " + valueColor + "Mutato permanentemente\n");
            message.append(labelColor + "Motivo: " + valueColor + (reason.isEmpty() ? "Non specificato" : reason) + "\n");
            message.append(labelColor + "Staff: " + valueColor + getSenderName(sender) + "\n");
            message.append(separatorColor + "======================");

            boolean broadcastMuteMessage = getConfig().getBoolean("settings.broadcast-mute-message", true);
            if (broadcastMuteMessage) {
                Bukkit.broadcastMessage(message.toString());
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("advancedmoderation.viewmutes")) {
                        player.sendMessage(message.toString());
                    }
                }
            }

            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
            if (targetPlayer != null) {
                targetPlayer.sendMessage(ChatColor.RED + "Sei stato mutato sul server!" + (reason.isEmpty() ? "" : " Motivo: " + reason));
            }

            sender.sendMessage(ChatColor.GREEN + "Giocatore mutato: " + targetPlayerName);

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Si è verificato un errore durante il mute.");
        }
        return true;
    }

    // ==============================
    // Comando /unmute
    // ==============================
    private boolean handleUnmuteCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /unmute <player>");
            return true;
        }

        String targetPlayerName = args[0].toLowerCase();

        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM MutedPlayers WHERE playerName = ?")) {
            stmt.setString(1, targetPlayerName);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Costruisci il messaggio formattato
                StringBuilder message = new StringBuilder();
                String separatorColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.separator", "&3"));
                String labelColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.label", "&e"));
                String valueColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.value", "&f"));

                message.append(separatorColor + "======================\n");
                message.append(labelColor + "Utente: " + valueColor + targetPlayerName + "\n");
                message.append(labelColor + "Stato: " + valueColor + "Smutato\n");
                message.append(labelColor + "Staff: " + valueColor + getSenderName(sender) + "\n");
                message.append(separatorColor + "======================");

                boolean broadcastUnmuteMessage = getConfig().getBoolean("settings.broadcast-unmute-message", true);
                if (broadcastUnmuteMessage) {
                    Bukkit.broadcastMessage(message.toString());
                } else {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.hasPermission("advancedmoderation.viewmutes")) {
                            player.sendMessage(message.toString());
                        }
                    }
                }

                Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.AQUA + "Sei stato smutato sul server.");
                }

                sender.sendMessage(ChatColor.GREEN + "Giocatore smutato: " + targetPlayerName);

            } else {
                sender.sendMessage(ChatColor.RED + "Il giocatore non è mutato.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Si è verificato un errore durante lo smuting.");
        }
        return true;
    }

    // ==============================
    // Comando /tempmute
    // ==============================
    private boolean handleTempMuteCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /tempmute <player> <duration> [reason]");
            return true;
        }

        String targetPlayerName = args[0];
        long duration = parseDuration(args[1]);
        if (duration <= 0) {
            sender.sendMessage(ChatColor.RED + "Durata non valida. Usa m, h o d come suffisso (es. 10m, 1h, 1d).");
            return true;
        }

        String reason = String.join(" ", args).replaceFirst(args[0] + " " + args[1], "").trim();

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO MutedPlayers (playerName, reason, mutedBy, timestamp, duration) VALUES (?, ?, ?, ?, ?)"
        )) {
            stmt.setString(1, targetPlayerName.toLowerCase());
            stmt.setString(2, reason.isEmpty() ? "No reason" : reason);
            stmt.setString(3, getSenderName(sender));
            stmt.setString(4, String.valueOf(System.currentTimeMillis()));
            stmt.setString(5, String.valueOf(duration));
            stmt.executeUpdate();

            // Costruisci il messaggio formattato
            StringBuilder message = new StringBuilder();
            String separatorColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.separator", "&3"));
            String labelColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.label", "&e"));
            String valueColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.value", "&f"));

            message.append(separatorColor + "======================\n");
            message.append(labelColor + "Utente: " + valueColor + targetPlayerName + "\n");
            message.append(labelColor + "Stato: " + valueColor + "Mutato temporaneamente\n");
            message.append(labelColor + "Motivo: " + valueColor + (reason.isEmpty() ? "Non specificato" : reason) + "\n");
            message.append(labelColor + "Durata: " + valueColor + formatDuration(duration) + "\n");
            message.append(labelColor + "Staff: " + valueColor + getSenderName(sender) + "\n");
            message.append(separatorColor + "======================");

            boolean broadcastTempMuteMessage = getConfig().getBoolean("settings.broadcast-tempmute-message", true);
            if (broadcastTempMuteMessage) {
                Bukkit.broadcastMessage(message.toString());
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("advancedmoderation.viewmutes")) {
                        player.sendMessage(message.toString());
                    }
                }
            }

            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
            if (targetPlayer != null) {
                targetPlayer.sendMessage(ChatColor.RED + "Sei stato mutato temporaneamente sul server!" + (reason.isEmpty() ? "" : " Motivo: " + reason));
            }

            sender.sendMessage(ChatColor.GREEN + "Giocatore mutato temporaneamente: " + targetPlayerName);

        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Si è verificato un errore durante il temp mute.");
        }
        return true;
    }

    // ==============================
    // Parsing della Durata per Temp Mute
    // ==============================
    private long parseDuration(String duration) {
        try {
            if (duration.endsWith("m")) {
                return Long.parseLong(duration.replace("m", "")) * 60 * 1000; // Minuti
            } else if (duration.endsWith("h")) {
                return Long.parseLong(duration.replace("h", "")) * 60 * 60 * 1000; // Ore
            } else if (duration.endsWith("d")) {
                return Long.parseLong(duration.replace("d", "")) * 24 * 60 * 60 * 1000; // Giorni
            }
        } catch (NumberFormatException ignored) {}
        return -1; // Restituisce -1 se la durata non è valida
    }

    // ==============================
    // Formattazione della Durata per il Messaggio
    // ==============================
    private String formatDuration(long duration) {
        if (duration < 60 * 1000) {
            return "<1 minuto";
        } else if (duration % (24 * 60 * 60 * 1000) == 0) {
            return (duration / (24 * 60 * 60 * 1000)) + " giorni";
        } else if (duration % (60 * 60 * 1000) == 0) {
            return (duration / (60 * 60 * 1000)) + " ore";
        } else if (duration % (60 * 1000) == 0) {
            return (duration / (60 * 1000)) + " minuti";
        } else {
            return "Durata sconosciuta";
        }
    }

    // ==============================
    // Controllo dei Mute Scaduti
    // ==============================
    private void checkExpiredMutes() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM MutedPlayers WHERE duration != 'PERMANENT'"
        )) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String playerName = rs.getString("playerName");
                long timestamp = Long.parseLong(rs.getString("timestamp"));
                long durationMs = Long.parseLong(rs.getString("duration"));

                if (System.currentTimeMillis() > (timestamp + durationMs)) {
                    try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM MutedPlayers WHERE playerName = ?")) {
                        deleteStmt.setString(1, playerName.toLowerCase());
                        deleteStmt.executeUpdate();

                        // Costruisci il messaggio di smuting automatico
                        StringBuilder message = new StringBuilder();
                        String separatorColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.separator", "&3"));
                        String labelColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.label", "&e"));
                        String valueColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.value", "&f"));

                        message.append(separatorColor + "======================\n");
                        message.append(labelColor + "Utente: " + valueColor + playerName + "\n");
                        message.append(labelColor + "Stato: " + valueColor + "Smutato automaticamente\n");
                        message.append(separatorColor + "======================");

                        boolean broadcastUnmuteMessage = getConfig().getBoolean("settings.broadcast-unmute-message", true);
                        if (broadcastUnmuteMessage) {
                            Bukkit.broadcastMessage(message.toString());
                        } else {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (player.hasPermission("advancedmoderation.viewmutes")) {
                                    player.sendMessage(message.toString());
                                }
                            }
                        }

                        Player targetPlayer = Bukkit.getPlayer(playerName);
                        if (targetPlayer != null) {
                            targetPlayer.sendMessage(ChatColor.AQUA + "Sei stato smutato automaticamente.");
                        }

                        getLogger().info("Giocatore smutato automaticamente: " + playerName);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==============================
// Verifica se un Giocatore è Mutato
// ==============================
    protected boolean isPlayerMuted(String playerName) { // Cambiato da private a protected
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM MutedPlayers WHERE playerName = ?")) {
            stmt.setString(1, playerName.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // Restituisce true se il giocatore è mutato
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==============================
    // Listener per Bloccare i Messaggi dei Giocatori Mutati
    // ==============================
    public class ChatListener implements org.bukkit.event.Listener {
        @org.bukkit.event.EventHandler
        public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();
            if (isPlayerMuted(player.getName())) {
                event.setCancelled(true); // Cancella il messaggio del giocatore mutato
                player.sendMessage(ChatColor.RED + "Non puoi parlare in chat perché sei mutato.");
            }
        }
    }

    // ==============================
    // Rilevatore di CAPS
    // ==============================
    public class CapsDetectorListener implements org.bukkit.event.Listener {

        private final AdvancedModerationSystem plugin;
        private final Map <String, Integer> capsCountMap = new HashMap<>(); // Mappa per conteggiare i messaggi in maiuscolo

        public CapsDetectorListener(AdvancedModerationSystem plugin) {
            this.plugin = plugin;
        }

        @org.bukkit.event.EventHandler
        public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
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

    // ==============================
    // Metodo per Gestionare gli Avvisi Automatici
    // ==============================
    protected void handleAutoWarn(String targetPlayerName, String reason) throws SQLException {
        String senderName = "Console"; // Gli avvisi automatici vengono assegnati dalla console

        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO WarnedPlayers (playerName, warns, reasons, warnedBy) VALUES (?, 1, ?, ?) ON CONFLICT(playerName) DO UPDATE SET warns = warns + 1, reasons = reasons || ' | ' || ?, warnedBy = warnedBy || ' | ' || ?"
        )) {
            stmt.setString(1, targetPlayerName.toLowerCase());
            stmt.setString(2, reason);
            stmt.setString(3, senderName);
            stmt.setString(4, reason);
            stmt.setString(5, senderName);
            stmt.executeUpdate();

            int currentWarns = getCurrentWarnCount(targetPlayerName);

            // Costruisci il messaggio formattato
            StringBuilder message = new StringBuilder();
            String separatorColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.separator", "&3"));
            String labelColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.label", "&e"));
            String valueColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.value", "&f"));
            String warnCountColor = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.colors.warn-count-color", "&c"));

            message.append(separatorColor + "======================\n");
            message.append(labelColor + "Utente: " + valueColor + targetPlayerName + "\n");
            message.append(labelColor + "N. Warn: " + warnCountColor + currentWarns + "/3\n");
            message.append(labelColor + "Motivo: " + valueColor + reason + "\n");
            message.append(labelColor + "Staff: " + valueColor + senderName + "\n");
            message.append(separatorColor + "======================");

            boolean broadcastWarnMessage = getConfig().getBoolean("settings.broadcast-warn-message", true);
            if (broadcastWarnMessage) {
                Bukkit.broadcastMessage(message.toString());
            } else {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("advancedmoderation.viewwarns")) {
                        player.sendMessage(message.toString());
                    }
                }
            }

            getLogger().info("Avviso automatico assegnato a: " + targetPlayerName);
        }
    }

    // ==============================
    // Listener per la Chat Staff
    // ==============================
    public class StaffChatListener implements org.bukkit.event.Listener {

        private final AdvancedModerationSystem plugin;

        public StaffChatListener(AdvancedModerationSystem plugin) {
            this.plugin = plugin;
        }

        @org.bukkit.event.EventHandler
        public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
            if (!event.getMessage().startsWith("/sc ")) {
                return; // Ignora i messaggi che non iniziano con /sc
            }

            event.setCancelled(true); // Cancella il messaggio dalla chat pubblica
            String staffMessage = event.getMessage().replaceFirst("/sc ", ""); // Rimuovi "/sc " dal messaggio

            // Leggi il prefisso dalla config.yml
            String staffChatPrefix = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("settings.staff-chat-prefix", "&9[StaffChat] &5%s&9: &f%s"));

            // Invia il messaggio solo agli utenti con il permesso appropriato
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("advancedmoderation.staffchat")) {
                    player.sendMessage(String.format(staffChatPrefix, event.getPlayer().getName(), staffMessage));
                }
            }
        }
    }

    public Connection getDatabaseConnection() {
        return connection;
    }
}