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
 * IEC 61850 GOOSE Header definition
 * The data format is defined in Standard IEC 61850-8-1
 * GOOSE is used mostly in the Electric power system industry for real-time communications.
 * 
 * @author  Philippe Venne
 * @version 0.1
 *
 */

package jgoose;

import org.jnetpcap.packet.JHeader;
import org.jnetpcap.packet.JRegistry;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.RegistryHeaderErrors;
import org.jnetpcap.packet.annotate.*;
import org.jnetpcap.protocol.lan.*;
import org.jnetpcap.nio.*;

import java.util.Date;


@Header(name = "IEC61850 GOOSE Header")  
public class IEC61850_GOOSE_Header extends JHeader
{
	// We define the constants for GOOSE protocol
	private static final int GOOSETYPE = 0x88b8;
	
	private static final int APPID_POS					= 0;
	private static final int APPID_LENGTH				= 2;
	private static final int RESERVED_LENGTH			= 4;
	private static final int LENGTH_POS					= 2;
	private static final int LENGTH_LENGTH				= 2;
	private static final int ADPU_LENGTH_TAG			= 0x61;
	private static final int ADPU_LENGTH_1_BYTE			= 0x81;
	private static final int ADPU_LENGTH_2_BYTE			= 0x82;
	private static final int ADPU_LENGTH_TAG_POS		= 8;
	private static final int GOCB_REF_TAG 				= 0x00;
	private static final int TIME_ALLOWED_TO_LIVE_TAG	= 0x01;
	private static final int DAT_SET_GOOSE_TAG			= 0x02;
	private static final int GO_ID_TAG					= 0x03;
	private static final int UTC_TAG					= 0x04;
	private static final int UTC_TAG_LENGTH				= 2;
	private static final int UTC_LENGTH					= 8;
	private static final int ST_NUM_TAG					= 0x05;
	private static final int SQ_NUM_TAG					= 0x06;
	private static final int TEST_TAG					= 0x07;
	private static final int CONF_REV_GOOSE_TAG			= 0x08;
	private static final int NDS_COM_TAG				= 0x09;
	private static final int NUM_DAT_SET_ENTRIES_TAG	= 0x0a;
	private static final int ALL_DATA_TAG				= 0x0b;
	//private static final int SECURITY_GOOSE_TAG			= 0x0c;
	
	//private static final int UNIVERSAL_PRIMITIVE			= 0x00;
	//private static final int UNIVERSAL_CONSTRUCTED			= 0x20;
	private static final int CONTEXT_SPECIFIC_PRIMITIVE		= 0x80;
	private static final int CONTEXT_SPECIFIC_CONSTRUCTED	= 0xa0;
	
	// Header variable length fields
	private int apduLength_length;
	private int goCBref_length;
	private int goCBref_tag_position;
	private int timeAllowedToLive_length;
	private int timeAllowedToLive_tag_position;
	private int datSet_length;
	private int datSet_tag_position;
	private int goID_length;
	private int goID_tag_position;
	private int utc_tag_position;
	private int stNum_length;
	private int stNum_tag_position;
	private int sqNum_length;
	private int sqNum_tag_position;
	private int test_length;
	private int test_tag_position;
	private int confRevGoose_length;
	private int confRevGoose_tag_position;
	private int ndsCom_length;
	private int ndsCom_tag_position;
	private int numDatSetEntries_length;
	private int numDatSetEntries_tag_position;
	private int allData_length;
	private int allData_tag_position;
	
	// for tests
	public int header_length;
	
	// Invalid header flag
	public int headerError = 0;
	
	// This variable represents the nanoseconds in the time stamp not represented in
	// utc milliseconds.
	// This value is initialised by the utc function
	// The milliseconds are already included in the milliseconds of the utc function
	public double utcNanoSeconds = 0;
	
	// The value TRUE of the attribute LeapSecondsKnown shall indicate
	// that the value of the time stamp contains all leap seconds occurred
	// This value is initialised by the utc function
	public boolean leapSecondsKnown;
	
	// The attribute clockFailure shall indicate that the time source of the sending
	// device is unreliable
	// This value is initialised by the utc function
	public boolean clockFailure;
	
	// When set, the attribute clockNotSynchronized shall indicate that
	// the time source of the sending device is not synchronized with the external UTC time
	// This value is initialised by the utc function
	public boolean clockNotSynchronized;
	
