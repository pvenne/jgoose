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
 * This class defines a task used to transmit an ICE61850 frame. 
 * The frame to transmit is associated with this task.
 * It extends an IEC61850_GOOSE_RescheduleTask.
 * It automatically transmits the frame after a maximum delay or when triggered.
 * 
 * @author  Philippe Venne
 * @version 0.1
 *
 */

package jgoose;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

//import org.jnetpcap.packet.JPacket;
//import com.gremwell.jnetbridge.PcapPort;

public class IEC61850_GOOSE_TransmitTask extends IEC61850_GOOSE_Task
{
	
	//String name;
	//protected Timer timer;

	//final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
	
	// Defined in IEC61850_GOOSE_Task
	//IEC61850_GOOSE_Frame goose_frame;
	//JMemoryPacket goose_memoryPacket;
	
	IEC61850_GOOSE_TransmitTask transmitTask;

	// This is the actual thread used to transmit
	IEC61850_GOOSE_TransmitTask_Transmitter transmitter;
	
	IEC61850_GOOSE_TaskEventHandler sendvalues_EventHandler = null;
	IEC61850_GOOSE_TaskEventHandler retransmit_EventHandler = null;
	
	int minimum_delay = 0;
	int maximum_delay = 0;
	
	class IEC61850_LockedFlag {
		boolean flag;
	}
	
	// This flag is used to notify the transmitter its time to stop
	IEC61850_LockedFlag cancel_flag = new IEC61850_LockedFlag ();
	
	// This flag is used to notify the transmitter data has changed
	IEC61850_LockedFlag dataHasChanged_flag = new IEC61850_LockedFlag ();
	
	// We define the TransmitTask state machine See 61850-8-1/2011 Figure 10
	public enum Transmitter_State
	{
		// The control block is disabled
		non_existent,
		
		// In the process of sending new values
		send_values,
		
		// waiting for retransmission
		retransmit_pending,
		
		// In the process of retransmitting old values
		retransmit;
	}
	
	private Transmitter_State current_state;
	
	
	public IEC61850_GOOSE_TransmitTask(IEC61850_GOOSE_Frame transmit_frame, 
			int minimum_delay, int maximum_delay) throws IEC61850_GOOSE_Exception
	{	
		transmitTask = this;
		
		// The initial state is non_existent
		current_state = Transmitter_State.non_existent;
		
		cancel_flag.flag = false;
		dataHasChanged_flag.flag = false;
		
		this.goose_frame = transmit_frame;
		this.minimum_delay = minimum_delay;
		this.maximum_delay = maximum_delay;
		
		goose_memoryPacket = transmit_frame.makeNewPacket();
		
	}
	
	// The method is used to start the transmission. It changes the state to send_values
	public  void enable() throws  InterruptedException, IEC61850_GOOSE_Exception
	{
		
		synchronized (current_state)
		{
		
			if (current_state == Transmitter_State.non_existent)
			{
				
				// When a Transmit task is enabled, it triggers sending the first values
				current_state = Transmitter_State.send_values;
				
				// We start the scheduler task. It goes in wait state right away
				transmitter = new  IEC61850_GOOSE_TransmitTask_Transmitter();
				transmitter.start();
				
			}
	
			else
			{
				throw new IEC61850_GOOSE_Exception("The task is already started. Ignored");
			}
		}
	}
	
	// The method is used to stop the transmission.
	public void disable() throws IEC61850_GOOSE_Exception
	{
		synchronized (current_state)
		{
		
			if (current_state != Transmitter_State.non_existent)
			{
				// we notify its time to refresh the timer
				synchronized (cancel_flag)
				{
					cancel_flag.flag = true;
				}
				
				// we wakeup the sleeper thread
				synchronized (transmitter)
				{
					transmitter.notify();
				}
			}
			else
				throw new IEC61850_GOOSE_Exception("The transmitter is already disabled, ignoring\n");
		}
		
	}
	
	// This method is used to notify the Transmit task that data has been changed since
	// the last transmission.
	public void dataHasBeenChanged() throws IEC61850_GOOSE_Exception
	{
		synchronized (current_state)
		{
		
			if (current_state != Transmitter_State.non_existent)
			{
				// we notify its time to refresh the timer
				synchronized (dataHasChanged_flag)
				{
					dataHasChanged_flag.flag = true;
				}
				
				// we wakeup the sleeper thread
				
				synchronized (transmitter)
				{
					transmitter.notify();
				}
			}
			else
				throw new IEC61850_GOOSE_Exception("The transmitter not enabled, ignoring\n");
		
		}

	}
	
	// This Event Handler is called when transmitting new data for the first time 
	public void registerEventHandler_sendvalues(IEC61850_GOOSE_TaskEventHandler local_eventHandler)
	{
		sendvalues_EventHandler = local_eventHandler;
	}
	
	// This Event Handler is called when re-transmitting the same date 
	public void registerEventHandler_retransmission(IEC61850_GOOSE_TaskEventHandler local_eventHandler)
	{
		retransmit_EventHandler = local_eventHandler;
	}
	
	
	// This class implements the transmitter task
	class IEC61850_GOOSE_TransmitTask_Transmitter extends Thread
	{
		Date last_transmitter_execution;
		int  retransmission_number;
		
		boolean running;
		
		IEC61850_GOOSE_SchedulerTask retransmit_scheduler;
		
		public IEC61850_GOOSE_TransmitTask_Transmitter()
		{
			retransmit_scheduler = new IEC61850_GOOSE_SchedulerTask();
			retransmission_number = 0;
			running = true;
		}
		
