package com.yahoo.phil_work.antifire;

import java.util.Date;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.ChatColor;

public class FireLogEntry {
	String playerName;
	UUID playerId;
	Date   date;
	Location loc;
	
	public FireLogEntry (String name, Date date, Location loc, UUID id) {
		playerName = name;
		this.date = date;
		this.loc = loc;
		playerId = id;
	}
	public FireLogEntry (String name, Date date, Location loc) {
		this (name, date, loc, null);
	}
	
	@Override
	public String toString () {
		return toString (false);
	}
	
	public String toString (boolean colors) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (colors)
			return (ChatColor.GRAY + dateFormat.format(this.date) + " " + this.toStringNoDate(colors));
		else
			return (dateFormat.format(this.date) + " " + this.toStringNoDate(false));
	}
	
	
	// String formats with colors do NOT include UUIDs
	
	public String toStringNoDate (boolean colors) {
		if (colors) 
			return (ChatColor.GOLD + this.playerName + ChatColor.GRAY + " started fire at " + 
					ChatColor.DARK_BLUE + loc.getWorld().getName() + ChatColor.RESET + " (x=" + loc.getX() + ",y=" + loc.getY() + ",z=" + loc.getZ() + ")");
		else
			return (this.playerName + ":"+ (this.playerId != null ? this.playerId : "") + " started fire at " + 
					loc.getWorld().getName() + " (x=" + loc.getX() + ",y=" + loc.getY() + ",z=" + loc.getZ() + ")");
		
	}
	public String toStringNoLoc (boolean colors) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (colors) 
			return (ChatColor.GOLD + this.playerName + ChatColor.GRAY + " started fire at " + dateFormat.format(this.date));		
		else 
			return (this.playerName + ":"+ (this.playerId != null ? this.playerId : "") + " started fire at " + dateFormat.format(this.date));		

	}
	
}
