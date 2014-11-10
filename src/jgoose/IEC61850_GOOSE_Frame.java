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
 * This class is used to manipulate an IEC61850 frame. It interfaces the IEC61850 data.
 * 
 * @author  Philippe Venne
 * @version 0.2
 *
 */

package jgoose;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.jnetpcap.packet.JMemoryPacket;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.JProtocol;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.lan.IEEE802dot1q;


// This class should manipulate the full GOOSE frame
public class IEC61850_GOOSE_Frame {
	
	// Default timing attributes
	private boolean leapSecondsKnown = true;
	private boolean clockFailure = false;
	private boolean clockNotSynchronized = true; // means the clock source is not synchronised with a UTC clock source
	private int 	timeAccuracy = 31;				 // means unspecified
	
	private long 	utc_time =0; // time a which the packet was updated last
	
	// This object hold the decoded fields of the GSEControlBlock including the signals
	// The signals hold a pointer to data elements
	public IEC61850_GOOSE_GSEControlBlock gseControlBlockAttributes;
	
	// This object holds the event handler for the current frame;
	public IEC61850_GOOSE_FrameEventHandler frameEventHandler;
	
	// This object hold the frame validity
	public IEC61850_GOOSE_FrameValidityType frameValidity;
	
	// Decoded fields of the header
	public int appID;
	public String goCBref; 
	public String datSet;
	public String goID;
	public boolean test;
	public boolean ndsCom;
	public long stNum; // unsigned, maximum 4 bytes
	public long sqNum; // unsigned, maximum 4 bytes
	public long timeAllowedToLive; // unsigned, maximum 4 bytes
	public long confRevGoose; // unsigned, maximum 4 bytes
	private int numDatSetEntries; // unsigned, maximum 4 bytes
	
	private String destinationMacAddress;
	public String sourceMacAddress;
	
	//public IEC61850_GOOSE_Header goose_header;
	public IEC61850_GOOSE_Data gooseData;
	
	// Maximum number of bytes in packet
	private static final int packetSize = 512;
	
	// Field length of the packet
	private int goCBref_length; 
	private int timeAllowedToLive_length; // time in milliseconds, 2 Bytes allows up to 65535
	private int datSet_length; 
	private int goID_length; 
	private int stNum_length; 
	private int sqNum_length; 
	private int confRevGoose_length; 
	private int numDatSetEntries_length; 
	private int allData_length; 
	
	final private int test_length = 1; // this is a true false flag 
	final private int ndsCom_length = 1; // this is a true false flag
	
	// Used to initialise an unknown frame. Used for default received frame
	public IEC61850_GOOSE_Frame(IEC61850_GOOSE_FrameEventHandler local_frameEventHandler) throws IEC61850_GOOSE_Exception
	{
		frameEventHandler = local_frameEventHandler;
		gseControlBlockAttributes = null;
		
		gooseData =null;
		
		frameValidity = IEC61850_GOOSE_FrameValidityType.invalid;
		
		this.destinationMacAddress = "";
		this.sourceMacAddress = "";
		
		this.appID = 0;
		
		this.goCBref = "";
		this.datSet = "";
		this.goID = "";

		this.test = false;
		this.ndsCom = false;
		this.stNum = 0;
		this.sqNum = 0;
		this.timeAllowedToLive = 0;
		this.confRevGoose = 0;
		this.numDatSetEntries = 0;
		
		// We set the data length
		stNum_length = sizeOf(stNum);
		sqNum_length = sizeOf(sqNum);
		
		// These are fields that will not change
		goCBref_length = sizeOf(goCBref); 
		timeAllowedToLive_length = sizeOf(timeAllowedToLive); 
		datSet_length = sizeOf(datSet);
		
		goID_length = sizeOf(goID); 
		confRevGoose_length = sizeOf(confRevGoose); 
		numDatSetEntries_length = sizeOf(numDatSetEntries);
		
		// We build the payload
		gooseData = new IEC61850_GOOSE_Data(numDatSetEntries);
		
		// The allData_length is not the final lenght as the data types and size are not initialised at this point
		allData_length =  sizeOf(gooseData);
		
	}
	
