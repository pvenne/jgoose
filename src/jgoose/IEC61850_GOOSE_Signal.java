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
 * This class is used to store all information relating to a particular Signal within a GSE control block.
 * A signal is linked to a data element. The data element itself is embedded in the IEC61850_GOOSE_Data object.
 * 
 * @author  Philippe Venne
 * @version 0.1
 *
 */

package jgoose;

public class IEC61850_GOOSE_Signal
{
	// This is the signal description
	String desc;
	
	// This is the signal data type
	String bType;
	
	// IEC 60870-5-104 information
	int casdu;
	int ioa;
	int ti;
	
	// signal position in the dataset. From 0 and up.
	int position;
	
	// A signal is linked to a data element.
	// The data element itself is embedded in the IEC61850_GOOSE_Data object
	IEC61850_GOOSE_Data_Element data;
	
	public IEC61850_GOOSE_Signal()
	{
		data = null;
	}
	
}