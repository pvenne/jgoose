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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import jgoose.*;

import org.jnetpcap.PcapDLT;
import org.jnetpcap.PcapIf;
import org.jnetpcap.packet.format.FormatUtils;
import org.kohsuke.args4j.*;

import com.gremwell.jnetbridge.*;

public class jgoose_demo {
	
	/**
	 * @param args
	 * @throws InterruptedException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws SecurityException 
	 * @throws IllegalArgumentException 
	 */
	public static void main(String[] args) throws PcapException, IOException, InterruptedException, IllegalAccessException, InstantiationException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException {
		
		final int receiveTimeoutMs = 1;
	
		/*
		 * We process the command line parameters
		 */
		final class cmdLineOptions {
			@Option(name="-printif",usage="prints interface list MAC")
		    boolean print_if = false;
			
			@Option(name = "-icd", metaVar = "<icd file name>",
			usage = "specifies the name of the ICD file example folder/file.icd")
			String icdfile;
			
			@Option(name = "-ied", metaVar = "<ied name>",
			usage = "specifies the name of the relevant IED in the ICD file")
			String ied_name;
			
			@Option(name = "-if", metaVar = "<net interface MAC>",
			usage = "specifies the MAC of the network interface 00:00:00:00:00:00")
			String network_MAC;
		}
		
		// We process the command line options
		final cmdLineOptions options = new cmdLineOptions();
		final CmdLineParser parser = new CmdLineParser(options);
		
		// if you have a wider console, you could increase the value;
        // here 80 is also the default
        parser.setUsageWidth(80);
		
        try {
            // parse the arguments.
            parser.parseArgument(args);

        } catch( CmdLineException e ) {
            // if there's a problem in the command line,
            // you'll get this exception. this will report
            // an error message.
            System.err.println(e.getMessage());
            System.err.println("java IEC61850_demo [options...]");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            return;
        }
        
        // If required, we print the interface list. It will be all that we will do.
        if(options.print_if)
        {
        	System.out.println("Available Ethernet interfaces:");
            for (PcapIf pcapIf : PcapUtils.getPcapDevices(false, PcapDLT.CONST_EN10MB)) {
            	if(pcapIf.getHardwareAddress()!= null)
            		System.out.println("\t" + FormatUtils.mac(pcapIf.getHardwareAddress()));
            }
        	
        	return;
        }
        
        /*
         * We define the data that is exchanged
         */
        
        // These are the variables to transmit and receive
        // We need to use a container class because it has to be "final"
        class dataContainer{
        	
        	/* ----
        	 * These variable are for transmission
        	 * -----
        	 */
        	Integer tx_GGIO_1_ISCSO_stVal; // casdu = 1; ioa = 1; ti = 9;
        	final String tx_GGIO_1_ISCSO_stVal_KEY = "1.1.9";
        	
        	Integer tx_GGIO_2_ISCSO_stVal; // casdu = 1; ioa = 2; ti = 9;
            final String tx_GGIO_2_ISCSO_stVal_KEY = "1.2.9";
            
            Integer tx_GGIO_3_ISCSO_stVal; // casdu = 1; ioa = 3; ti = 9;
            final String tx_GGIO_3_ISCSO_stVal_KEY = "1.3.9";
            
            Integer tx_GGIO_4_ISCSO_stVal; // casdu = 1; ioa = 4; ti = 9;
            final String tx_GGIO_4_ISCSO_stVal_KEY = "1.4.9";
            
            Float tx_GGIO_5_SPCSO_stVal; // casdu = 1; ioa = 5; ti = 9;
            final String tx_GGIO_5_SPCSO_stVal_KEY = "1.5.9";
        	
            boolean tx_testFlag;
            
            /* ----
        	 * These variable are for reception
        	 * -----
        	 */
            
            Integer rx_GGIO_1_ISCSO_stVal; // casdu = 2; ioa = 1; ti = 9;
        	final String rx_GGIO_1_ISCSO_stVal_KEY = "2.1.9";
        	
        	Integer rx_GGIO_2_ISCSO_stVal; // casdu = 2; ioa = 2; ti = 9;
            final String rx_GGIO_2_ISCSO_stVal_KEY = "2.2.9";
            
            Integer rx_GGIO_3_ISCSO_stVal; // casdu = 2; ioa = 3; ti = 9;
            final String rx_GGIO_3_ISCSO_stVal_KEY = "2.3.9";
            
            Integer rx_GGIO_4_ISCSO_stVal; // casdu = 2; ioa = 4; ti = 9;
            final String rx_GGIO_4_ISCSO_stVal_KEY = "2.4.9";
        	
            boolean rx_testFlag;
            
        }
        
        final dataContainer exchangedData = new dataContainer();
        
        // We initialise the variables to transmit
        exchangedData.tx_GGIO_1_ISCSO_stVal = 101;
        exchangedData.tx_GGIO_2_ISCSO_stVal = 102; 
        exchangedData.tx_GGIO_3_ISCSO_stVal = 103; 
        exchangedData.tx_GGIO_4_ISCSO_stVal = 104;
        exchangedData.tx_GGIO_5_SPCSO_stVal = 1001.5678f;
        exchangedData.tx_testFlag = false;

        
        /*
         * We define the event handlers
         */
        
		// We define the event handler used to receive GOOSE messages
		class TO_PC_EventHandler implements IEC61850_GOOSE_FrameEventHandler 
		{
			@Override
			public void eventHandler(IEC61850_GOOSE_Frame gooseFrame) 
			{
				System.out.printf("In TO_PC_EventHandler\n");

				System.out.printf("stNum=%d sqNum=%d test=%b confRev=%d ndsCom=%b numDatSetEntries=%d\n", 
						gooseFrame.stNum, gooseFrame.sqNum, 
						gooseFrame.test,gooseFrame.confRevGoose,
						gooseFrame.ndsCom, gooseFrame.gooseData.numEntries);
				
				try 
				{
					exchangedData.rx_GGIO_1_ISCSO_stVal = 
						(Integer)gooseFrame.getValueByKey(exchangedData.rx_GGIO_1_ISCSO_stVal_KEY);
					
					exchangedData.rx_GGIO_2_ISCSO_stVal = 
						(Integer)gooseFrame.getValueByKey(exchangedData.rx_GGIO_2_ISCSO_stVal_KEY);
					
					exchangedData.rx_GGIO_3_ISCSO_stVal = 
						(Integer)gooseFrame.getValueByKey(exchangedData.rx_GGIO_3_ISCSO_stVal_KEY);
					
					exchangedData.rx_GGIO_4_ISCSO_stVal = 
						(Integer)gooseFrame.getValueByKey(exchangedData.rx_GGIO_4_ISCSO_stVal_KEY);
					
					System.out.printf("rx_GGIO_1: %d /n", exchangedData.rx_GGIO_1_ISCSO_stVal);
					System.out.printf("rx_GGIO_2: %d /n", exchangedData.rx_GGIO_2_ISCSO_stVal);
					System.out.printf("rx_GGIO_3: %d /n", exchangedData.rx_GGIO_3_ISCSO_stVal);
					System.out.printf("rx_GGIO_4: %d /n", exchangedData.rx_GGIO_4_ISCSO_stVal);
					
				} 
				catch (IEC61850_GOOSE_Exception e) 
				{
					System.err.printf("Error in Receive Event Handler TO_PC_EventHandler\n");
					e.printStackTrace();
				}
				
				exchangedData.rx_testFlag = gooseFrame.test;
				
				System.out.printf("rx_testFlag: %b /n", exchangedData.rx_testFlag);
				
				if ( gooseFrame.frameValidity == IEC61850_GOOSE_FrameValidityType.good)
				{
					System.out.printf("The frame validity is GOOD/n");
				}
				else if  ( gooseFrame.frameValidity == IEC61850_GOOSE_FrameValidityType.questionable)
				{
					System.out.printf("The frame validity is QUESTIONABLE/n");
				}
				else
				{
					System.out.printf("The frame validity is INVALID/n");
				}
				
			}
		}
		
		// We define the DEFAULT event handler used to receive GOOSE messages
		
		// If a default event handler is not defined, a MAC address filter will be used to
		// receive only the messages of interest
		
		// If filters are used, the MAC adress must be defined for all received GSE control block. If one is
		// defined as 00-00-00-00-00-00, all filters will be disabled
		
		class DEFAULT_EventHandler implements IEC61850_GOOSE_FrameEventHandler 
		{
			@Override
			public void eventHandler(IEC61850_GOOSE_Frame gooseFrame) 
			{
				System.out.printf("In DEFAULT_EventHandler\n");
				
				System.out.printf("stNum=%d sqNum=%d test=%b confRev=%d ndsCom=%b numDatSetEntries=%d\n", 
						gooseFrame.stNum, gooseFrame.sqNum, 
						gooseFrame.test,gooseFrame.confRevGoose,
						gooseFrame.ndsCom, gooseFrame.gooseData.numEntries);
				
				//We print the values
				for(int position = 0; position < gooseFrame.gooseData.numEntries; position++)
				{
					Object value;
					value = gooseFrame.gooseData.getValue(position);
					
					if (value.getClass() == byte.class)
					{
						switch(gooseFrame.gooseData.getType(position))
						{
							case booln:
		            			System.out.printf("booln data[%d]=%d ", position, ((Byte) value).intValue());
		            			break;
								
							case integer: // MMS limits to INT32
		            			System.out.printf("integer data[%d]=%d ", position, ((Byte) value).intValue());
		            			break;
								
							case unsign: // MMS limits to UINT32
		            			System.out.printf("unsign data[%d]=%d ", position, ((Byte) value).intValue());
		            			break;
								
							default:
								System.out.printf("Data type not supported\n");
						}
					}
					else if( value.getClass() == short.class)
					{
						switch(gooseFrame.gooseData.getType(position))
						{
							case booln:
		            			System.out.printf("booln data[%d]=%d ", position, ((Short) value).intValue());
		            			break;
								
							case integer: // MMS limits to INT32
		            			System.out.printf("integer data[%d]=%d ", position, ((Short) value).intValue());
		            			break;
								
							case unsign: // MMS limits to UINT32
		            			System.out.printf("unsign data[%d]=%d ", position, ((Short) value).intValue());
		            			break;
								
							default:
								System.out.printf("Data type not supported\n");
						}
					}
					else if( value.getClass() == int.class)
					{
						switch(gooseFrame.gooseData.getType(position))
						{
							case booln:
		            			System.out.printf("booln data[%d]=%d ", position, ((Integer) value).intValue());
		            			break;
								
							case integer: // MMS limits to INT32
		            			System.out.printf("integer data[%d]=%d ", position, ((Integer) value).intValue());
		            			break;
								
							case unsign: // MMS limits to UINT32
		            			System.out.printf("unsign data[%d]=%d ", position, ((Integer) value).intValue());
		            			break;
								
							default:
								System.out.printf("Data type not supported\n");
						}
					}
					else if ( value.getClass() == long.class)
					{
						switch(gooseFrame.gooseData.getType(position))
						{
							case booln:
		            			System.out.printf("booln data[%d]=%d ", position, ((Long) value).intValue());
		            			break;
								
							case integer: // MMS limits to INT32
		            			System.out.printf("integer data[%d]=%d ", position, ((Long) value).intValue());
		            			break;
								
							case unsign: // MMS limits to UINT32
		            			System.out.printf("unsign data[%d]=%d ", position, ((Long) value).intValue());
		            			break;
								
							default:
								System.out.printf("Data type not supported\n");
						}
					}
					else
					{
						System.out.printf("Unknown type");
					}
				}
				System.out.printf("\n");
				
			}
		}
		
		// We define the event handler used to transmit GOOSE messages
		class FROM_PC_EventHandler implements IEC61850_GOOSE_FrameEventHandler 
		{
			@Override
			public void eventHandler(IEC61850_GOOSE_Frame gooseFrame) {

				System.out.printf("In FROM_PC_EventHandler\n");
				
				// We set the values according to the current value of the variables
				try 
				{
					gooseFrame.setValueByKey(exchangedData.tx_GGIO_1_ISCSO_stVal_KEY, 
							exchangedData.tx_GGIO_1_ISCSO_stVal);
				
					gooseFrame.setValueByKey(exchangedData.tx_GGIO_2_ISCSO_stVal_KEY, 
							exchangedData.tx_GGIO_2_ISCSO_stVal);
					
					gooseFrame.setValueByKey(exchangedData.tx_GGIO_3_ISCSO_stVal_KEY, 
							exchangedData.tx_GGIO_3_ISCSO_stVal);
					
					gooseFrame.setValueByKey(exchangedData.tx_GGIO_4_ISCSO_stVal_KEY, 
							exchangedData.tx_GGIO_4_ISCSO_stVal);
					
					gooseFrame.setValueByKey(exchangedData.tx_GGIO_5_SPCSO_stVal_KEY, 
							exchangedData.tx_GGIO_5_SPCSO_stVal);
				
				} 
				catch (IEC61850_GOOSE_Exception e) 
				{
					System.err.printf("Error in Transmit Event Handler FROM_PC_EventHandler\n");
					e.printStackTrace();
				}
				
				gooseFrame.test = exchangedData.tx_testFlag;
				
				System.out.printf("Done FROM_PC_EventHandler\n");
			}
		}
		
		/*
		 * We initialise the goose_port and the API
		 */
		
		// We configure the port used to send and receive packets
		final PcapPort goose_port = new PcapPort(PcapUtils.macToName(options.network_MAC),receiveTimeoutMs);
		
		// We create an object to hold the API. It is not initialised at this point
		IEC61850_GOOSE_API api_instance;
		
		// Create instances of event handlers
		IEC61850_GOOSE_FrameEventHandler from_handler = new FROM_PC_EventHandler();
		IEC61850_GOOSE_FrameEventHandler default_handler = new DEFAULT_EventHandler();
		IEC61850_GOOSE_FrameEventHandler to_handler = new TO_PC_EventHandler();
		
		try 
		{
			// Initialise the new instance of the API
			api_instance = new IEC61850_GOOSE_API(options.icdfile, options.ied_name, goose_port);
			
			// Now we have to register all event handlers
			
			// Register the Event handler used to process GOOSE messages of control block ID=GSE_APPID_FROM_PC
			api_instance.registerGSEControlBlock(IEC61850_GOOSE_FrameEventHandlerType.receive, "GSE_APPID_TO_PC", to_handler);
		
			// Register the default Event handler used to process GOOSE messages of undefined control block ID
			api_instance.registerGSEControlBlock(IEC61850_GOOSE_FrameEventHandlerType.receive, "DEFAULT", default_handler);
		
			// Register the GOOSE control block used to transmit messages
			api_instance.registerGSEControlBlock(IEC61850_GOOSE_FrameEventHandlerType.transmit, "GSE_APPID_FROM_PC", from_handler);
		}
		catch ( IEC61850_GOOSE_Exception e )
		{
            System.err.println("Could not initialize IEC61850_GOOSE_API");
            System.err.println(e.getMessage());
            e.printStackTrace();
            
            return;
		}
		
		// We start the IEC61850 API transmit and receive processes
		api_instance.startIEC61850API();
		
		try {
			// TODO enable receive frame
			
			// If a DEFAULT receive handler is defined, it is automatically enabled when the API is started
			
			// We have to enable the GSEControlBlock of the transmit frame for transmission to start
			api_instance.enableGSEControlBlock("GSE_APPID_FROM_PC");

		} catch (IEC61850_GOOSE_Exception e1) {
			System.err.println("Could not enable task");
			e1.printStackTrace();
		}
		// api_instance.disableGSEControlBlock("GSE_APPID_FROM_PC")
		
		Thread.sleep(100);
		
		// We trigger a first transmit event
		try {
			api_instance.triggerEvent("GSE_APPID_FROM_PC");
		} catch (IEC61850_GOOSE_Exception e) {
			System.err.println("Could not trigger Event");
			e.printStackTrace();
		}
		
		System.out.printf("Done trigerring #1 event\n");
		
		Thread.sleep(1000);
		
		// We update the data
		exchangedData.tx_GGIO_1_ISCSO_stVal = 201;
        exchangedData.tx_GGIO_2_ISCSO_stVal = 202; 
        exchangedData.tx_GGIO_3_ISCSO_stVal = 203; 
        exchangedData.tx_GGIO_4_ISCSO_stVal = 204;
        exchangedData.tx_GGIO_5_SPCSO_stVal = 2002.5678f; 
        exchangedData.tx_testFlag = false;
		
        // We trigger a second transmit event
		try {
			api_instance.triggerEvent("GSE_APPID_FROM_PC");
		} catch (IEC61850_GOOSE_Exception e) {
			System.err.println("Could not trigger Event");
			e.printStackTrace();
		}

		System.out.printf("Done trigerring #2 event\n");
		
		Thread.sleep(1000);
		
		// We update the data
		exchangedData.tx_GGIO_1_ISCSO_stVal = 301;
        exchangedData.tx_GGIO_2_ISCSO_stVal = 302; 
        exchangedData.tx_GGIO_3_ISCSO_stVal = 303; 
        exchangedData.tx_GGIO_4_ISCSO_stVal = 304;
        exchangedData.tx_GGIO_5_SPCSO_stVal = 3003.5678f; 
        exchangedData.tx_testFlag = false;
        
        // TODO test it
        // Sets the timing attributes of the clock source
		//api_instance.setTimingAttributes(true, false, true, 31);
		
        // We trigger a second transmit event
		try {
			api_instance.triggerEvent("GSE_APPID_FROM_PC");
		} catch (IEC61850_GOOSE_Exception e) {
			System.err.println("Could not trigger Event");
			e.printStackTrace();
		}
		
		System.out.printf("Done trigerring #3 event\n");
		
		Thread.sleep(1000);
		
		// We update the data
		exchangedData.tx_GGIO_1_ISCSO_stVal = 401;
        exchangedData.tx_GGIO_2_ISCSO_stVal = 402; 
        exchangedData.tx_GGIO_3_ISCSO_stVal = 403; 
        exchangedData.tx_GGIO_4_ISCSO_stVal = 404;
        exchangedData.tx_GGIO_5_SPCSO_stVal = 4004.5678f; 
        exchangedData.tx_testFlag = false;
		
        // We trigger a second transmit event
		try {
			api_instance.triggerEvent("GSE_APPID_FROM_PC");
		} catch (IEC61850_GOOSE_Exception e) {
			System.err.println("Could not trigger Event");
			e.printStackTrace();
		}
		
		System.out.printf("Done trigerring #4 event\n");
		
		Thread.sleep(4000);
		
		// We update the data
		exchangedData.tx_GGIO_1_ISCSO_stVal = 1001;
        exchangedData.tx_GGIO_2_ISCSO_stVal = 1002; 
        exchangedData.tx_GGIO_3_ISCSO_stVal = 1003; 
        exchangedData.tx_GGIO_4_ISCSO_stVal = 1004;
        exchangedData.tx_GGIO_5_SPCSO_stVal = 11001.5678f; 
        exchangedData.tx_testFlag = false;
        
        // TODO test disable event
        /*
        try {
			api_instance.disableGSEControlBlock("GSE_APPID_FROM_PC");

		} catch (IEC61850_GOOSE_Exception e1) {
			System.err.println("Could not enable task");
			e1.printStackTrace();
		}
		*/
		
        // We trigger a second transmit event
		try {
			api_instance.triggerEvent("GSE_APPID_FROM_PC");
		} catch (IEC61850_GOOSE_Exception e) {
			System.err.println("Could not trigger Event");
			e.printStackTrace();
		}
		
		System.out.printf("Done trigerring #5 event\n");
		
		Thread.sleep(5000);
		
		
		// We stop the API
		try {
			api_instance.stopIEC61850API();
		} catch (IEC61850_GOOSE_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// We close the port
		goose_port.close();
	}
}
