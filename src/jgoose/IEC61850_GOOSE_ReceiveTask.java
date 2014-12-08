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
 * This class defined a task scheduler based on a watchdog. 
 * When the watchdog expires, a user defined function is called.
 * 
 * @author  Philippe Venne
 * @version 0.1
 *
 */

package jgoose;

import java.lang.System;

public class IEC61850_GOOSE_ReceiveTask extends IEC61850_GOOSE_Task{
	
	String name;
	IEC61850_WatchdogTask_Sleeper sleeper;
	long delay;
	
	IEC61850_GOOSE_TaskEventHandler watchdogEventHandler = null;
	
	// Defined in IEC61850_GOOSE_Task
	//JMemoryPacket goose_memoryPacket;
	//IEC61850_GOOSE_Frame goose_frame;
	
	IEC61850_GOOSE_ReceiveTask receiveTask = this;
	
	/**
	 * 
	 * This class is used to define a synchronized flag that is used for multi thread 
	 * communications within the IEC61850_GOOSE_WatchdogTask class
	 * 
	 * @author  Philippe Venne
	 * @version 0.1
	 *
	 */
	class IEC61850_LockedFlag {
		boolean flag;
	}
	
	IEC61850_LockedFlag refresh_flag = new IEC61850_LockedFlag ();
	IEC61850_LockedFlag cancel_flag = new IEC61850_LockedFlag ();
	
	
	/**
	 * 
	 * This class is used to define the state machine of the IEC61850_GOOSE_WatchdogTask class
	 * 
	 * @author  Philippe Venne
	 * @version 0.1
	 *
	 */
	// We define the WatchdogTask state machine
	public enum WatchdogTask_State
	{
		not_started,
		
		// the watchdog is running normally
		running,
		
		// the watchdog has expired but can still be restarted
		expired,
		
		stopped;
	}
	
	WatchdogTask_State current_state;
	
	/**
	 * Constructor of the IEC61850_GOOSE_WatchdogTask class
	 * 
	 * @param local_received_frame	The frame that is attached with this watchdog
	 * @param name					Name of this watchdog
	 * @throws IEC61850_GOOSE_Exception 
	 * 
	 */
	public IEC61850_GOOSE_ReceiveTask(IEC61850_GOOSE_Frame local_received_frame, String name) throws IEC61850_GOOSE_Exception
	{
		this.name = name;
		goose_frame = local_received_frame;
		
		refresh_flag.flag = false;
		cancel_flag.flag = false;
		
		current_state = WatchdogTask_State.not_started;
		
		//The goose_memoryPacket is not needed for received frames
		goose_memoryPacket = null;
		goose_header = null;
	}
	
	
	/**
	 * Enables the receive task. The watchdog starts in expired state as no packet were received.
	 * 
	 */
	public void enable(long local_delay)
	{
		if (current_state == WatchdogTask_State.not_started)
		{	
			delay = local_delay;
			
			// The watchdog starts in expired state
			synchronized (current_state)
			{
				current_state = WatchdogTask_State.expired;
			}
		}
		else
		{
			System.err.printf("The Receive Task is already started, ignoring\n");
		}
	}

	/**
	 * Resets the watchdog timer
	 * 
	 */
	public void refresh()
	{
		if (current_state == WatchdogTask_State.running)
		{
			// we notify its time to refresh the timer
			synchronized (refresh_flag)
			{
				refresh_flag.flag = true;
			}
			
			// we wakeup the sleeper thread
			
			synchronized (sleeper)
			{
				sleeper.notify();
			}
		}
		else if (current_state == WatchdogTask_State.expired)
		{
			synchronized (current_state)
			{
				current_state = WatchdogTask_State.running;
			}
			
			// start a new sleeper thread
			sleeper = new IEC61850_WatchdogTask_Sleeper();
			sleeper.start();
		}
		else
			System.err.printf("The watchdog not started or stopped, ignoring\n");

	}
	
	/**
	 * Cancels the watchdog right away. Does not call the event handler.
	 * 
	 */
	public void disable()
	{
		if (current_state != WatchdogTask_State.stopped)
		{
			// we notify its time to refresh the timer
			synchronized (cancel_flag)
			{
				cancel_flag.flag = true;
			}
			
			// we wakeup the sleeper thread
			synchronized (sleeper)
			{
				sleeper.notify();
			}
		}
		else
			System.err.printf("The watchdog is already stopped, ignoring\n");
	}
	
	/**
	 * Registers the event handler function that will be called when the watchdog expires
	 * 
	 * @param local_received_frame	The event handler function
	 * 
	 */
	public void registerEventHandler(IEC61850_GOOSE_TaskEventHandler local_eventHandler)
	{
		watchdogEventHandler = local_eventHandler;
	}
	
	/**
	 * 
	 * This class is the core of the watchdog. It is the thread that manages the timer
	 * and the state machine.
	 * 
	 * @author  Philippe Venne
	 * @version 0.1
	 *
	 */
	class IEC61850_WatchdogTask_Sleeper extends Thread{
		
		@Override
	    public void run() 
	    {
			while (! cancel_flag.flag)
			{
				// The thread waits until the delay is expired or it is notified
				synchronized(sleeper)
				{
					try {
						sleeper.wait(delay);
					} catch (InterruptedException e) {

						System.err.printf("Sleeping thread was interrupted\n");
						e.printStackTrace();
					}
				}
				
				// The delay was expired or we were notified
				
				if ( refresh_flag.flag )
				{
					// we have to refresh the watchdog
					synchronized (refresh_flag)
					{
						refresh_flag.flag = false;
					}
					
					// Now we will go to sleep again
				}
				/*
				else if (cancel_flag.flag)
				{
					synchronized (cancel_flag)
					{
						cancel_flag.flag = false;
					}
				}
				*/
				
				// If we woke up and its not a cancel flag or a refresh flag,
				// then the watchdog has expired
				else if ( ! cancel_flag.flag)
				{
					synchronized (current_state)
					{
						current_state = WatchdogTask_State.expired;
					}
					
					if (watchdogEventHandler == null)
						System.err.printf("Uninitialized watchdog event handler\n");
					else
						watchdogEventHandler.eventHandler(goose_frame, receiveTask);
					
					// Once the time has expired, we wait until we are cancelled or refreshed
					synchronized(sleeper)
					{
						try {
							sleeper.wait();
						} catch (InterruptedException e) {

							System.err.printf("Sleeping thread was interrupted\n");
							e.printStackTrace();
						}
					}
				}
			}
			
			// We received a cancel flag
			synchronized (current_state)
			{
				synchronized (cancel_flag)
				{
					cancel_flag.flag = false;
				}
				
				current_state = WatchdogTask_State.stopped;
			}
			
			//System.out.printf("End of sleeper thread. \n");

	    }
		
	}
	
}
