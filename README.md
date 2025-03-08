### **Descrizione del Plugin: AdvancedModerationSystem**

**AdvancedModerationSystem** è un plugin di moderazione avanzata per server Minecraft che offre uno strumento completo per gestire i giocatori in modo efficace. Con funzionalità come warn, mute permanente, mute temporaneo, chat staff privata e rilevamento automatico di CAPS, questo plugin ti permette di mantenere l'ordine sul tuo server senza compromettere la flessibilità e la personalizzazione.
------
#### **Funzionalità Chiave**
- **Warn System:** Assegna avvertimenti ai giocatori con un limite configurabile prima del ban automatico.
- **Mute Permanente:** Impedisce a un giocatore di scrivere in chat fino a quando non viene smutato.
- **Mute Temporaneo:** Applica un mute con una durata specificata (minuti, ore, giorni).
- **Chat Staff Privata:** Consente ai membri dello staff di comunicare in una chat dedicata, visibile solo a chi ha il permesso appropriato.
- **Rilevatore di CAPS Automatico:** Avvisa automaticamente i giocatori dopo 3 messaggi consecutivi in maiuscolo, prevenendo lo spam di testi troppo invasivi.
- **Configurazione Flessibile:** Tutte le impostazioni (colori, messaggi, permessi, limiti) sono completamente configurabili tramite il file `config.yml`.
- **Integrazione con AdvancedBanSystem:** Se installato, il plugin può eseguire ban automatici quando un giocatore supera il numero massimo di warn.
-----
#### **Caratteristiche Distinctive**
- **Messaggi Formattati e Colorati:** Ogni azione di moderazione viene visualizzata in chat con un formato chiaro e colorato, migliorando la leggibilità e l'esperienza utente.
- **Database Integrato:** Utilizza un database SQLite interno per registrare tutti i dati relativi ai warn e ai mute, garantendo persistenza e tracciabilità.
- **Personalizzazione:** Adatta il plugin alle tue esigenze modificando semplicemente il file di configurazione.
- **Compatibilità:** Progettato per funzionare con versioni moderne di Spigot/Bukkit (da 1.20 in poi).
-----
#### **Comandi Disponibili**
| Comando        | Descrizione                                      | Permesso                     |
|----------------|-------------------------------------------------|------------------------------|
| `/warn <player> [reason]` | Assegna un warn a un giocatore.               | `advancedmoderation.warn`    |
| `/resetwarns <player>`   | Resetta il conteggio dei warn per un giocatore. | `advancedmoderation.resetwarns` |
| `/warnlist <player>`     | Visualizza la lista dei warn assegnati a un giocatore. | `advancedmoderation.warnlist`  |
| `/staffchat <message>`   | Invia un messaggio nella chat staff privata.   | `advancedmoderation.staffchat` |
| `/mute <player> [reason]`| Applica un mute permanente a un giocatore.      | `advancedmoderation.mute`     |
| `/unmute <player>`       | Rimuove il mute da un giocatore.              | `advancedmoderation.unmute`   |
| `/tempmute <player> <duration> [reason]` | Applica un mute temporaneo con durata specificata. | `advancedmoderation.tempmute` |

-----
#### **Impostazioni Configurabili**
Il plugin include un file `config.yml` che ti consente di personalizzare diverse opzioni:
- **Numero Massimo di Warn Prima del Ban Automatico:** Imposta quante volte un giocatore può essere warnato prima di essere bannato.
- **Messaggi Personalizzati:** Modifica i messaggi mostrati ai giocatori durante le azioni di moderazione.
- **Colori dei Messaggi:** Scegli i colori per le etichette, i separatori e i valori nei messaggi formattati.
- **Broadcasting:** Configura se i messaggi di warn e mute devono essere visibili a tutti o solo allo staff.
-----
#### **Requisiti**
- **Versione di Minecraft:** 1.20 o successiva.
- **Dipendenze Opzionali:**
  - **AdvancedBanSystem:** Richiesto per il ban automatico.
------
#### **Installazione**
1. Scarica il file JAR del plugin da questa [pagina](https://github.com/Matty47ghigo/AdvancedModerationSystem/releases/) o compila il progetto tramite Maven.
2. Copia il file JAR nella cartella `plugins` del tuo server.
3. Avvia o riavvia il server per generare il file `config.yml`.
4. Configura le impostazioni nel file `config.yml` secondo le tue preferenze.
5. (Opzionale) Installa **AdvancedBanSystem** per abilitare il ban automatico [link](https://github.com/Matty47ghigo/AdvancedBanSystem/releases/).
------
#### **Utilizzo**
- Usa i comandi elencati sopra per gestire i giocatori.
- La chat staff (`/staffchat`) è accessibile solo agli utenti con il permesso `advancedmoderation.staffchat`.
- Il rilevatore di CAPS avvisa automaticamente i giocatori dopo 3 messaggi consecutivi in maiuscolo.
------
#### **Crediti**
Questo plugin è stato sviluppato per fornire uno strumento potente e facile da usare per la moderazione dei server Minecraft. Grazie alla community di Bukkit/Spigot e alle loro risorse, il plugin utilizza SQLite per la gestione dei dati e fornisce una base solida per ulteriori estensioni.
-----
#### **Supporto**
Se hai problemi o domande riguardo al plugin, puoi contattare lo sviluppatore o aprire un issue su GitHub. Assicurati di fornire dettagli sufficienti per facilitare la risoluzione del problema.
