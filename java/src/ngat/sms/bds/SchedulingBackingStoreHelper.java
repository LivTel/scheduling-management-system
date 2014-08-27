/**
 * 
 */
package ngat.sms.bds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import ngat.net.telemetry.MysqlBackingStore;
import ngat.net.telemetry.SecondaryCache;
import ngat.net.telemetry.StatusCategory;
import ngat.sms.events.CandidateAddedEvent;
import ngat.sms.events.CandidateRejectedEvent;
import ngat.sms.events.SchedulingStatus;
import ngat.sms.events.SweepCompletedEvent;
import ngat.sms.events.SweepStartingEvent;

/**
 * @author eng
 * 
 */
public class SchedulingBackingStoreHelper extends MysqlBackingStore implements SecondaryCache {

	private Connection connection;

	private static final String INSERT = "insert into sched_status(type, time, ref) values (?, ?, ?)";
	private static final String INSERT1 = "insert into sched_sweep (swid, start) values (?, ?)";

	private static final String INSERT2 = "update sched_sweep set end = ? where id = ?";

	private static final String INSERT3 = "insert into sched_candidate (sid, gid, score) values (?, ?, ?)";
	private static final String INSERT4 = "insert into sched_reject (sid, gid, reason) values (?, ?, ?)";

	private PreparedStatement storeStatementStatus;
	private PreparedStatement storeStatementSweepStart;
	private PreparedStatement storeStatementSweepEnd;
	private PreparedStatement storeStatementCandidate;
	private PreparedStatement storeStatementReject;

	private int currentSweep;
	
	/**
	 * @param arg0
	 * @throws Exceptiondays when the institution is closed in the interests of efficiency, 
	 */
	public SchedulingBackingStoreHelper(String mysqlUrl) throws Exception {
		super(mysqlUrl);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ngat.net.telemetry.MysqlBackingStore#prepareStatements(java.sql.Connection
	 * )
	 */
	@Override
	protected void prepareStatements(Connection connection) throws Exception {

		storeStatementStatus = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS);
		storeStatementSweepStart = connection.prepareStatement(INSERT1, Statement.RETURN_GENERATED_KEYS);
		storeStatementSweepEnd = connection.prepareStatement(INSERT2, Statement.RETURN_GENERATED_KEYS);
		storeStatementCandidate = connection.prepareStatement(INSERT3, Statement.RETURN_GENERATED_KEYS);
		storeStatementReject = connection.prepareStatement(INSERT4, Statement.RETURN_GENERATED_KEYS);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.net.telemetry.MysqlBackingStore#retrieveStatus(long, long)
	 */
	@Override
	public List<StatusCategory> retrieveStatus(long arg0, long arg1) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.net.telemetry.MysqlBackingStore#storeStatus(ngat.net.telemetry.
	 * StatusCategory)
	 */
	@Override
	public void storeStatus(StatusCategory s) throws Exception {

		SchedulingStatus status = (SchedulingStatus) s;
		
		System.err.println("Processing status class: "+status.getClass().getSimpleName());
	
		if (s instanceof SweepStartingEvent) {
			SweepStartingEvent sse = (SweepStartingEvent) status;
			// insert into sched_sweep (swid, start) values (?, ?)
			
			storeStatementSweepStart.setInt(1, sse.getSweepId());
			storeStatementSweepStart.setDouble(2, (double)sse.getStatusTimeStamp()/1000.0);
			storeStatementSweepStart.executeUpdate();
			System.err.println("Start local sweep no: "+sse.getSweepId());
			
	          ResultSet results = storeStatementSweepStart.getGeneratedKeys();
	          if (results.next()) {
	              currentSweep = results.getInt(1);
	              System.err.println("Record starting as global sweep: "+currentSweep);
	          }
		
		} else if (status instanceof SweepCompletedEvent) {
			SweepCompletedEvent sce = (SweepCompletedEvent)status;
			// insert into sched_sweep (end) values (?) where id = ?
			System.err.println("End this global sweep: "+currentSweep);
			storeStatementSweepEnd.setDouble(1, (double)sce.getStatusTimeStamp()/1000.0);
			storeStatementSweepEnd.setInt(2, currentSweep);
			storeStatementSweepEnd.executeUpdate();
			
		} else if (status instanceof CandidateAddedEvent) {
			CandidateAddedEvent cad = (CandidateAddedEvent)status;
			// insert into sched_candidate (sid, gid, score) values (?, ?, ?)
			System.err.println("Add candidate");
			storeStatementCandidate.setInt(1, currentSweep);
			storeStatementCandidate.setInt(2, (int)cad.getGroup().getID());
			storeStatementCandidate.setDouble(3, cad.getScore());
			storeStatementCandidate.executeUpdate();
			
		} else if (status instanceof CandidateRejectedEvent) {
			CandidateRejectedEvent rej = (CandidateRejectedEvent)status;
			// insert into sched_reject (sid, gid, reason) values (?, ?, ?)
			storeStatementReject.setInt(1, currentSweep);
			storeStatementReject.setInt(2, (int)rej.getGroup().getID());
			storeStatementReject.setString(3, rej.getReason());
			storeStatementReject.executeUpdate();
			
		}

	}

}
