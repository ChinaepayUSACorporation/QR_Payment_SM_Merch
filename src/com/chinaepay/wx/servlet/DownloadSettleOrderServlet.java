package com.chinaepay.wx.servlet;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.entity.DownloadSettleOrderEntity;

/**
 * ���ؽ��㵥��
 * @author xinwuhen
 */
public class DownloadSettleOrderServlet extends InquiryControllerServlet {
	private final String DOWNLOAD_SETTLE_ORDER_URL = CommonInfo.HONG_KONG_CN_SERVER_URL + "/pay/settlementquery";
	final long lngOneDay = 24 * 60 * 60 * 1000L;
	
	public void init() {
		try {
			super.init();
			
			// ��ȡ���ؽ��㵥��ָ��ʱ��
			String strHour = this.getServletContext().getInitParameter("Hour_DownloadSettle");
			String strMinute = this.getServletContext().getInitParameter("Minute_DownloadSettle");
			String strSecond = this.getServletContext().getInitParameter("Second_DownloadSettle");
	
			// �������ؽ��㵥�������߳�
			DownloadSettleOrderThread dsot = new DownloadSettleOrderThread(strHour, strMinute, strSecond);
			dsot.start();
			
		} catch (ServletException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Ϊǰ�˶��˽���Ԥ������֤�˶Խ��㵥���ֲ��ʱ��������ǰ�˽��淢�����ؽ��㵥�����롣
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		// ��ȡ��startTimeForTransOrderSucc���롾endTimeForTransOrderSucc��֮������гɹ���֧���� 
		String startTimeForSettleOrder = request.getParameter("startTimeForSettleOrder"); 
		String endTimeForSettleOrder = request.getParameter("endTimeForSettleOrder");
		ClosableTimer closableTimer = new ClosableTimer(true);	// ִ���������ر�Timer
		TimerTask task = new DownloadSettleOrderTask(closableTimer, startTimeForSettleOrder, endTimeForSettleOrder);
        closableTimer.schedule(task, 0L);
        
        // ��������ѯ������ص������ն�
 		String strDispatcherURL = "../sucess.jsp?result=" + DownloadSettleOrderEntity.SUCCESS + "&msg=���ؽ��㵥�������ύ��" ;
 		System.out.println("strDispatcherURL = " + strDispatcherURL);
 		try {
 			request.getRequestDispatcher(strDispatcherURL).forward(request, response);
 		} catch (ServletException | IOException e) {
 			e.printStackTrace();
 		}
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		doGet(request, response);
	}
	
	@Override
	public void updateInquiryRstToTbl(Map<String, String> mapArgs) {}
	
	/**
	 * �������ν��㵥��������̡߳�
	 * @author xinwuhen
	 */
	public class DownloadSettleOrderThread extends Thread {
		private String strHour = null;
		private String strMinute = null;
		private String strSecond = null;
		public DownloadSettleOrderThread(String strHour, String strMinute, String strSecond) {
			this.strHour = strHour;
			this.strMinute = strMinute;
			this.strSecond = strSecond;
		}
		
		
		public void run() {
			// ��ǰʱ��
			long lngNowMillSec = new Date().getTime();
			
			// ��ȡָ������������ʱ��
			Date defineDate = getFixDateBasedOnArgs(strHour, strMinute, strSecond);
			long lngDefMillSec = defineDate.getTime();
			
			ClosableTimer closableTimer = null;
			TimerTask task = null;
			// ��ǰʱ��������6��֮ǰ, ��Ҫ�ȵ�����6��ʱִ�����񣬲�������24Сʱ����ѯ����ִ����ͬ����
			if (lngNowMillSec < lngDefMillSec) {
				// ִ��һ������(����ץȡ���ݵ�ʱ��)���������������ر�
				closableTimer = new ClosableTimer(false);
		        task = new DownloadSettleOrderTask(closableTimer);
		        closableTimer.scheduleAtFixedRate(task, defineDate, lngOneDay);
			}
			// ��ǰʱ��������6��֮����Ҫ����ִ��һ�����񣬲��ڴ��յ�����6�㿪ʼִ��һ�Σ��Ժ�ÿ��24Сʱ����ѯ����ִ����ͬ����
			else {
				// ִ��һ������(����ץȡ���ݵ�ʱ��)���������������ر�
				closableTimer = new ClosableTimer(true);
				task = new DownloadSettleOrderTask(closableTimer);
				closableTimer.schedule(task, 0L);
				
				// �ڴ�������6�㿪ʼִ��һ������ �Ժ�ÿ��24Сʱ����ѯ����ִ����ͬ����
				closableTimer = new ClosableTimer(false);
				task = new DownloadSettleOrderTask(closableTimer);
				Date nextDay = CommonTool.getBefOrAftDate(defineDate, lngOneDay);
				closableTimer.scheduleAtFixedRate(task, nextDay, lngOneDay);
			}
		}
	}
	
	/**
	 * ���ؽ��㵥�����ࡣ
	 * @author xinwuhen
	 *
	 */
	public class DownloadSettleOrderTask extends TimerTask {
		private ClosableTimer closableTimer = null;
		private String startTimeForSettleOrder = null;
		private String endTimeForSettleOrder = null;
		
		public DownloadSettleOrderTask(ClosableTimer closableTimer) {
			super();
			this.closableTimer = closableTimer;
		}
		
		public DownloadSettleOrderTask(ClosableTimer closableTimer, String startTimeForSettleOrder, String endTimeForSettleOrder) {
			this(closableTimer);
			this.startTimeForSettleOrder = startTimeForSettleOrder;
			this.endTimeForSettleOrder = endTimeForSettleOrder;
		}
		
		/**
		 * ����Ѷ���ؽ��㵥��Ϣ��
		 */
		@Override
		public void run() {
			String strProcesserId = null;
			
			// ��ʼʱ��Ϊ��
			String strCurrentDay = CommonTool.getFormatDateStr(new Date(), "yyyyMMdd");
			if (startTimeForSettleOrder == null || "".equals(startTimeForSettleOrder)) {	// ��ʼʱ��Ϊ��
				if (endTimeForSettleOrder == null || "".equals(endTimeForSettleOrder)) {	// ����ʱ��Ϊ��ʱ��Ϊ������һ����ǰʱ��
					endTimeForSettleOrder = strCurrentDay;
				}
				
				startTimeForSettleOrder = endTimeForSettleOrder;
			} else {	// ��ʼʱ�䲻Ϊ��
				if (endTimeForSettleOrder == null || "".equals(endTimeForSettleOrder)) {	// ����ʱ��Ϊ��ʱ��Ϊ������һ����ǰʱ��
					endTimeForSettleOrder = strCurrentDay;
				}
			}
			
			System.out.println("startTimeForSettleOrder = " + startTimeForSettleOrder);
			System.out.println("endTimeForSettleOrder = " + endTimeForSettleOrder);
			
			// ����㵥���ؿ��������һ���¼�¼�����ڼ�¼���㵥�����ؼ��������
			strProcesserId = this.insertSettleProcInfoToTbl(startTimeForSettleOrder, endTimeForSettleOrder);
					
			// ����Ѷ��̨�������󣬲�����Ӧ����
			Map<String, Object> mapWxRespInfo = this.getSettleRespInfoFromWx(startTimeForSettleOrder, endTimeForSettleOrder, 
															DownloadSettleOrderEntity.HAS_SETTLED_INQUIRY);	// ֧���ɹ��Ķ�����֧������
			
			// ���½��㵥���ݵ����ݿ��
			boolean blnUpSettleRst = updateSettleOrderInquiryRst(mapWxRespInfo);
			
			String strSettleProcRst = null;
			if (blnUpSettleRst) {
				strSettleProcRst = DownloadSettleOrderEntity.SETTLE_PROC_SUCCESS;
			} else {
				strSettleProcRst = DownloadSettleOrderEntity.SETTLE_PROC_FAIL;
			}
			
			// ���½��㵥���ؿ�����Ĵ�����
			this.updateSettleProcInfoToTbl(strProcesserId, strSettleProcRst);
			
			// �ж��Ƿ���Ҫ�ر�����ʱ��
			if (closableTimer.isNeedClose()) {
				closableTimer.cancel();
			}
		}
		
		/**
		 * ��ѯָ������ʱ��������н��㵥ʱ���˷�����¼�ò�ѯ����Ĵ�����̡�
		 * @param startTimeForSettleOrder
		 * @param endTimeForSettleOrder
		 * @return
		 */
		private String insertSettleProcInfoToTbl(String startTimeForSettleOrder, String endTimeForSettleOrder) {
			String strProcesserId = "";
			
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			PreparedStatement prst = null;
			
			try {
				strProcesserId = CommonTool.getRandomString(16);
				String strSql = "replace into tbl_settle_order_proc_result(processer_id, mch_id, sett_start_date, "
								+ " sett_end_date, proc_start_time, proc_finish_time, proc_status) values('"
								+ strProcesserId
								+ "', '"
								+ CommonInfo.NOB_MCH_ID
								+ "', '"
								+ startTimeForSettleOrder
								+ "', '"
								+ endTimeForSettleOrder
								+ "', '"
								+ CommonTool.getFormatDateStr(new Date(), "yyyyMMddHHmmss")
								+ "', '', '"
								+ DownloadSettleOrderEntity.SETTLE_PROCESSING
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
		 * ����Ѷ��̨���Ͳ�ѯ���󣬲����ز�ѯ�����Ϣ��
		 * @param startTimeForSettleOrder
		 * @param endTimeForSettleOrder
		 * @param strInquiryType
		 * @return
		 */
		private Map<String, Object> getSettleRespInfoFromWx(String startTimeForSettleOrder, String endTimeForSettleOrder, String strInquiryType) {
			// ����Ѷ��̨��������ǰ�������������
			Map<String, String> mapInquiryOrderArgs = new HashMap<String, String>();
			
			Map<String, String> mapInquiryArgs = new HashMap<String, String>();
			mapInquiryArgs.put("mch_id", CommonInfo.NOB_MCH_ID);
			String strSubMchId = getTblFieldValue("sub_mch_id", "tbl_merch_submch", mapInquiryArgs);
			
			mapInquiryOrderArgs.put(DownloadSettleOrderEntity.SUB_MCH_ID, CommonTool.formatNullStrToSpace(strSubMchId));
			mapInquiryOrderArgs.put(DownloadSettleOrderEntity.USETAG, strInquiryType);
			mapInquiryOrderArgs.put(DownloadSettleOrderEntity.NONCE_STR, CommonTool.getRandomString(32));
			mapInquiryOrderArgs.put(DownloadSettleOrderEntity.OFFSET, "0");
			mapInquiryOrderArgs.put(DownloadSettleOrderEntity.LIMIT, "20");
			mapInquiryOrderArgs.put(DownloadSettleOrderEntity.DATE_START, startTimeForSettleOrder);
			mapInquiryOrderArgs.put(DownloadSettleOrderEntity.DATE_END, endTimeForSettleOrder);
			mapInquiryOrderArgs = CommonTool.getAppendMap(mapInquiryOrderArgs, CommonTool.getHarvestTransInfo());
			mapInquiryOrderArgs.put(DownloadSettleOrderEntity.SIGN, CommonTool.getEntitySign(mapInquiryOrderArgs));
			
			// ����Ѷ��̨����ѯ����
			String strReqInfo = formatReqInfoToXML(mapInquiryOrderArgs);
			System.out.println("strReqInfo = " + strReqInfo);
			String strWxRespInfo = sendReqAndGetResp(DOWNLOAD_SETTLE_ORDER_URL, strReqInfo, CommonTool.getDefaultHttpClient());
			System.out.println("strWxRespInfo = " + strWxRespInfo);
			Map<String, Object> mapWxRespInfo = new ParsingWXResponseXML().formatWechatXMLRespToMap(strWxRespInfo);
			
			return mapWxRespInfo;
		}
		
		/**
		 * ���½��㵥���ݵ����ݿ��
		 * @param mapWxRespInfo
		 * @return
		 */
		private boolean updateSettleOrderInquiryRst(Map<String, Object> mapWxRespInfo) {
			boolean blnUpRst = false;
			
			String strReturnCode = CommonTool.formatNullStrToSpace((String) mapWxRespInfo.get(DownloadSettleOrderEntity.RETURN_CODE));
			String strResultCode = CommonTool.formatNullStrToSpace((String) mapWxRespInfo.get(DownloadSettleOrderEntity.RESULT_CODE));
			
			if (strReturnCode.equals(DownloadSettleOrderEntity.FAIL) || strResultCode.equals(DownloadSettleOrderEntity.FAIL)) {
				blnUpRst = false;
				return blnUpRst;
			} else {
				Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
				PreparedStatement prst = null;
				
				try {
					String strSql = "replace into tbl_wx_calculated_settle_fee(mch_id, fbatchno, date_settlement, date_start, date_end,"
									+ " settlement_fee, unsettlement_fee, settlementfee_type, pay_fee, refund_fee, pay_net_fee, poundage_fee) "
									+ " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
					prst = conn.prepareStatement(strSql);
					String strRecdSize = CommonTool.formatNullStrToZero((String) mapWxRespInfo.get(DownloadSettleOrderEntity.RECORD_NUM));
					int iRecdSize = Integer.valueOf(strRecdSize);
					Map<String, String> mapSubInfo = null;
					for (int i = 0; i < iRecdSize; i++) {
						mapSubInfo = (Map<String, String>) mapWxRespInfo.get("setteinfo_" + i);
						prst.setString(1, CommonInfo.NOB_MCH_ID);
						prst.setString(2, mapSubInfo.get(DownloadSettleOrderEntity.FBATCHNO));
						prst.setString(3, mapSubInfo.get(DownloadSettleOrderEntity.DATE_SETTLEMENT));
						prst.setString(4, mapSubInfo.get(DownloadSettleOrderEntity.DATE_START));
						prst.setString(5, mapSubInfo.get(DownloadSettleOrderEntity.DATE_END));
						prst.setString(6, mapSubInfo.get(DownloadSettleOrderEntity.SETTLEMENT_FEE));
						prst.setString(7, mapSubInfo.get(DownloadSettleOrderEntity.UNSETTLEMENT_FEE));
						prst.setString(8, mapSubInfo.get(DownloadSettleOrderEntity.SETTLEMENTFEE_TYPE));
						prst.setString(9, mapSubInfo.get(DownloadSettleOrderEntity.PAY_FEE));
						prst.setString(10, mapSubInfo.get(DownloadSettleOrderEntity.REFUND_FEE));
						prst.setString(11, mapSubInfo.get(DownloadSettleOrderEntity.PAY_NET_FEE));
						prst.setString(12, mapSubInfo.get(DownloadSettleOrderEntity.POUNDAGE_FEE));
						prst.addBatch();
					}
					
					prst.executeBatch();
					conn.commit();
					blnUpRst = true;
					
				} catch(SQLException se) {
					se.printStackTrace();
					MysqlConnectionPool.getInstance().rollback(conn);
					
					blnUpRst = false;
				} finally {
					MysqlConnectionPool.getInstance().releaseConnInfo(prst, conn);
				}
			}
			
			return blnUpRst;
		}
		
		/**
		 * ���½��㵥���ؿ�����Ĵ�������
		 * @param strProcesserId
		 * @param strSettleProcRst
		 */
		private void updateSettleProcInfoToTbl(String strProcesserId, String strSettleProcRst) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			PreparedStatement prst = null;
			
			try {
				String strSql = "update tbl_settle_order_proc_result set proc_finish_time='"
								+ CommonTool.getFormatDateStr(new Date(), "yyyyMMddHHmmss")
								+ "', proc_status='"
								+ strSettleProcRst
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
	}
	
	public class ParsingWXResponseXML {
		private Map<String, Object> mapWXRespResult = new HashMap<String, Object>();

		/**
		 * ����XML��������Map�С�
		 * @param strWxResponseResult
		 * @return
		 * @throws ParserConfigurationException 
		 * @throws IOException 
		 * @throws SAXException 
		 */
		public Map<String, Object> formatWechatXMLRespToMap(String strWxResponseResult) {
			if (strWxResponseResult == null || "".equals(strWxResponseResult)) {
				return null;
			}
			
			if (strWxResponseResult.toLowerCase().startsWith("<xml>")) {
				DocumentBuilderFactory docBuilderFact = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = null;
				Document document = null;
				
				try {
					docBuilder = docBuilderFact.newDocumentBuilder();
					document = docBuilder.parse(new InputSource(new StringReader(strWxResponseResult)));
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// ����XML��ʽ���ַ����������ַ������ԡ���-ֵ���Ե���ʽ��ӵ�MAP�С�
				if (document != null) {
					appendElementNameAndValue(document);
				}
			}
			
			return mapWXRespResult;
		}
		
		/**
		 * ȡ��Ԫ�ؽڵ�Ľڵ������ڵ�ֵ��
		 * @param node
		 * @return
		 */
		private void appendElementNameAndValue(Node node) {
			if (node != null) {  // �жϽڵ��Ƿ�Ϊ��
				if (node.hasChildNodes()) {	// ��Ԫ�ؽڵ��»����ӽڵ�
					NodeList nodeList = node.getChildNodes();
					String strNodeName = node.getNodeName();
					Map<String, String> mapSubInfo = new HashMap<String, String>();
					if (strNodeName.startsWith("setteinfo_")) {
						for (int i = 0; i < nodeList.getLength(); i++) {
							Node childNode = nodeList.item(i);
							mapSubInfo.put(childNode.getNodeName(), childNode.getChildNodes().item(0).getNodeValue());
						}
						mapWXRespResult.put(strNodeName, mapSubInfo);
					} else {
						for (int i = 0; i < nodeList.getLength(); i++) {
							Node childNode = nodeList.item(i);
							appendElementNameAndValue(childNode);
						}
					}
				} else {	// ��Ԫ�ؽڵ����Ѿ�û���ӽڵ�
					Node nodeParent = null;
					if ((nodeParent = node.getParentNode()) != null) {
						mapWXRespResult.put(nodeParent.getNodeName(), node.getNodeValue());
					}
				}
			}
		}
	}
}
