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
 * This class defines an API use to transmit and receive IEC61850 GOOSE messages
 * It was developed by Philippe Venne in the context of a PhD project
 * related to distributed control of wind farms.
 *   
 * @author  Philippe Venne
 * @version 0.1 
 * @see IEC61850_GOOSE_Data
 * @see IEC61850_GOOSE_Header
 */ 

package jgoose;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jgoose.IEC61850_GOOSE_ReceiveTask.WatchdogTask_State;

import org.jdom2.JDOMException;
import org.jnetpcap.packet.JMemoryPacket;

import com.gremwell.jnetbridge.*;

public class IEC61850_GOOSE_API 
{
	
	// The default MaxTime used for transmit thread when not specified in ICD file
	private int default_maxtime = 2000;
	
	// Represents the XML ICD file
	IEC61850_GOOSE_ICD_file ICD_Config_file;

	// Variables holding the IED related information
	String ipAddress;
	String ipSubnet;
	String ipGateway;
	String macAddress;
		
	// Classes used by JNetBridge
	PcapPort api_port;
	QueueingPortListener portListener;
	
	// Threads
	private Thread mainReceiveThread = null;
	
	// Object map to hold the event handlers for processing frames
	// The key is the GSEControl Block appID name 
	Map<String, IEC61850_GOOSE_ReceiveTask> receiveFrameTaskMap;
	
	// Object map to hold the transmit tasks for transmitting frames
	// The key is the GSEControl Block appID name 
	Map<String, IEC61850_GOOSE_TransmitTask> transmitFrameTaskMap;
	
	
	// We define the event handler used to send new values GOOSE messages
	class TransmitTask_SendValues_EventHandler implements IEC61850_GOOSE_TaskEventHandler 
	{
		@Override
		public void eventHandler(IEC61850_GOOSE_Frame gooseFrame, IEC61850_GOOSE_Task transmit_task) 
		{
			// First, we call the user defined event handler
			// The user will update the data the way he wants to
			gooseFrame.frameEventHandler.eventHandler(gooseFrame);
			
			// We now have to update the data sixe in case data has changed
			//gooseFrame.updateDataSize(gooseFrame.gooseData);
			
			// We mark the frame as good
			gooseFrame.frameValidity = IEC61850_GOOSE_FrameValidityType.good;
			
			// Now, we need to increment the state number
			// We increment the sequence number in smaller than 4294967295
			if ((gooseFrame.stNum < 4294967295L))
			{
				gooseFrame.stNum++;
			}
			else // if larger, we wrap
			{
				gooseFrame.stNum = 1;
			}
			
			// Now, we need to set the sequence number to 1
			gooseFrame.sqNum = 1;
			
			// Now we update the packet
			try 
			{
				transmit_task = gooseFrame.updatePacket_From_Frame(transmit_task);
			} 
			catch (IEC61850_GOOSE_Exception e) 
			{
				e.printStackTrace();
			}
			
			api_port.send(transmit_task.goose_memoryPacket);
		}
	}
	
	// We define the event handler used to retransmit GOOSE messages
	class TransmitTask_Retransmission_EventHandler implements IEC61850_GOOSE_TaskEventHandler 
	{
		@Override
		public void eventHandler(IEC61850_GOOSE_Frame gooseFrame, IEC61850_GOOSE_Task transmit_task) 
		{
			// Now, we need to increment the sequence number
			try 
			{
				transmit_task = gooseFrame.incrementSqNum(transmit_task);
			} 
			catch (IEC61850_GOOSE_Exception e) 
			{
				e.printStackTrace();
			}

			api_port.send(transmit_task.goose_memoryPacket);
		}
	}
	
	// We define the event handler called when a received GOOSE message has expired
	class ReceiveTask_TimerExpired_EventHandler implements IEC61850_GOOSE_TaskEventHandler 
	{
		@Override
		public void eventHandler(IEC61850_GOOSE_Frame gooseFrame, IEC61850_GOOSE_Task receive_task) 
		{
			// 1. We change frame validity to questionable
			gooseFrame.frameValidity = IEC61850_GOOSE_FrameValidityType.questionable;
			
			// 2. We call the user defined event handler
			gooseFrame.frameEventHandler.eventHandler(gooseFrame);	
		}
	}
	
