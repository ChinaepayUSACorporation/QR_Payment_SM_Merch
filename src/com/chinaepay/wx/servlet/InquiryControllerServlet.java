package com.chinaepay.wx.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;

public abstract class InquiryControllerServlet extends CommControllerServlet {
	public static final long INQUIRY_ORDER_WAIT_TIME = 30 * 1000L;
	
	/**
	 * У���̻��Ƿ��в�ѯ��������ѯ���㵥�����ض��˵���Ȩ�ޡ�
	 */
	public boolean validSubMchIsUsable(String strSubMerchId) {
		boolean blnValSubMchIsUsable = false;
		Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
		PreparedStatement prst = null;
		ResultSet rs = null;
		try {
			String strSql = "select * from t_merchant where sub_merchant_code='" + CommonTool.formatNullStrToSpace(strSubMerchId) + "';";
			prst = conn.prepareStatement(strSql);
			rs = prst.executeQuery();
			if (rs.next()) {
				int iAuditStatus = rs.getInt("audit_status");
				int iStatus = rs.getInt("status");
				int iAccountStatus = rs.getInt("account_status");
				String strDelFlag = rs.getString("del_flag");
				
				if (iAuditStatus == 2 // ���״̬ 1:����� 2:���ͨ�� 3:��˲�ͨ��
						&& iStatus == 1 	// �̻�״̬ 1:���� 0:����
//						&& iAccountStatus == 1	// �˻�״̬  1:���� 2: ����
						&& CommonTool.formatNullStrToSpace(strDelFlag).equals("0")) {	// 1:��ɾ��  0:δɾ��
					blnValSubMchIsUsable = true;
				}
			}
		} catch (SQLException se) {
			se.printStackTrace();
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
		}
		
		return blnValSubMchIsUsable;
	}
	
	/**
	 * ����ҵ���ѯ�������Ӧ�����ݿ��
	 */
	public abstract void updateInquiryRstToTbl(Map<String, String> mapArgs);
	
	/**
	 * ��ȡ�̳߳ء�
	 * @param iCorePoolSize
	 * @param intMaxPoolSize
	 * @param intKeepAliveTime
	 * @param intTaskQueueSize
	 * @return
	 */
	public ThreadPoolExecutor getThreadPoolExecutor(int iCorePoolSize, int intMaxPoolSize, int intKeepAliveTime, int intTaskQueueSize) {
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(iCorePoolSize, intMaxPoolSize, intKeepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(intTaskQueueSize));
//		threadPoolExecutor.prestartAllCoreThreads();
		threadPoolExecutor.allowCoreThreadTimeOut(false);	// ����ָ�������ĺ����߳����У������ܳ�ʱ���õ�Ӱ��
		return threadPoolExecutor;
	}
	
	/**
	 * ����������ִ��������ͣ��ʱ���ࡣ
	 * @author xinwuhen
	 */
	public class ClosableTimer extends Timer {
		private boolean blnNeedCloseTimer = false;
		
		public ClosableTimer(boolean blnNeedCloseTimer) {
			super();
			this.blnNeedCloseTimer = blnNeedCloseTimer;
		}
		
		public boolean isNeedClose() {
			return blnNeedCloseTimer;
		}
		
		/*
		public void schedule(TimerTask task, long delay) {
			super.schedule(task, delay);
			
			if (blnNeedCloseTimer) {
				this.cancel();
			}
		}
		
		public void schedule(TimerTask task, Date time) {
			super.schedule(task, time);
			
			if (blnNeedCloseTimer) {
				this.cancel();
			}
		}
		
		public void schedule(TimerTask task, long delay, long period) {
			super.schedule(task, delay, period);
			
			if (blnNeedCloseTimer) {
				this.cancel();
			}
		}
		
		public void schedule(TimerTask task, Date firstTime, long period) {
			super.schedule(task, firstTime, period);
			
			if (blnNeedCloseTimer) {
				this.cancel();
			}
		}
		
		public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
			super.scheduleAtFixedRate(task, delay, period);
			
			if (blnNeedCloseTimer) {
				this.cancel();
			}
		}
		
		public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
			super.scheduleAtFixedRate(task, firstTime, period);
 
			if (blnNeedCloseTimer) {
				this.cancel();
			}
		}
		*/
	}
}
