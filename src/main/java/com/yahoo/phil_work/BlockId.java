/* Origins: Thanks to package com.nitnelave.CreeperHeal;
 * History: 
 * 	3 Dec 2013 : Permit initialization with MaterialDataStringer
 */

package com.yahoo.phil_work;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import com.yahoo.phil_work.MaterialDataStringer;
 
public class BlockId {
	int id;
	byte data;
	boolean hasData;
	boolean fromInts;

	public BlockId () {
		this (0, (byte)0);
	}
	public BlockId(int type_id){
		this (type_id, (byte)0);
	}
	
	public BlockId(int type_id, byte data_value) {
		id = type_id;
		data = data_value;
		hasData = (data_value != 0);
		fromInts = true;
	}
	
	// alternative way to create; expected use is new BlockId (MaterialDataStringer.matchMaterialData (string))
	public BlockId (MaterialData md) {
		if (md != null) {
			id = md.getItemTypeId();
			data = md.getData();
			hasData = (data != 0);
			
			fromInts = false;
		} else {
			id = data = 0;
			hasData = false;
		}
	}
	
	public BlockId(String str){
		this();
		str = str.trim();
		fromInts = true;
		try{
			id = Integer.parseInt(str);
		}
		catch(NumberFormatException e){
			String[] split = str.split(":");
			if(split.length == 2){
				try{
					id = Integer.parseInt(split[0]);
					data = Byte.parseByte(split[1]);
					hasData = true;
				}
				catch(NumberFormatException ex){
				}
			}
			else {
			}
		}
	}
	
	public int getId() {
		return id;
	}
	
	public byte getData() {
		return data;
	}
	
	public boolean hasData() {
		return hasData;
	}
	
	// report in the format that was supplied
	@Override
	public String toString(){
		String str; 
		
		if (fromInts) {
			str = new String();
			str += String.valueOf(id);
			if(hasData)
				str += ":" + String.valueOf(data);
		} else {
			if (hasData) 
				str = new MaterialDataStringer (id, data).toString();
			else
				str = Material.getMaterial (id).toString();
		}
		return str;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		
		if(!(obj instanceof BlockId))
			return false;
		
		BlockId block = (BlockId) obj;
		if(block.id != id)
			return false;
		
		if(block.hasData){ 
		   // block with no data will match all supplied blocks with data
		   //  ex. 35 will match all types of 35:1, 35:2.....
			return block.data == data;
		}
		else {
			return true;
		}
	}
	
	@Override
	public int hashCode(){
		return 37*id;
	}
}