	/**
	 * 
	 * This class is used to create the Threads that receives GSEControlBlock
	 * 
	 * @author  Philippe Venne
	 * @version 0.1
	 *
	 */
	
	public class GSEControlBlockReceiver implements Runnable 
	{

		@Override
		public void run ()
		{
			boolean running = true;
			IngressPacket incomingPacket = null;
			
			// We have to create an instance of the GOOSE header for binding to work
	        @SuppressWarnings("unused")
	        IEC61850_GOOSE_Header dummy_goose_header = new IEC61850_GOOSE_Header();
			
			// First, we check if a default handler is defined
			boolean containsDefault = false;
			
			if(receiveFrameTaskMap.containsKey("DEFAULT"))
				containsDefault=true;
			
			while (running) 
			{
	            // wait for an incoming packet
	            try 
	            {
	            	// Receive method is of blocking type
	            	incomingPacket = portListener.receive();
	            	
	            	// The Pcap port already decoded the ETHERNET header
	            	// We need to decode other headers
	            	//incomingPacket.packet.scan(JProtocol.ETHERNET_ID);
	            	//incomingPacket.packet.scan(JProtocol.)
					
					// We have to bind the goose_header to the JMemoryPacket
		            IEC61850_GOOSE_Header packet_goose_header = incomingPacket.packet.getHeader( new IEC61850_GOOSE_Header());
		            
		            if (packet_goose_header == null)
		            {
		            	// This is not a GOOSE packet, we do nothing
		            }
		            else if (packet_goose_header.isValidHeader() == false)
	            		System.err.printf("#%d: IEEE GOOSE Valid = %b headerError = %d \n", 
	            				incomingPacket.packet.getFrameNumber(), packet_goose_header.isValidHeader(), packet_goose_header.headerError);
	            	
					// The Goose Header is Valid
					else
	            	{
						if(receiveFrameTaskMap.containsKey(packet_goose_header.goID()))
	            		{
	            			// We check if the IEC61850_GOOSE_ReceiveTask is enabled
	            			IEC61850_GOOSE_ReceiveTask current_task = receiveFrameTaskMap.get(packet_goose_header.goID());
	            			
	            			// We save the packet for future use
							current_task.goose_memoryPacket = new JMemoryPacket(incomingPacket.packet);
	            			
	            			if(current_task.current_state != IEC61850_GOOSE_ReceiveTask.WatchdogTask_State.stopped)
	            			{
	            				// The Frame is recognised and the corresponding receive task is enabled
	            				
	            				// 1. we refresh the watch dog
	            				current_task.refresh();
	            				
	            				// 2. We check if the state number has changed
	            				if (packet_goose_header.stNum() != current_task.goose_frame.stNum)
	            				{
	            					// The state number has changed
	            					// 2.1. We decode the packet
	            					current_task.goose_frame.updateFrame_From_Packet(incomingPacket.packet);
	            					
	            					// 2.2. We update packet validity if necessary
	            					if(current_task.goose_frame.frameValidity != IEC61850_GOOSE_FrameValidityType.good)
	            					{
	            						current_task.goose_frame.frameValidity = IEC61850_GOOSE_FrameValidityType.good;
	            					}
				
	            					// 2.3. We call the user defined event handler
	            					current_task.goose_frame.frameEventHandler.eventHandler(current_task.goose_frame);	
	            				}
	            				// 3. If the state number did not change
	            				else
	            				{
		            				// 3.1. We update the packet validity if necessary
	            					if(current_task.goose_frame.frameValidity != IEC61850_GOOSE_FrameValidityType.good)
	            					{
	            						current_task.goose_frame.frameValidity = IEC61850_GOOSE_FrameValidityType.good;
	            						
	            						// 3.1.1 If we updated the packet validity, we call the user defined event handler
	            						current_task.goose_frame.frameEventHandler.eventHandler(current_task.goose_frame);
	            					}
	            				}
	            			}	
	            		}
						// TODO fix this
	            		else if (containsDefault)
	            		{
	            			// There is a default handler
	            			IEC61850_GOOSE_ReceiveTask current_task = receiveFrameTaskMap.get("DEFAULT");
	            			
	            			// 1. We decode the packet
        					try {
								current_task.goose_frame.updateFrame_From_UnknownPacket(incomingPacket.packet);
							} catch (IEC61850_GOOSE_Exception e) {
								e.printStackTrace();
							}
        					
        					// 2. We call the user defined DEFAULT event handler
        					current_task.goose_frame.frameEventHandler.eventHandler(current_task.goose_frame);
	            		}
	            		else
	        			{
	        				// The default frame is not defined. Nothing to do.
	        				System.err.printf("Unknown Frame with GoID %s received. Nothing to do with it. This is strange ?!?", packet_goose_header.goID());
	        			}
					}
				} 
	            catch (InterruptedException e) 
	            {
					// We received and interrupted exception, it time to stop
	            	running = false;
				} 
	            /*catch (IEC61850_GOOSE_Exception e) 
	            {
					e.printStackTrace();
				}*/
			}
		}
	}
	
	
	
