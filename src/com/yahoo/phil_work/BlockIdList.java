package com.yahoo.phil_work;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class BlockIdList {
	List <BlockId> blockList;
	private final Logger log;
	private final JavaPlugin plugin;
	private String listName;

	public BlockIdList (JavaPlugin instance) {
		this.log = instance.getLogger();
		this.plugin = instance;
		blockList = new ArrayList<BlockId>();
	}

	public boolean contains (BlockId block) {
		return blockList.contains (block);
	}
	
	public boolean isEmpty() {
		return (blockList == null || blockList.size() == 0);
	}
	
    // returns number of items loaded into list
	public int loadBlockList (String listname) {
		this.listName = listname;
		
		try{
			String tmp_str1 = this.plugin.getConfig().getString(listname, "").trim();
			String[] split = tmp_str1.split(",");

			if(split!=null){        //split the list into single strings of integer
				for(String elem : split) {  // may include form of #:#
					if (elem.length() > 0) {
						blockList.add(new BlockId(elem));
					}
				}
			}
			else {
				log.config ("Empty " + listname);
				blockList.clear();
				blockList = null;
			}
		}
		catch (Exception e) {
			this.log.warning ("Wrong values for " + listname + "field");

			blockList.clear();
			blockList = null;
		}
		return (blockList != null? blockList.size() : 0);
	}

	public void printList () {
		String ListAsString = new String();

		if (blockList == null) {
			this.log.config ("Empty " + listName + ":null value");
			return;
		}
		for (BlockId block : blockList) {
			String asString = block.toString();
			ListAsString += asString + ",";
		}
		this.log.config (listName + "[" + blockList.size() + "]: " + ListAsString);
	}
}
