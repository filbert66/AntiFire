/*
 * AntiFire.java
 * 
 * HISTORY: 
 *  12 Apr 2012 : first commit
 *  16 Apr 2012 : PSW : Adding commands
 *   8 May 2012 : PSW : Added permanent firestart log file, only written on disable
 *                      Added coloring to chat messages.
 *  21 Aug 2012 : PSW : Added 'af' admin commands, flushLog()
 *  15 Nov 2012 : PSW : Added 'extinguish' command
 *  24 Nov 2012 : PSW : Added 'reload' command
 *  12 Dec 2012 : PSW : Added "last" option to "extinguish" command, and Player name.
 *  12 Jan 2013 : PSW : Add commands for logstart, nodamageto
 *  16 Apr 2013 : PSW : Added commands for charcoal, crystal, explosion, noburn(mob|player)by.* 
 *  17 May 2013 : PSW : Added commands for charcoaldrop settings; can't set charcoaldrop.treetypedrop
 *  12 Sep 2013 : PSW : Updated per latest Metrics API.
 *  23 Sep 2013 : PSW : Added commands for lava placement config
 *  15 Oct 2013 : PSW : Added commands for timed config modding.
 *  20 May 2014 : PSW : TODO: add UUID log & transport command-line support. 
 * 					  : Practically to support, use PlayerChatTabCompleteEvent() to complete UUIDs.
 * 						complete names and UUIDs? Both are alphanumeric
 *  07 Sep 2014 : PSW : Added language support. 
 *  19 Dec 2014 : PSW : Unitialized logFile variables.
 *  12 Jul 2015 : PSW : Added command support for variable timed range.
 *  03 Aug 2015 : PSW : Added command support for world-setting of rain, nether variables for "timed".
 */
 
package com.yahoo.phil_work.antifire;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
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
import org.bukkit.util.Vector;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import org.mcstats.Metrics;
//import org.spigotmc.Metrics;

import com.yahoo.phil_work.LanguageWrapper;

import com.yahoo.phil_work.antifire.FireLogEntry;
import com.yahoo.phil_work.antifire.AntifireLog;
import com.yahoo.phil_work.BlockIdList;
import com.yahoo.phil_work.antifire.IgniteCause;


/**
 * Fire control, for placement, damage, spread. Includes logging and command to TP to last fire placement
 * 
 * @author Filber66
 * inspired by @requestor x0pk1n
 */

public class AntiFire extends JavaPlugin {

    public Logger log;
    LanguageWrapper language;
	PluginDescriptionFile pdfFile;
	protected final AntiFireman antiFire = new AntiFireman (this);
	private String Log_Level;
	public AntifireLog fireLog;
	private Map<CommandSender, Integer> Lastlog;  // for log "next" command
	private File logFile = null; 
	private BufferedWriter logFileWriter = null;
	public String pluginName; // contains chat color controls
	
	// Metrics
	private int blocksSaved = 0, entitiesSaved = 0;
	private int logQueries = 0, teleports = 0, extinguishCommands = 0;

	public boolean flushLog (CommandSender requestor) {
		return this.flushLog (requestor, 0);  // zero seconds from Now
	}
	
    public boolean flushLog (CommandSender requestor, int secondsAge) {
		// Write fireLog to a file
		long youngest = new Date().getTime() - (secondsAge * 1000); // anything younger won't get flushed
		int logCount = 0;
		
		if (logFileWriter == null) {
			log.warning ("Error flushing log. Restart server to reinitialize logfile");
			return false;
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
		return true;
	}
	
	public void onDisable() {
		// Write fireLog to a file, and then close it.
		try {
			if (this.flushLog(null))
				logFileWriter.close();
		} catch (IOException ex) {
			log.warning ("Error closing log " + logFile.getName() + ":" + ex.getMessage());

		}
		logFileWriter = null;
		logFile = null;
		fireLog = null;
		Lastlog.clear();
		
		antiFire.clearTimedConfig ();
        
		System.out.println(pdfFile.getName() + " disabled.");
    }

    public static boolean validName(String name) {
        return name.length() > 2 && 
        	  (name.length() < 17 || (name.indexOf("_placed_lava") != -1 && name.length() < 29)) && 
        	  !name.matches("(?i).*[^a-z0-9_].*");
    }
	private static UUID rID = UUID.randomUUID();
	private static int lengthUUID = rID.toString().length();
    public boolean validUUID (String s) {
		log.info ("checking ID of length " + s.length() + "; expecting " + lengthUUID);
		log.info ("ID pattern match: " + s.matches("(?i)(-[A-Za-z0-9])*"));
		return s.length() == lengthUUID		 && 
        	  !s.matches("(?i)(-[A-Za-z0-9])*");
    }
    public UUID getUUIDFromName (String playerName) {
		UUID id;
		
		if ( !validUUID (playerName))
			return null;
		
		try {
			log.info ("Checking for UUID in " + playerName);
			id = UUID.fromString (playerName);
			log.info ("Found valid UUID " + id);
		} catch (IllegalArgumentException e) {
			id = null;
		}
    	return id;
    }

    public void onEnable() {
        // Register our events
		this.getServer().getPluginManager().registerEvents (this.antiFire, this);

		pdfFile = this.getDescription();
		pluginName = ChatColor.DARK_RED + pdfFile.getName() + ChatColor.RESET;
		language = new LanguageWrapper(this, "eng"); // English locale
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
				log.setLevel (Level.parse (Log_Level));
				log.info ("successfully set log level to " + log.getLevel());
			}
			catch (Throwable IllegalArgumentException) {
				log.warning ("Illegal log_level string argument '" + Log_Level);
			}
		} else 
			log.setLevel (Level.INFO);

