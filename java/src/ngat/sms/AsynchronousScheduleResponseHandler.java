package ngat.sms;

import java.rmi.*;


public interface AsynchronousScheduleResponseHandler extends Remote {
    
	public void asynchronousScheduleProgress(String message) throws RemoteException;
	
    public void asynchronousScheduleResponse(ScheduleItem sched) throws RemoteException;

    public void asynchronousScheduleFailure(int code, String message) throws RemoteException;

}