/**
 * @author xinwuhen
 */
package com.chinaepay.wx.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

/**
 * @author xinwuhen
 *
 */
public class MysqlConnectionPool {
	private static MysqlConnectionPool mysqlConnPoolObj = null;
	/** ���ݿ����ӳ� **/
	public static final Map<Connection, Boolean> mysqlConntPool = new HashMap<Connection, Boolean>();
	private static MysqlDataSource mysqlDs = null;
	public static String MYSQL_URL = null;
	public static String MYSQL_USER_NAME = null;
	public static String MYSQL_USER_PASSWD = null;
	public static int MYSQL_CONN_INITAL_SIZE = 0;	// ���ӳصĳ�ʼ������
	public static int MYSQL_CONN_MAX_SIZE = 0;	// ���ӳ����������������
	public static float MYSQL_CONN_INCREMENTAL_RATE = 0.00f;	// ���ֶα�ʾ��������С���������������������£���û�п������ӹ�ʹ��ʱ���������ӳ��Զ��������Ӷ���İٷֱȡ��˴�����Ϊ0.20f,��:20%��������
	public static long MYSQL_GET_CONN_SLEEP_TIME = 0;	// ���ֶα�ʾ���ӳ���û�п������������ӳص����Ӷ���(Connection)�Ѿ��ﵽ���������ֵ(MYSQL_CONN_MAX_SIZE)ʱ�����´ӳ��ڻ�ȡ�������ȴ���ʱ��(ms)��
	public static long MYSQL_VALIDATE_CONN_INTERVAL_TIME = 0;	// ���ֶα�ʾ����У�������Ƿ���õ�ʱ��������λΪ������
	
	// ��Դͬ����
	private final static Class<MysqlConnectionPool> SYNC_LOCK_OBJ = MysqlConnectionPool.class;
	
	/**
	 * ��ȡ�����ʵ����
	 * @return
	 */
	public static MysqlConnectionPool getInstance() {
		synchronized (SYNC_LOCK_OBJ) {
			if (mysqlConnPoolObj == null) {
				mysqlConnPoolObj = new MysqlConnectionPool();
			}
			return mysqlConnPoolObj;
		}
	}
	
	/**
	 * ˽�л�����Ĺ��췽����
	 */
	private MysqlConnectionPool() {
		// ���������ļ�
		loadConf();
		
		// ��ʼ�����Ӷ���ѹ�����ӳ�
		initPool();
		
		// ��������У��(Ping)���ӵĶ����߳�
		if (MYSQL_VALIDATE_CONN_INTERVAL_TIME > 0 && mysqlConntPool.size() > 0) {
			new ValidateConnectionThread(MYSQL_VALIDATE_CONN_INTERVAL_TIME, mysqlConntPool).start();
		}
	}
	