	// Used to initialise a known frame
	public IEC61850_GOOSE_Frame(IEC61850_GOOSE_FrameEventHandler local_frameEventHandler,
			IEC61850_GOOSE_GSEControlBlock local_gseControlBlockAttributes) throws IEC61850_GOOSE_Exception
	{	
		frameEventHandler = local_frameEventHandler;
		gseControlBlockAttributes = local_gseControlBlockAttributes;
		
		gooseData =null;
		
		frameValidity = IEC61850_GOOSE_FrameValidityType.invalid;
		
		this.destinationMacAddress = gseControlBlockAttributes.macAddress;
		this.sourceMacAddress = "";
		
		this.appID = local_gseControlBlockAttributes.AppID;
		
		this.goCBref = build_GoCBref(gseControlBlockAttributes);
		this.datSet = build_datSet(gseControlBlockAttributes);
		this.goID = build_goID(gseControlBlockAttributes);

		this.test = false;
		this.ndsCom = false;
		this.stNum = 0;
		this.sqNum = 0;
		this.timeAllowedToLive = local_gseControlBlockAttributes.maxtime;
		this.confRevGoose = 1;
		this.numDatSetEntries = local_gseControlBlockAttributes.GOOSESignalsMap.size();
		
		// We set the data length
		stNum_length = sizeOf(stNum);
		sqNum_length = sizeOf(sqNum);
		
		// These are fields that will not change
		goCBref_length = sizeOf(goCBref); 
		timeAllowedToLive_length = sizeOf(timeAllowedToLive); 
		datSet_length = sizeOf(datSet);
		
		goID_length = sizeOf(goID); 
		confRevGoose_length = sizeOf(confRevGoose); 
		numDatSetEntries_length = sizeOf(numDatSetEntries);
		
		// We build the payload
		gooseData = new IEC61850_GOOSE_Data(numDatSetEntries);
		
		Iterator<Map.Entry<String, IEC61850_GOOSE_Signal>> GOOSESignalsMap_IT
			= gseControlBlockAttributes.GOOSESignalsMap.entrySet().iterator();
		
		for(int map_position = 0; map_position < numDatSetEntries; map_position++)
		{
			Map.Entry<String, IEC61850_GOOSE_Signal> current_signal_map_entry
				= GOOSESignalsMap_IT.next();
			
			int position = current_signal_map_entry.getValue().position;
			
			// We link the Data_Element object reference in the GOOSE_Signal object with the
			// actual Data_Element object in the gooseData object
			current_signal_map_entry.getValue().data = gooseData.data[position];
			
			// We initialise the signal type for every data_element
			gooseData.setType(position, IEC61850_GOOSE_MMS_DataType.get(current_signal_map_entry.getValue().bType));
			//current_signal_map_entry.getValue().data.dataType = IEC61850_GOOSE_MMS_DataType.get(current_signal_map_entry.getValue().bType);
			
			// We initialise the data length for every data_element
			// The number of byte is set according to the type of element specified in the ICD file
			gooseData.setLength(position, IEC61850_GOOSE_MMS_DataType.get_size(current_signal_map_entry.getValue().bType));
			
			// We initialise every element
			switch(IEC61850_GOOSE_MMS_DataType.get(current_signal_map_entry.getValue().bType))
			{
				case booln:
					gooseData.setValue(position, new Boolean(false));
					break;
					
				case integer: // MMS limits to INT32
					gooseData.setValue(position, new Integer(0));
					break;
					
				case unsign: // MMS limits to UINT32
					gooseData.setValue(position, new Integer(0));
					break;
					
				case float_point:
					gooseData.setValue(position, new Double(0));
					break;
					
				default:
					throw new UnsupportedOperationException("Unsupported data type");
			}
		}
		
		allData_length =  sizeOf(gooseData);
	}
	
	/*
	public void updateDataSize(IEC61850_GOOSE_Data local_goose_data)
	{
		try 
		{
			allData_length = sizeOf(local_goose_data);
		} 
		catch (IEC61850_GOOSE_Exception e) 
		{
			e.printStackTrace();
		}
	}
	*/
	