	// The attribute TimeAccuracy shall represent the time accuracy class of the
	// time respective time stamp relative to the external UTC time as defined
	// in IEC 61850-7-2 section 6.1.2.9.2
	// This value is initialised by the utc function
	public byte timeAccuracy;
	
	// We set the GOOSE header length including the goose data
	@HeaderLength
	public static int headerLength(JBuffer buffer, int offset)
	{	
		return (buffer.getUShort(offset + LENGTH_POS));
	}
	
	// We set the bindings
	@Bind(to = Ethernet.class)
	public static boolean bindGooseToEthernet(JPacket packet, Ethernet eth)
	{
		return eth.type() == GOOSETYPE;
	}
	
	@Bind(to = IEEE802dot1q.class)
	public static boolean bindGooseToIEEE802dot1q(JPacket packet, IEEE802dot1q ieee1q)
	{
		return ieee1q.type() == GOOSETYPE;
	}
	
	// We decode the header to compute the offset and length of each field
	// This method gets called by JNetPcap every time a header is successfully peered with new
	// buffer and/or state structure.
	@Override
	protected void decodeHeader()
	{
		int headerPosition = ADPU_LENGTH_TAG_POS;
		
		// Validate the ADPU_LENGTH_TAG
		if ( super.getUByte(headerPosition) != ADPU_LENGTH_TAG)
		{
			headerError = -1;
			return;
		}
		
		headerPosition ++;
		
		// The ADPU length field has a variable length. It can be 1 byte, 2 bytes or 3 bytes
		if ( super.getUByte(headerPosition) <= 127)
			apduLength_length =  1;	// 2 bytes length; 1 for tag and 1 for data
		
		else if ( super.getUByte(headerPosition) == ADPU_LENGTH_1_BYTE) // <= 255
		{
			apduLength_length =  2;	// 3 bytes length; 2 for tag and 1 for data
		}
		
		else
			apduLength_length =  3;	// 4 bytes length; 2 for tag and 2 for data
		
		headerPosition += apduLength_length;
		goCBref_tag_position = headerPosition;
		
		// Validate the GOCB_REF_TAG
		if ( super.getUByte( headerPosition ) != (CONTEXT_SPECIFIC_PRIMITIVE | GOCB_REF_TAG))
		{
			headerError = -2;
			return;
		}
		
		headerPosition ++;
		goCBref_length = super.getUByte(headerPosition);
		headerPosition += (goCBref_length + 1);
		timeAllowedToLive_tag_position = headerPosition;
		
		
		// Validate the TIME_ALLOWED_TO_LIVE_TAG
		if ( super.getUByte( headerPosition) != (CONTEXT_SPECIFIC_PRIMITIVE | TIME_ALLOWED_TO_LIVE_TAG))
		{
			headerError = -3;
			return;
		}
		
		headerPosition ++;
		timeAllowedToLive_length = super.getUByte( headerPosition);
		headerPosition += (timeAllowedToLive_length + 1);
		datSet_tag_position = headerPosition;
		
		// Validate the DAT_SET_GOOSE_TAG
		if ( super.getUByte(headerPosition) != (CONTEXT_SPECIFIC_PRIMITIVE | DAT_SET_GOOSE_TAG))
		{
			headerError = -4;
			return;
		}
		
		headerPosition ++;
		datSet_length = super.getUByte(headerPosition);
		headerPosition += (datSet_length + 1);
		
		// Validate if a goID field is present. This field is optional
		goID_length = 0;
		goID_tag_position = 0;
		if ( super.getUByte(headerPosition) == (CONTEXT_SPECIFIC_PRIMITIVE | GO_ID_TAG))
		{
			goID_tag_position = headerPosition;
			headerPosition ++;
			goID_length = super.getUByte(headerPosition);
			headerPosition += (goID_length + 1);
		}
		
		utc_tag_position = headerPosition;
		
		// Validate the UTC_TAG
		if ( super.getUByte(headerPosition) != (CONTEXT_SPECIFIC_PRIMITIVE | UTC_TAG))
		{
			headerError = -5;
			return;
		}
		
		headerPosition += UTC_TAG_LENGTH;
		headerPosition += UTC_LENGTH;
		stNum_tag_position = headerPosition;
		
		// Validate the ST_NUM_TAG
		if ( super.getUByte(headerPosition) != (CONTEXT_SPECIFIC_PRIMITIVE | ST_NUM_TAG))
		{
			headerError = -6;
			return;
		}
		
		headerPosition ++;
		stNum_length = super.getUByte(headerPosition);
		headerPosition += (stNum_length + 1);
		sqNum_tag_position = headerPosition;
		
		// Validate the SQ_NUM_TAG
		if ( super.getUByte(headerPosition) != (CONTEXT_SPECIFIC_PRIMITIVE | SQ_NUM_TAG))
		{
			headerError = -7;
			return;
		}
		
		headerPosition ++;
		sqNum_length = super.getUByte(headerPosition);
		headerPosition += (sqNum_length + 1);
		test_tag_position = headerPosition;
		
		// Validate the TEST_TAG
		if ( super.getUByte(headerPosition) != (CONTEXT_SPECIFIC_PRIMITIVE | TEST_TAG))
		{
			headerError = -8;
			return;
		}
		
		headerPosition ++;
		test_length = super.getUByte(headerPosition);
		headerPosition += (test_length + 1);
		confRevGoose_tag_position = headerPosition;
		
		// Validate the CONF_REV_GOOSE_TAG
		if ( super.getUByte(headerPosition) != (CONTEXT_SPECIFIC_PRIMITIVE | CONF_REV_GOOSE_TAG))
		{
			headerError = -9;
			return;
		}
		
		headerPosition ++;
		confRevGoose_length = super.getUByte(headerPosition);
		headerPosition += (confRevGoose_length + 1);
		ndsCom_tag_position = headerPosition;
		
		// Validate the NDS_COM_TAG
		if ( super.getUByte(headerPosition) != (CONTEXT_SPECIFIC_PRIMITIVE | NDS_COM_TAG))
		{
			headerError = -10;
			return;
		}
		
		headerPosition ++;
		ndsCom_length = super.getUByte(headerPosition);
		headerPosition += (ndsCom_length + 1);
		numDatSetEntries_tag_position = headerPosition;
		
		// Validate the NUM_DAT_SET_ENTRIES_TAG
		if ( super.getUByte(headerPosition) != (CONTEXT_SPECIFIC_PRIMITIVE | NUM_DAT_SET_ENTRIES_TAG))
		{
			headerError = -11;
			return;
		}
		
		headerPosition ++;
		numDatSetEntries_length = super.getUByte(headerPosition);
		
		headerPosition += (numDatSetEntries_length + 1);
		allData_tag_position = headerPosition;
		
		// Validate the ALL_DATA_TAG
		if ( super.getUByte(headerPosition) != (CONTEXT_SPECIFIC_CONSTRUCTED | ALL_DATA_TAG))
		{
			headerError = -12;
			return;
		}

		headerPosition ++;
		allData_length = super.getUByte(headerPosition);		
	}
	
