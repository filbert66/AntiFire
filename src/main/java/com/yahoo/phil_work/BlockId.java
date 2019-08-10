/* Origins: Thanks to package com.nitnelave.CreeperHeal;
 * History: 
 * 	3 Dec 2013 : Permit initialization with MaterialDataStringer
 * 13 Dec 2017 : Deprecated use of Magic IDs.
 ******
 * 05 Aug 2019 : Spigot 1.13: no longer use MaterialData, but just Material and strings
 */

package com.yahoo.phil_work;

import org.bukkit.Material;
//import org.bukkit.material.MaterialData;
import com.yahoo.phil_work.MaterialDataStringer;
 
public class BlockId {
	Material mat;

	public BlockId () {
		mat = null;
	}
	
	// alternative way to create; expected use is new BlockId (MaterialDataStringer.matchMaterialData (string))
	public BlockId (Material m) {
		if (m != null) {
			mat = m;
		}
	}
	
	// report in the format that was supplied
	@Override public String toString () {
		if (mat != null)
			return mat.toString();
		else 
			return null;
	}
	
	@Override public boolean equals(Object obj){
		if(obj == this)
			return true;
		
		if(!(obj instanceof BlockId))
			return false;
		
		BlockId block = (BlockId) obj;
		if(block.mat != mat)
			return false;
		else 
			return true;
	}
	
	@Override public int hashCode(){
		return mat.hashCode();
	}
}
