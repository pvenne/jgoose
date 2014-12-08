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

package jgoose;

import org.jnetpcap.packet.JMemoryPacket;

public abstract class IEC61850_GOOSE_Task 
{

	JMemoryPacket goose_memoryPacket;
	IEC61850_GOOSE_Header goose_header;
	IEC61850_GOOSE_Frame goose_frame;
}
