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
 * This class defines an enumeration used by the API user to define an event handler mode.
 * The mode can either be transmit or receive.
 * 
 * @author  Philippe Venne
 * @version 0.1
 *
 */

package jgoose;

public enum IEC61850_GOOSE_FrameEventHandlerType
{
	transmit	(1),
	receive		(2);
	
	// Holds the tag for each value of the enumeration 
	public final int tag;
	
	IEC61850_GOOSE_FrameEventHandlerType (int tag)
	{
		this.tag = tag;
	}
}