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
 * This class is used to represent a single data element within and IEC61850 frame. 
 * 
 * @author  Philippe Venne
 * @version 0.2
 *
 */

package jgoose;

import org.jnetpcap.nio.JBuffer;

public class IEC61850_GOOSE_Data_Element
{
	// number of embedded element within this element
	// Only relevant for Array and Structure type
	//public int nbOfElements;
	public int length;
	public IEC61850_GOOSE_MMS_DataType dataType;
	public Object value;
	
	IEC61850_GOOSE_Data_Element()
	{
		//nbOfElements = 0; // Used with arrays and structures
		length = 0;
		dataType = null;
		value = null;
	}
	
	// This method could not be tested
	/*
	private int getNumberOfElements(JBuffer currentEntry, int offset, int length)
	{
		int nbOfElements = 0;
		int currentElementLenght = 0;
		
		while (length != 0)
		{
			// The first element is the tag
			offset++;
			// We read the length of the current element
			currentElementLenght = currentEntry.getUByte(offset);
			
			// We remove form the length the length of the current element,
			// the length of the tag (1) and the length of the length field (1)
			length -= currentElementLenght + 2;
			
			nbOfElements ++;
			offset += currentElementLenght + 1;
		}
		
		return nbOfElements;
	}
	*/
	
	protected void decode(JBuffer currentEntry, int offset)
	{
		// The first byte is the data type
		this.dataType = IEC61850_GOOSE_MMS_DataType.get(currentEntry.getUByte(offset));
		// The second byte is the data length
		this.length = currentEntry.getUByte(offset+1);
		
		switch (dataType)
		{
			/*
			case array:
			case structure:
				// The array and structure code could not be tested
				
				// The number of elements in the structure or array
				nbOfElements = getNumberOfElements(currentEntry,offset+2,this.length);

				// Add code to read data
				
				break;
			*/
				
			case booln:
				if (this.length == 1)
					value = new Boolean(currentEntry.getUByte(offset+2) != 0);
				
				else
					throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::decode Cannot read boolean on more than  1 byte");
				
				break;
				
			case integer:
				if (this.length == 1)
					value = new Byte(currentEntry.getByte(offset+2));
				
				else if (this.length == 2)
					value = new Short(currentEntry.getShort(offset+2));
				
				else if (this.length == 4)
					value = new Integer(currentEntry.getInt(offset+2));
				
				else if (this.length == 8)
					value = new Long(currentEntry.getLong(offset+2));
					
				else
					throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::decode Cannot read integer on 3 bytes");
				break;
				
			case unsign:
				if (this.length == 1)
					value = new Integer(currentEntry.getUByte(offset+2));
				
				else if (this.length == 2)
					value = new Integer(currentEntry.getUShort(offset+2));
				
				else if (this.length == 4)
					value = new Long(currentEntry.getUInt(offset+2));
					
				else
					throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::decode Cannot read unsigned integer on 3 bytes");
				break;
				
			case float_point:
				if (this.length == 4)
					//TODO test this
					value = new Float(currentEntry.getFloat(offset+2));

				else if (this.length == 8)
					//TODO test this
					value = new Double(currentEntry.getDouble(offset+2));
				
				else
					throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::decode Cannot read float on other than 4 or 8 bytes");
				
				break;
				
			default:
				throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::decode Unsupported data type");
		}
	}