		@Override
	    public void run()
	    {
			// We have to create an instance of the GOOSE header for binding to work
	        @SuppressWarnings("unused")
	        IEC61850_GOOSE_Header dummy_goose_header = new IEC61850_GOOSE_Header();
			
	        boolean time_to_sleep = false;
	        
			while (running)
			{	
				synchronized (current_state)
				{
					switch(current_state)
					{
						case send_values:
							
							// We call the proper event handler
							if (sendvalues_EventHandler == null)
								System.err.printf("Uninitialized sendvalues event handler\n");
							
							else
							{
								last_transmitter_execution = new Date();
								sendvalues_EventHandler.eventHandler(goose_frame, transmitTask);
							}
							
							current_state = Transmitter_State.retransmit_pending;
							
							// we reset the retransmission number
							retransmission_number = 1;
							
							// We start the scheduler here
							// The scheduler will notify the transmitter task to retransmit
							// The first time, the delay is divided by 5 according to 61850-8-1/2011 18.1.2.5.1
							retransmit_scheduler.start(maximum_delay /5);
							
							break;
							
						case retransmit_pending:
							
							// Normally, it is the retransmit_scheduler that changes the state to retransmit
							
							// If we were asked to disable, we stop
							if(cancel_flag.flag)
							{
								synchronized(cancel_flag)
								{
									cancel_flag.flag = false;
								}
								
								// We turn off the scheduler and cancel any task scheduled
								retransmit_scheduler.terminate();
								
								current_state = Transmitter_State.non_existent;
								running = false;
							}
							
							// New data is received
							else if(dataHasChanged_flag.flag)
							{
								Date current_time = new Date();
								
								if((current_time.getTime() - last_transmitter_execution.getTime()) < minimum_delay)
								{
									// Its too early to send values. We have to wait
									
									// Stop the next retransmission
									retransmit_scheduler.cancel();
									
									// Schedule the next time we wakeup at the right time
									retransmit_scheduler.start(minimum_delay - (current_time.getTime() - last_transmitter_execution.getTime()));
									
								}
								else
								{
									synchronized(dataHasChanged_flag)
									{
										dataHasChanged_flag.flag = false;
									}
									
									// We turn off the scheduler and cancel any task scheduled
									retransmit_scheduler.cancel();
									
									current_state = Transmitter_State.send_values;
								}
								
							}
							else
							{	
								time_to_sleep = true;
								
								/*// The thread waits until an event wakes up the thread
								synchronized(this)
								{
									try 
									{
										this.wait();
										
									} catch (InterruptedException e) {
		
										System.err.printf("Scheduler thread was interrupted\n");
										e.printStackTrace();
									}
								}*/
							}
							
							break;
							
						case retransmit:
							
							// We call the proper event handler
							if (retransmit_EventHandler == null)
								System.err.printf("Uninitialized retransmit event handler\n");
							
							else
							{
								// We start by scheduling the next execution
								// increase transmission time according to 61850-8-1/2011 18.1.2.5.1
								
								if ( retransmission_number > 3) // 4
									retransmit_scheduler.start(maximum_delay);
								
								else if (retransmission_number < 2) // 0,1
								{
									retransmit_scheduler.start(maximum_delay /5);
									retransmission_number++;
								}
								else if (retransmission_number == 2)
								{
									retransmit_scheduler.start(maximum_delay * 2/5);
									retransmission_number++;
								}
								else if (retransmission_number == 3)
								{
									retransmit_scheduler.start(maximum_delay * 3/5);
									retransmission_number++;
								}	
								
								last_transmitter_execution = new Date();
								
								// We call the event handler that does the actual retransmission
								retransmit_EventHandler.eventHandler(goose_frame, transmitTask);

								current_state = Transmitter_State.retransmit_pending;
							}
							
							break;
							
					} // switch(current_state)
					
				} // synchronized (current_state)
				
				if(time_to_sleep)
				{
					// The thread waits until an event wakes up the thread
					synchronized(this)
					{
						try 
						{
							this.wait();
							
						} catch (InterruptedException e) {
	
							System.err.printf("Scheduler thread was interrupted\n");
							e.printStackTrace();
						}
					}
					
					time_to_sleep = false;
				}
				
			}
	    }
	}
	
	// This class implements the scheduler of the transmitter task
	class IEC61850_GOOSE_SchedulerTask
	{
		String name;
		protected Timer timer;
		
		public IEC61850_GOOSE_SchedulerTask()
		{
			timer = new Timer();
		}
		
		class IEC61850_WakeupTask extends TimerTask
		{
		
			// The run method is called when the timer expires
			@Override
		    public void run() 
		    {
				synchronized (current_state)
				{
				
					// If the current state is send_values or retransmit, we do nothing. The system will automatically retransmit
					if (current_state == Transmitter_State.retransmit_pending )	
					{
						// If the dataHasChanged_flag is not set, we go re retransmission
						if(! dataHasChanged_flag.flag)
							// We change the current state to retransmit
							synchronized (current_state)
							{
								current_state = Transmitter_State.retransmit;
							}
						
						// we notify the scheduler thread
						synchronized (transmitter)
						{
							transmitter.notify();
						}
					}
					else if (current_state == Transmitter_State.non_existent )
					{
						System.err.printf("Time to retransmit, but current state is non_existent. Something is Really wrong!\n");
					}
				}
		
		    }
		}
		
		IEC61850_WakeupTask currentTask;
		
		public void start(long period){
			
			currentTask = new IEC61850_WakeupTask();
			
			// simplest code
			timer.schedule(currentTask, period);
		}
		
		public void terminate()
		{
			timer.cancel();
		}
		
		public void cancel()
		{
			currentTask.cancel();
		}
	}
}
