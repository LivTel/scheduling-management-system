/**
 * 
 */
package ngat.sms.bds;

import java.rmi.RemoteException;
import java.sql.*;
import java.text.SimpleDateFormat;

import java.util.*;
import java.util.Date;

import ngat.oss.model.IHistoryModel;
import ngat.phase2.IExecutionFailureContext;
import ngat.phase2.IHistoryItem;
import ngat.phase2.IQosMetric;
import ngat.phase2.XBasicExecutionFailureContext;
import ngat.phase2.XExposureInfo;
import ngat.phase2.XHistoryItem;
import ngat.util.logging.LogGenerator;
import ngat.util.logging.LogManager;
import ngat.util.logging.Logger;

/**
 * @author eng
 * 
 */
public class MysqlHistoryModel implements IHistoryModel {

	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	public static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

	Connection connection;
	LogGenerator logger;

	public MysqlHistoryModel(String url) throws Exception {
		// Setup DB connection..
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		//String url = "jdbc:mysql://ltdev1/phase2odb?user=oss&password=ng@toss";

		Logger alogger = LogManager.getLogger("SMS");
		logger = alogger.generate();
		logger.system("SMS").subSystem("SchedulingStatusProvider").srcCompClass("MysqlHistModel");
		logger.create().extractCallInfo().info().level(1).msg("Ready to create connection to MysqlDB using:" + url)
				.send();
		connection = DriverManager.getConnection(url);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.oss.model.IHistoryModel#addHistoryItem(long)
	 */
	public long addHistoryItem(long gid) throws RemoteException {

		// CREATE TABLE `HISTORY_ITEM` (
		// `id` int NOT NULL auto_increment,
		// `gid` int default NULL, /** group id **/
		// `scheduledTime` datetime default NULL,
		// `completionStatus` tinyint default NULL,
		// `completionTime` datetime default NULL,
		// `errorCode` tinyint default NULL,
		// `errorMessage` varchar(64) default NULL
		// `description` varchar(64) default NULL,
		// PRIMARY KEY (`id`));

		int oid = -1;
		try {
			String insert = "insert into HISTORY_ITEM (gid, scheduledTime, errorCode) values (" + gid + ", "
					+ (double)(System.currentTimeMillis()) + ",0)";
			logger.create().extractCallInfo().info().level(5).msg("execInsert: " + insert).send();
			Statement statement = connection.createStatement();
			logger.create().extractCallInfo().info().level(5).msg("execInsert: Statement ready").send();

			statement.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			logger.create().extractCallInfo().info().level(5).msg("execInsert: statement executed").send();

			ResultSet results = statement.getGeneratedKeys();
			while (results.next()) {
				oid = results.getInt(1);
			}
			logger.create().extractCallInfo().info().level(4).msg("History entry has ID = " + oid).send();
		} catch (Exception e) {
			throw new RemoteException("MysqlHistoryModel:addHistory", e);
		}
		return oid;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.oss.model.IHistoryModel#listHistoryItemHeaders(long)
	 */
	public List listHistoryItems(long gid) throws RemoteException {
		// CREATE TABLE `HISTORY_ITEM` (
		// `id` int NOT NULL auto_increment,
		// `gid` int default NULL, /** group id **/
		// `scheduledTime` datetime default NULL,
		// `completionStatus` tinyint default NULL,
		// `completionTime` datetime default NULL,
		// `errorCode` tinyint default NULL,
		// `errorMessage` varchar(64) default NULL
		// `description` varchar(64) default NULL,
		// PRIMARY KEY (`id`));

		List list = new Vector();
		int ng = 0;
		String query = "select id, scheduledTime, completionStatus, completionTime, errorCode, errorMessage from HISTORY_ITEM where gid="
				+ gid;
		logger.create().extractCallInfo().info().level(5).msg("execQuery: " + query).send();

		try {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);

			while (resultSet.next()) {
				ng++;

				XHistoryItem item = new XHistoryItem();
				item.setID(resultSet.getInt(1));
				item.setScheduledTime((long)(resultSet.getDouble(2)));
				item.setCompletionStatus(resultSet.getInt(3));
				item.setCompletionTime((long)(resultSet.getDouble(4)));
				item.setErrorCode(resultSet.getInt(5));
				item.setErrorMessage(resultSet.getString(6));
				
				list.add(item);
			}
		} catch (Exception e) {
			throw new RemoteException("MysqlHistoryModel:listHistoryItems(" + gid + ")", e);
		}
		return list;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ngat.oss.model.IHistoryModel#updateHistory(long, int, long,
	 * ngat.phase2.IExecutionFailureContext, java.util.Set)
	 */
	public void updateHistory(long hid, int cstat, long ctime, IExecutionFailureContext efc, Set qosStats)
			throws RemoteException {
		// CREATE TABLE `HISTORY_ITEM` (
		// `id` int NOT NULL auto_increment,
		// `gid` int default NULL, /** group id **/
		// `scheduledTime` datetime default NULL,
		// `completionStatus` tinyint default NULL,
		// `completionTime` datetime default NULL,
		// `errorCode` tinyint default NULL,
		// `errorMessage` varchar(64) default NULL
		// `description` varchar(64) default NULL,
		// PRIMARY KEY (`id`));

	    try {
		    execUpdate("update HISTORY_ITEM set completionStatus=" + cstat + " where id = " + hid);
			execUpdate("update HISTORY_ITEM set completionTime=" + (double)ctime + " where id = " + hid);
			execUpdate("update HISTORY_ITEM set errorCode=" + (efc == null ? 0 : efc.getErrorCode()) + " where id = "
					+ hid);
			String errmsg = null;
			if (efc == null) {
				errmsg = "OKAY";
			} else {				
				errmsg = efc.getErrorMessage();
				if (errmsg.length() > 60)
					errmsg = errmsg.substring(0,60);
			}
			System.err.println("MysqlHM::UpdateHistory() Error message= ["+errmsg+"]");
			execUpdate("update HISTORY_ITEM set errorMessage=\'" +errmsg+ "\' where id = " + hid);
			//execUpdate("update HISTORY_ITEM set errorMessage='An error occurred' where id = " + hid);
			
		} catch (Exception e) {
			throw new RemoteException("MysqlHistoryModel:updateHistory", e);
		}

		// Insert any QOS statistics here
		Iterator qi = qosStats.iterator();
		while (qi.hasNext()) {
		    IQosMetric qm = (IQosMetric) qi.next();
		    String qosadd = "insert into QOS_ITEM (hid, qosName, qosValue) values (" + hid + ", '" + qm.getMetricID()
			+ "', " + qm.getMetricValue() + ")";
		    try {
			execUpdate(qosadd);
		    } catch (Exception e) {
			throw new RemoteException("MysqlHistoryModel:updateHistory", e);
		    }
		}
	}

	private void execUpdate(String update) throws Exception {
		logger.create().extractCallInfo().info().level(4).msg("execUpdate: " + update).send();
		Statement statement = connection.createStatement();
		statement.executeUpdate(update);

	}

	public void addExposureUpdate(long hid, long expId, long expTime, String fileName) throws RemoteException {
		/*
		 * CREATE TABLE `EXPOSURE_ITEM` ( `id` int NOT NULL auto_increment,
		 * `hid` int default NULL, `fileName` varchar(32) default NULL, `time`
		 * datetime default NULL, PRIMARY KEY (`id`));
		 */

		logger.create().extractCallInfo().info().level(4).msg(
				"addExpUpdate() hid=" + hid + ", expid=" + expId + ", exptime=" + (double)expTime + ", file=" + fileName)
				.send();

		int oid = -1;
		try {
			String insert = "insert into EXPOSURE_ITEM (hid, time, fileName) values (" + hid + ", " + expTime + ", '"
					+ fileName + "')";
			logger.create().extractCallInfo().info().level(4).msg("execInsert: " + insert).send();
			Statement statement = connection.createStatement();
			logger.create().extractCallInfo().info().level(4).msg("execInsert: Statement ready").send();

			statement.executeUpdate(insert, Statement.RETURN_GENERATED_KEYS);
			logger.create().extractCallInfo().info().level(5).msg("execInsert: Statement executed").send();

			ResultSet results = statement.getGeneratedKeys();
			while (results.next()) {
				oid = results.getInt(1);
			}
			logger.create().extractCallInfo().info().level(4).msg("ExposureUpdate entry has ID = " + oid).send();
		} catch (Exception e) {
			throw new RemoteException("MysqlHistoryModel:addExposureUpdate", e);
		}
		// return oid;

	}

	/**
	 * This may be renamed later
	 * 
	 * @return A List of Exposure Items.
	 */
	public List listExposureItems(long histID) throws RemoteException {

		List list = new Vector();

		String query = "select id, time, fileName from EXPOSURE_ITEM where hid=" + histID;
		logger.create().extractCallInfo().info().level(5).msg("execQuery: " + query).send();

		try {
			Statement statement = connection.createStatement();
			ResultSet results = statement.executeQuery(query);

			while (results.next()) {

				XExposureInfo expinfo = new XExposureInfo();
				expinfo.setID(results.getInt(1));
				expinfo.setExposureTime((long)(results.getDouble(2)));
				expinfo.setFileName(results.getString(3));

				list.add(expinfo);
			}
		} catch (Exception e) {
			throw new RemoteException("MysqlHistoryModel:listExposureItems(" + histID + ")", e);
		}
		return list;

	}

	/**
	 * This may be renamed later
	 * 
	 * @return A List of QOS Statistics Items.
	 */
	public List listQosItems(long histID) throws RemoteException {
		return null;
		// TODO Humanoid-generated method stub
	}

}