	// This function is called when the data was changed
	public JMemoryPacket updatePacket_From_Frame(JMemoryPacket goose_memoryPacket) throws IEC61850_GOOSE_Exception{
		
		int new_stNum_length = sizeOf(stNum);
		int new_sqNum_length = sizeOf(sqNum);
		
		// we update the data length of the data in the frame
		int new_allData_length = sizeOf(gooseData); 
		
		// We need to rescan to identify the goose header
		goose_memoryPacket.scan(JProtocol.ETHERNET_ID);
		
		//Ethernet eth_header = goose_memoryPacket.getHeader( new Ethernet());
		
		// We have to bind the goose_header to the JMemoryPacket
		IEC61850_GOOSE_Header goose_header = goose_memoryPacket.getHeader( new IEC61850_GOOSE_Header());
		
		// If the length of a variable length field has changed, we need to update the header
		if( (new_stNum_length != goose_header.stNumLength()) || 
				(new_sqNum_length != goose_header.sqNumLength()) || 
				(new_allData_length != goose_header.gooseDataLength()))
		{
			stNum_length = new_stNum_length;
			sqNum_length = new_sqNum_length;
			allData_length = new_allData_length;
			
			// We need to make a new packet
			goose_memoryPacket = makeNewPacket();
			
			// We need to rescan because the goose header length has changed
			goose_memoryPacket.scan(JProtocol.ETHERNET_ID);
			
			goose_header = goose_memoryPacket.getHeader( new IEC61850_GOOSE_Header());
		}
		
		goose_header.sqNum(sqNum);
		goose_header.stNum(stNum);
		
		// We set the UTC flags
		goose_header.leapSecondsKnown = leapSecondsKnown;
		goose_header.clockFailure = clockFailure;
		goose_header.clockNotSynchronized = clockNotSynchronized;
		goose_header.timeAccuracy = (byte) timeAccuracy;
		
		// We encode the data
		gooseData.encodeData(goose_header.gooseData());
		
		// time stamp the packet
		// The time must be at the moment at which stNum was incremented. IEC61850-7-2 18.2.3.5
		goose_header.utc(new Date());
		
		return goose_memoryPacket;
	}
	
	public void updateFrame_From_UnknownPacket(JPacket local_jPacket) throws IEC61850_GOOSE_Exception
	{
		// We initialise empty headers. Required to decode the packet.
		Ethernet eth_header = new Ethernet();
        IEEE802dot1q dot1q_header = new IEEE802dot1q();
        IEC61850_GOOSE_Header goose_header = new IEC61850_GOOSE_Header();
        
        if (local_jPacket.hasHeader(eth_header)) 
        {  
        	this.destinationMacAddress = FormatUtils.mac(eth_header.destination());
    		this.sourceMacAddress = FormatUtils.mac(eth_header.source());
        }
        else
        {
        	throw new IEC61850_GOOSE_Exception("Typing to decode an unknown packet of a type that is not of type Ethernet: " 
        			+ "Frame number: " + local_jPacket.getFrameNumber() 
        			+ "Ethernet source: " +  FormatUtils.mac(eth_header.source())
        			+ "Ethernet destination: " +  FormatUtils.mac(eth_header.destination())
        			+ "Packet type: " + eth_header.type());
        }
        // We have to check for IEEE 802.1Q NOTE: Q in Q not supported
        if (local_jPacket.hasHeader(dot1q_header))
        {
        	/* May be usefull in the future
        	int dot1q_priority = dot1q_header.priority();
        	int dot1q_cfi = dot1q_header.cfi();
        	int dot1q_id = dot1q_header.id();
        	*/
        }
    	
        if (local_jPacket.hasHeader(goose_header))
        {
        	if (goose_header.isValidHeader() == false)
	        	throw new IEC61850_GOOSE_Exception("Typing to decode an unknown packet that does not have a GOOSE header: " 
	        			+ "Frame number: " + local_jPacket.getFrameNumber() 
	        			+ "GOOSE header error: " +  goose_header.headerError);
        	
        	else
        	{
        		// We decode the content of the header
        		this.appID = goose_header.appID();
        		
        		this.goCBref = goose_header.goCBref();
        		this.datSet = goose_header.datSet();
        		this.goID = goose_header.goID();

        		this.test = goose_header.test();
        		this.ndsCom = goose_header.ndsCom();
        		this.stNum = goose_header.stNum();
        		this.sqNum = goose_header.sqNum();
        		this.timeAllowedToLive = goose_header.timeAllowedToLive();
        		this.confRevGoose = goose_header.confRev();
        		this.numDatSetEntries = goose_header.numDatSetEntries();
        		
        		// Reading the time in the header updates the time quality information
        		this.utc_time = goose_header.utc();
        		
        		this.leapSecondsKnown = goose_header.leapSecondsKnown;
        		this.clockFailure = goose_header.clockFailure;
        		this.clockNotSynchronized = goose_header.clockNotSynchronized;
        		this.timeAccuracy = goose_header.timeAccuracy;
        		
        		// We set the data length
        		stNum_length = sizeOf(stNum);
        		sqNum_length = sizeOf(sqNum);
    
        		// Verify frame validity
        		if ((new Date()).getTime() - this.utc_time > this.timeAllowedToLive)
        			frameValidity = IEC61850_GOOSE_FrameValidityType.questionable;
        		else
        			frameValidity = IEC61850_GOOSE_FrameValidityType.good;
        		
        		// We read the payload
        		gooseData = new IEC61850_GOOSE_Data(goose_header.numDatSetEntries());
        		
        		// We decode the data
        		gooseData.decodeData(goose_header.gooseData());
        	}
        }
	}
	
