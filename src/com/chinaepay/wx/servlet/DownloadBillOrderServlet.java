package com.chinaepay.wx.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.common.JsonRespObj;
import com.chinaepay.wx.entity.DownloadBillOrderEntity;
import com.chinaepay.wx.entity.DownloadBillPayOrderEntity;

import net.sf.json.JSONObject;

public abstract class DownloadBillOrderServlet extends InquiryControllerServlet {
	private final String DOWNLOAD_BILL_ORDER_URL = CommonInfo.HONG_KONG_CN_SERVER_URL + "/pay/downloadbill";
	final long lngOneDay = 24 * 60 * 60 * 1000L;
	
	public void init() {
		try {
			super.init();
			
			// ��ȡ���ض��˵���ָ��ʱ��
			String strHour = this.getServletContext().getInitParameter("Hour_DownloadBill");
			String strMinute = this.getServletContext().getInitParameter("Minute_DownloadBill");
			String strSecond = this.getServletContext().getInitParameter("Second_DownloadBill");
			String strDelayTime = this.getServletContext().getInitParameter("DelayTime_DownloadBill");
	
			// �������ض��˵��������߳�
			DownloadBillOrderThread dbpot = new DownloadBillOrderThread(strHour, strMinute, strSecond, strDelayTime);
			dbpot.start();
		} catch (ServletException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Ϊǰ�˶��˽���Ԥ������֤���˳��ֲ��ʱ��������ǰ�˽��淢�����ض��˵������롣
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		// ��ȡ��startTimeForBillOrderSucc���롾endTimeForBillOrderSucc��֮������гɹ���֧���� 
		// ��ע�⡿��ʱ���ʽ��ȷ�����족���磺20180302
		String startTimeForBillOrderSucc = request.getParameter("startTimeForBillOrderSucc"); 
		String endTimeForBillOrderSucc = request.getParameter("endTimeForBillOrderSucc");
		
		ClosableTimer closableTimer = new ClosableTimer(true);	// ִ���������ر�Timer
		TimerTask task = getNewDownloadBillOrderTask(closableTimer, startTimeForBillOrderSucc, endTimeForBillOrderSucc);
        closableTimer.schedule(task, 0L);
        
        // ��������ѯ������ص������ն�
        JsonRespObj respObj = new JsonRespObj();
		String strResult = "1";
		String strReturnMsg = "����Ѷ�����ض��˵�(����֧�������˿)�����Ѿ��ύ��ִ̨�У�";
        respObj.setRespCode(strResult);
		respObj.setRespMsg(strReturnMsg);
		JSONObject jsonObj = JSONObject.fromObject(respObj);
		super.returnJsonObj(response, jsonObj);
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		doGet(request, response);
	}
	
	/**
	 * �����µ����ض��˵����񣬻ص�������������Ѿ�ʵ�ֵķ�����
	 * @return
	 */
	public abstract DownloadBillOrderTask getNewDownloadBillOrderTask(ClosableTimer closableTimer);
	
	/**, endTimeForBillOrderSucc
	 * �����µ����ض��˵����񣬻ص�������������Ѿ�ʵ�ֵķ�����
	 * @param startTimeForBillOrderSucc	�����صĶ��˵����ݵ���ʼʱ�䡣
	 * @param endTimeForBillOrderSucc		�����صĶ��˵����ݵĽ���ʱ�䡣
	 * @return
	 */
	public abstract DownloadBillOrderTask getNewDownloadBillOrderTask(ClosableTimer closableTimer, String startTimeForBillOrderSucc, String endTimeForBillOrderSucc);
	
	
	public void updateInquiryRstToTbl(Map<String, String> mapArgs) {};
	
	/**
	 * �������ζ��˵���������̡߳�
	 * @author xinwuhen
	 */
	public class DownloadBillOrderThread extends Thread {
		private String strHour = null;
		private String strMinute = null;
		private String strSecond = null;
		private String strDelayTime = null;
		
		public DownloadBillOrderThread(String strHour, String strMinute, String strSecond, String strDelayTime) {
			this.strHour = strHour;
			this.strMinute = strMinute;
			this.strSecond = strSecond;
			this.strDelayTime = strDelayTime;
		}
		
		
		public void run() {
			// ��ǰʱ��
			long lngNowMillSec = new Date().getTime();
			
			// ��ȡָ������������ʱ��
			Date defineDate = getFixDateBasedOnArgs(strHour, strMinute, strSecond);
			long lngDefMillSec = defineDate.getTime();
			
			ClosableTimer closableTimer = null;
			TimerTask task = null;
			// ��ǰʱ����10��֮ǰ, ��Ҫ�ȵ�10��ʱִ�����񣬲�������24Сʱ����ѯ����ִ����ͬ����
			if (lngNowMillSec < lngDefMillSec) {
				closableTimer = new ClosableTimer(false);
		        task = getNewDownloadBillOrderTask(closableTimer);
		        closableTimer.scheduleAtFixedRate(task, defineDate, lngOneDay);
			}
			// ��ǰʱ����10��֮����Ҫ����ִ��һ�����񣬲��ڴ��յ�10�㿪ʼִ��һ�Σ��Ժ�ÿ��24Сʱ����ѯ����ִ����ͬ����
			else {
				// ִ��һ������(����ץȡ���ݵ�ʱ��)���������������ر�
				closableTimer = new ClosableTimer(true);
				task = getNewDownloadBillOrderTask(closableTimer);
				closableTimer.schedule(task, Long.parseLong(strDelayTime) * 60 * 1000);	// ������ת��Ϊ����
				
				// �ڴ���10�㿪ʼִ��һ������ �Ժ�ÿ��24Сʱ����ѯ����ִ����ͬ����
				closableTimer = new ClosableTimer(false);
				task = getNewDownloadBillOrderTask(closableTimer);
				Date nextDay = CommonTool.getBefOrAftDate(defineDate, lngOneDay);
				closableTimer.scheduleAtFixedRate(task, nextDay, lngOneDay);
			}
		}
	}
	
	/**
	 * ���ض��˵������ࡣ
	 * @author xinwuhen
	 *
	 */
	public abstract class DownloadBillOrderTask extends TimerTask {
		private ClosableTimer closableTimer = null;
		String startTimeForBillOrderSucc = null;
		String endTimeForBillOrderSucc = null;
		
		public DownloadBillOrderTask(ClosableTimer closableTimer) {
			super();
			this.closableTimer = closableTimer;
		}
		
		public DownloadBillOrderTask(ClosableTimer closableTimer, String startTimeForBillOrderSucc, String endTimeForBillOrderSucc) {
			this(closableTimer);
			this.startTimeForBillOrderSucc = startTimeForBillOrderSucc;
			this.endTimeForBillOrderSucc = endTimeForBillOrderSucc;
		}
		
		/**
		 * ���ض��˵������ҽ����˵���Ϣ���µ����ݿ⡣
		 */
		public void downloadAndUpdateBillInfo(String strBillType) {
			String strProcesserId = null;
			String strBillDate = null;
			String strWxRespInfo = null;
			
			// ���˵�������Ʊ��еġ����˵����͡��ֶ�
			String strNewBillType = null;
			if (strBillType.equals(DownloadBillPayOrderEntity.SUCCESS)) {
				strNewBillType = DownloadBillPayOrderEntity.BILL_ORDER_PAYMENT;
			} else if (strBillType.equals(DownloadBillPayOrderEntity.REFUND)) {
				strNewBillType = DownloadBillPayOrderEntity.BILL_ORDER_REFUND;
			}
			
			if (startTimeForBillOrderSucc != null && !"".equals(startTimeForBillOrderSucc)) {	// �п�ʼʱ��
				if (endTimeForBillOrderSucc == null || "".equals(endTimeForBillOrderSucc)) {	// ����ʱ��Ϊ�գ�Ĭ�Ͻ���ʱ��Ϊ����
					endTimeForBillOrderSucc = CommonTool.getBefOrAftFormatDate(new Date(), -lngOneDay, "yyyyMMdd");
				}
				
				Date startDate = CommonTool.getDateBaseOnChars("yyyyMMdd", startTimeForBillOrderSucc);
				long lngStartTime = startDate.getTime();
				long lngEndTime = CommonTool.getDateBaseOnChars("yyyyMMdd", endTimeForBillOrderSucc).getTime();
				long lngDayIndx = lngStartTime;
				while (lngDayIndx <= lngEndTime) {	// ��ʼʱ�䲻���ڽ���ʱ��
					// ����Ѷ���Ͷ��˵���ѯ���󣬲����ݷ��صĽ�����¶��˵����ݱ�
					strBillDate = CommonTool.getFormatDateStr(startDate, "yyyyMMdd");
					
					// ����˵����ؿ��������һ���¼�¼�����ڼ�¼���˵������ؼ��������
					strProcesserId = this.insertBillProcInfoToTbl(strBillDate, strNewBillType);
					
					// ����Ѷ��̨�������󣬲�����Ӧ����
					strWxRespInfo = this.getBillRespInfoFromWx(strBillType, strBillDate);	// ֧���ɹ��Ķ�����֧������
					this.parseRespInfoAndUpdateTbl(strProcesserId, strWxRespInfo);
					
					// ȡ��һ����ʱ��
					startDate = CommonTool.getBefOrAftDate(startDate, lngOneDay);
					lngDayIndx = startDate.getTime();
				}
			} else {	// ��ʼʱ��Ϊ�գ�Ĭ��ȡ���������
				strBillDate = CommonTool.getBefOrAftFormatDate(new Date(), -lngOneDay, "yyyyMMdd");
				
				// ����˵����ؿ��������һ���¼�¼�����ڼ�¼���˵������ؼ��������
				strProcesserId = this.insertBillProcInfoToTbl(strBillDate, strNewBillType);
		        
				strWxRespInfo = this.getBillRespInfoFromWx(strBillType, strBillDate);	// ֧���ɹ��Ķ�����֧������
				
				this.parseRespInfoAndUpdateTbl(strProcesserId, strWxRespInfo);
			}
			
			// �ж��Ƿ���Ҫ�ر�����ʱ��
			if (closableTimer.isNeedClose()) {
				closableTimer.cancel();
			}
		}
		
		/**
		 * ����˵����������ʼ�����ݡ�
		 * @param strBillDate
		 * @param strNewBillType
		 */
		private String insertBillProcInfoToTbl(String strBillDate, String strNewBillType) {
			String strProcesserId = "";
			
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			PreparedStatement prst = null;
			
			try {
				strProcesserId = CommonTool.getRandomString(16);
				String strSql = "replace into tbl_bill_order_proc_result(processer_id, mch_id, bill_order_type, belong_date, proc_start_time, proc_finish_time, proc_status) values('"
								+ strProcesserId
								+ "', '"
								+ CommonInfo.NOB_MCH_ID
								+ "', '"
								+ strNewBillType
								+ "', '"
								+ strBillDate
								+ "', '"
								+ CommonTool.getFormatDateStr(new Date(), "yyyyMMddHHmmss")
								+ "', '', '"
								+ DownloadBillOrderEntity.BILL_PROCESSING
								+ "')";
				prst = conn.prepareStatement(strSql);
				prst.execute();
				conn.commit();
			} catch(SQLException se) {
				se.printStackTrace();
				MysqlConnectionPool.getInstance().rollback(conn);
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(prst, conn);
			}
			
			return strProcesserId;
		}
		
		/**
		 * ���¶��˵�����������ڵļ�¼״̬��
		 * @param strProcesserId
		 * @param strProcStatus
		 */
		private void updateBillProcInfoToTbl(String strProcesserId, String strProcStatus) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			PreparedStatement prst = null;
			
			try {
				String strSql = "update tbl_bill_order_proc_result set proc_finish_time='"
								+ CommonTool.getFormatDateStr(new Date(), "yyyyMMddHHmmss")
								+ "', proc_status='"
								+ strProcStatus
								+ "' where processer_id='"
								+ strProcesserId
								+ "';";
				prst = conn.prepareStatement(strSql);
				prst.executeUpdate();
				conn.commit();
			} catch(SQLException se) {
				se.printStackTrace();
				MysqlConnectionPool.getInstance().rollback(conn);
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(prst, conn);
			}
		}
		
		/**
		 * ����΢�ŷ��ص�Ӧ�����ݣ�������ȷ���������ݿ������ݡ�
		 * @param strWxRespInfo
		 */
		private void parseRespInfoAndUpdateTbl(String strProcesserId, String strWxRespInfo) {
			if (strWxRespInfo.toLowerCase().startsWith("<xml>")) {	// ���ض��˵�����
				System.out.println("strWxRespInfo = " + strWxRespInfo);
				Map<String, String> mapWxRespInfo = new ParsingWXResponseXML().formatWechatXMLRespToMap(strWxRespInfo);
				String strReturnCode = mapWxRespInfo.get(DownloadBillOrderEntity.RETURN_CODE);
				if (strReturnCode != null && strReturnCode.equals(DownloadBillOrderEntity.FAIL)) {
					String strReturnMsg = mapWxRespInfo.get(DownloadBillOrderEntity.RETURN_MSG);
					System.out.println("���ض��˵�ʱ����������ϢΪ��" + strReturnMsg);
				}
				
				// ���¶��˵�������״̬Ϊ������ʧ�ܡ�
				this.updateBillProcInfoToTbl(strProcesserId, DownloadBillOrderEntity.BILL_PROC_FAIL);
			} else {	// �������˵���������, �����µ����ݿ���
				List<String> lstBillOrderData = this.getBillOrderData(strWxRespInfo);
				
				// ���ɸ���֧�����˵����˿����˵���SQL���
				String strBatchUpSql = this.getUpdateBillOrderBatchSql();
				boolean blnUpBillRst = updateBillOrderToTbl(strBatchUpSql, lstBillOrderData);
				
				String strBillProcRst = null;
				if (blnUpBillRst) {
					strBillProcRst = DownloadBillOrderEntity.BILL_PROC_SUCCESS;
				} else {
					strBillProcRst = DownloadBillOrderEntity.BILL_PROC_FAIL;
				}
				
				// ���¶��˵����ؼ����ʱ��ִ�н��
				this.updateBillProcInfoToTbl(strProcesserId, strBillProcRst);
			}
		}
		
		/**
		 * �������˵��������е����ݣ������ַ������ݽ��д洢��
		 * ÿ���ַ�����ʽΪ��data1, data2, data3, ... ...
		 * @param strWxRespInfo
		 * @return
		 */
		private List<String> getBillOrderData(String strWxRespInfo) {
			List<String> lstBillOrderData = new ArrayList<String>();
			BufferedReader br = new BufferedReader(new StringReader(strWxRespInfo));
			String strLine = "";
			while (true) {
				try {
					strLine = br.readLine();
					if (strLine == null) {
						break;
					} else {
						if (strLine.startsWith("`")) {	// ���˵��ڵ���ʵ����(������)
							String strOrderData = strLine.replaceAll("`", "");
							lstBillOrderData.add(strOrderData);
						} else if (strLine.toLowerCase().startsWith("Total transaction count".toLowerCase())) {	// ����ȡ���˵���������еĻ�������
							break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}
			
			return lstBillOrderData;
		}
		
		/**
		 * �������յĽ������΢�Ŷ��õ��Ķ��˵����ݣ������ݿ⡣
		 * @param strBatchUpSql
		 * @param lstBillOrderData
		 * @return
		 */
		private boolean updateBillOrderToTbl(String strBatchUpSql, List<String> lstBillOrderData) {
			boolean blnUpBillRst = false;
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			PreparedStatement prst = null;
			
			try {
				System.out.println("&&&strBatchUpSql = " + strBatchUpSql);
				prst = conn.prepareStatement(strBatchUpSql);
				
				int iBillDataSize = lstBillOrderData.size();
				for (int i = 0; i < iBillDataSize; i++) {
					String strLine = lstBillOrderData.get(i);
					
					String[] strValue = strLine.split(",");
					for (int j = 0; j < strValue.length; j++) {
						prst.setString(j + 1, strValue[j]);
					}
					prst.addBatch();
					
					// ÿ10000����¼ִ��һ��������
					if ((i + 1) % 10000 == 0) {
						prst.executeBatch();
						conn.commit();
						prst.clearBatch();
					}
				}
				// �ύ���һ�����
				prst.executeBatch();
				conn.commit();
				
				// ����������˵�������
				blnUpBillRst = true;
				
			} catch(SQLException se) {
				se.printStackTrace();
				MysqlConnectionPool.getInstance().rollback(conn);
				
				blnUpBillRst = false;
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(prst, conn);
			}
			
			return blnUpBillRst;
		}
		
		/**
		 * ���ɲ�ͬ�Ķ��˵�������Ҫ��SQL��䣨�������£���
		 * @return
		 */
		public abstract String getUpdateBillOrderBatchSql();
		
		/**
		 * ���ݶ������ͣ�֧���� or �˿��������Ѷ���ض��˵���Ϣ��
		 * @param strBillType
		 * @return
		 */
		private String getBillRespInfoFromWx(String strBillType, String strBillDate) {
			if (strBillType == null || "".equals(strBillType) || strBillDate == null && "".equals(strBillDate)) {
				return null;
			}
			
			// ����Ѷ��̨��������ǰ�������������
			Map<String, String> mapInquiryOrderArgs = new HashMap<String, String>();
			mapInquiryOrderArgs.put(DownloadBillOrderEntity.NONCE_STR, CommonTool.getRandomString(32));
			mapInquiryOrderArgs.put(DownloadBillOrderEntity.BILL_DATE, strBillDate);
			mapInquiryOrderArgs.put(DownloadBillOrderEntity.BILL_TYPE, strBillType);
			mapInquiryOrderArgs = CommonTool.getAppendMap(mapInquiryOrderArgs, CommonTool.getHarvestTransInfo());
			mapInquiryOrderArgs.put(DownloadBillOrderEntity.SIGN, CommonTool.getEntitySign(mapInquiryOrderArgs));
			
			// ����Ѷ��̨����ѯ����
			String strReqInfo = formatReqInfoToXML(mapInquiryOrderArgs);
			System.out.println("strReqInfo = " + strReqInfo);
			String strWxRespInfo = sendReqAndGetResp(DOWNLOAD_BILL_ORDER_URL, strReqInfo, CommonTool.getDefaultHttpClient());
			
			System.out.println(">>**strWxRespInfo = " + strWxRespInfo);
			
			return strWxRespInfo;
		}
	}
}
