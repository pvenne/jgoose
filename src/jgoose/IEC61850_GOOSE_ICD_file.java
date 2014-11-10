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
 * This class is used to decode an ICD file.
 * 
 * @author  Philippe Venne
 * @version 0.3
 *
 */

package jgoose;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filter;
import org.jdom2.input.SAXBuilder;

public class IEC61850_GOOSE_ICD_file {
	
	// Variables used to store classes needed to read the XML file
	private final File xml_file;
	private final SAXBuilder xml_builder;
	private final Document xml_document;
	private final Namespace root_namespace;
	
	public String iedName;
	public String apName;
	
	private Element ConnectedAP_in_Comm_section;
	private Element IED_section;
	private Element GSEControl_node;
	private Element GSE_node;
	private Element DataSet_node;
	
	private List<Element> FCDA_nodes_LIST;
	
	// Data from Address section
	public String ipAddress;
	public String ipSubnet;
	public String ipGateway;
	public String macAddress;
	
	// Data from GSEControl section
	public String gseControlBlockName;
	public String gseControlBlockConfRev;
	public String gseControlBlockDatSet;
	public String deviceName;
	public String ln0ClassName;

	// Data from GSE section
	public String gseldInst;
	public int gseAPPID;
	public String gseMACAddress;
	public int gseMaxTime;
	public int gseMinTime;
	
	public class GOOSESignalAttributes
	{
		String lnClass;
		String lnType_id;
		String do_type;
		int lnInst;
		String doName;
		String daName;
		String fc;
		String cdc;
		
		// This is the signal description
		String desc;
		
		// This is the signal data type
		String bType;
		
		// IEC 60870-5-104 information
		int casdu;
		int ioa;
		int ti;
	}
	
	List <GOOSESignalAttributes> GOOSESignalsList;
	
	public IEC61850_GOOSE_ICD_file(String icd_filename, String ied_name) 
		throws JDOMException, IOException, IEC61850_GOOSE_Exception
	{
		boolean found_ConnectedAP = false;
		short found_Addressing_data = 0;
		boolean found_IED = false;
		
		xml_builder = new SAXBuilder();
		xml_file = new File(icd_filename);
		xml_document = (Document) xml_builder.build(xml_file);
		
		// Checks that the XML file is indeed an SCL file
		if ( xml_document.getRootElement().getName() != "SCL")
			throw new IEC61850_GOOSE_Exception("XML input file is not of SCL type");
		
		root_namespace = xml_document.getRootElement().getNamespace();

		// Retrieve Node Communication
		Element comm_element = xml_document.getRootElement().getChild("Communication", root_namespace);
		
		// Retrieves all elements named "ConnectedAP" within Communication node
		Filter<Element> elementFilter = new ElementFilter("ConnectedAP");
		
		// Search for a ConnectedAP with a matching iedName
		for (Iterator<Element> ConnectedAP_IT = comm_element.getDescendants(elementFilter);
				ConnectedAP_IT.hasNext();) 
		{
		       Element current_element = ConnectedAP_IT.next();
		       
		       if(current_element.getAttributeValue("iedName").equals(ied_name))
		       {
		    	    // We found the ConnectedAP node with the correct iedName
					found_ConnectedAP = true;
					
					iedName = current_element.getAttributeValue("iedName");
					
					ConnectedAP_in_Comm_section = current_element;
					apName = ConnectedAP_in_Comm_section.getAttributeValue("apName");
					
					// walks to the "Address" children 
					Element ConnectedAP_in_Comm_section_Address = ConnectedAP_in_Comm_section.getChild("Address", root_namespace);
					List<Element> p_nodes_LIST = ConnectedAP_in_Comm_section_Address.getChildren("P", root_namespace);
					
					// Walks all P nodes to retrieve addressing data
					for ( int position = 0; position < p_nodes_LIST.size(); position++)
					{
						if(p_nodes_LIST.get(position).getAttributeValue("type").contentEquals("IP"))
						{
							ipAddress = p_nodes_LIST.get(position).getValue();
							found_Addressing_data |= 1;
						}
						else if (p_nodes_LIST.get(position).getAttributeValue("type").contentEquals("IP-SUBNET"))
						{
							ipSubnet = p_nodes_LIST.get(position).getValue();
							found_Addressing_data |= 2;
						}
						else if (p_nodes_LIST.get(position).getAttributeValue("type").contentEquals("IP-GATEWAY"))
						{
							ipGateway = p_nodes_LIST.get(position).getValue();
							found_Addressing_data |= 4;
						}
						else if (p_nodes_LIST.get(position).getAttributeValue("type").contentEquals("MAC-Address"))
						{
							macAddress = p_nodes_LIST.get(position).getValue();
							found_Addressing_data |= 8;
						}	
					}
					
					// If all 4 addressing data fields were not found, we rise an exception.
					if (found_Addressing_data != 15)
						throw new IEC61850_GOOSE_Exception("Missing addressing data in SCL file: " + icd_filename + " iedName: " + iedName + " apName: " + apName);
		       }
		}

		if (found_ConnectedAP == false)
			throw new IEC61850_GOOSE_Exception("ConnectedAP section with corresponding IED name: "+ iedName +" not found in SCL file: " + icd_filename);
		
		// Now we have to find the IED and the Access Point in the <IED> section
		// Retrieve IED Children
		List<Element> ied_nodes_LIST = xml_document.getRootElement().getChildren("IED", root_namespace);
		for ( int position = 0; position < ied_nodes_LIST.size(); position++)
		{
			if(ied_nodes_LIST.get(position).getAttributeValue("name").equals(ied_name) && 
				ied_nodes_LIST.get(position).getChild("AccessPoint", root_namespace).
				getAttributeValue("name").equals(apName))
			{
				IED_section = ied_nodes_LIST.get(position);
				found_IED = true;
			}
		}
		
		if (found_IED == false)
			throw new IEC61850_GOOSE_Exception("IED section with corresponding AccessPoint name: " + apName + " not found in SCL file: " + icd_filename);
	}
	