	/**
	 * Constructor of the API. 1. Reads the ICD file 2. SAves Addressing information 3. Checks for a valid IED
	 * 
	 * @param icd_filename	Name of the file containing the definition of the GOOSE messages.
	 * 
	 */
	public IEC61850_GOOSE_API(String icd_filename, String ied_name, PcapPort param_port) throws IEC61850_GOOSE_Exception
	{		
		// We have to create an instance of the GOOSE header for binding to work
        @SuppressWarnings("unused")
        IEC61850_GOOSE_Header dummy_goose_header = new IEC61850_GOOSE_Header();
		
		/*
		 * We initialise the GOOSE port data
		 */
		api_port = param_port;
		
		// initialize port listener
        portListener = new QueueingPortListener();
		api_port.setListener(portListener);
		
		/*
		 * We verify the ICD file
		 */
		
		try {
			ICD_Config_file = new IEC61850_GOOSE_ICD_file(icd_filename, ied_name);
		} 
		catch (JDOMException e) {
			e.printStackTrace();
			throw new IEC61850_GOOSE_Exception("Incorrect XML syntax in ICD file");
		} 
		catch (IOException e) {
			e.printStackTrace();
			throw new IEC61850_GOOSE_Exception("Could not open ICD file");
		}
			
		// Retrieves addressing data
		ipAddress = ICD_Config_file.ipAddress;
		ipSubnet = ICD_Config_file.ipSubnet;
		ipGateway = ICD_Config_file.ipGateway;
		macAddress = ICD_Config_file.macAddress;
		
		// Initialise HashMap
		receiveFrameTaskMap
			= new HashMap<String, IEC61850_GOOSE_ReceiveTask> ();
		
		transmitFrameTaskMap
			= new HashMap<String, IEC61850_GOOSE_TransmitTask> ();
	}

