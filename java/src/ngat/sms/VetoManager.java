package ngat.sms;

import java.rmi.*;
import java.util.List;
import java.util.Map;

public interface VetoManager extends Remote {

	/** Veto level indicating NO veto to be applied.*/
	public static final int VETO_LEVEL_NONE = 0;
	
	/** Veto level indicating a low level of veto.*/
	public static final int VETO_LEVEL_LOW = 1;
	
	/** Veto level indicating a mdeium level of veto.*/
	public static final int VETO_LEVEL_MEDIUM = 2;
	
	/** Veto level indicating a high level of veto.*/
	public static final int VETO_LEVEL_HIGH = 3;
	
	/** Veto level indicating a permanent veto.*/
	public static final int VETO_LEVEL_PERMANENT = 4;
	
    public void vetoGroup(long gid, long time) throws RemoteException;
    
    public void removeVeto(long gid) throws RemoteException;
    
    public long getVetoTime(long gid) throws RemoteException;

    public List<Veto> listActiveVetos() throws RemoteException;
}