	void decodeGSEControlBlock(String appID_name) throws IEC61850_GOOSE_Exception
	{	
		boolean found_GSEControlBlock = false;
		
		// Retrieves all elements named "GSEControl" within IED node
		Filter<Element> elementFilter = new ElementFilter("GSEControl");
		
		// Search for a GSEControl block with a matching appID
		for (Iterator<Element> GSEControl_IT = IED_section.getDescendants(elementFilter);
				GSEControl_IT.hasNext();)
		{
			Element current_element = GSEControl_IT.next();
			
			if(current_element.getAttributeValue("type").equals("GOOSE") && 
					current_element.getAttributeValue("appID").equals(appID_name))
			{
				// We found the right control block
				GSEControl_node =  current_element;
				found_GSEControlBlock = true;
			}
		}
		
		if (found_GSEControlBlock == false)
			throw new IEC61850_GOOSE_Exception("<GSEControl> Block with corresponding appID_name: " + appID_name + " name not found in <IED>");
		
		gseControlBlockName = GSEControl_node.getAttributeValue("name");
		gseControlBlockConfRev = GSEControl_node.getAttributeValue("confRev");
		gseControlBlockDatSet = GSEControl_node.getAttributeValue("datSet");
		
		ln0ClassName = GSEControl_node.getParentElement().getAttributeValue("lnClass");
	
		return;
	}
	
