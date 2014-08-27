package ngat.sms;

import java.rmi.*;

public interface AsynchronousScheduler extends Remote {

    public void requestSchedule(AsynchronousScheduleResponseHandler asrh) throws RemoteException;

}