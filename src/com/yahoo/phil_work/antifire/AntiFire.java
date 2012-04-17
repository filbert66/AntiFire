package com.yahoo.phil_work.antifire;

import java.util.ArrayList;
import java.util.Date;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

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

    public void onDisable() {
        System.out.println(pdfFile.getName() + " disabled.");
    }

    public static boolean validName(String name) {
        return name.length() > 2 && name.length() < 17 && !name.matches("(?i).*[^a-z0-9_].*");
    }

    public void onEnable() {
        // Register our events
		this.getServer().getPluginManager().registerEvents (this.antiFire, this);
		pdfFile = this.getDescription();
		log = this.getLogger();

		antiFire.initConfig(); 
		
		if (getConfig().isString ("log_level")) {
			Log_Level = getConfig().getString("log_level", "INFO"); // hidden config item
			try {
				log.setLevel (log.getLevel().parse (Log_Level));
				log.info ("successfully set log level to " + log.getLevel());
			}
			catch (Throwable IllegalArgumentException) {
				log.warning ("Illegal log_level string argument '" + Log_Level);
			}
		}
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
            return logCommand(sender, trimmedArgs);
		}
		if (commandName.equals("tp")) {
            return tpCommand(sender, trimmedArgs);
		}
		
        return false;
    }
	private boolean logCommand(CommandSender sender, String[] args) {
        sender.sendMessage ("Sorry, commands not yet implemented");
		return true;
	}
	private boolean tpCommand(CommandSender sender, String[] args) {

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
