/*
 * AntiFire.java
 * 
 * HISTORY: 
 *  12 Apr 2012 : first commit
 *  16 Apr 2012 : PSW : Adding commands
 *   8 May 2012 : PSW : Added permanent firestart log file, only written on disable
 *                      Added coloring to chat messages.
 */
 
 package com.yahoo.phil_work.antifire;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.NoSuchElementException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.yahoo.phil_work.antifire.FireLogEntry;
import com.yahoo.phil_work.antifire.AntifireLog;

/**
 * Fire control, for placement, damage, spread. Includes logging and command to TP to last fire placement
 * 
 * @author Filber66
 * inspired by @requestor x0pk1n
 */

public class AntiFire extends JavaPlugin {

    public Logger log;
	public PluginDescriptionFile pdfFile;
	private final AntiFireman antiFire = new AntiFireman (this);
	private String Log_Level;
	public AntifireLog fireLog;
	private Map<CommandSender, Integer> Lastlog;  // for log "next" command
	private File logFile; 
	private BufferedWriter logFileWriter;
	public String pluginName; // contains chat color controls
	
    public void onDisable() {
		// Write fireLog to a file, and then close it.
		int logCount = 0;
		try {
			for (FireLogEntry l : fireLog.list) {
				logCount++;
				logFileWriter.write (l.toString());
				logFileWriter.newLine();
			}
		}
		catch (IOException ex) {
			log.warning ("Error writing log " + logFile.getName() + ":" + ex.getMessage());
		}
		finally {
			try {
				logFileWriter.flush();
				logFileWriter.close();
            } catch (IOException ex) {
				log.warning ("Error flushing log " + logFile.getName() + ":" + ex.getMessage());
            }
			log.info ("Successfully wrote " + logCount + " log entries");
		}
		logFileWriter = null;
		logFile = null;
		fireLog = null;
		Lastlog.clear();
        
		System.out.println(pdfFile.getName() + " disabled.");
    }

    public static boolean validName(String name) {
        return name.length() > 2 && name.length() < 17 && !name.matches("(?i).*[^a-z0-9_].*");
    }

    public void onEnable() {
        // Register our events
		this.getServer().getPluginManager().registerEvents (this.antiFire, this);

		pdfFile = this.getDescription();
		pluginName = ChatColor.DARK_RED + pdfFile.getName() + ChatColor.RESET;
		log = this.getLogger();

		if (logFile == null) 
			try {
				logFile = new File(this.getDataFolder(), "antifire.log");
				logFileWriter = new BufferedWriter (new FileWriter (logFile, /* append= */true));
			}
			catch (IOException ex) {
				log.warning ("Error opening for write " + logFile.getName() + ":" + ex.getMessage());
			}
		
		Lastlog = new HashMap <CommandSender, Integer>();

		antiFire.initConfig();
		fireLog = new AntifireLog (this);
		
		// Set log level
		if (getConfig().isString ("log_level")) {
			Log_Level = getConfig().getString("log_level", "INFO"); // hidden config item
			try {
				log.setLevel (log.getLevel().parse (Log_Level));
				log.info ("successfully set log level to " + log.getLevel());
			}
			catch (Throwable IllegalArgumentException) {
				log.warning ("Illegal log_level string argument '" + Log_Level);
			}
		} else 
			log.setLevel (Level.INFO);

		antiFire.printConfig();

        log.info ("enabled, brought to you by Filbert66"); 
    }

    public String combineSplit(int startIndex, String[] string, String seperator) {
        StringBuilder builder = new StringBuilder();

        for (int i = startIndex; i < string.length; i++) {
            builder.append(string[i]);
            builder.append(seperator);
        }
        builder.deleteCharAt(builder.length() - seperator.length()); // remove
        return builder.toString();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        String commandName = command.getName().toLowerCase();
        String[] trimmedArgs = args;

        // sender.sendMessage(ChatColor.GREEN + trimmedArgs[0]);
        if (commandName.equals("antifire")) {
            return afCommands(sender, trimmedArgs);
		}
		if (commandName.equals("log")) {
			return logCommand (sender, trimmedArgs);
		}
		if (commandName.equals("tpf")) {
            return tpCommand(sender, trimmedArgs);
		}
		
        return false;
    }
	