	protected void encode(JBuffer currentEntry, int offset)
	{
		// The first byte is the data type
		currentEntry.setUByte(offset,this.dataType.getTag());
		
		// The second byte is the data length
		currentEntry.setUByte(offset +1,this.length);
		
		switch (this.dataType)
		{
			/*
			case array:
			case structure:
				// The array and structure code could not be tested
				
				// The number of elements in the structure or array
				nbOfElements = getNumberOfElements(currentEntry,offset+2,this.length);

				// Add code to read data
				
				break;
			*/
				
			case booln:
				if (this.length == 1)
				{
					currentEntry.setUByte(offset+2,(((Boolean)this.value).booleanValue())?1:0);
				}
				else
					throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Cannot write boolean on more than  1 byte");
				
				break;
				
			case integer:
				if (this.length == 1)
				{
					if (this.value.getClass() == Byte.class)
					{
						currentEntry.setByte(offset+2,((Byte)this.value).byteValue());
					}
					else if (this.value.getClass() == Short.class)
					{
						currentEntry.setByte(offset+2,((Short)this.value).byteValue());
					}
					else if (this.value.getClass() == Integer.class)
					{
						currentEntry.setByte(offset+2,((Integer)this.value).byteValue());
					}
					else if (this.value.getClass() == Long.class)
					{
						currentEntry.setByte(offset+2,((Long)this.value).byteValue());
					}
					else
						throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");
					
				}
				else if (this.length == 2)
				{
					if (this.value.getClass() == Byte.class)
					{
						currentEntry.setShort(offset+2,((Byte)this.value).shortValue());
					}
					else if (this.value.getClass() == Short.class)
					{
						currentEntry.setShort(offset+2,((Short)this.value).shortValue());
					}
					else if (this.value.getClass() == Integer.class)
					{
						currentEntry.setShort(offset+2,((Integer)this.value).shortValue());
					}
					else if (this.value.getClass() == Long.class)
					{
						currentEntry.setShort(offset+2,((Long)this.value).shortValue());
					}
					else
						throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");

				}
				else if (this.length == 4)
				{
					if (this.value.getClass() == Byte.class)
					{
						currentEntry.setInt(offset+2,((Byte)this.value).intValue());
					}
					else if (this.value.getClass() == Short.class)
					{
						currentEntry.setInt(offset+2,((Short)this.value).intValue());
					}
					else if (this.value.getClass() == Integer.class)
					{
						currentEntry.setInt(offset+2,((Integer)this.value).intValue());
					}
					else if (this.value.getClass() == Long.class)
					{
						currentEntry.setInt(offset+2,((Long)this.value).intValue());
					}
					else
						throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");
					
				}
				else if (this.length == 8)
				{
					if (this.value.getClass() == Byte.class)
					{
						currentEntry.setLong(offset+2,((Byte)this.value).intValue());
					}
					else if (this.value.getClass() == Short.class)
					{
						currentEntry.setLong(offset+2,((Short)this.value).intValue());
					}
					else if (this.value.getClass() == Integer.class)
					{
						currentEntry.setLong(offset+2,((Integer)this.value).intValue());
					}
					else if (this.value.getClass() == Long.class)
					{
						currentEntry.setLong(offset+2,((Long)this.value).intValue());
					}
					else
						throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");
					
				}	
				else
					throw new UnsupportedOperationException("Cannot write integer on 3 bytes");
				
				break;
				
			case unsign:
				if (this.length == 1)
				{
					if (this.value.getClass() == Byte.class)
					{
						currentEntry.setUByte(offset+2,((Byte)this.value).byteValue());
					}
					else if (this.value.getClass() == Short.class)
					{
						currentEntry.setUByte(offset+2,((Short)this.value).byteValue());
					}
					else if (this.value.getClass() == Integer.class)
					{
						currentEntry.setUByte(offset+2,((Integer)this.value).byteValue());
					}
					else if (this.value.getClass() == Long.class)
					{
						currentEntry.setUByte(offset+2,((Long)this.value).byteValue());
					}
					else
						throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");
					
				}
				else if (this.length == 2)
				{
					if (this.value.getClass() == Byte.class)
					{
						currentEntry.setUShort(offset+2,((Byte)this.value).shortValue());
					}
					else if (this.value.getClass() == Short.class)
					{
						currentEntry.setUShort(offset+2,((Short)this.value).shortValue());
					}
					else if (this.value.getClass() == Integer.class)
					{
						currentEntry.setUShort(offset+2,((Integer)this.value).shortValue());
					}
					else if (this.value.getClass() == Long.class)
					{
						currentEntry.setUShort(offset+2,((Long)this.value).shortValue());
					}
					else
						throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");

				}
				else if (this.length == 4)
				{
					if (this.value.getClass() == Byte.class)
					{
						currentEntry.setUInt(offset+2,((Byte)this.value).intValue());
					}
					else if (this.value.getClass() == Short.class)
					{
						currentEntry.setUInt(offset+2,((Short)this.value).intValue());
					}
					else if (this.value.getClass() == Integer.class)
					{
						currentEntry.setUInt(offset+2,((Integer)this.value).intValue());
					}
					else if (this.value.getClass() == Long.class)
					{
						currentEntry.setUInt(offset+2,((Long)this.value).intValue());
					}
					else
						throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");
					
				}	
				else
					throw new UnsupportedOperationException("Cannot write integer on 3 bytes");
				
				break;
				
			case float_point:
				
				if (this.length == 4)
				{
					if (this.value.getClass() == Float.class)
					{
						currentEntry.setFloat(offset+2, ((Float)this.value).floatValue());
					}
					else if (this.value.getClass() == Double.class)
					{
						currentEntry.setFloat(offset+2, ((Double)this.value).floatValue());
					}
					else
						throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");
				}
				else if (this.length == 8)
				{
					if (this.value.getClass() == Float.class)
					{
						currentEntry.setDouble(offset+2, ((Float)this.value).doubleValue());
					}
					else if (this.value.getClass() == Double.class)
					{
						currentEntry.setDouble(offset+2, ((Double)this.value).doubleValue());
					}
					else
						throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");
				}
				else
					throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");
				
				break;
				
			default:
				throw new UnsupportedOperationException("In IEC61850_GOOSE_Data_Element::encode Unsupported object type");
		}
	}
}