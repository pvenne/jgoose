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
 * This class defines an interface used by the API user to define an event handler function
 * that will be called after receiving a GSE control block of before transmitting a GSE control
 * block
 * 
 * @author  Philippe Venne
 * @version 0.1
 *
 */

package jgoose;

// To hold the event handler that will be user defined
public interface IEC61850_GOOSE_FrameEventHandler
{
	void eventHandler(IEC61850_GOOSE_Frame gooseFrame);
}