	void decodeGSEBlock(String GSEControlBlock_name) throws IEC61850_GOOSE_Exception
	{
		boolean found_GSEBlock = false;
		boolean found_APPID = false;
		boolean found_MACAddress = false;
		
		// Retrieves all elements named "GSE" within ConnectedAP in Communication section
		Filter<Element> elementFilter = new ElementFilter("GSE");
		
		// Search for a GSE with a matching cbName
		for (Iterator<Element> GSE_IT = ConnectedAP_in_Comm_section.getDescendants(elementFilter);
			GSE_IT.hasNext();)
		{
			Element current_element = GSE_IT.next();
			
			if(current_element.getAttributeValue("cbName").equals(GSEControlBlock_name))
			{
				GSE_node = current_element;
				found_GSEBlock = true;
			}
		}
		
		if (found_GSEBlock == false)
			throw new IEC61850_GOOSE_Exception("<GSE> Block with cbName: " + GSEControlBlock_name + " not found in <ConnectedAP> block");
		
		gseldInst = GSE_node.getAttributeValue("ldInst");
		
		// walks to the "Address" children 
		Element GSE_Address = GSE_node.getChild("Address", root_namespace);
		List<Element> p_nodes_LIST = GSE_Address.getChildren("P", root_namespace);
		
		// Walks all P nodes to retrieve addressing data
		for ( int position = 0; position < p_nodes_LIST.size(); position++)
		{
			if(p_nodes_LIST.get(position).getAttributeValue("type").contentEquals("APPID"))
			{
				gseAPPID = Integer.parseInt(p_nodes_LIST.get(position).getValue());
				found_APPID = true;
			}
			
			if (p_nodes_LIST.get(position).getAttributeValue("type").contentEquals("MAC-Address"))
			{
				gseMACAddress = p_nodes_LIST.get(position).getValue();
				found_MACAddress = true;
			}
		}
		
		if (found_APPID == false)
			throw new IEC61850_GOOSE_Exception("<APPID> type not found in <GSE> Block with cbName:" + GSEControlBlock_name);
		
		if(found_MACAddress == false)
			throw new IEC61850_GOOSE_Exception("<MAC-Address> type not found in <GSE> Block with cbName:" + GSEControlBlock_name);
			
		// Retrieves the maxtime
		Element GSE_Maxtime = GSE_node.getChild("MaxTime", root_namespace);
		
		if (GSE_Maxtime == null)
			//throw new IEC61850_GOOSE_Exception("<MaxTime> not found in <GSE> Block with cbName:" + GSEControlBlock_name);
			// If the value is not set in the ICD file, we set it to 0.
			gseMaxTime = 0;
		
		else
			gseMaxTime =  Integer.parseInt(GSE_Maxtime.getValue());
		
		// Retrieves the mintime
		Element GSE_Mintime = GSE_node.getChild("MinTime", root_namespace);
		
		if (GSE_Mintime == null)
			//throw new IEC61850_GOOSE_Exception("<MinTime> not found in <GSE> Block with cbName:" + GSEControlBlock_name);
			// If the value is not set in the ICD file, we set it to 0.
			gseMinTime = 0;
		
		else
			gseMinTime = Integer.parseInt(GSE_Mintime.getValue());
		
		return;
	}
	