	public void updateFrame_From_Packet(JPacket local_jPacket){
		
		@SuppressWarnings("unused")
		Ethernet eth_header = local_jPacket.getHeader( new Ethernet());
		
		@SuppressWarnings("unused")
        IEEE802dot1q dot1q_header = local_jPacket.getHeader(new IEEE802dot1q()); 
		
		IEC61850_GOOSE_Header goose_header = local_jPacket.getHeader( new IEC61850_GOOSE_Header());
		
		this.test = goose_header.test();
		this.ndsCom = goose_header.ndsCom();
		this.stNum = goose_header.stNum();
		this.sqNum = goose_header.sqNum();
		this.timeAllowedToLive = goose_header.timeAllowedToLive();
		
		// Reading the time in the header updates the time quality information
		this.utc_time = goose_header.utc();
		
		this.leapSecondsKnown = goose_header.leapSecondsKnown;
		this.clockFailure = goose_header.clockFailure;
		this.clockNotSynchronized = goose_header.clockNotSynchronized;
		this.timeAccuracy = goose_header.timeAccuracy;
		
		// We decode the data
		gooseData.decodeData(goose_header.gooseData());
	}
	
	// This function only increments the sequence number ain the frame and the packet
	public JMemoryPacket incrementSqNum(JMemoryPacket goose_memoryPacket) throws IEC61850_GOOSE_Exception{
		
		// We need to rescan to identify the goose header
		goose_memoryPacket.scan(JProtocol.ETHERNET_ID); 
		
		// We have to bind the goose_header to the JMemoryPacket
		IEC61850_GOOSE_Header goose_header = goose_memoryPacket.getHeader( new IEC61850_GOOSE_Header());
		
		// We increment the sequence number in smaller than 4294967295
		if ((sqNum < 4294967295L))
		{
			sqNum++;
		}
		else // if larger, we wrap
		{
			sqNum = 1;
		}
		
		int new_sqNum_length = sizeOf(sqNum);
		
		// If the length of a variable length field has changed, we need make a new packet from scratch
		if (new_sqNum_length != sqNum_length)
		{			
			long time_stamp_original_packet;
			sqNum_length = new_sqNum_length;
			
			// We save the timestamp of the original packet
			time_stamp_original_packet = goose_header.utc();
			
			// We need to make a new packet
			goose_memoryPacket = makeNewPacket();
			
			// We need to rescan because the goose header length has changed
			goose_memoryPacket.scan(JProtocol.ETHERNET_ID);
			
			// We have to bind the goose_header to the new JMemoryPacket
			goose_header = goose_memoryPacket.getHeader( new IEC61850_GOOSE_Header());
			
			// We set the UTC flags
			goose_header.leapSecondsKnown = leapSecondsKnown;
			goose_header.clockFailure = clockFailure;
			goose_header.clockNotSynchronized = clockNotSynchronized;
			goose_header.timeAccuracy = (byte) timeAccuracy;
			
			// We encode the data
			gooseData.encodeData(goose_header.gooseData());
		
			// time stamp the new packet
			goose_header.utc(time_stamp_original_packet);
			
		}
		else
		{
			// The goose_packet structure did not change. we just need to update sqNum in the header.
			goose_header.sqNum(sqNum);
		}
		
		return goose_memoryPacket;
	}
	