	/**
	 * Registers a GOOSE Control block used for either transmit or receive. Once a block is registered,
	 * the event handler function will be called every time a corresponding GOOSE message is received or
	 * before transmitting a new GOOSE message of this type.
	 * 
	 * GOOSE messages are sent either when the GSE control block MaxTime is reached from
	 * <SCL>/<Comminication>/<SubNetwork>/<ConnectedAP>/<GSE>/<MaxTime> or when a triggerEvent 
	 * is manually triggered
	 * 
	 * @param appID_name	appID of the <GSEControl> block registered
	 * @param event_handler Event handler function
	 */
	public void registerGSEControlBlock(IEC61850_GOOSE_FrameEventHandlerType mode, String appID_name,
			IEC61850_GOOSE_FrameEventHandler event_handler) throws IEC61850_GOOSE_Exception
	{
		IEC61850_GOOSE_Frame local_GOOSE_Frame;
		
		IEC61850_GOOSE_GSEControlBlock new_GSEControlBlock = new IEC61850_GOOSE_GSEControlBlock();
		
		// We save the IED name
		new_GSEControlBlock.iedName = ICD_Config_file.iedName;
		
		// We check if we are defining the default receive event handler
		if(appID_name.contentEquals("DEFAULT") && (mode == IEC61850_GOOSE_FrameEventHandlerType.receive) )
		{
			// For the default GOOSE Frame, we only have the event handler
			IEC61850_GOOSE_ReceiveTask receive_task;
			
			local_GOOSE_Frame = new IEC61850_GOOSE_Frame(event_handler);
			
			receive_task = new IEC61850_GOOSE_ReceiveTask(local_GOOSE_Frame,appID_name);
			receiveFrameTaskMap.put(appID_name, receive_task);
		}
		else
		{
			// We decode the GSE Control block
			ICD_Config_file.decodeGSEControlBlock(appID_name);
			
			new_GSEControlBlock.ln0ClassName = ICD_Config_file.ln0ClassName;
			
			// We save the GSEControl node, the name, the confRev and the datSet
			new_GSEControlBlock.gseControlName = ICD_Config_file.gseControlBlockName;
			new_GSEControlBlock.gseControlAppIDName = ICD_Config_file.gseControlBlockAppIDName;
			new_GSEControlBlock.confRev = ICD_Config_file.gseControlBlockConfRev;
			new_GSEControlBlock.datSet = ICD_Config_file.gseControlBlockDatSet;
			
			// We decode the GSE block
			ICD_Config_file.decodeGSEBlock(new_GSEControlBlock.gseControlName);
			
			// Saves GSE properties
			new_GSEControlBlock.deviceName = ICD_Config_file.gseldInst;
			new_GSEControlBlock.AppID = ICD_Config_file.gseAPPID;
			new_GSEControlBlock.macAddress = ICD_Config_file.gseMACAddress;
			new_GSEControlBlock.mintime = ICD_Config_file.gseMinTime;
			new_GSEControlBlock.maxtime = ICD_Config_file.gseMaxTime;
			
			// We decode the DataSet block the signals associated with the DataSet block
			ICD_Config_file.decodeDataSetBlock(new_GSEControlBlock.datSet, ICD_Config_file.gseldInst);
			
			// Now we have to extract all signal attributes
			for ( int position = 0; position < ICD_Config_file.GOOSESignalsList.size(); position++)
			{
				IEC61850_GOOSE_Signal new_GOOSESignal = new IEC61850_GOOSE_Signal();
				
				new_GOOSESignal.position = position;
				
				new_GOOSESignal.bType = ICD_Config_file.GOOSESignalsList.get(position).bType;
				new_GOOSESignal.casdu = ICD_Config_file.GOOSESignalsList.get(position).casdu;
				new_GOOSESignal.desc = ICD_Config_file.GOOSESignalsList.get(position).desc;
				new_GOOSESignal.ioa = ICD_Config_file.GOOSESignalsList.get(position).ioa;
				new_GOOSESignal.ti = ICD_Config_file.GOOSESignalsList.get(position).ti;
				
				// The Map string is the IEC_60870_5_104 string "casdu.ioa.ti" example "1.1.9"
				String new_GOOSESignal_key = new_GOOSESignal.casdu + "." + new_GOOSESignal.ioa + "." + new_GOOSESignal.ti ;
				new_GSEControlBlock.GOOSESignalsMap.put(new_GOOSESignal_key, new_GOOSESignal);
			}

			// Now that all is fine, we save the GSEControlBlockAttributes object
			switch (mode)
			{
				case receive:
					
					// For a received GOOSE Frame, we have the event handler and the GSEControlBlock
					IEC61850_GOOSE_ReceiveTask receive_task;
					
					if(new_GSEControlBlock.mintime == 0)
					{
						System.out.println("MinTime not specified for receive frame with appID_name:" + appID_name + " using 0");
					}
					
					if(new_GSEControlBlock.maxtime == 0)
					{
						System.out.println("MaxTime not specified for receive frame with appID_name:" + appID_name + " using " + default_maxtime);
						new_GSEControlBlock.maxtime = default_maxtime;
					}
					
					// This is the user defined event handler that is called when a new frame is received
					local_GOOSE_Frame = new IEC61850_GOOSE_Frame(event_handler,new_GSEControlBlock);
					
					receive_task = new IEC61850_GOOSE_ReceiveTask(local_GOOSE_Frame,appID_name);
					
					// Create instances of event handlers
					IEC61850_GOOSE_TaskEventHandler receive_expired_handler = new ReceiveTask_TimerExpired_EventHandler();
					
					// This is the system event handler that is called when a packet has expired
					receive_task.registerEventHandler(receive_expired_handler);
					
					receiveFrameTaskMap.put(appID_name, receive_task);
					
					break;
				
				case transmit:

					IEC61850_GOOSE_TransmitTask transmit_task;
					
					if(new_GSEControlBlock.mintime == 0)
					{
						System.out.println("MinTime not specified for transmit frame with appID_name:" + appID_name + " using 0");
					}
					
					if(new_GSEControlBlock.maxtime == 0)
					{
						System.out.println("MaxTime not specified for transmit frame with appID_name:" + appID_name + " using " + default_maxtime);
						new_GSEControlBlock.maxtime = default_maxtime;
					}
					
					local_GOOSE_Frame = new IEC61850_GOOSE_Frame(event_handler,new_GSEControlBlock);
					local_GOOSE_Frame.sourceMacAddress = this.macAddress;
					
					// The thread is called every maxtime or when triggered
					transmit_task = new IEC61850_GOOSE_TransmitTask(local_GOOSE_Frame, new_GSEControlBlock.mintime, new_GSEControlBlock.maxtime);
					
					// Create instances of event handlers
					IEC61850_GOOSE_TaskEventHandler send_values_handler = new TransmitTask_SendValues_EventHandler();
					IEC61850_GOOSE_TaskEventHandler retransmission_handler = new TransmitTask_Retransmission_EventHandler();
					
					transmit_task.registerEventHandler_sendvalues(send_values_handler);
					transmit_task.registerEventHandler_retransmission(retransmission_handler);
					
					transmitFrameTaskMap.put(appID_name, transmit_task);
					break;
					
				default:
					throw new IEC61850_GOOSE_Exception("Unsupported EventHandlerType");
			}
		}
	}
	
