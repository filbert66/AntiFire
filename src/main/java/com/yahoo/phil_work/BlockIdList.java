/* File: BlockIdList.java
 *   Shared between AntiFire & AntiCreeper projects
 * 
 * History:
 *  21 Aug 2012: Allow printList() commandSender to allow sendMessage() vs. plugin.log
 *               Added append(), setList(), parseBlockList(), asString()
 *  01 Dec 2012: Added option to use matchMaterial(string).
 *  04 Dec 2012: Allow for spaces in pattern match; fixed NP bug in parseBlockList (String, CommandSender) 
 *  31 Jan 2013: Ignore space for string material names
 */

package com.yahoo.phil_work;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.lang.StringBuilder;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Material;

import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class BlockIdList {
	List <BlockId> blockList;
	private final Logger log;
	private final JavaPlugin plugin;
	private String listName;

	// Constructors
	public BlockIdList (JavaPlugin instance, String init, CommandSender sender) {
		this.log = instance.getLogger();
		this.plugin = instance;
		blockList = new ArrayList<BlockId>();

		if (init != null)
			this.parseBlockList (init, sender);
	}
	public BlockIdList (JavaPlugin instance) {
		this (instance, null, null);
	}
	
	// Methods
	public boolean contains (BlockId block) {
		return blockList.contains (block);
	}
	public String name () {
		return listName;
	}
	public int size (){
		return blockList.size();
	}
	public boolean isEmpty() {
		return (blockList == null || blockList.size() == 0);
	}
	
	public BlockIdList append (BlockIdList more) {
		this.blockList.addAll (more.blockList);
		return this;
	}
	public BlockIdList setList (BlockIdList newList) {
		this.blockList = newList.blockList;
		return this;
	}
	
	public void parseBlockList (String init) {
		this.parseBlockList (init, null);
	}
	
    // Adds given string of IDs to existing list.
	public BlockIdList parseBlockList (String init, CommandSender sender) {
		try{
			String[] split = init.split(",");
			
			if(split!=null){        //split the list into single strings of integer
				Pattern p = Pattern.compile(" *[0-9:]* *");
			    Matcher m;

				for(String elem : split)
				 {  // may include form of #:#					
					if (elem.length() > 0)
					 {
						Material mat = Material.matchMaterial (elem.trim());
						if (mat != null) 
							blockList.add (new BlockId (mat.getId()));
						else
					    {
							m = p.matcher (elem);
							
							if (m.matches())
								blockList.add (new BlockId(elem.trim()));
							else {
								if (sender == null)
									log.config ("Ignoring bad block ID string '" + elem + "'");
								else 
								   sender.sendMessage ("Ignoring bad block ID string '" + elem + "'");
							}						    
						}
					}
				}
			}
			else {
				if (sender == null)
					log.config ("Empty " + listName);
				else {
					sender.sendMessage ("No block IDs found in '" + init + "'");
				}

				blockList.clear();
				blockList = null;
			}
		}
 		catch (Exception e) {
			if (sender == null)
				this.log.warning ("Wrong values for " + listName + " field: " + e);
			else {
				sender.sendMessage ("Wrong values in '" + init + "': " + e);
			}
			
			blockList.clear();
			blockList = null;
		}
		return this;
	}
	
    // returns number of items loaded into list
	public int loadBlockList (String listname) {
		this.listName = listname;
		
		String tmp_str1 = this.plugin.getConfig().getString(listname, "").trim();
		parseBlockList (tmp_str1);
		
		return (blockList != null? blockList.size() : 0);
	}

	public String asString () { return this.toString(); }

	@Override
	public String toString () {
		String ListAsString = new String();
		
		if (blockList == null) {
			return "";
		}
		for (BlockId block : blockList) {
			String asString = block.toString();
			ListAsString += asString + ",";
		}
		return ListAsString;
	}
	
	public void printList (CommandSender sender) {
		String ListAsString = this.asString();

		if (ListAsString == "") {
			this.log.config ("Empty " + listName + ":null value");
			return;
		}
		if (sender != null) {
			if (sender instanceof Player) {
				int period = listName.indexOf('.');

				/* DEBUG */log.fine ("Printing " + listName);
				sender.sendMessage (ChatColor.DARK_BLUE + listName.substring(0,period+1) + 
								   ChatColor.BLUE + listName.substring (period + 1, listName.length()) + 
								   "[" + blockList.size() + "]: " + 
								   ChatColor.GRAY + ListAsString);
			} else // not player, but SERVER. colors may show in terminal, but not in text file
				sender.sendMessage (listName + "[" + blockList.size() + "]: " + ListAsString);
		}
		else
			this.log.config (listName + "[" + blockList.size() + "]: " + ListAsString);
	}
}