		antiFire.printConfig();

		// Add Metrics to mcstats.org
		try {
			Metrics metrics = new Metrics(this);
			
			Metrics.Graph graph = metrics.createGraph("Extinguished");
			graph.addPlotter(new Metrics.Plotter("Commands executed") {
		
					@Override
					public int getValue() {
							return extinguishCommands; 
					}
			});

			graph.addPlotter(new Metrics.Plotter("Blocks") {
		
					@Override
					public int getValue() {
							return blocksSaved; 
					}
			});
		
			graph.addPlotter(new Metrics.Plotter("Entities") {
		
					@Override
					public int getValue() {
							return entitiesSaved;
					}
			});

			Metrics.Graph graph1 = metrics.createGraph("AF Data");
			graph1.addPlotter(new Metrics.Plotter("Total Log Queries") {
		
				@Override
				public int getValue() {
					return logQueries;
				}
		
			});

			graph1.addPlotter(new Metrics.Plotter("Total Teleports to Firestarts") {
		
				@Override
				public int getValue() {
					return teleports;
				}
			});

			graph1.addPlotter(new Metrics.Plotter("Log Entries") {
		
				@Override
				public int getValue() {
					return antiFire.logEntries;
				}
			});
			graph1.addPlotter(new Metrics.Plotter("Fireproofed starts") {
		
				@Override
				public int getValue() {
					return antiFire.fireProofed;
				}
			});
			graph1.addPlotter(new Metrics.Plotter("Nerfed starts") {
		
				@Override
				public int getValue() {
					return antiFire.nerfedStart;
				}
			});
			graph1.addPlotter(new Metrics.Plotter("Nerfed lava") {
		
				@Override
				public int getValue() {
					return antiFire.nerfedLava;
				}
			});

			metrics.start();
		} catch (IOException e) {
			// Failed to submit the stats :-(
			log.warning ("Unable to start mcstats.org metrics: " +e);
		}