	public JMemoryPacket makeNewPacket() throws IEC61850_GOOSE_Exception
	{
		// We initialise the new packet
		JMemoryPacket goose_memoryPacket = new JMemoryPacket(packetSize);
		goose_memoryPacket.order(java.nio.ByteOrder.BIG_ENDIAN);
		
		// decodes the packet. Assign ETHERNET type to the first header
		goose_memoryPacket.scan(JProtocol.ETHERNET_ID);
		
		// We set the Ethernet source and destination
		Ethernet eth_header = goose_memoryPacket.getHeader( new Ethernet() );
		
		eth_header.destination(FormatUtils.toByteArray(destinationMacAddress.replaceAll("-", "")));
		eth_header.source(FormatUtils.toByteArray(sourceMacAddress.replaceAll("-", "")));
		
		// We identify the next header as GOOSE
		eth_header.type(0x88b8);
		
		// set GOOSE header length to a possible value for the GOOSE header to be identified
		goose_memoryPacket.setByte(17, (byte)(packetSize - 14));
		
		// We need to rescan to identify the goose header
		goose_memoryPacket.scan(JProtocol.ETHERNET_ID); 
		
		// We create a new GOOSE header object
		IEC61850_GOOSE_Header goose_header = goose_memoryPacket.getHeader( new IEC61850_GOOSE_Header());
		
		goose_header.encodeHeader(goCBref_length, timeAllowedToLive_length, 
				datSet_length, goID_length, stNum_length, sqNum_length, test_length, 
				confRevGoose_length, ndsCom_length, numDatSetEntries_length, allData_length);
		
		// We need to rescan because the goose header length has changed
		goose_memoryPacket.scan(JProtocol.ETHERNET_ID);
		
		// There is a BUG in JNetPcap, function packet.scan() does not update packet length
		// We now adjust the packet size
		goose_memoryPacket.setSize(goose_header.length() + eth_header.getLength());
			
		// Now we populate all constant fields of the header
		goose_header.goCBref(goCBref);
		goose_header.datSet(datSet);
		goose_header.goID(goID);
		goose_header.test(test);
		goose_header.ndsCom(ndsCom);
		goose_header.timeAllowedToLive(timeAllowedToLive);
		goose_header.confRev(confRevGoose);
		goose_header.numDatSetEntries(numDatSetEntries);
		goose_header.appID(appID);
		
		return goose_memoryPacket;
	}

	public void setValueByKey(String key_name,Object value) throws IEC61850_GOOSE_Exception{
		
		// Verify if the key exists
		if (gseControlBlockAttributes.GOOSESignalsMap.containsKey(key_name) == false)
			throw new IEC61850_GOOSE_Exception("Invalid key in setDataByKey");
		
		// Sets the value of the data element
		// BE CAREFULL
		// The GOOSE_Signal data attribute is a reference to the GOOSE_Data_Element object that is held 
		// inside the gooseData attribute of the GOOSE_Frame
		gseControlBlockAttributes.GOOSESignalsMap.get(key_name).data.value = value;
		
		// We update the dataLength
		gseControlBlockAttributes.GOOSESignalsMap.get(key_name).data.length 
			= sizeOf(gseControlBlockAttributes.GOOSESignalsMap.get(key_name).data);
	}
	
	public Object getValueByKey(String key_name) throws IEC61850_GOOSE_Exception
	{
		// Verify if the key exists
		if (gseControlBlockAttributes.GOOSESignalsMap.containsKey(key_name) == false)
			throw new IEC61850_GOOSE_Exception("Invalid key in setDataByKey");
		
		return gseControlBlockAttributes.GOOSESignalsMap.get(key_name).data.value;
		
		//return dataMap.get(key_name).data;
		// return null;
	}
	
	private static String build_datSet(IEC61850_GOOSE_GSEControlBlock blockAttributes)
	{
		String dataSet_string = blockAttributes.iedName + blockAttributes.deviceName + "/" + 
		blockAttributes.ln0ClassName + "$GO$" + blockAttributes.datSet;
		
		// IEC61850 limits us to 35 characters
		if (dataSet_string.length() > 35)
		{
			// We need to cut it
			dataSet_string = dataSet_string.substring(dataSet_string.length()-35);
		}
		
		// Object reference description defined in IEC 61850-8-1
		return dataSet_string;
	}
	
	private static String build_GoCBref(IEC61850_GOOSE_GSEControlBlock blockAttributes)
	{
		String GoCBref_string = blockAttributes.iedName + blockAttributes.deviceName + "/" + 
		blockAttributes.ln0ClassName + "$GO$" + blockAttributes.gseControlName;
		
		// IEC61850 limits us to 35 characters
		if (GoCBref_string.length() > 35)
		{
			// We need to cut it
			GoCBref_string = GoCBref_string.substring(GoCBref_string.length()-35);
		}
		
		
		// Object reference description defined in IEC 61850-8-1
		return GoCBref_string;
	}
	
