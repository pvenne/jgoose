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
 * IEC 61850 GOOSE Data definition
 * The data format is defined in Standard IEC 61850-8-1
 * GOOSE is used mostly in the Electric power system industry for real-time communications.
 * 
 * @author  Philippe Venne
 * @version 0.1
 *
 */

package jgoose;

import org.jnetpcap.nio.JBuffer;

public class IEC61850_GOOSE_Data {

	// Variable to hold local data
	public IEC61850_GOOSE_Data_Element data[];
	public final int numEntries;

	public IEC61850_GOOSE_Data(int numEntries)
	{
		data = new IEC61850_GOOSE_Data_Element[numEntries];
		
		for (int i = 0; i < numEntries; i++)
			data[i] = new IEC61850_GOOSE_Data_Element();
		
		this.numEntries = numEntries;
	}
	
	public void decodeData(JBuffer payload)
	{
		int currentBuffPosition = 0;
		
		// We walk through the payload to decode each data entry
		for (int currentEntry = 0; currentEntry < numEntries; currentEntry++)
		{
			// We decode the current data entry
			this.data[currentEntry].decode(payload, currentBuffPosition);
			
			currentBuffPosition++;
			// We read the current data entry length
			currentBuffPosition += (payload.getByte(currentBuffPosition) +1);		
		}
	}
	
	public void encodeData(JBuffer payload)
	{
		int currentBuffPosition = 0;
		
		// We walk through the payload to encode each data entry
		for (int currentEntry = 0; currentEntry < numEntries; currentEntry++)
		{
			// We encode the current data entry
			this.data[currentEntry].encode(payload, currentBuffPosition);
			
			currentBuffPosition++;
			// We read the current data entry length
			currentBuffPosition += (this.data[currentEntry].length +1);		
		}
	}
	
	public IEC61850_GOOSE_MMS_DataType getType(int entryNumber)
	{
		return data[entryNumber].dataType;
	}

	public Object getValue(int entryNumber)
	{
		return data[entryNumber].value;
	}
	
	public int getLength(int entryNumber)
	{
		return data[entryNumber].length;
	}
	
	public void setValue(int entryNumber, Object value)
	{
		data[entryNumber].value = value;
	}
	
	public void setType(int entryNumber, IEC61850_GOOSE_MMS_DataType type)
	{
		data[entryNumber].dataType = type;
	}
	
	public void setLength(int entryNumber, int length)
	{
		data[entryNumber].length = length;
	}
}
