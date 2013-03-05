/* 
 * History:
 *   21 Aug 2012 : PSW: Added return value from add()
 */

package com.yahoo.phil_work.antifire;

import java.util.logging.Logger;
import java.io.BufferedWriter;
import java.io.File;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.NoSuchElementException;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.Location;
import org.bukkit.World;

import com.yahoo.phil_work.antifire.FireLogEntry;

public class AntifireLog {
	private final Logger log;
	private final JavaPlugin plugin;
	public LinkedList <FireLogEntry> list;
	
	public AntifireLog (JavaPlugin instance) {
		this.log = instance.getLogger();
		this.plugin = instance;
		
		list = new LinkedList <FireLogEntry>();
	}		

	public int add (String player, Location loc) {
		FireLogEntry entry = new FireLogEntry (player, new Date(), loc)	;
		list.addLast (entry);
		return list.size();
	}
    public int add (Player starter, Location loc) {
		return this.add (starter.getName(), loc);
	}	

    public boolean contains (OfflinePlayer p) {
		String name = p.getName();
		
		return this.contains (name);
	}

    public boolean contains (String name) {		
		for (FireLogEntry l : list)
			if (l.playerName.equalsIgnoreCase (name))
				return true;
		return false;
	}
	
	// For the command to teleport to last by name
	public FireLogEntry lastBy (String name) {
		Iterator<FireLogEntry> reverseIterator = list.descendingIterator ();
		FireLogEntry l;
		
		while (reverseIterator.hasNext()) {
			l = reverseIterator.next();
			if (l.playerName.equalsIgnoreCase (name))
				return l;
		}
		return null;
	}
	public FireLogEntry lastBy (OfflinePlayer p) {
		String name = p.getName();
		
		return this.lastBy (name);
	}

	// For the command to find the last minus Nth entry	
	public FireLogEntry lastMinus (int delta) {
		Iterator<FireLogEntry> reverseIterator = list.descendingIterator ();
		
		try {
			FireLogEntry l;
			
			for (l = reverseIterator.next(); delta > 0; delta--) {
				l = reverseIterator.next();
			}
			return l;
		}
		catch (NoSuchElementException exc) {
			return null;
		}
	}	
	
	// Returns the most recent # of entries specified, as strings
	// For use by the log function
	public ArrayList <String> lastFew (int number, boolean colors) {
		return nextFewFrom (number, 0, colors);
	}
		
	// Returns the most recent # of entries specified, as strings
	// For use by the log function
	public ArrayList <String> nextFewFrom (int number, int offset, boolean colors) {
		ArrayList<String> entries = new ArrayList <String>();
		Iterator<FireLogEntry> reverseIterator = list.descendingIterator ();
		
		try {
			FireLogEntry l;
			
			for (l = reverseIterator.next(); offset > 0; offset--)
				l = reverseIterator.next();

			for (; number > 0; number--) {
				entries.add (l.toString(colors));
				
				l = reverseIterator.next();
			}
		}
		catch (NoSuchElementException exc) {
			entries.add ("END of LOG");
		}
		return entries;
	}
	
	
	// Returns 'number' most recent fire start events by specified player name
	public ArrayList <String> lastFewBy (int number, String playerName, boolean colors) {
		ArrayList<String> entries = new ArrayList <String>();
		Iterator<FireLogEntry> reverseIterator = list.descendingIterator ();
		int maxcount = number;
		
		try {
			FireLogEntry l;
			
			while (reverseIterator.hasNext() && maxcount > 0) {				
				l = reverseIterator.next();

				if (l.playerName.equalsIgnoreCase (playerName)) {
					entries.add (l.toString(colors));
					maxcount--;
				}
			}
		}
		catch (NoSuchElementException exc) {
			entries.add ("END of LOG");
		}
		return entries;
	}
	

}
