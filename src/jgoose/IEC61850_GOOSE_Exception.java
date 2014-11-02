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
 * This class is used to define an IEC61850_GOOSE exception.
 * 
 * @author  Philippe Venne
 * @version 0.1
 *
 */

package jgoose;

public class IEC61850_GOOSE_Exception extends Exception{

	private static final long serialVersionUID = -8624258129829974119L;
	
	public IEC61850_GOOSE_Exception(String message) 
	{
        super(message);
    }
}
