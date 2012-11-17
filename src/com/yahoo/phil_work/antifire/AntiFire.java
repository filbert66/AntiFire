/*
 * AntiFire.java
 * 
 * HISTORY: 
 *  12 Apr 2012 : first commit
 *  16 Apr 2012 : PSW : Adding commands
 *   8 May 2012 : PSW : Added permanent firestart log file, only written on disable
 *                      Added coloring to chat messages.
 *  21 Aug 2012 : PSW : Added 'af' admin commands, flushLog()
 */
 
 package com.yahoo.phil_work.antifire;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
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
import com.yahoo.phil_work.BlockIdList;

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
	
	public void flushLog (CommandSender requestor) {
		this.flushLog (requestor, 0);  // zero seconds from Now
	}
	
    public void flushLog (CommandSender requestor, int secondsAge) {
		// Write fireLog to a file
		long youngest = new Date().getTime() - (secondsAge * 1000); // anything younger won't get flushed
		int logCount = 0;
		
		if (logFileWriter == null) {
			log.warning ("Error flushing log. Restart server to reinitialize logfile");
			return;
		} 
		try {
			FireLogEntry l;
			
			while (fireLog.list.size() > 0 && fireLog.list.peek().date.getTime() <= youngest) { 
				// only keep going if first entry is older than youngest
				logCount++;
				logFileWriter.write (fireLog.list.remove().toString());
				logFileWriter.newLine();
			}
		}
		catch (IOException ex) {
			log.warning ("Error writing log " + logFile.getName() + ":" + ex.getMessage());
		}
		finally {
			try {
				logFileWriter.flush();
            } catch (IOException ex) {
				log.warning ("Error flushing log " + logFile.getName() + ":" + ex.getMessage());
            }
			if (logCount > 0) {
				String msg = "Successfully wrote " + logCount + " firelog entries";
				
				if (requestor == null)
					log.info (msg);
				else 
					requestor.sendMessage (msg);
			}
		}
	}
	
	public void onDisable() {
		// Write fireLog to a file, and then close it.
		try {
			this.flushLog(null);
			logFileWriter.close();
		} catch (IOException ex) {
			log.warning ("Error closing log " + logFile.getName() + ":" + ex.getMessage());

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
        if (commandName.equals("af")) {
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
	
		if (args.length == 0) 
			return false; // print usage

		String commandName = args[0].toLowerCase();
        if (commandName.equals("print")) {
            return antiFire.printConfig (sender);
		}
		else if (commandName.equals("fireproof") || commandName.equals ("burnable")) {
			boolean whitelist = this.getConfig().getBoolean ("nerf_fire.whitelist");
			if (args.length == 1) { // no changes, just print
				if (whitelist)
					sender.sendMessage ("Blocks below are burnable:");
				else {
					sender.sendMessage ("Blocks below are fireproof");
				}
				antiFire.FireResistantList.printList (sender);
			}
			else {
				BlockIdList newBlocks = new BlockIdList (this, args[1], sender); // parse new list
				 // use new list so that if it fails, we don't wipe out old one.
				
				if ( !newBlocks.isEmpty()) {
					if ((whitelist && !commandName.equals ("burnable")) ||
						(!whitelist && !commandName.equals ("fireproof")) )
					{ // then command will also reset nerf_fire.whitelist 
						this.getConfig().set ("nerf_fire.whitelist", commandName.equals ("burnable"));
						
						antiFire.FireResistantList.setList (newBlocks); // keep other stuff same
						this.getConfig().set ("nerf_fire.blocklist", antiFire.FireResistantList.asString());
						
						sender.sendMessage ("Reset to " + (commandName.equals ("burnable") ? "whitelist" : "blacklist") + " with new blocklist below");
					}
					else 
					{ // commandname matches current setting of .whitelist. Add to list
						antiFire.FireResistantList.append (newBlocks);
						sender.sendMessage ("Appended to " + (commandName.equals ("burnable") ? "whitelist" : "blacklist") + " with new blocklist below");
					}
					antiFire.FireResistantList.printList (sender);
				}
			}
			return true;									
		}
		else if (commandName.equals("save")) {
			this.saveConfig();
			return true;									
		}
		else if (commandName.equals("flush")) {
			flushLog(sender);
			return true;									
		}
		else if (commandName.equals("spread")) {
			if (args.length == 1) {
				if ( !(sender instanceof Player)) // no params or can't impute world
					sender.sendMessage ("Fire spread is disabled in:" + this.getConfig().getString("nerf_fire.nospread")); // do I need to check for list?
				else {
					String wName = ((Player)sender).getLocation().getWorld().getName();
				
					sender.sendMessage ("Fire spread in your world is " + 
										(antiFire.ifConfigContains ("nerf_fire.nospread", wName) ? ChatColor.BLACK + "NOT " + ChatColor.RESET : "") + "allowed");
				}
			} else if ( !(sender instanceof Player)) {
				sender.sendMessage (pdfFile.getName() + ": Cannot get current world of SERVER");
			} else {
				// have a parameter
				Player p = (Player)sender;
				boolean turnOnSpread = args[1].toLowerCase().equals("on");
				String wName = p.getLocation().getWorld().getName();

				// Not working when config is a string. Need to 
				List <String> noSpread = (this.getConfig().isString ("nerf_fire.nospread") ?
											new ArrayList(Arrays.asList(getConfig().getString ("nerf_fire.nospread").split(","))) : 
											getConfig().getStringList ("nerf_fire.nospread") );
				this.log.fine ("Current nospread=" + noSpread);
				if (turnOnSpread) { // turn on spread means remove nospread
					noSpread.remove (wName);
				}
				else if (!turnOnSpread && !antiFire.ifConfigContains ("nerf_fire.nospread", wName))
					noSpread.add (wName);
				
				this.getConfig().set ("nerf_fire.nospread", noSpread);
				sender.sendMessage (ChatColor.BLUE + "Nospread now effective in: " + ChatColor.GRAY + noSpread);
			}
			return true;									
		}
		
		try {
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return false;
	}

}