	void decodeDataSetBlock(String dataSetName, String ldInstance) throws IEC61850_GOOSE_Exception
	{
		/*
		 * In this part, we decode the the DataSet block in order to find each signal in it
		 */
		
		boolean found_DataSet = false;
		GOOSESignalsList = new ArrayList <GOOSESignalAttributes> ();
		
		// Retrieves all elements named "DataSet" within IED
		Filter<Element> elementFilter = new ElementFilter("DataSet");
		
		// Search for a DataSet with a matching name
		for (Iterator<Element> DataSet_IT = IED_section.getDescendants(elementFilter);
				DataSet_IT.hasNext();)
		{
			Element current_element = DataSet_IT.next();
			
			if(current_element.getAttributeValue("name").equals(dataSetName))
			{
				DataSet_node = current_element;
				found_DataSet = true;
			}
		}
		
		if (found_DataSet == false)
			throw new IEC61850_GOOSE_Exception("<DataSet> named" + dataSetName + "not found");
		
		/*
		 * Now that we found the DataSet, we are looking for the signals in it
		 */
	
		FCDA_nodes_LIST = DataSet_node.getChildren("FCDA", root_namespace);
		
		// Walks all FCDA nodes to retrieve the signals
		for ( int position = 0; position < FCDA_nodes_LIST.size(); position++)
		{
			
			// Search for a signals, FCDA nodes, with matching ldInst
			if(FCDA_nodes_LIST.get(position).getAttributeValue("ldInst").contentEquals(ldInstance))
			{
				// We found a signal with the right ldInst
				GOOSESignalAttributes new_GOOSESignal = new GOOSESignalAttributes();
				
				// We save all signal information
				new_GOOSESignal.lnClass = FCDA_nodes_LIST.get(position).getAttributeValue("lnClass");
				new_GOOSESignal.lnInst = Integer.parseInt(FCDA_nodes_LIST.get(position).getAttributeValue("lnInst"));
				new_GOOSESignal.doName = FCDA_nodes_LIST.get(position).getAttributeValue("doName");
				new_GOOSESignal.daName = FCDA_nodes_LIST.get(position).getAttributeValue("daName");
				new_GOOSESignal.fc = FCDA_nodes_LIST.get(position).getAttributeValue("fc");
				
				// We insert the signal in the signal list
				GOOSESignalsList.add(new_GOOSESignal);
			}
		}
		
		if(GOOSESignalsList.size() == 0)
		{
			throw new IEC61850_GOOSE_Exception("Could not find any signal with ldInst name: " + ldInstance + " in DataSet: " + dataSetName);
		}
		
		/*
		 * Now we have to extract all signal attributes
		 */

		for ( int position = 0; position < GOOSESignalsList.size(); position++)
		{

			GOOSESignalAttributes current_GOOSESignal = GOOSESignalsList.get(position);
						
			boolean found_NodeType = false;
			
			/*
			 * Now we have to extract the lnType using the lnClass and the inst number
			 */
			
			boolean found_LDevice = false;
			
			// Retrieves all elements named "LDevice" within the SCL file
			elementFilter = new ElementFilter("LDevice");
			
			// Search for a LDevice block with name matching ldInstance in the IED section
			for(Iterator<Element> LDevice_IT = IED_section.getDescendants(elementFilter); LDevice_IT.hasNext();)	
			{
				Element current_element = LDevice_IT.next();
				
				// We found one matching element
				if(current_element.getAttributeValue("inst").equals(ldInstance))
				{
					// Something is wrong, its the second time we find a LDevice block with a matching name
					if (found_LDevice == true)
						throw new IEC61850_GOOSE_Exception("There is more than one <LDevice> block named:" + ldInstance + " in the SCL file");
					else
						found_LDevice = true;
					
					boolean found_IECData = false;
					
					// Retrieves all elements named "LN" within IED node
					elementFilter = new ElementFilter("LN");
					
					// Search for a LN block with a matching lnClass and inst prefix
					for (Iterator<Element> LN_IT = current_element.getDescendants(elementFilter); LN_IT.hasNext();)
					{
						Element innerloop_element = LN_IT.next();
						
						if(innerloop_element.getAttributeValue("lnClass").equals(current_GOOSESignal.lnClass) &&
							innerloop_element.getAttributeValue("inst").equals(String.valueOf(current_GOOSESignal.lnInst)))
						{
							/*
							 *  We found the right LN node. We save the lnType
							 */
							current_GOOSESignal.lnType_id = innerloop_element.getAttributeValue("lnType");
							
							Element DOI_element = innerloop_element.getChild("DOI", root_namespace);
							
							if(DOI_element.getAttributeValue("name").equals(current_GOOSESignal.doName))
							{
								Element DAI_element = DOI_element.getChild("DAI", root_namespace);
								
								if(DAI_element.getAttributeValue("name").equals(current_GOOSESignal.daName))
								{
									Element Private_element = DAI_element.getChild("Private", root_namespace);
									
									/* 
									 * Save IEC 60870-5-104 data for every signal
									 */
									
									if(Private_element.getAttributeValue("type").contentEquals("IEC_60870_5_104"))
									{
										Namespace iec_60870_5_104_namespace = Namespace.getNamespace("IEC_60870_5_104", "http://www.iec.ch/61850-80-1/2007/SCL");
										Element IEC_element = Private_element.getChild("GlobalAddress104", iec_60870_5_104_namespace);
										
										current_GOOSESignal.casdu =  Integer.parseInt(IEC_element.getAttributeValue("casdu"));
										current_GOOSESignal.ioa = Integer.parseInt(IEC_element.getAttributeValue("ioa"));
										current_GOOSESignal.ti = Integer.parseInt(IEC_element.getAttributeValue("ti"));
										
										found_IECData = true;
									}
								}
							}
							
						}
						
					}
					
					if (found_IECData == false)
						throw new IEC61850_GOOSE_Exception("<LN> block with corresponding lnClass, inst, lnType, DOI name"+
								"and  DAI name not found in <IED> for signal " + String.valueOf(position+1)); 
				}
			}
			
			if (found_LDevice == false)
				throw new IEC61850_GOOSE_Exception("There is no <LDevice> block named:" + ldInstance + " in the SCL file");

			// Retrieve Node DataTypeTemplates
			Element dataTypeTemplate_element = xml_document.getRootElement().getChild("DataTypeTemplates", root_namespace);
			
			// Retrieves all elements named "LNodeType" within DataTypeTemplates node
			elementFilter = new ElementFilter("LNodeType");
			
			// Search for a LNodeType block with a matching lnClass and id
			for (Iterator<Element> LNodeType_IT = dataTypeTemplate_element.getDescendants(elementFilter); LNodeType_IT.hasNext();)
			{
				Element current_element = LNodeType_IT.next();
				
				if(current_element.getAttributeValue("lnClass").equals(current_GOOSESignal.lnClass) &&
						current_element.getAttributeValue("id").equals(String.valueOf(current_GOOSESignal.lnType_id)))
				{
					// Walks all DO nodes
					List<Element> do_nodes_LIST = current_element.getChildren("DO", root_namespace);
					
					// Walks all DO nodes to retrieve addressing data
					for ( int do_position = 0; do_position < do_nodes_LIST.size(); do_position++)
					{
						// Looks for a DO node with the correct name
						if(do_nodes_LIST.get(do_position).getAttributeValue("name").equals(current_GOOSESignal.doName))
						{
							current_GOOSESignal.do_type = do_nodes_LIST.get(do_position).getAttributeValue("type");
							found_NodeType = true;
						}
					}
				}
			}
			
			if (found_NodeType == false)
				throw new IEC61850_GOOSE_Exception("<DO> node within <LNodeType> block with corresponding name, "+
						"lnClass and id not found in <DataTypeTemplates> for signal " + String.valueOf(position+1));
			
			boolean found_NodebType = false;
			
			// Retrieves all elements named "DOType" within DataTypeTemplates node
			elementFilter = new ElementFilter("DOType");
			
			// Search for a DOType block with a matching id
			for (Iterator<Element> DOType_IT = dataTypeTemplate_element.getDescendants(elementFilter); DOType_IT.hasNext();)
			{
				Element current_element = DOType_IT.next();
				
				if(current_element.getAttributeValue("id").equals(current_GOOSESignal.do_type))
				{
					current_GOOSESignal.cdc = current_element.getAttributeValue("cdc");
					current_GOOSESignal.desc = current_element.getAttributeValue("desc");
					
					// Walks all DA nodes
					List<Element> da_nodes_LIST = current_element.getChildren("DA", root_namespace);
					
					// Walks all DO nodes to retrieve addressing data
					for ( int da_position = 0; da_position < da_nodes_LIST.size(); da_position++)
					{
						// Looks for a DA node with the correct fc and name
						if(da_nodes_LIST.get(da_position).getAttributeValue("fc").equals(current_GOOSESignal.fc) &&
							da_nodes_LIST.get(da_position).getAttributeValue("name").equals(current_GOOSESignal.daName))
						{
							current_GOOSESignal.bType = da_nodes_LIST.get(da_position).getAttributeValue("bType");
							found_NodebType = true;
						}
					}
					
				}
			}
			
			if (found_NodebType == false)
				throw new IEC61850_GOOSE_Exception("<DA> node within <DOType> block with corresponding fc, name,"+
						"  and id not found in <DataTypeTemplates> for signal " + String.valueOf(position+1));
		}
		
		return;
	}
	
	
}