	/**
	 * ���������ļ��ڵ���Ϣ��
	 */
	private void loadConf() {
		String SYSTEM_PATH_CHARACTOR = System.getProperty("file.separator");
		String strOSWebAppPath = CommonTool.getAbsolutWebAppPath(this.getClass(), SYSTEM_PATH_CHARACTOR);
		String strFilePathName = strOSWebAppPath + SYSTEM_PATH_CHARACTOR + "conf" + SYSTEM_PATH_CHARACTOR + "config.properties";
		
		Properties properties = new Properties();
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(strFilePathName);
			properties.load(inputStream);
			inputStream.close(); // �ر���`
			
			MYSQL_URL = properties.getProperty("URL");
			MYSQL_USER_NAME = properties.getProperty("user");
			MYSQL_USER_PASSWD = properties.getProperty("password");

			String strInitConn = properties.getProperty("init_conn");
			MYSQL_CONN_INITAL_SIZE = (strInitConn == null || "".equals(strInitConn)) ? 0 : Integer.parseInt(strInitConn);

			String strMaxConn = properties.getProperty("max_conn");
			MYSQL_CONN_MAX_SIZE = (strMaxConn == null || "".equals(strMaxConn)) ? 0 : Integer.parseInt(strMaxConn);
		
			String strIncRate = properties.getProperty("incremental_rate");
			MYSQL_CONN_INCREMENTAL_RATE = (strIncRate == null || "".equals(strIncRate)) ? 0.00f : Float.parseFloat(strIncRate);
			
			String strSleepTime = properties.getProperty("get_conn_sleep_time");
			MYSQL_GET_CONN_SLEEP_TIME = (strSleepTime == null || "".equals(strSleepTime)) ? 0L : Long.parseLong(strSleepTime);
			
			String strValidateTime = properties.getProperty("valid_conn_interval_time");
			MYSQL_VALIDATE_CONN_INTERVAL_TIME = (strValidateTime == null || "".equals(strValidateTime)) ? 0L : Long.parseLong(strValidateTime);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ����������Դ����ʼ��һ�����������Ӷ���
	 */
	private void initPool() {
		if (mysqlDs == null) {
			mysqlDs = new MysqlDataSource();
			mysqlDs.setURL(MYSQL_URL);
			mysqlDs.setUser(MYSQL_USER_NAME);
			mysqlDs.setPassword(MYSQL_USER_PASSWD);
			mysqlDs.setUseUnicode(true);
			mysqlDs.setEncoding("UTF-8");
			
			for(int i = 0; i < MYSQL_CONN_INITAL_SIZE; i++) {
				try {
					mysqlConntPool.put(mysqlDs.getConnection(), true);	// �˴��ĵڶ�������(Booleanֵ)��ʾ��ǰ������(Connection)�Ƿ��ڿ���״̬��
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * �����ӳ��л�ȡ����
	 * @param isAutoCommit
	 * @return
	 */
	public Connection getConnection(boolean isAutoCommit) {
		synchronized (SYNC_LOCK_OBJ) {
			Connection connt = null;

			if (mysqlConntPool.size() > 0) {
				Connection[] conns = mysqlConntPool.keySet().toArray(new Connection[0]);
				System.out.println("conns.length = " + conns.length);
				for(int i = 0; i < conns.length; i++) {
					Connection conn = conns[i];
					if (conn != null) {
						try {
							if (conn.isClosed() || !conn.isValid(5)) {	// isValid(5)��������ʾ�����˽���һ������У�飬����У��ĳ�ʱʱ��Ϊ5�롣
								mysqlConntPool.remove(conn);
								continue;
							}
							
							// ����δ�رղ�������Ч����
							boolean blnIsIdle = mysqlConntPool.get(conn);
							if (blnIsIdle) {	// �����ǿ���״̬
								connt = conn;
								mysqlConntPool.put(conn, false);
								break;
							}								
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			if (connt == null) {
				int intCurrSize = mysqlConntPool.size();
				System.out.println("intCurrSize = " + intCurrSize);
				
				int intNeedIncrementalValue = 0;
				if (intCurrSize < MYSQL_CONN_INITAL_SIZE) {
					intNeedIncrementalValue = MYSQL_CONN_INITAL_SIZE - intCurrSize;
				} else if (intCurrSize >= MYSQL_CONN_INITAL_SIZE && intCurrSize < MYSQL_CONN_MAX_SIZE) {
					int intAfterRate = (int) (intCurrSize * (1 + MYSQL_CONN_INCREMENTAL_RATE));
					if (intAfterRate > MYSQL_CONN_MAX_SIZE) {
						intNeedIncrementalValue = MYSQL_CONN_MAX_SIZE - intCurrSize;
					} else {
						intNeedIncrementalValue = intAfterRate - intCurrSize;
					}
				}
				
				System.out.println("intNeedIncrementalValue = " + intNeedIncrementalValue);
				
				for (int i = 0; i < intNeedIncrementalValue; i++) {
					try {
						mysqlConntPool.put(mysqlDs.getConnection(), true);
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				
				try {
					Thread.sleep(MYSQL_GET_CONN_SLEEP_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				connt = getConnection(isAutoCommit);
			}
			
			if (connt != null) {
				try {
					connt.setAutoCommit(isAutoCommit);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			return connt;			
		}
	}
	
	/**
	 * �ͷ�������Ϣ��
	 * @param conn
	 */
	public void releaseConnInfo(Connection conn) {
		releaseConnection(conn);
	}
	
	/**
	 * �ͷ�������Ϣ��
	 * @param rs
	 * @param conn
	 */
	public void releaseConnInfo(PreparedStatement prst, Connection conn) {
		if (prst != null) {
			try {
				prst.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		releaseConnection(conn);
	}
	
	/**
	 * ���ݻع���
	 * @param conn
	 */
	public void rollback(Connection conn) {
		if (conn != null) {
			try {
				conn.rollback();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * �ͷ�������Ϣ��
	 * @param rs
	 * @param prst
	 * @param conn
	 */
	public void releaseConnInfo(ResultSet rs, PreparedStatement prst, Connection conn) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		if (prst != null) {
			try {
				prst.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		releaseConnection(conn);
	}
	
	/**
	 * �ͷ����ӳض���
	 * @param conn
	 */
	private void releaseConnection(Connection conn) {
		if (conn == null) {
			return;
		}
		
		synchronized (SYNC_LOCK_OBJ) {
			try {
				if(mysqlConntPool.containsKey(conn)) {
					if (conn.isClosed()) {	// �����ӹر�
						mysqlConntPool.remove(conn);
					} else {
//						if (!conn.getAutoCommit()) {
//							//	Connection�����AutoCommitֵĬ��ΪTrue, ��ǰ���ǽ��˹��ָܻ�ΪĬ��ֵ�� ֻ���û������ӳ��л�ȡ����ʱ�������û������Ƿ�ʹ���Զ��ύģʽ��
//							//	�����û����ͷ�Connectionǰ������������ģʽ(��������AutoCommit=false), ����ȴû��ִ��commit()���������ڴ˴�ִ��setAutoCommit(true),��˷�����������û��ύSQL��ִ�н����
//							conn.setAutoCommit(true); 
//						}
						
						mysqlConntPool.put(conn, true);
					}
				} else {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ����У��(Ping)���ӵĶ����̡߳�
	 * @author xinwuhen
	 */
	private class ValidateConnectionThread extends Thread {
		private long lValidConnTime = 0;
		private Map<Connection, Boolean> mysqlConntPool = null;
		
		public ValidateConnectionThread (long lValidConnTime, Map<Connection, Boolean> mysqlConntPool) {
			this.lValidConnTime = lValidConnTime;
			this.mysqlConntPool = mysqlConntPool;
		}
		
		public void run() {
			if (lValidConnTime > 0) {
				while (true) {
					try {
						Thread.sleep(lValidConnTime * 1000);
						
						synchronized (SYNC_LOCK_OBJ) {
							if (mysqlConntPool != null) {
								System.out.println("mysqlConntPool.size = " + mysqlConntPool.size());
								
								Connection[] connKeys = mysqlConntPool.keySet().toArray(new Connection[0]);
								if (connKeys != null) {
									for (int i = 0; i < connKeys.length; i++) {
										Connection conn = connKeys[i];
										if (conn != null) {
											try {
												// ��MySQL������֮�����һ������(Ӧ��ʱʱ������Ϊ5��)����У�������Ƿ���Ч
												boolean blnIsValid = conn.isValid(5);
//												System.out.println("blnIsValid = " + blnIsValid);
												if (blnIsValid == false || conn.isClosed()) {
													mysqlConntPool.remove(conn);
												}
											} catch (SQLException e) {
												e.printStackTrace();
											}
										}
									}
								}
							}
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