	//     usage: <command> [next|<player>]
	private boolean logCommand(CommandSender sender, String[] args) {
		boolean colors = (sender instanceof Player) ? true : false;
		
		if (args.length == 0) {
			// default: display last 10
			Lastlog.put (sender, 10);
			for (String s : fireLog.lastFew(10, colors))
				 sender.sendMessage (s);
			return true;		
		}
		
		if (args [0].equals("next")) {
			int next;
			if ( !Lastlog.containsKey (sender))
				next = 0; // why you next and you haven't done it before?
			else 
				next = Lastlog.get(sender);
			
			for (String s : fireLog.nextFewFrom(10, next, colors)) {
				sender.sendMessage (s);
			}
			Lastlog.put (sender, next + 10);
			return true;		
		} else {
			// Try to see if it's a player name
			String p = args [0];
			if ( !validName (p)) {
				sender.sendMessage(ChatColor.RED + "bad player name '" + p + "'");
                return true;
            } else if ( !sender.getServer().getOfflinePlayer(p).hasPlayedBefore()) {
				sender.sendMessage ("'" + p + "' has never played before");
				return true;
			}
			boolean empty = true;
			for (String s : fireLog.lastFewBy(10, p, colors)) {
				empty = false;
				sender.sendMessage (s);
			}
			if (empty)
				sender.sendMessage (pluginName + ": no entries in log by " + p);
			return true;
		}
	}
	
	// usage: <command> [last|#|<playername>] 
	private boolean tpCommand(CommandSender sender, String[] args) {
		if ( !(sender instanceof Player)) {
			sender.sendMessage (pdfFile.getName() + ": Cannot teleport SERVER");
			return true;
		}
		Player p = (Player)sender;
		FireLogEntry logEntry;
		
		// default is "last"
		if (args.length == 0 || args [0].equalsIgnoreCase("last")) {
			try {
				logEntry = fireLog.list.getLast();
			} catch (NoSuchElementException exc) {
				logEntry = null;
			}
			if (logEntry == null) {
				sender.sendMessage (pluginName + ": no entries in log!");
				return true;
			}
		} else if (args [0].length() == 1 && Character.isDigit(args[0].charAt(0))) {
			// # format, meaning back up # 
			logEntry = fireLog.lastMinus (Integer.parseInt(args[0]));
			if (logEntry == null) {
				sender.sendMessage (pluginName + ": too far back in log");
				return true;
			}
		} else { // try to parse as a player name. BUG: Crash on multi-digit "name"
			String n = args [0];
			if ( !validName (n)) {
				sender.sendMessage(ChatColor.RED + "bad player name '" + n + "'");
                return true;
            } else if ( !sender.getServer().getOfflinePlayer(n).hasPlayedBefore()) {
				sender.sendMessage ("'" + n + "' has never played before");
				return true;
			}
			logEntry = fireLog.lastBy (n);
			if (logEntry == null) {
				sender.sendMessage (pluginName + ": no entries in log for " + n);
				return true;
			}			
		}
		p.teleport (logEntry.loc, TeleportCause.COMMAND);	
		p.sendMessage (logEntry.toStringNoLoc(/*colors=*/true)); 
		return true;
	}		

    private boolean afCommands(CommandSender sender, String[] args) {
        sender.sendMessage ("Sorry, commands not yet implemented");
		return true;
	/*	
		try {
            Player player = null;
            String kicker = "SERVER";
            if (sender instanceof Player) {
                player = (Player) sender;
                kicker = player.getName();
				if ( !player.isOp()) {
					sender.sendMessage("must be OP to ban");
					return true;
				}						
            }

            // Has enough arguments?
            if (args.length < 1)
                return false;

            String p = args[0]; // Get the victim's name
            if (!validName(p)) {
                sender.sendMessage("bad player name '" + p + "'");
                return true;
            }
            p = p.toLowerCase();
            Player victim = this.getServer().getPlayer(p); // What player is
            // really the victim?
            // Reason stuff
            String reason = null;
            if (args.length > 1) {  // reason on rest of line
                reason = combineSplit(1, args, " ");
            }
			reason = (reason != null ? "for " + reason : "");
			
            boolean broadcast = false;
            String ip = null;
            if (victim != null) {
                ip = victim.getAddress().getAddress().getHostAddress();
            }
			else {
				sender.sendMessage(p + " not online");
			}
			
            // Log in console
            log.info(kicker + " banning player '" + p + "' " + reason);

            if (victim != null) { 
                // Send message to victim
                String kickerMsg = kicker + " has banned you " + reason;          
				this.getServer().banIP (ip);
				victim.setBanned (true);
				victim.kickPlayer(kickerMsg);
            }
            // If he isn't online we should check to see if the server even
            // knows who he is
            else {
                OfflinePlayer off = getServer().getOfflinePlayer(p);
                if (!off.hasPlayedBefore()) {
                    // get offline player is case sensitive ...
                    OfflinePlayer[] oPlayers = getServer().getOfflinePlayers();
                    for (OfflinePlayer oP : oPlayers) {
                        if (oP.getName().equalsIgnoreCase(p)) {
                            off = oP;
                            break;
                        }
                    }
                }
                if (off == null || !off.hasPlayedBefore()) {
                    sender.sendMessage("Never saw player '" + p + "' before");
                } else {
					off.setBanned (true);
					Date d = new Date (off.getLastPlayed());
					sender.sendMessage ("banned (unknown IP) " + p + ", who was last seen on " + d);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return true;
	 */
	}

}