	// This function is used to create a new GOOSE FRAME
	@FieldSetter
	public void encodeHeader(int goCBref_length, int timeAllowedToLive_length,
			int datSet_length, int goID_length, int stNum_length, int sqNum_length, int test_length,
			int confRevGoose_length, int ndsCom_length, int numDatSetEntries_length, int allData_length)
	{
		int headerPosition = ADPU_LENGTH_TAG_POS;
		
		// We compute the apduLength value. It corresponds to all fields length + tags + length fields
		int apduLengthValue = 2 + goCBref_length + 2 + timeAllowedToLive_length + 2 + datSet_length +
			2 + UTC_LENGTH + 2 + stNum_length + 2 + sqNum_length + 2 + test_length + 
			2 + confRevGoose_length + 2 + ndsCom_length + 2 + numDatSetEntries_length + 
			2 + allData_length;
		
		if (goID_length != 0)
			apduLengthValue += (2 + goID_length);
		
		// Writes the ADPU_LENGTH_TAG
		super.setUByte(headerPosition, ADPU_LENGTH_TAG);
		headerPosition ++;
		
		// Writes the apduLen and the correct tags
		// BY IEC 61850-8-1 2004-05 Annex C, ADPU <= 1492
		if ( apduLengthValue <= 127)
		{
			super.setUByte(headerPosition, apduLengthValue);
			apduLength_length = 1;
		}
		else if ( apduLengthValue <= 255)
		{
			super.setUByte(headerPosition, ADPU_LENGTH_1_BYTE);
			super.setUByte(headerPosition + 1, (byte)apduLengthValue);
			apduLength_length = 2;
		}
		else
		{
			super.setUByte(headerPosition, ADPU_LENGTH_2_BYTE);
			super.setUShort(headerPosition + 1, (short)apduLengthValue);
			apduLength_length = 3;
		}
		
		headerPosition += apduLength_length;
		
		// Writes the header length
		// We do it here because we need the apduLength_length
		super.setUShort(LENGTH_POS, (short)apduLengthValue + 
				APPID_LENGTH + LENGTH_LENGTH + RESERVED_LENGTH + 1 + apduLength_length);
		
		// Writes the GOCB_REF_TAG
		this.goCBref_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | GOCB_REF_TAG);
		headerPosition ++;
		