	/**
	 * This method enables a GSEControlBlock.
	 * When applied to a Transmit block, it triggers the first transmission.
	 * When applied to a Receive block, it enables decoding that particular block. If a block that
	 * is not enabled is received, it will end up calling the default event handler if one is defined.
	 * 
	 * @param appID_name	appID of the <GSEControl>
	 * @throws IEC61850_GOOSE_Exception 
	 */
	
	public void enableGSEControlBlock (String appID_name) throws IEC61850_GOOSE_Exception
	{

		
		IEC61850_GOOSE_TransmitTask transmit_task;
		transmit_task = transmitFrameTaskMap.get(appID_name);
		
		// This is not a transmit block
		if (transmit_task == null)
		{
			// We identify the correct event handler
			IEC61850_GOOSE_ReceiveTask receive_task;
			receive_task = receiveFrameTaskMap.get(appID_name);
			
			if (receive_task == null)
				throw new IEC61850_GOOSE_Exception("appID not found");
			else if(appID_name.contentEquals("DEFAULT"))
			{
				// We do not enable the DEFAULT task
			}
			else
				// The maxTime is used as the watch dog value for the receive frame
				receive_task.enable(receive_task.goose_frame.gseControlBlockAttributes.maxtime);
			
		}
		else
			try 
			{
				transmit_task.enable();
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
	}
	
	/**
	 * This method disables to a GSEControlBlock transmission.
	 * It also disables receiving a particular GSEControlBlock. If a block that is not enabled is received, it
	 * will end up calling the default event handler if one is defined.
	 * 
	 * @param appID_name	appID of the <GSEControl>
	 * @throws IEC61850_GOOSE_Exception 
	 */
	
	public void disableGSEControlBlock (String appID_name) throws IEC61850_GOOSE_Exception
	{
		IEC61850_GOOSE_TransmitTask transmit_task;
		transmit_task = transmitFrameTaskMap.get(appID_name);
		
		// This is not a transmit block
		if (transmit_task == null)
		{
			// We identify the correct event handler
			IEC61850_GOOSE_ReceiveTask receive_task;
			receive_task = receiveFrameTaskMap.get(appID_name);
			
			if (receive_task == null)
				throw new IEC61850_GOOSE_Exception("appID not found");
			else
			{
				// we disable the task
				receive_task.disable();
				
				// we change frame validity to invalid
				receive_task.goose_frame.frameValidity = IEC61850_GOOSE_FrameValidityType.invalid;
			}
			
		}
		else
			transmit_task.disable();
	}
	
	
	/**
	 * This method is used to start the API. It starts the timers used so trigger sending of GOOSE
	 * messages.
	 * 
	 * @param if_mac_address	MAC address of the interface used to send and receive GOOSE messages.
	 * @throws IOException 
	 * 
	 */
	public void startIEC61850API() throws IOException
	{
		// We have to create an instance of the GOOSE header for binding to work
        @SuppressWarnings("unused")
        IEC61850_GOOSE_Header dummy_goose_header = new IEC61850_GOOSE_Header();
		
		// ----
		// We define receive filters
		// ----
	
        // We should only receive GOOSE messages
		final String goose_filter_str = "ether proto 0x88B8";
		
        /*************************************************************************** 
         * We define a filter on the protocol and on MAC addresses
         **************************************************************************/
		
		String filter_str = new String();
		
		// if a default receive handler is defined, we will not filter on MAC addresses
		// else, we only receive listed MAC addresses
		
		// TODO Test this
		if(! receiveFrameTaskMap.containsKey("DEFAULT"))
		{
			boolean undefined_mac_address = false;
			
			Iterator<IEC61850_GOOSE_ReceiveTask> receiveFrameTaskMap_IT = receiveFrameTaskMap.values().iterator();
			
			// Adds every MAC addresses
			for(int position = 0; position < receiveFrameTaskMap.size(); position++)
			{
				IEC61850_GOOSE_ReceiveTask current_IEC61850_GOOSE_ReceiveTask = receiveFrameTaskMap_IT.next();
				
				// If one of the IEC61850_GOOSE_ReceiveTask has an undefined MAS address, we cannot use MAC filters
				if(current_IEC61850_GOOSE_ReceiveTask.goose_frame.gseControlBlockAttributes.macAddress.contentEquals("00-00-00-00-00-00") )
					undefined_mac_address = true;
				
				if(position > 0)
					filter_str += " or ";
				
				filter_str += "( ";
				filter_str += goose_filter_str;
				filter_str += " and ether dst ";
				filter_str += (current_IEC61850_GOOSE_ReceiveTask.goose_frame.gseControlBlockAttributes.macAddress).replace('-',':');
				filter_str += " )";
			}
			
			// If one of the GSEControlBlock has an undefined MAS address, we only keep the GOOSE filter
			if (undefined_mac_address)
				filter_str = goose_filter_str;
		}
        
		// We set the filter for the current port
		try 
		{
			api_port.setFilter(filter_str);
		} 
		catch (PcapException e) 
		{
			System.err.println(e.getMessage());
			return;
		}
        
		
		// We start the main receive thread
		// TODO Beginning of code to enable receive thread
        if(mainReceiveThread == null)
        {
        	mainReceiveThread = new Thread(new GSEControlBlockReceiver());
        	mainReceiveThread.start();
        }
        // TODO End of code to enable receive thread
        
        // the start the transmit and receive port
        this.api_port.start();
	}
	
	/**
	 * This method is used to stop the API. It stops the timers used so trigger sending of GOOSE
	 * messages.
	 * @throws InterruptedException 
	 * @throws IEC61850_GOOSE_Exception 
	 * 
	 */
	public void stopIEC61850API() throws InterruptedException, IEC61850_GOOSE_Exception
	{
	
		// First we disable all transmit threads
		Iterator<IEC61850_GOOSE_TransmitTask> frameTransmitTask_IT;
		frameTransmitTask_IT = transmitFrameTaskMap.values().iterator();
		
		for(int position = 0; position < transmitFrameTaskMap.size(); position++)
		{
			IEC61850_GOOSE_TransmitTask current_TransmitTask = frameTransmitTask_IT.next();
			current_TransmitTask.disable();
		}
		
		// Second we stop the port thread
		this.api_port.stop();
		
		// Third we stop the main receive thread
		// TODO Beginning of code to enable receive thread
		mainReceiveThread.interrupt();
		mainReceiveThread.join();
		// TODO End of code to enable receive thread
		
		// Last we disable all receive threads
		Iterator<IEC61850_GOOSE_ReceiveTask> frameReceiveTask_IT;
		frameReceiveTask_IT = receiveFrameTaskMap.values().iterator();
		
		for(int position = 0; position < receiveFrameTaskMap.size(); position++)
		{
			IEC61850_GOOSE_ReceiveTask current_ReceiveTask = frameReceiveTask_IT.next();
			
			if ((current_ReceiveTask.current_state == WatchdogTask_State.running) 
					|| (current_ReceiveTask.current_state == WatchdogTask_State.expired) )
			{
				current_ReceiveTask.disable();
			}
		}
	}
	
	/**
	 * This method is used to manually trigger an update event on a GOOSE message. When
	 * a trigger is received on a GOOSE message, the event handler associated with it is called.
	 * Use this method with a transmit frame to trigger sending a new packet right away.
	 * Use this method with a receive frame to call the event handler of the frame right away. This method
	 * does NOT reset the receive watchdog that monitors packet validity.
	 * 
	 * @param appID_name	appID of the GOOSE message on which we want to trigger an event
	 * @throws IEC61850_GOOSE_Exception 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 * 
	 */
	public void triggerEvent(String appID_name) throws IEC61850_GOOSE_Exception
	{
		IEC61850_GOOSE_TransmitTask transmit_task;
		transmit_task = transmitFrameTaskMap.get(appID_name);
		
		// This is not a transmit block
		if (transmit_task == null)
		{
			// We identify the correct event handler
			IEC61850_GOOSE_ReceiveTask receive_task;
			receive_task = receiveFrameTaskMap.get(appID_name);
			
			if (receive_task == null)
				throw new IEC61850_GOOSE_Exception("appID not found");
			else
				// As no new packet was received, we just call the event handler on the latest received packet
				// for that specific receive task
				receive_task.goose_frame.frameEventHandler.eventHandler(receive_task.goose_frame);
		}
		else
			// We set the flag to signify that data is changed
			transmit_task.dataHasBeenChanged();
	}
	
	
	

	/**
	 * This method sets the timing attributed for all transmit frames
	 * 
	 * @param leapSecondsKnown The value TRUE indicates that the value of the time stamp contains all leap seconds occurred
	 * @param clockFailure The value TRUE indicates that the time source of the sending device is unreliable
	 * @param clockNotSynchronized The value TRUE indicates that the time source of the sending device is not synchronized with the external UTC time
	 * @param timeAccuracy The attribute TimeAccuracy shall represent the time accuracy class of the time respective time stamp relative to the external UTC time as defined in IEC 61850-7-2 section 6.1.2.9.2
	 * 
	 */
	
	public void setTimingAttributes(boolean leapSecondsKnown, boolean clockFailure, boolean clockNotSynchronized, int timeAccuracy)
	{
		// Updates the timing attributes in all transmit frames
		Iterator<IEC61850_GOOSE_TransmitTask> frameTransmitTask_IT;
		
		frameTransmitTask_IT = transmitFrameTaskMap.values().iterator();
		
		for(int position = 0; position < transmitFrameTaskMap.size(); position++)
		{
			IEC61850_GOOSE_TransmitTask current_TransmitTask = frameTransmitTask_IT.next();
			
			current_TransmitTask.goose_frame.setTimingAttributes(leapSecondsKnown, clockFailure, clockNotSynchronized, timeAccuracy);
		}
	}
}
