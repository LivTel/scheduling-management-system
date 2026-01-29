import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;

import ngat.message.SMS.SCHEDULE_REQUEST;
import ngat.message.SMS.SCHEDULE_REQUEST_DONE;
import ngat.message.base.COMMAND;
import ngat.message.base.ACK;
import ngat.message.base.COMMAND_DONE;
import ngat.net.TCPClientConnectionThreadMA;
import ngat.phase2.*;
import ngat.sms.GroupItem;

/**
 * This class sends a SCHEDULE_REQUEST command to the scheduler, and prints out the return. 
 * @author Chris Mottram
 * @version $Revision: 1.3 $
 */
public class TestSendScheduleRequest
{
	/**
	 * The default port number to send SMS commands to.
	 */
	static final int DEFAULT_SMS_PORT_NUMBER = 8776;
	/**
	 * The ip address to send the SCHEDULE_REQUEST to, this should be the machine the SMS is on.
	 */
	private InetAddress address = null;
	/**
	 * The port number to send the SCHEDULE_REQUEST command to the SMS.
	 */
	private int smsPortNumber = DEFAULT_SMS_PORT_NUMBER;
	/**
	 * The stream to write error messages to - defaults to System.err.
	 */
	private PrintStream errorStream = System.err;

	/**
	 * This is the initialisation routine.
	 */
	private void init()
	{
	}

	/**
	 * This routine creates a SCHEDULE_REQUEST command. 
	 * @return An instance of SCHEDULE_REQUEST.
	 */
	private SCHEDULE_REQUEST createScheduleRequest()
	{
		SCHEDULE_REQUEST scheduleRequest = new SCHEDULE_REQUEST("test");
		return scheduleRequest;
	}

	/**
	 * This is the run routine. It creates a SCHEDULE_REQUEST object and sends it to sms the using a 
	 * TCPClientConnectionThreadMA, and awaiting the thread termination to signify message
	 * completion. 
	 * @return The routine returns true if the command succeeded, false if it failed.
	 * @exception Exception Thrown if an exception occurs.
	 * @see #createScheduleRequest
	 * @see ngat.net.TCPClientConnectionThreadMA
	 * @see #getThreadResult
	 */
	private boolean run() throws Exception
	{
		COMMAND command = null;
		TCPClientConnectionThreadMA thread = null;
		boolean retval;

		command = (COMMAND)(createScheduleRequest());
		thread = new TCPClientConnectionThreadMA(address,smsPortNumber,command);
		thread.start();
		while(thread.isAlive())
		{
			try
			{
				thread.join();
			}
			catch(InterruptedException e)
			{
				System.err.println("run:join interrupted:"+e);
			}
		}// end while isAlive
		retval = getThreadResult(thread);
		return retval;
	}

	/**
	 * Find out the completion status of the thread and print out the final status of some variables.
	 * @param thread The Thread to print some information for.
	 * @return The routine returns true if the thread completed successfully,
	 * 	false if some error occured.
	 */
	private boolean getThreadResult(TCPClientConnectionThreadMA thread)
	{
		boolean retval;

		if(thread.getAcknowledge() == null)
			System.err.println("Acknowledge was null");
		else
			System.err.println("Acknowledge with timeToComplete:"+
				thread.getAcknowledge().getTimeToComplete());
		if(thread.getDone() == null)
		{
			System.out.println("Done was null");
			retval = false;
		}
		else
		{
			if(thread.getDone().getSuccessful())
			{
				System.out.println("Done was successful");
				if(thread.getDone() instanceof SCHEDULE_REQUEST_DONE)
				{
					SCHEDULE_REQUEST_DONE scheduleRequestDone = null;
					GroupItem group = null;
					
					scheduleRequestDone = (SCHEDULE_REQUEST_DONE)(thread.getDone());
					group = scheduleRequestDone.getGroup();
					System.out.println("\tScheduled Group:"+group);
				}
				retval = true;
			}
			else
			{
				System.out.println("Done returned error("+thread.getDone().getErrorNum()+
					"): "+thread.getDone().getErrorString());
				retval = false;
			}
		}
		return retval;
	}

	/**
	 * This routine parses arguments passed into TestSendScheduleRequest.
	 * @see #smsPortNumber
	 * @see #address
	 * @see #help
	 */
	private void parseArgs(String[] args)
	{
		for(int i = 0; i < args.length;i++)
		{
			if(args[i].equals("-h")||args[i].equals("-help"))
			{
				help();
				System.exit(0);
			}
			else if(args[i].equals("-ip")||args[i].equals("-address"))
			{
				if((i+1)< args.length)
				{
					try
					{
						address = InetAddress.getByName(args[i+1]);
					}
					catch(UnknownHostException e)
					{
						System.err.println(this.getClass().getName()+":illegal address:"+
							args[i+1]+":"+e);
					}
					i++;
				}
				else
					errorStream.println("-address requires an address");
			}
			else if(args[i].equals("-p")||args[i].equals("-port"))
			{
				if((i+1)< args.length)
				{
					smsPortNumber = Integer.parseInt(args[i+1]);
					i++;
				}
				else
					errorStream.println("-port requires a port number");
			}
			else
				System.out.println(this.getClass().getName()+":Option not supported:"+args[i]);
		}
	}

	/**
	 * Help message routine.
	 */
	private void help()
	{
		System.out.println(this.getClass().getName()+" Help:");
		System.out.println("Options are:");
		System.out.println("\t-p[ort] <port number> - Scheduler Port to send commands to.");
		System.out.println("\t-[ip]|[address] <address> - Scheduler machine Address to send commands to.");
		System.out.println("The default scheduler port is "+DEFAULT_SMS_PORT_NUMBER+".");
	}

	/**
	 * The main routine, called when TestSendScheduleRequest is executed. This initialises the object, parses
	 * it's arguments, opens the filename, runs the run routine, and then closes the file.
	 * @see #parseArgs
	 * @see #init
	 * @see #run
	 */
	public static void main(String[] args)
	{
		boolean retval;
		TestSendScheduleRequest tssr = new TestSendScheduleRequest();

		tssr.parseArgs(args);
		tssr.init();
		if(tssr.address == null)
		{
			System.err.println("No Scheduler Address Specified.");
			tssr.help();
			System.exit(1);
		}
		try
		{
			retval = tssr.run();
		}
		catch (Exception e)
		{
			retval = false;
			System.err.println("run failed:"+e);

		}
		if(retval)
			System.exit(0);
		else
			System.exit(2);
	}
}
