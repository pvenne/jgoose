/**
 *  This file is part of jgoose.
 *
 *  jgoose is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  jgoose is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with jgoose.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * 
 * This class is used to define the MMS_DataType used by IEC61850 frames.
 * It related the MMS Datatype with the IEC61850 type Tag and the IEC Datatype
 * 
 * @author  Philippe Venne
 * @version 0.2
 *
 */

package jgoose;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public enum IEC61850_GOOSE_MMS_DataType
{
	 // MMS Datatype    Tag     IEC Datatype, Byte size
	 array				(0xa1),
	 structure			(0xa2,	"Struct", 	"0"),
	 booln				(0x83,	"BOOLEAN", 	"1"),
	 bit_string			(0x84,	"Quality", 	"0",	"Timestamp","0"),
	 integer			(0x85,	"INT16", 	"2",	"INT32",	"4", 	"INT8",		"1",	"Enum",	"1"),
	 unsign				(0x86,	"INT8U", 	"1", 	"INT16U", 	"2", 	"INT32U", 	"4"),
	 float_point		(0x87,	"FLOAT64",	"8", 	"FLOAT32", 	"4"),
	 octet_string		(0x89),
	 visible_string		(0x8a),
	 generalized_time	(0x8b),
	 binary_time		(0x8c),
	 bcd				(0x8d),
	 boolean_array		(0x8e),
	 obj_id				(0x8f),
	 utc_time			(0x91,	"Timestamp","0");
	 
	 // Holds the tag for each value of the enumeration 
	 public final int tag;
	 
	 // Holds the IEC_Datatype for each value of the enumeration 
	 public final List<String> iec_list = new ArrayList<String>();
	 
	 // Holds the pairs of Byte size and IEC Datatype
	 public final Map<String,Integer> iec_size = new HashMap<String,Integer>();
	 
	 // This Map holds each Integer MMS_DataType pair
	 private static final Map<Integer,IEC61850_GOOSE_MMS_DataType> tag_lookup 
     = new HashMap<Integer,IEC61850_GOOSE_MMS_DataType>();
     
     // This Map holds each MMS_DataType iec_list pair
     private static final Map<IEC61850_GOOSE_MMS_DataType, List<String>> iec_lookup 
     = new HashMap<IEC61850_GOOSE_MMS_DataType, List<String>>();
     
     // This map hold each size and IEC data type pair
     private static final Map<String,Integer> iec_size_lookup 
     = new HashMap<String,Integer>();
     
     static 
     {
    	 // Populates the map
         for(IEC61850_GOOSE_MMS_DataType t : EnumSet.allOf(IEC61850_GOOSE_MMS_DataType.class))
        	 tag_lookup.put(t.getTag(), t);
         
         // Populates the map
         for(IEC61850_GOOSE_MMS_DataType t : EnumSet.allOf(IEC61850_GOOSE_MMS_DataType.class))
        	 iec_lookup.put(t, t.getIECList());
         
         // Populates the map
         for(IEC61850_GOOSE_MMS_DataType t : EnumSet.allOf(IEC61850_GOOSE_MMS_DataType.class))
        	 iec_size_lookup.putAll(t.iec_size);
    }
	 
	 IEC61850_GOOSE_MMS_DataType (int tag)
	 {
		 this.tag = tag;
	 }
	 
	 IEC61850_GOOSE_MMS_DataType (int tag, String ... string_list)
	 {
		 this.tag = tag;
		 
		 for(int position = 0; position< string_list.length; position+=2)
		 {
			 // We save the IEC Datatype in the list
			 this.iec_list.add(string_list[position]);
			 
			 // The save the Byte size and IEC Datatype in the map
			 this.iec_size.put(string_list[position],Integer.parseInt(string_list[position+1]));
		 }
	 }
	 
	 public int getTag() 
	 {
		 return tag; 
	 }
	 
	 public List<String> getIECList() 
	 {
		 return iec_list; 
	 }
	 
	 public static IEC61850_GOOSE_MMS_DataType get(int tag) 
	 { 
          return tag_lookup.get(tag); 
     }
	 
	 public static int get_size(String iec_type) throws IEC61850_GOOSE_Exception
	 {
		 if(iec_size_lookup.containsKey(iec_type))
			 return iec_size_lookup.get(iec_type);
		 
		 else
			 // If the IEC type is not found, we throw an exception
			 throw new IEC61850_GOOSE_Exception("No MMS type corresponding to IEC type");
	 }
	 
	 public static IEC61850_GOOSE_MMS_DataType get(String iec_type) throws IEC61850_GOOSE_Exception
	 {
		 Iterator<Map.Entry<IEC61850_GOOSE_MMS_DataType,List<String>>> 
		 	iec_lookup_entries_IT = iec_lookup.entrySet().iterator();
		 
		 // We walk through the map to search each list
		 for(int map_position = 0; map_position < iec_lookup.size(); map_position++)
		 {
			 Map.Entry<IEC61850_GOOSE_MMS_DataType,List<String>> current_iec_map_entry 
			 	= iec_lookup_entries_IT.next();
			 
			 // We walk through each list to find the iec_type
			 for(int list_position = 0; 
			 	list_position < current_iec_map_entry.getValue().size(); list_position++)
			 {
				 // If the IEC type is found, we return it
				 if(current_iec_map_entry.getValue().get(list_position).equals(iec_type))
				 {
					 return current_iec_map_entry.getKey(); 
				 }
			 }
		 }
		 
		 // If the IEC type is not found, we throw an exception
		 throw new IEC61850_GOOSE_Exception("No MMS type corresponding to IEC type");
	 }
}