		// Writes the goCBref_length
		super.setUByte(headerPosition, goCBref_length);
		headerPosition ++;
		headerPosition += goCBref_length;
		this.goCBref_length = goCBref_length;
		
		// Writes the TIME_ALLOWED_TO_LIVE_TAG
		this.timeAllowedToLive_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | TIME_ALLOWED_TO_LIVE_TAG);
		headerPosition ++;
		
		// Writes the timeAllowedToLive_length
		super.setUByte(headerPosition, timeAllowedToLive_length);
		headerPosition ++;
		headerPosition += timeAllowedToLive_length;
		this.timeAllowedToLive_length = timeAllowedToLive_length;
		
		// Writes the DAT_SET_GOOSE_TAG
		this.datSet_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | DAT_SET_GOOSE_TAG);
		headerPosition ++;
		
		// Writes the datSet_length
		super.setUByte(headerPosition, datSet_length);
		headerPosition ++;
		headerPosition += datSet_length;
		this.datSet_length = datSet_length;
		
		// This is an optional field
		if (goID_length != 0)
		{
			// Writes the GO_ID_TAG
			goID_tag_position = headerPosition;
			super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | GO_ID_TAG);
			headerPosition ++;
			
			// Writes the goID_length
			super.setUByte(headerPosition, goID_length);
			headerPosition ++;
			headerPosition += goID_length;
		}
		else
		{
			goID_tag_position = 0;
		}
		this.goID_length = goID_length;
		
		// Writes the UTC_TAG
		this.utc_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | UTC_TAG);
		headerPosition ++;
		
		// Writes the UTC_LENGTH
		super.setUByte(headerPosition, UTC_LENGTH);
		headerPosition ++;
		headerPosition += UTC_LENGTH;
		
		// Writes the ST_NUM_TAG
		this.stNum_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | ST_NUM_TAG);
		headerPosition ++;
		
		// Writes the stNum_length
		super.setUByte(headerPosition, stNum_length);
		headerPosition ++;
		headerPosition += stNum_length;
		this.stNum_length = stNum_length;
		
		// Writes the SQ_NUM_TAG
		this.sqNum_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | SQ_NUM_TAG);
		headerPosition ++;
		
		// Writes the sqNum_length
		super.setUByte(headerPosition, sqNum_length);
		headerPosition ++;
		headerPosition += sqNum_length;
		this.sqNum_length = sqNum_length;
		
		// Writes the TEST_TAG
		this.test_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | TEST_TAG);
		headerPosition ++;
		
		// Writes the test_length
		super.setUByte(headerPosition, test_length);
		headerPosition ++;
		headerPosition += test_length;
		this.test_length = test_length;
		
		// Writes the CONF_REV_GOOSE_TAG
		this.confRevGoose_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | CONF_REV_GOOSE_TAG);
		headerPosition ++;
		
		// Writes the confRevGoose_length
		super.setUByte(headerPosition, confRevGoose_length);
		headerPosition ++;
		headerPosition += confRevGoose_length;
		this.confRevGoose_length = confRevGoose_length;
		
		// Writes the NDS_COM_TAG
		this.ndsCom_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | NDS_COM_TAG);
		headerPosition ++;
		
		// Writes the ndsCom_length
		super.setUByte(headerPosition, ndsCom_length);
		headerPosition ++;
		headerPosition += ndsCom_length;
		this.ndsCom_length = ndsCom_length;
		
		// Writes the NUM_DAT_SET_ENTRIES_TAG
		this.numDatSetEntries_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_PRIMITIVE | NUM_DAT_SET_ENTRIES_TAG);
		headerPosition ++;
		
		// Writes the numDatSetEntries_length
		super.setUByte(headerPosition, numDatSetEntries_length);
		headerPosition ++;
		headerPosition += numDatSetEntries_length;
		this.numDatSetEntries_length = numDatSetEntries_length;
		
		// Writes the ALL_DATA_TAG
		this.allData_tag_position = headerPosition;
		super.setUByte(headerPosition, CONTEXT_SPECIFIC_CONSTRUCTED | ALL_DATA_TAG);
		headerPosition ++;
		
		// Writes the allData_length
		super.setUByte(headerPosition, allData_length);
		this.allData_length = allData_length;
	}	
	
	public boolean isValidHeader()
	{
		return (headerError == 0);
	}
	
	public boolean goIDPresent()
	{
		return (goID_length != 0);
	}
	
	// We define the GOOSE header fields
	@Field (offset = APPID_POS * 8, length = APPID_LENGTH * 8, description = "Application ID" )
	public int appID()
	{
		// appID can be in the range of 0x0000 to 0x3FFF IEC 61850-8-1 annex C
		return super.getUShort(APPID_POS); // Offset 0, length 2 bytes
	}
	
	@FieldSetter
	public void appID(int value)
	{
		super.setUShort(APPID_POS, value);
	}
	
	// This length field is the sum if the GOOSE header and data
	@Field (offset = LENGTH_POS * 8, length = LENGTH_LENGTH * 8, description = "GOOSE packet length" )
	public int length()
	{
		return super.getUShort(LENGTH_POS); // Offset 2, length 2 bytes
	}
	
	@Field (offset = ADPU_LENGTH_TAG_POS * 8, description = "Application Protocol Data Unit length" )
	public int apduLen()
	{
		// BY IEC 61850-8-1 2004-05 Annex C, ADPU <= 1492
		
		// The base tag is 0x61 at offset 8
		if ( super.getUByte(ADPU_LENGTH_TAG_POS + 1) <= 127)
			return super.getUByte(ADPU_LENGTH_TAG_POS + 1);	// 2 bytes length; 1 for tag and 1 for data
		
		else if ( super.getUByte(ADPU_LENGTH_TAG_POS + 1) == 0x81)
			return super.getUByte(ADPU_LENGTH_TAG_POS + 1 +1);	// 3 bytes length; 2 for tag and 1 for data
		
		else
			return super.getUShort(ADPU_LENGTH_TAG_POS + 1 +1);	// 4 bytes length; 2 for tag and 2 for data
	}
	
	// The ADPU length field has a variable length. It can be 1 byte, 2 bytes or 3 bytes
	@Dynamic(Field.Property.LENGTH)
	public int apduLenLength()
	{
		return apduLength_length;
	}
	
	@Field (description = "GOOSE Control Block Reference" )
	public String goCBref()
	{	
		byte [] byteArray = super.getByteArray(goCBref_tag_position + 2, goCBref_length);
		return new String(byteArray);
	}
	
	@FieldSetter
	public void goCBref(String value)
	{
		byte [] byteArray = value.getBytes();
		super.setByteArray(goCBref_tag_position + 2, byteArray);
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int goCBrefOffset()
	{
		return goCBref_tag_position * 8;
	}

	@Dynamic(Field.Property.LENGTH)
	public int goCBrefLength()
	{
		return goCBref_length; 
	}
	
	@Field (description = "Time allowed to Live" )
	public long timeAllowedToLive()
	{
		if (timeAllowedToLive_length == 1)
			return super.getUByte(timeAllowedToLive_tag_position+2);
		
		else if (timeAllowedToLive_length == 2)
			return super.getUShort(timeAllowedToLive_tag_position+2);
		
		else if (timeAllowedToLive_length == 4)
			return super.getUInt(timeAllowedToLive_tag_position+2);
			
		else
			throw new UnsupportedOperationException("Cannot read timeAllowedToLive on 3 bytes");
	}
	
	@FieldSetter
	public void timeAllowedToLive(long value)
	{
		if (timeAllowedToLive_length == 1)
			super.setUByte(timeAllowedToLive_tag_position+2, (int) value);
		
		else if (timeAllowedToLive_length == 2)
			super.setUShort(timeAllowedToLive_tag_position+2, (int) value);
		
		else if (timeAllowedToLive_length == 4)
			super.setUInt(timeAllowedToLive_tag_position+2, value);
			
		else
			throw new UnsupportedOperationException("Cannot write timeAllowedToLive on 3 bytes");
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int timeAllowedToLiveOffset()
	{
		return timeAllowedToLive_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int timeAllowedToLiveLength()
	{
		return timeAllowedToLive_length; // Add one byte for TAG and one byte for length
	}
	
	@Field (description = "Data set reference" )
	public String datSet()
	{
		byte [] byteArray = super.getByteArray(datSet_tag_position + 2, datSet_length);
		return new String(byteArray);
	}
	
	@FieldSetter
	public void datSet(String value)
	{
		byte [] byteArray = value.getBytes();
		super.setByteArray(datSet_tag_position + 2, byteArray);
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int datSetOffset()
	{
		return datSet_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int datSetLength()
	{
		return datSet_length; 
	}
	
	@Field (description = "GOOSE identifier" )
	public String goID()
	{
		// goID is an optional field.
		if (goID_tag_position == 0)
			return new String("");
		else
		{
			byte [] byteArray = super.getByteArray(goID_tag_position + 2, goID_length);
			return new String(byteArray);
		}
	}
	
	@FieldSetter
	public void goID(String value)
	{
		// goID is an optional field.
		if (goID_tag_position != 0)
		{
			byte [] byteArray = value.getBytes();
			super.setByteArray(goID_tag_position + 2, byteArray);
		}
		//else
		//	throw new UnsupportedOperationException("Undefined goID field");
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int goIDOffset()
	{
		// goID_tag_position = 0 if the goID field is not defined
		return goID_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int goIDLength()
	{
		// goID_length = 0 if the goID field is not defined
		return goID_length;
	}
	
	@Field (description = "UTC Time Stamp" )
	public long utc()
	{
		/* UTC field in GOOSE frame is defined in IEC 61850-7-2 section 6.1.2.9.2 page 26
		 * The first 32 bits represents the number of seconds since midnight (00:00:00) of
		 * 1970-01-01
		 * The next 24 bits define the factions of seconds
		 * The last 8 bits represents the time quality flags
		 * Java defines UTC as the number of milliseconds since midnight (00:00:00) of
		 * 1970-01-01
		 */
		
		// The first 4 bytes define the number of seconds since midnight (00:00:00) of
		// 1970-01-01
		long utcMilliseconds = super.getUInt(utc_tag_position + 2)* 1000;
		
		// The last 4 bytes define the faction bits and the flags
		long utcFractionBits = super.getUInt(utc_tag_position +4 + 2);
		
		// We decode the flag bits
		leapSecondsKnown = (utcFractionBits & 0x80L) != 0;
		clockFailure = (utcFractionBits & 0x40L) != 0;
		clockNotSynchronized = (utcFractionBits & 0x20L) != 0;
		timeAccuracy = (byte)(utcFractionBits & 0x1FL);
		
		// We mask the flag bits
		utcFractionBits &= 0xFFFFFF00L;
		
		/* double type representation in IEEE-754
		 * bit 63 = sign bit. 0 = positive, 1 = negative
		 * bit 62 -> 52 = 11 bits for exponent
		 * bit 51 -> 0 = 52 bits for fraction
		 * For 1 <= E <= 2046, N = (-1)^S × 1.F × 2^(E-1023) : Normalised form
		 * For E = 0, N = (-1)^S × 0.F × 2^(-1022) : Denormalised form
		 */
		
		// This is a bit buffer to play with in order to build our double
		long doubleAsLong = 0;
		
		// We set S = 0
		// We set E = 1023 for 2^(E-1023) to be equal 1
		doubleAsLong |= 1023L << 52;
		
		// We shift 20 bits left so the first bit
		// of the 24 fraction bits from utc is at position 51
		doubleAsLong |= utcFractionBits << 20 ;
		
		// Using the normalised form, we have to substract 1 to the faction
		double utcFractionOfSeconds = Double.longBitsToDouble(doubleAsLong) - 1;
		
		// This variable represents the nanoseconds in the time stamp not represented in
		// utc milliseconds.
		utcNanoSeconds = (utcFractionOfSeconds % 0.001) * 1000000;
		
		// We return the sum of milliseconds from the date and the milliseconds that are
		// fractions of 1 second
		return utcMilliseconds + (long)((utcFractionOfSeconds - (utcFractionOfSeconds % 0.001))*1000);
	}
	
	@FieldSetter
	public void utc(Date value)
	{
		// The first 4 bytes define the number of seconds since midnight (00:00:00) of
		// 1970-01-01
		long utcSeconds = value.getTime() / 1000;
		super.setUInt(utc_tag_position + 2, utcSeconds);
		
		// The next 24 bits define the factions of seconds
		double utcFractionOfSeconds = ((double)(value.getTime() % 1000))/1000;
		// We have to add 1 to be in double normalized form
		utcFractionOfSeconds += 1;
		
		long doubleAsLong = Double.doubleToLongBits(utcFractionOfSeconds);
		
		// Faction bits ok.
		// exponent bits = 1023
		// We extract the faction bits 51 -> 27 ; the first 24 bits.
		doubleAsLong &= 0xFFFFFF0000000L;
		
		// we shift 20 bits right
		doubleAsLong = doubleAsLong >> 20;
		
		// The last 8 bits represents the time quality flags
		// We encode the flag bits
		// int myInt = (myBoolean)?1:0 ; // Cast boolean to int
		long utcFlags = (((leapSecondsKnown)?1:0) << 7) | 
			(((clockFailure)?1:0) << 6) | (((clockNotSynchronized)?1:0) << 5) |
			(timeAccuracy & 0x1F);
		
		// we insert the flag bits
		doubleAsLong |= utcFlags;
		
		super.setUInt(utc_tag_position +4 + 2, doubleAsLong);
	}
	
	@FieldSetter
	public void utc(long value)
	{
		super.setUInt(utc_tag_position +4 + 2, value);
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int utcOffset()
	{
		return utc_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int utcLength()
	{
		return 8; // 8 bytes for UTC
	}
	
	@Field (description = "State number" )
	public long stNum()
	{
		if (stNum_length == 1)
			return super.getUByte(stNum_tag_position+2);
		
		else if (stNum_length == 2)
			return super.getUShort(stNum_tag_position+2);
		
		else if (stNum_length == 4)
			return super.getUInt(stNum_tag_position+2);
			
		else
			throw new UnsupportedOperationException("Cannot read stNum on 3 bytes");

	}
	
	@FieldSetter
	public void stNum(long value)
	{
		if (stNum_length == 1)
			super.setUByte(stNum_tag_position+2, (short) value);
		
		else if (stNum_length == 2)
			super.setUShort(stNum_tag_position+2, (int) value);
		
		else if (stNum_length == 4)
			super.setUInt(stNum_tag_position+2, value);
			
		else
			throw new UnsupportedOperationException("Cannot write stNum on 3 bytes");
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int stNumOffset()
	{
		return stNum_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int stNumLength()
	{
		return stNum_length;
	}
	
	@Field (description = "Sequence number" )
	public long sqNum()
	{
		if (sqNum_length == 1)
			return super.getUByte(sqNum_tag_position+2);
		
		else if (sqNum_length == 2)
			return super.getUShort(sqNum_tag_position+2);
		
		else if (sqNum_length == 4)
			return super.getUInt(sqNum_tag_position+2);
			
		else
			throw new UnsupportedOperationException("Cannot read sqNum on 3 bytes");

	}
	
	@FieldSetter
	public void sqNum(long value)
	{
		if (sqNum_length == 1)
			super.setUByte(sqNum_tag_position+2, (short) value);
		
		else if (sqNum_length == 2)
			super.setUShort(sqNum_tag_position+2, (int) value);
		
		else if (sqNum_length == 4)
			super.setUInt(sqNum_tag_position+2, value);
			
		else
			throw new UnsupportedOperationException("Cannot write sqNum on 3 bytes");
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int sqNumOffset()
	{
		return sqNum_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int sqNumLength()
	{
		return sqNum_length; 
	}
	
	@Field (description = "Test" )
	public boolean test()
	{
		if (test_length == 1)
			return super.getUByte(test_tag_position+2) != 0;
		else
			throw new UnsupportedOperationException("Cannot read test on more than 1 byte");
	}
	
	@FieldSetter
	public void test(boolean value)
	{
		if (test_length == 1)
			setUByte(test_tag_position+2, ((value)?1:0) );
		else
			throw new UnsupportedOperationException("Cannot write test on more than 1 byte");
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int testOffset()
	{
		return test_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int testLength()
	{
		return test_length; // Add one byte for TAG and one byte for length
	}
	
	@Field (description = "Configuration revision" )
	public long confRev()
	{
		if (confRevGoose_length == 1)
			return super.getUByte(confRevGoose_tag_position+2);
		
		else if (confRevGoose_length == 2)
			return super.getUShort(confRevGoose_tag_position+2);
		
		else if (confRevGoose_length == 4)
			return super.getUInt(confRevGoose_tag_position+2);
			
		else
			throw new UnsupportedOperationException("Cannot read confRev on 3 bytes");
	}
	
	@FieldSetter
	public void confRev(long value)
	{
		if (confRevGoose_length == 1)
			setUByte(confRevGoose_tag_position+2,(short) value);
		
		else if (confRevGoose_length == 2)
			setUShort(confRevGoose_tag_position+2,(int) value);
		
		else if (confRevGoose_length == 4)
			setUInt(confRevGoose_tag_position+2, value);
			
		else
			throw new UnsupportedOperationException("Cannot write confRev on 3 bytes");
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int confRevOffset()
	{
		return confRevGoose_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int confRevLength()
	{
		return confRevGoose_length;
	}
	
	@Field (description = "Needs commissioning" )
	public boolean ndsCom()
	{
		if (ndsCom_length == 1)
			return super.getUByte(ndsCom_tag_position+2) != 0;
		else
			throw new UnsupportedOperationException("Cannot read ndsCom on more than  1 byte");
	}
	
	@FieldSetter
	public void ndsCom(boolean value)
	{
		if (ndsCom_length == 1)
			setUByte(ndsCom_tag_position+2, ((value)?1:0));
		else
			throw new UnsupportedOperationException("Cannot write ndsCom on more than  1 byte");
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int ndsComOffset()
	{
		return ndsCom_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int ndsComLength()
	{
		return ndsCom_length; 
	}
	
	@Field (description = "Number of data set entries" )
	public int numDatSetEntries()
	{
		
		if (numDatSetEntries_length == 1)
			return super.getUByte(numDatSetEntries_tag_position+2);
		
		else if (numDatSetEntries_length == 2)
			return super.getUShort(numDatSetEntries_tag_position+2);
			
		else
			throw new UnsupportedOperationException("Cannot read numDatSetEntries on more than 2 bytes");
	}
	
	@FieldSetter
	public void numDatSetEntries(int value)
	{
		if (numDatSetEntries_length == 1)
			setUByte(numDatSetEntries_tag_position+2,(short) value);
		
		else if (numDatSetEntries_length == 2)
			setUShort(numDatSetEntries_tag_position+2, value);
			
		else
			throw new UnsupportedOperationException("Cannot read numDatSetEntries on more than 2 bytes");
	}
	
	@Dynamic(Field.Property.OFFSET)
	public int numDatSetEntriesOffset()
	{
		return numDatSetEntries_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int numDatSetEntriesLength()
	{
		return numDatSetEntries_length; // Add one byte for TAG and one byte for length
	}
	
	@Field (description = "GOOSE data" )
	public JBuffer gooseData()
	{
		// Dataset described according to IEC 8825-1
		
		JBuffer gooseDataBuffer = new JBuffer(Type.POINTER);
		gooseDataBuffer.order(java.nio.ByteOrder.BIG_ENDIAN);
		
		gooseDataBuffer.peer(super.getParent(), allData_tag_position + 2, allData_length);
		
		return gooseDataBuffer;
	}
	
	/* Unused
	@FieldSetter
	public void gooseData(JBuffer local_gooseData)
	{

	}
	*/
	
	@Dynamic(Field.Property.OFFSET)
	public int gooseDataOffset()
	{
		return allData_tag_position * 8;
	}
	
	@Dynamic(Field.Property.LENGTH)
	public int gooseDataLength()
	{
		return allData_length; 
	}
	
	// We register the new protocol class in JNetPcap Registry
	static 
	{  
		  try 
		  {
			  JRegistry.register(IEC61850_GOOSE_Header.class);
			  //int headerID = JRegistry.register(IEC61850_GOOSE_Header.class);  
			  //System.out.printf("Header registered successfully, its numeric ID is %d\n", headerID);
			  
		  } 
		  catch (RegistryHeaderErrors e) 
		  {  
			  e.printStackTrace();  
		  }  
	}
}
