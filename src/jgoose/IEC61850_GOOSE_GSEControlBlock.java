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
 * This class is used to store all information relating to a particular GSE control block
 * It also holds a Map to each signal contained in the control block.
 * 
 * @author  Philippe Venne
 * @version 0.1
 *
 */

package jgoose;

import java.util.HashMap;
import java.util.Map;


public class IEC61850_GOOSE_GSEControlBlock
{
	// To hold the event handler for the GSEControlBlock
	//FrameEventHandler eventHandler;
	
	String iedName;
	String deviceName;
	String ln0ClassName;
	
	// Variables holding the GSEControl block related information
	String gseControlName;
	String confRev;
	String datSet;
	
	// Variables holding the GSE block related information
	//String ldInst;
	int AppID;
	String macAddress;
	int mintime;
	int maxtime;
	
	// This Map hold the signal of current control block
	// The Map string is the IEC_60870_5_104 string "casdu.ioa.ti" example "1.1.9"
	Map<String, IEC61850_GOOSE_Signal> GOOSESignalsMap;
	
	/**
	 * Constructor of the GSEControlBlockAttributes. It is required to initialise the list of GOOSE signals
	 * 
	 */
	public IEC61850_GOOSE_GSEControlBlock()
	{
		// Initialises the GOOSESignalsMap
		GOOSESignalsMap = new HashMap<String, IEC61850_GOOSE_Signal>();
	}
}