	private static String build_goID(IEC61850_GOOSE_GSEControlBlock blockAttributes)
	{
		String goID_string = blockAttributes.gseControlName;
		
		// IEC61850 limits us to 35 characters
		if (goID_string.length() > 35)
		{
			// We need to cut it
			goID_string = goID_string.substring(goID_string.length()-35);
		}
		
		return goID_string;
	}
	
	private int sizeOf(Object data) throws IEC61850_GOOSE_Exception{
		// Used by many different types
		// String, int, IEC61850_GOOSE_Data_Element, IEC61850_GOOSE_Data
		
		//int local_value;
		
		if (data.getClass() == Integer.class)
		{
			// When type is int, we assume it will be unsigned
			if ((Integer)data <= 255)
				return 1;

			else if ((Integer)data <= 65535)
				return 2;

			else if ((Integer)data <= 4294967295L)
				return 4;

			else
				throw new IEC61850_GOOSE_Exception("Value out of range");
			
		}
		
		if(data.getClass() == Long.class)
		{
			// When type is long, we assume it will be unsigned
			if ((Long)data <= 255)
				return 1;

			else if ((Long)data <= 65535)
				return 2;

			else if ((Long)data <= 4294967295L)
				return 4;

			else
				throw new IEC61850_GOOSE_Exception("Value out of range");
		}
		
		else if (data.getClass() == Boolean.class)
			// boolean is always of size 1
			return 1;

		else if (data.getClass() == String.class)
		{
			// The size is the number of characters
			int size = ((String)data).length();
			
			// The maximum size for 61850 visiblestring is 35
			if (size <= 35)
				return size;
			else
				throw new IEC61850_GOOSE_Exception("Value out of range");
			
		}
		
		if(data.getClass() == Float.class)
		{
			return 4;
		}
		
		else if(data.getClass() == Double.class)
		{
			return 8;
		}
		
		else if (data.getClass() == IEC61850_GOOSE_Data.class)
		{
			int total_size = 0;
			
			for(int currentEntry = 0; currentEntry < ((IEC61850_GOOSE_Data)data).numEntries; currentEntry++)
			{
				// We add 1 byte for the flag and 1 byte for the size
				total_size += 2;
				// We add the actual size
				total_size += ((IEC61850_GOOSE_Data)data).getLength(currentEntry);
			}
			
			return total_size;
		}
		else if (data.getClass() == IEC61850_GOOSE_Data_Element.class)
		{
			return ((IEC61850_GOOSE_Data_Element)data).length;
			
			/*
			
			switch(((IEC61850_GOOSE_Data_Element)data).dataType)
			{
				case booln:
					return 1;
					
				case integer: // MMS limits to INT32
					
					local_value = (Integer)(((IEC61850_GOOSE_Data_Element)data).value);
					
					if ((local_value >= -128) && (local_value < 127) )
						return 1;

					else if ((local_value >= -32768) && (local_value < 32767) )
						return 2;

					else if ((local_value >= -2147483648) && (local_value < 2147483647) )
						return 4;

					else
						throw new IEC61850_GOOSE_Exception("Value out of range");
					
				case unsign: // MMS limits to UINT32
					
					local_value = (Integer)(((IEC61850_GOOSE_Data_Element)data).value);

					if (local_value <= 255)
						return 1;

					else if (local_value <= 65535)
						return 2;

					else if (local_value <= 4294967295L)
						return 4;

					else
						throw new IEC61850_GOOSE_Exception("Value out of range");
				
				// Finish it up
				case float_point:
				
					if(data.getClass() == Float.class)
					{
						return 4;
					}
					else if(data.getClass() == Double.class)
					{
						return 8;
					}
					
				default:
					throw new UnsupportedOperationException("Unsupported data type");
			}
			*/
		}
		else
		{
			throw new IEC61850_GOOSE_Exception("Undefined Type: " + data.getClass() + " in function sizeOf");
			
		}
	}
	
	public void setTimingAttributes(boolean leapSecondsKnown,boolean clockFailure,boolean clockNotSynchronized,int timeAccuracy)
	{
		this.leapSecondsKnown = leapSecondsKnown;
		this.clockFailure = clockFailure;
		this.clockNotSynchronized = clockNotSynchronized;
		this.timeAccuracy = (byte) timeAccuracy;
	}
}