        log.info ("enabled, brought to you by Filbert66"); 
    }

    void sendMsg (CommandSender requestor, String msg)
	{
		if (requestor == null)
			return;
			
		if (requestor instanceof Player) {
			requestor.sendMessage (msg);
		}
		else // Server console. Color codes may look nice on a terminal, but not on text file
		{
			requestor.sendMessage(ChatColor.stripColor (msg));
		}
	}

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        String commandName = command.getName().toLowerCase();
        String[] trimmedArgs = args;

        if (commandName.equals("af")) {
            return afCommands(sender, trimmedArgs);
		}
		if (commandName.equals("log")) {
			this.logQueries++;
			return logCommand (sender, trimmedArgs);
		}
		if (commandName.equals("tpf")) {
            return tpCommand(sender, trimmedArgs);
		}
		if (commandName.equals ("extinguish")) {
			this.extinguishCommands++;
			return extinguishCommand (sender, trimmedArgs);
		}
        return false;
    }
	

	//     usage: <command> [next|<player>]
	@SuppressWarnings("deprecation") // ignore getOfflinePlayer(String)
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
			String p = args [0];

			UUID id = getUUIDFromName (p); 
			// Try to see if it's a player name
			if (id == null && !validName (p)) {
				sendMsg (sender, language.get (sender, "badName", ChatColor.RED + "bad player name '{0}'", p));
                return true;
            } 

			// Allow for lava suffix in player name
			String pruned = p;
            int start_placed = p.indexOf ("_placed_lava");
            if (start_placed != -1) 
            	pruned = p.substring (0, start_placed);
				
	        if ((id != null && !sender.getServer().getOfflinePlayer (id).hasPlayedBefore()) ||
             	(id == null && !sender.getServer().getOfflinePlayer(pruned).hasPlayedBefore()) ) 
            {
				sendMsg (sender, language.get (sender, "unseenPlayer", "'{0}' has never played before", pruned));
				return true;
			}
			boolean empty = true;
			if (id != null) {
				for (String s : fireLog.lastFewBy(10, id, colors)) {
					empty = false;
					sender.sendMessage (s);
				}
			} else {				
				for (String s : fireLog.lastFewBy(10, p, colors)) {
					empty = false;
					sender.sendMessage (s);
				}
			}
			if (empty)
				sendMsg (sender, language.get (sender, "noLogBy", pluginName + ": no entries in log by {0}", p));
			return true;
		}
	}
	
	// usage: <command> [last|#|<playername>] 
	@SuppressWarnings("deprecation") // ignore getOfflinePlayer(String)
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
				sendMsg (sender, language.get (sender, "noLog", pluginName + ": no entries in log!"));
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
			UUID id = getUUIDFromName (n);
			if (id == null && !validName (n)) {
				sendMsg (sender, language.get (sender, "badName", ChatColor.RED + "bad player name '{0}'", n));
				return true;
			} 
				
            if ((id != null && !sender.getServer().getOfflinePlayer (id).hasPlayedBefore () ) ||
            	(id == null && !sender.getServer().getOfflinePlayer(n).hasPlayedBefore()) ) {
				sendMsg (sender, language.get (sender, "unseenPlayer", "'{0}' has never played before", n));
				return true;
			}
			logEntry = (id != null ? fireLog.lastBy (id) : fireLog.lastBy (n));
			if (logEntry == null) {
				sendMsg (sender, language.get (sender, "noLogBy", pluginName + ": no entries in log by {0}", n));
				return true;
			}			
		}
		p.teleport (logEntry.loc, TeleportCause.COMMAND);	
		p.sendMessage (logEntry.toStringNoLoc(/*colors=*/true)); 
		
		this.teleports++;
		
		return true;
	}		

    private boolean afCommands(CommandSender sender, String[] args) {
	
		if (args.length == 0) 
			return false; // print usage

		String commandName = args[0].toLowerCase();
        if (commandName.equals("print")) {
            return antiFire.printConfig (sender);
		}
		// For analyzing timed block design
		else if (commandName.equals ("time")) {
			if ( !(sender instanceof Player) || args.length > 1) { // any arg prints full list
				for (World w : this.getServer().getWorlds()) {
					sender.sendMessage (w.getName() + " time: " + w.getTime() + ", full: " + w.getFullTime());					
				}
			} else {
				World w = ((Player)sender).getLocation().getWorld();
				sender.sendMessage (w.getName() + " time: " + w.getTime() + ", full: " + w.getFullTime());					
			}
			return true;
		}	
		else if (commandName.equals("fireproof") || commandName.equals ("burnable")) {
			boolean whitelist = this.getConfig().getBoolean ("nerf_fire.whitelist");
			if (args.length == 1) { // no changes, just print
				if (whitelist)
					sender.sendMessage (language.get (sender, "burnList", "Blocks below are burnable:"));
				else {
					sender.sendMessage (language.get (sender, "proofList", "Blocks below are fireproof"));
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
		else if (commandName.equals("reload")) {
			this.reloadConfig();
			this.antiFire.initConfig();
			return true;									
		}

		else if (commandName.equals("flush")) {
			flushLog(sender);
			return true;									
		}
		else if (commandName.equals("timed")) {
			final String TimedName = "nerf_fire.timedcauses";
			final String netherName = "nerf_fire.timeNetherackToo";
			final String rainName = "nerf_fire.rainOverridesTimed";
			
			if (args.length == 1) {
				antiFire.printConfig (sender, TimedName);
				antiFire.printConfigKey (sender, netherName);
			} else {
				String key = args[1];
				IgniteCause cause = IgniteCause.matchIgniteCause (key);
				if (cause == null) {
				
					if (key.startsWith ("netherack") || key.startsWith ("rain")) 
					{
						final String item = (key.startsWith ("netherack") ? netherName : rainName);
						if (args.length == 3) {
							boolean newVal = args[2].equalsIgnoreCase ("true");
							if ( !(sender instanceof Player)) {// no params or can't impute world
								sender.sendMessage (pdfFile.getName() + ": Cannot get current world of SERVER");
								return false;
							}
							String wName = ((Player)sender).getLocation().getWorld().getName();
							
							List <String> tNeth = (this.getConfig().isString (item) ?
													new ArrayList<String>(Arrays.asList(getConfig().getString (item).split(","))) : 
													getConfig().getStringList (item) );
							// this.log.fine ("Current tNeth=" + tNeth);
							if (! newVal) { 
								tNeth.remove (wName);
							}
							else if (! antiFire.ifConfigContains (item, wName))
								tNeth.add (wName);
				
							this.getConfig().set (item, tNeth);
							final String msg = (key.startsWith ("netherack") ? "Timing netherack" : "Rain stops timed");
							sender.sendMessage (ChatColor.BLUE + msg + " now in: " + ChatColor.GRAY + tNeth);						
						} else 
							antiFire.printConfigKey (sender, item);
						return true;

					} else if ( !key.equals ("clear")) {
						sendMsg (sender, ChatColor.RED + "Unknown ignite cause '" + ChatColor.RESET + key + "'");
						return false;					
					}
					// else equals clear
					if (antiFire.clearTimedConfig ()) {
						sender.sendMessage ("Removed all fire delays");
					} else
						sendMsg (sender, ChatColor.YELLOW + "No configured fire delays to clear");
					return true;
				}
				if (args.length == 2) { // no param; just print that one
					String FQK = TimedName + "." + cause.toString();
					if (this.getConfig().isSet (FQK))
						return antiFire.printConfigKey (sender, FQK);
					else 
						sendMsg (sender, key + ChatColor.YELLOW + " is not a currently configured timed cause.");
				} else { // want to set one value
					if (args[2].equals ("clear")) { 
						if (antiFire.clearTimedConfig (cause))
							sender.sendMessage ("Removed delay for fire cause " + cause);
						else
							sendMsg (sender, ChatColor.YELLOW + "No delay configured for " + cause);
					} else try { 
						String[] cutArgs = Arrays.copyOfRange (args, 2, args.length);
						String delay = Arrays.toString(cutArgs).replaceAll (", ", " ");
						delay = delay.substring (1, delay.length()-1); // remove leading/trailing ']'
						boolean result = antiFire.setTimedConfig (cause, delay);
						if ( !result)
							sendMsg (sender, ChatColor.YELLOW + "Unable to set " + cause + " to " + delay + " delay");
						else 
							sender.sendMessage ("Set delay of " + delay + "ms for fire cause of " + cause);
					} catch (Exception exc) {
						sendMsg (sender, language.get (sender, "invalid", ChatColor.RED + "invalid value for {0}", "timed delay"));
						return false;
					}
				}
			}
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

				List <String> noSpread = (this.getConfig().isString ("nerf_fire.nospread") ?
											new ArrayList<String>(Arrays.asList(getConfig().getString ("nerf_fire.nospread").split(","))) : 
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
		else if (commandName.equals("charcoal")) {
			if (args.length == 1) {
				if ( !(sender instanceof Player)) // no params or can't impute world
					sender.sendMessage ("Wood drops charcoal in: " + this.getConfig().getString("nerf_fire.wooddropscharcoal")); // do I need to check for list?
				else {
					String wName = ((Player)sender).getLocation().getWorld().getName();
				
					sender.sendMessage ("Wood drops charcoal is " + 
										(antiFire.ifConfigContains ("nerf_fire.wooddropscharcoal", wName) ? ChatColor.RED + "ON " + ChatColor.RESET : 
										 "off ") );
				}
			} else {
				// have a parameter
				if (args[1].toLowerCase().equals("max")) {
					if (args.length == 2) { // print only
						sender.sendMessage ("Current charcoal drop is " + 
											(this.getConfig().getBoolean ("nerf_fire.charcoaldrop.random") ? "random up to " : "a fixed ") + 
											this.getConfig().getInt ("nerf_fire.charcoaldrop.max") );
						return true;
					} // else have anotehr param
					
					try { 
						int max = Integer.parseInt(args[2]);
						this.getConfig().set("nerf_fire.charcoaldrop.max", max);

						sendMsg (sender, "Set " + ChatColor.BLUE + "nerf_fire.charcoaldrop.max" + ChatColor.RESET + " to " + max);
						return true;
					} catch (Exception exc) {
						sendMsg (sender, language.get (sender, "invalid", ChatColor.RED + "invalid value for {0}", "charcoaldrop.max"));
						return false;
					}	
				} else if (args[1].toLowerCase().equals ("random") || args[1].toLowerCase().equals ("fixed")) {
					boolean ifRandom = args[1].toLowerCase().equals("random");
					this.getConfig().set("nerf_fire.charcoaldrop.random", ifRandom);

					sendMsg (sender, "Set " + ChatColor.BLUE + "nerf_fire.charcoaldrop.random" + ChatColor.RESET + " to " + ifRandom);
					return true;
				} else if ( !(sender instanceof Player)) {
					sender.sendMessage (pdfFile.getName() + ": Cannot get current world of SERVER");
				}
				// else interpret param as a boolean for whether to drop or not
				
				Player p = (Player)sender;
				boolean charcoalOn = args[1].toLowerCase().equals("on");
				String wName = p.getLocation().getWorld().getName();

				// Not working when config is a string. Need to 
				List <String> noSpread = (this.getConfig().isString ("nerf_fire.wooddropscharcoal") ?
											new ArrayList <String>(Arrays.asList(getConfig().getString ("nerf_fire.wooddropscharcoal").split(","))) : 
											getConfig().getStringList ("nerf_fire.wooddropscharcoal") );
				// this.log.fine ("Current nospread=" + noSpread);
				if ( !charcoalOn) { // turn on spread means remove nospread
					noSpread.remove (wName);
				}
				else if (charcoalOn && !antiFire.ifConfigContains ("nerf_fire.wooddropscharcoal", wName))
					noSpread.add (wName);
				
				this.getConfig().set ("nerf_fire.wooddropscharcoal", noSpread);
				sender.sendMessage (ChatColor.BLUE + "Charcoal drops now On in: " + ChatColor.GRAY + noSpread);
			}
			return true;									
		}
		
		else if (commandName.equals ("opstart")) {
			boolean ifSet = this.getConfig().getBoolean ("nerf_fire.nostartby.op");
			
			if (args.length == 1) {
				sendMsg (sender, "OPs are " + (ifSet ? "" : ChatColor.RED + "NOT " + ChatColor.RESET) + "allowed to start fires");
				return true;
			}
			// else have a param
			boolean turnOn = args[1].toLowerCase().equals ("true");
			this.getConfig().set ("nerf_fire.nostartby.op", !turnOn);
			sendMsg (sender, ChatColor.BLUE + "nerf_fire.nostartby.op" + ChatColor.DARK_BLUE + 
								" now " + ChatColor.GRAY + !turnOn);
			return true;
		}				
		else if (commandName.equals ("opplace")) {
			boolean ifSet = this.getConfig().getBoolean ("nerf_fire.noplacelavaby.op");
			
			if (args.length == 1) {
				sendMsg (sender, "OPs are " + (ifSet ? ChatColor.RED + "NOT " + ChatColor.RESET : "") + "allowed to place lava");
				return true;
			}
			// else have a param
			boolean turnOn = args[1].toLowerCase().equals ("true");
			this.getConfig().set ("nerf_fire.noplacelavaby.op", !turnOn);
			sendMsg (sender, ChatColor.BLUE + "nerf_fire.noplacelavaby.op" + ChatColor.DARK_BLUE + 
								" now " + ChatColor.GRAY + !turnOn);
			return true;
		}				

		// All world lists params
		else if ((commandName.indexOf ("nostart") != -1) || (commandName.indexOf ("noplace") != -1) ||
				 (commandName.indexOf ("nodamage") != -1) ||
				 commandName.equals ("logstart") || commandName.equals ("logplace") ||
				 commandName.indexOf ("noburn") != -1)
		{				 
			String node;
			Set<String> leafNames;

			if (commandName.indexOf ("nostart") != -1) 			
			{ 
				node = "nostartby";
				leafNames = new HashSet<String>(Arrays.asList("lava", "lightning", "fireball", "player", "explosion", "crystal"));
			}
			else if (commandName.indexOf ("nodamage") != -1) {
				node = "nodamageto";
				leafNames = new HashSet<String>(Arrays.asList("block", "player.fromlava", "player.fromfire", "nonplayer.fromlava", "nonplayer.fromfire"));
			}
			else if (commandName.indexOf ("noplace") != -1) {
				node = "noplacelavaby";
				leafNames = new HashSet<String>(Arrays.asList("player", "op"));
			}
			else if (commandName.equals ("logstart")) {
				node = commandName;
				leafNames = new HashSet<String>(Arrays.asList("player", "lava", "lightning", "fireball", "crystal", "explosion"));
			}
			else if (commandName.equals ("logplace")) {
				node = commandName;
				leafNames = new HashSet<String>(Arrays.asList("lava"));
			}
			else if (commandName.indexOf ("noburn") != -1) {
				if (commandName.indexOf ("noburnmob") != -1)
					node = "noburnmobby";
				else if (commandName.indexOf ("noburnplayer") != -1)
					node = "noburnplayerby";
				else {
					sender.sendMessage ("Unknown command '" + commandName + "'");
					return false;
				}
				leafNames = new HashSet<String>(Arrays.asList ("player", "mob", "op"));
			} else 
				return false;
				
						
			if (args.length == 1) {
				if ( !(sender instanceof Player)) { // no params or can't impute world
					sender.sendMessage ("Can't infer world of SERVER");
					return false;
				}
				else {
					String wName = ((Player)sender).getLocation().getWorld().getName();
					String activeTriggers = "";
					
					for (String s : leafNames) {						
						if (antiFire.ifConfigContains ("nerf_fire." + node + "." + s, wName))
							activeTriggers += s + " ";
						if (s.equals ("op") && this.getConfig().getBoolean ("nerf_fire." + node + ".op"))
							activeTriggers += s + " ";
					}
					if (activeTriggers.length() == 0)
						activeTriggers = "nothing";
						
					if (node.equals ("nostartby"))	
						sender.sendMessage ("The following are UNable to start fires in your world: " + 
											ChatColor.YELLOW + activeTriggers);
					else if (node.equals ("noplacelavaby"))	
						sender.sendMessage ("The following are UNable to place lava in your world: " + 
											ChatColor.YELLOW + activeTriggers);
					else if (node.equals ("logstart"))
						sender.sendMessage ("Logging in your world is active for starts by: " + 
										ChatColor.YELLOW + activeTriggers);
					else if (node.equals ("logplace"))
						sender.sendMessage ("Logging in your world is active for placement of: " + 
										ChatColor.YELLOW + activeTriggers);
					else if (node.equals ("nodamageto"))
						sender.sendMessage ("The following are safe from damage by fires in your world: " + 
											ChatColor.YELLOW + activeTriggers);			
					else if (node.equals ("noburnmobby"))
						sender.sendMessage ("Following cannot set mobs alight in your world: " + 
											ChatColor.YELLOW + activeTriggers);
					else if (node.equals ("noburnplayerby"))
						sender.sendMessage ("Following cannot set players alight in your world: " + 
											ChatColor.YELLOW + activeTriggers);						
				}
			} else {
				// have a parameter
				String wName;
				String item = args[1].toLowerCase();

				if ( !(sender instanceof Player)) {
					if (args.length < 4 && !item.equals ("op")) {
						sender.sendMessage (pdfFile.getName() + ": Please provide 'true|false all'");
						return false;
					} else {
						wName = "fooblitzki"; // nonsense for now
					}
				} else
					 wName = ((Player)sender).getLocation().getWorld().getName();

				String logConfig;
				boolean ActiveInWorld;
				
				if ( !leafNames.contains (item)) {
					sender.sendMessage ("invalid " + node + " item:" + item);
					return false;
				}
				else
					logConfig = "nerf_fire." + node + "." + item;

				List <String> newLog = null;
				
				if (item.equals ("op"))
					ActiveInWorld = this.getConfig().getBoolean (logConfig);
				else {
					ActiveInWorld = antiFire.ifConfigContains (logConfig, wName);

					newLog = (this.getConfig().isString (logConfig) ?
 							new ArrayList <String>(Arrays.asList(getConfig().getString (logConfig).split(","))) : 
							getConfig().getStringList (logConfig) );
					this.log.fine ("Current " + logConfig + "=" + newLog);
				}
				if (args.length == 2) { // no setting. Assume means turn on due to "nostart"
					if (ActiveInWorld) {
						if (node.equals ("nostartby"))	
							sender.sendMessage (item + " is already disabled from starting fires for this world");
						else if (node.equals ("noplacelavaby"))	
							sender.sendMessage (item + " is already disabled from placing lava for this world");
						else if (node.equals ("logstart"))
							sender.sendMessage ("Logging for " + item + " is already active for this world");
						else if (node.equals ("logplace"))
							sender.sendMessage ("Logging for " + item + " is already active for this world");
						else if (node.equals ("nodamageto"))
							sender.sendMessage (item + " is already safe from fire damage in this world");
						else if (node.equals ("noburnmobby"))
							sender.sendMessage (item + " is already disabled from lighting mobs in this world");
						else if (node.equals ("noburnplayerby"))
							sender.sendMessage (item + " is already disabled from lighting players in this world");
						return true;
					}
					else if (item.equals ("op")) {
						this.getConfig().set (logConfig, true);
						sendMsg (sender, ChatColor.BLUE + logConfig + ChatColor.DARK_BLUE + " now " + ChatColor.RED + "effective");
						return true;
					} else
						newLog.add (wName);
						
				} else { // have a param				
					boolean turnOn = args[2].toLowerCase().equals ("true");
					
					if (item.equals ("op")) {
						this.getConfig().set (logConfig, turnOn);
						sendMsg (sender, ChatColor.BLUE + logConfig + ChatColor.DARK_BLUE + " now " + (turnOn ? ChatColor.RED + "effective" : "off"));
						return true;
					}
					
					if (args.length == 3) {
					  // only on/off param				
						if (turnOn && !ActiveInWorld) { 
							newLog.add (wName);
						}
						else if (!turnOn && ActiveInWorld)
							newLog.remove (wName);
					} else if (args[3].toLowerCase().equals("all")) { 
						newLog.clear(); // if off

						if (turnOn) {
						  for (World w : sender.getServer().getWorlds()) 
						    	newLog.add (w.getName());
						}
					}
				}				
				this.getConfig().set (logConfig, newLog);
				sendMsg (sender, ChatColor.BLUE + logConfig + ChatColor.DARK_BLUE + 
									" now effective in: " + ChatColor.GRAY + newLog);
			}
			return true;									
		}			
					
		return false;
	}
	
	private List <Entity> getNearbyEntities (Location loc, int radius) {
		Chunk center = loc.getChunk();
		List <Entity> eList = new ArrayList<Entity> ();
		int chunks = 1;
		
		// Process local chunk
		for (Entity e: center.getEntities()) {
			if (loc.distance (e.getLocation()) <= radius) { // within range
				eList.add (e);
			}
		}
		// Process nearby chunks (could be only +1 away!)
		double centerX = center.getX(), centerZ = center.getZ();
			
		for (double i = -radius ; i<radius; i += 16) // increment by chunk size
			for (double j = -radius; j < radius; j += 16) {
				Vector newL = loc.toVector().add (new Vector (i, 0, j));
			 	Chunk newChunk = newL.toLocation(loc.getWorld()).getChunk();
			 	if (newChunk.getX() != centerX || newChunk.getZ() != centerZ) {
			 		chunks++;													
					for (Entity e: newChunk.getEntities()) {
						if (loc.distance (e.getLocation()) <= radius) { // within range
							eList.add (e);
						}
					}
				}
			}
		//* DEBUG */ log.fine ("getNearbyEntities (loc, " + radius + ") processed " + chunks + " chunks, found " + eList.size() + " entities");
		return eList;
	}		
	
	private boolean extinguishRadius (CommandSender sender, Location l, int radius) {
		if (radius < 1)
			return false;
		
		double x,y,z; 
		int savedBlocks = 0, savedEntities= 0;
		Material fire = Material.FIRE;
		World w = l.getWorld();
		List <Entity> eList;
		
		for (x= l.getX() - radius; x < l.getX()+ radius; x++)
			for (y= l.getY() - radius; y < l.getY()+ radius; y++)
				for (z= l.getZ() - radius; z < l.getZ()+ radius; z++) 
					if (w.getBlockAt((int)x,(int)y,(int)z).getType() == fire) { // faster check than getBlock()
						w.getBlockAt ((int)x,(int)y,(int)z).setType (Material.AIR); // i.e. extinguish
						savedBlocks++;
					}
/*
		if (sender instanceof Player) {					
			// Should search for entities too. By whole world? Ugh. By chunk? Possible, but not exact
		    // almost exact. It's a box, rather than a sphere, but it's a lot more efficient
			eList = ((Player)sender).getNearbyEntities(radius, radius, radius);
		} else 
*/
			eList = getNearbyEntities (l, radius);

		for (Entity e : eList) {
			if (e.getFireTicks() > 0) {
				e.setFireTicks (0); // extinguish
				savedEntities++;
			}
		}

		String loc = l.toString();
		if (sender != null && sender instanceof Player)	{
		  if (l.equals (((Player)sender).getLocation())) 
		     loc = "you";
  		} 
		sendMsg (sender, language.get (sender, "extingRad", pluginName + ": " + "extinguished {0} blocks and {1} entities within {2} blocks of {3}", savedBlocks, savedEntities, radius, loc));
		
		this.entitiesSaved += savedEntities;
		this.blocksSaved += savedBlocks;
		   
		return true;
	}
		

    // Normally used by a player to extinguish around himself
	private boolean extinguishRadius (Player center, int radius) {
		return extinguishRadius (center, center.getLocation(), radius);
	}
    // Normally used by console or Op to extinguish around someone else
	private boolean extinguishRadius (CommandSender sender, Player center, int radius) {
		boolean value = extinguishRadius (sender, center.getLocation(), radius);
		
		if (value)
			sendMsg (center, language.get (center, "otherExting", pluginName + ": {0} extinguished fire around you", sender.getName()));
		
		return value;
	}
	
	// Only searches loaded chunks, where fire might actually be burning
	private boolean extinguishWorld (CommandSender sender, World w) {
		int savedBlocks = 0, savedEntities =0, chunks=0;
        int x,y,z;
        
		for (Chunk c : w.getLoadedChunks()) {
			chunks++;	
			// extinguish blocks
			for (x=0; x<16; x++)
			  for (y=0; y<128; y++)
			    for (z=0; z<16; z++) { // gah!! This is 64k blocks
				    Block b = c.getBlock(x,y,z);
				  		      			    
					if (b.getType() == Material.FIRE) {
						b.setType (Material.AIR); // i.e. extinguish
						savedBlocks++;
					}
				}
			
			// Now extinguish entities
			for (Entity e : c.getEntities ()) {
				if (e.getFireTicks() > 0) {
					e.setFireTicks (0); // extinguish
					savedEntities++;
				}
			}
		}
		//*DEBUG*/log.info ("Processed " + blocks + " blocks");
		sendMsg (sender, language.get (sender, "extingWorld", 
									   pluginName + ": extinguished {0} blocks and {1} entities in {2} chunks of {3}",
									   savedBlocks, savedEntities, chunks, w.getName() ));
						    
		this.entitiesSaved += savedEntities;
		this.blocksSaved += savedBlocks;

		return true;
	}
	/* extinguish [#|world [all|name]] to extinguish within block radius or entire world or all server worlds
	 *  no params means within default radius, so only good for Players
	 *  number command means supplied radius, so also only good for Players
	 *  'world' param alone means current world, so only good for Players
 	 */
 	@SuppressWarnings("deprecation") // offlinePlayer(String)
	private boolean extinguishCommand(CommandSender sender, String[] args) 
	{
		boolean colors = (sender != null && sender instanceof Player) ? true : false;
		int radius = 20;
		Server server = sender.getServer();
		String param;
				
		if (args.length == 0) {
			if (colors) {
				// default: extinguish within 20 blocks
				return extinguishRadius ((Player)sender, radius);
			} else {
				log.warning ("Cannot infer world. Supply on command line.");
				return false;
			}
		}
		else param = args[0].toLowerCase();
		
		if (param.equals ("world")) {
			if (args.length == 1) {
				if (! colors) {
					log.warning ("Cannot infer world. Supply on command line.");
					return false;
				}
				return extinguishWorld (sender, ((Player)sender).getLocation().getWorld());
			}
			if (args[1].equals ("all")) { // all worlds!
				sender.sendMessage ("This may take a while...");
				for (World w : sender.getServer().getWorlds()) {
					extinguishWorld (sender, w);
				}
				return true;
			}
			// Extinguish in supplied world
			for (World w : sender.getServer().getWorlds()) {
				if (w.getName().equals (args [1])) 
					return extinguishWorld (sender, w);
			}					
			sendMsg (sender, language.get (sender, "noWorld", "World '{0}' not found.", args[1]));
			return true;
		}
		else if (param.equals ("all")) {
			sender.sendMessage ("This may take a while...");
			for (World w : sender.getServer().getWorlds()) {
				extinguishWorld (sender, w);
			}
			return true;
		}
		else if (param.equals ("last")) {
			FireLogEntry logEntry;

			if (args.length == 1) { // no param
				try {
					logEntry = fireLog.list.getLast();
				} catch (NoSuchElementException exc) {
					logEntry = null;
				}
				if (logEntry == null) {
					sendMsg (sender, language.get (sender, "noLog", pluginName + ": no entries in log!"));
					return true;
				}
			} else if (args [1].length() == 1 && Character.isDigit(args[1].charAt(0))) {
				// # format, meaning back up # 
				logEntry = fireLog.lastMinus (Integer.parseInt(args[1]));
				if (logEntry == null) {
					sender.sendMessage (pluginName + ": too far back in log");
					return true;
				}
			} else { // try to parse as a player name. BUG: Crash on multi-digit "name"
				String n = args [1];
				UUID id = getUUIDFromName (n);
				if (id == null && !validName (n)) {
					sendMsg (sender, language.get (sender, "badName", ChatColor.RED + "bad player name '{0}'", n));
					return true;
				} 
				
				if ((id != null && !sender.getServer().getOfflinePlayer (id).hasPlayedBefore () ) ||
					(id == null && !sender.getServer().getOfflinePlayer(n).hasPlayedBefore()) )
				{   // n is string version so works if it's a UUID too
					sendMsg (sender, language.get (sender, "unseenPlayer", "'{0}' has never played before", n));
					return true;
				}
				logEntry = (id != null ? fireLog.lastBy (id) : fireLog.lastBy (n));
				if (logEntry == null) {
					sendMsg (sender, language.get (sender, "noLogBy", pluginName + ": no entries in log by {0}", n));
					return true;
				}			
			}
			// If got here, have a valid log entry
			return extinguishRadius (sender, logEntry.loc, radius);
		}	
		else if (validName (args[0])) {
			// extinguish around supplied player
			// don't bother with UUID because this is for a player currently online
			Player p = getServer().getPlayer(args[0]);
			if (p == null) {
				sendMsg (sender, language.get (sender, "notOnline", "Cannot find player '{0}' online", args[0]));
				return true;
			}
			return extinguishRadius (sender, p, radius);
		} else if (! colors) {
			log.warning ("Cannot infer location.");
			return false;
		}		
		// else supplied a # and have a implied location.
		 try {
			radius = Integer.parseInt (args [0]);

			if (radius < 0) {
				sendMsg (sender, language.get (sender, "invalid", ChatColor.RED + "invalid value for {0}", "radius"));
				return false;
			}
			return extinguishRadius ((Player)sender, radius);
		
		} catch (Exception exc) {
			sendMsg (sender, language.get (sender, "invalid", ChatColor.RED + "invalid value for {0}", "radius"));
			return false;
		}	
	}	

}
