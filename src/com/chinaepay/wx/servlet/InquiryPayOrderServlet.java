/**
 * @author xinwuhen
 */
package com.chinaepay.wx.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import javax.servlet.ServletException;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.entity.PayOrderEntiry;

/**
 * ��ѯ���׵���Ϣ�ӿڡ�
 * @author xinwuhen
 */
public class InquiryPayOrderServlet extends InquiryControllerServlet {
	private final String INQUIRY_PAY_ORDER_URL = CommonInfo.HONG_KONG_CN_SERVER_URL + "/pay/orderquery";
	
	// �̳߳ض���
	private static ThreadPoolExecutor threadPoolExecutor = null;
	
	// �����̴߳�С
	private static final int iCorePoolSize = 500;
	// ����߳���
	private static final int intMaxPoolSize = 3000;
	// ��ǰ�̳߳����߳����������������̴߳�Сʱ�������߳���������ʱ��ֵ����λ���룩
	private static final int intKeepAliveTime = 1 * 60;	// 1����
	// ������е����ֵ
	private static final int intTaskQueueSize = Integer.MAX_VALUE;
	
	public void init() {
		try {
			super.init();
			
			// �����̳߳أ�ÿ���߳�����ִ��֧�������Ĳ�ѯ�Լ�Ӧ���ĸ��µ����ݵĲ���
			if (threadPoolExecutor == null) {
				threadPoolExecutor = super.getThreadPoolExecutor(iCorePoolSize, intMaxPoolSize, intKeepAliveTime, intTaskQueueSize);
			}
			
			// ����У��֧������״̬�����߳�
			ValidatePayOrderThread vlidPayOrderThread = new ValidatePayOrderThread(threadPoolExecutor);
			vlidPayOrderThread.start();
		} catch (ServletException e) {
			e.printStackTrace();
		}
	}
	
//	public void doGet(HttpServletRequest request, HttpServletResponse response) {
//		// �̻�������
//		String strOutTradeNo = request.getParameter("out_trade_no");
//		
//		// ����Ѷ��̨����֧������ѯ���󣬲����ݻ�õ�Ӧ���ĸ��������Ϣ
//		Map<String, String> mapWxRespInfo = this.getAndUpdatePayOrderRespInfo(strOutTradeNo);
//		
//		// ��������ѯ������ص������ն�
//		String strDispatcherURL = null;
//		String strReturnCode = CommonTool.formatNullStrToSpace(mapWxRespInfo.get(PayOrderEntiry.RETURN_CODE));
//		String strResultCode = CommonTool.formatNullStrToSpace(mapWxRespInfo.get(PayOrderEntiry.RESULT_CODE));
//		String strTradeStatus = CommonTool.formatNullStrToSpace(mapWxRespInfo.get(PayOrderEntiry.TRADE_STATE));
//		if (strReturnCode.equals(PayOrderEntiry.SUCCESS) 
//				&& strResultCode.equals(PayOrderEntiry.SUCCESS)
//				&& strTradeStatus.equals(PayOrderEntiry.SUCCESS)) {
//			strDispatcherURL = "../sucess.jsp?outTradeNo=" + CommonTool.formatNullStrToSpace(strOutTradeNo) 
//								+ "&paymentResult=" + PayOrderEntiry.SUCCESS + "&msg=" + mapWxRespInfo.get(PayOrderEntiry.TRADE_STATE_DESC);
//		} else if (strReturnCode.equals(PayOrderEntiry.FAIL)) {
//			strDispatcherURL = "../error.jsp?msg=" + mapWxRespInfo.get(PayOrderEntiry.RETURN_MSG);
//		} else if (strResultCode.equalsIgnoreCase(PayOrderEntiry.FAIL)) {
//			strDispatcherURL = "../error.jsp?msg=" + mapWxRespInfo.get(PayOrderEntiry.ERR_CODE_DES);
//		} else {
//			strDispatcherURL = "../error.jsp?msg=" + mapWxRespInfo.get(PayOrderEntiry.TRADE_STATE_DESC);
//		}
//		
//		System.out.println("strDispatcherURL = " + strDispatcherURL);
//		try {
//			request.getRequestDispatcher(strDispatcherURL).forward(request, response);
//		} catch (ServletException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
//	public void doPost(HttpServletRequest request, HttpServletResponse response) {
//		this.doGet(request, response);
//	}

	@Override
	public void updateInquiryRstToTbl(Map<String, String> mapArgs) {
		if (mapArgs == null || mapArgs.size() == 0) {
			return;
		}
		
		Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
		PreparedStatement prst = null;
		
		try {
			String strSql = "update tbl_trans_order set transaction_id='" 
							+ mapArgs.get(PayOrderEntiry.TRANSACTION_ID) 
							+ "', time_end='"
							+ mapArgs.get(PayOrderEntiry.TIME_END)
							+"', trade_state='"
							+ mapArgs.get(PayOrderEntiry.TRADE_STATE)
							+ "', trade_state_desc='"
							+ mapArgs.get(PayOrderEntiry.TRADE_STATE_DESC) 
							+ "', bank_type='"
							+ mapArgs.get(PayOrderEntiry.BANK_TYPE) 
							+ "', cash_fee='"
							+ mapArgs.get(PayOrderEntiry.CASH_FEE) 
							+ "', cash_fee_type='"
							+ mapArgs.get(PayOrderEntiry.CASH_FEE_TYPE) 
							+ "', rate='"
							+ mapArgs.get(PayOrderEntiry.RATE) 
							+ "' where out_trade_no='"
							+ mapArgs.get(PayOrderEntiry.OUT_TRADE_NO)
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
	 * ����Ѷ��̨����֧������ѯ���󣬲����ݻ�õ�Ӧ���ĸ��������Ϣ��
	 * @param strOutTradeNo
	 */
	private Map<String, String> getPayOrderRespInfo(String strOutTradeNo) {
		// ͨ���̻������ţ���ȡ���̻�ID
		Map<String, String> mapArgs = new HashMap<String, String>();
		mapArgs.put("out_trade_no", strOutTradeNo);
		String strSubMerchId = super.getTblFieldValue("sub_mch_id", "tbl_submch_trans_order", mapArgs);
//		String strTransactionId = super.getTblFieldValue("transaction_id", "tbl_trans_order", mapArgs);
		
		// ����Ѷ��̨��������ǰ�������������
		Map<String, String> mapInquiryOrderArgs = new HashMap<String, String>();
		mapInquiryOrderArgs.put(PayOrderEntiry.SUB_MCH_ID, CommonTool.formatNullStrToSpace(strSubMerchId));
//		mapInquiryOrderArgs.put(PayOrderEntiry.TRANSACTION_ID, CommonTool.formatNullStrToSpace(strTransactionId));
		mapInquiryOrderArgs.put(PayOrderEntiry.OUT_TRADE_NO, CommonTool.formatNullStrToSpace(strOutTradeNo));
		mapInquiryOrderArgs.put(PayOrderEntiry.NONCE_STR, CommonTool.getRandomString(32));
		mapInquiryOrderArgs = CommonTool.getAppendMap(mapInquiryOrderArgs, CommonTool.getHarvestTransInfo());
		mapInquiryOrderArgs.put(PayOrderEntiry.SIGN, CommonTool.getEntitySign(mapInquiryOrderArgs));
		
		// ����Ѷ��̨����ѯ����
		String strReqInfo = super.formatReqInfoToXML(mapInquiryOrderArgs);
		System.out.println("strReqInfo >>>>>= " + strReqInfo);
		String strWxRespInfo = super.sendReqAndGetResp(INQUIRY_PAY_ORDER_URL, strReqInfo, CommonTool.getDefaultHttpClient());
		System.out.println("strWxRespInfo> = " + strWxRespInfo);
		Map<String, String> mapWxRespInfo = new ParsingWXResponseXML().formatWechatXMLRespToMap(strWxRespInfo);
		System.out.println("<<>>>>>mapWxRespInfo = " + mapWxRespInfo);
		
		return mapWxRespInfo;
	}
	
	/**
	 * ��֧�������ѯ״̬Ϊ��δ֧����֧���С��Ķ���ID��
	 * @param strField
	 * @param strTblName
	 * @return
	 */
	private List<String> getNoOrPayingOutTradeNo(String strField, String strTblName) {
		List<String[]> listArgs = new ArrayList<String[]>();
		
		// ��֧�������ѯ״̬Ϊ��δ֧������֧����ID
		listArgs.add(new String[] {"trade_state", "=", PayOrderEntiry.NOTPAY});
//		Date newDate = new Date(new Date().getTime() - 3 * 60 * 60 * 1000L);	// ֻȡ3Сʱ֮�ڵĶ���
//		listArgs.add(new String[] {"time_start", ">", CommonTool.getFormatDateStr(newDate, "yyyyMMddHHmmss")});	
		List<String> lstNoPayOutTradeNo = super.getTblFieldValueList(strField, strTblName, listArgs);
		
		// ��֧�������ѯ״̬Ϊ��֧���С���֧����ID
		listArgs.clear();
		listArgs.add(new String[] {"trade_state", "=", PayOrderEntiry.USERPAYING});
//		listArgs.add(new String[] {"time_start", ">", CommonTool.getFormatDateStr(newDate, "yyyyMMddHHmmss")});
		List<String> lstPayingOutTradeNo = super.getTblFieldValueList(strField, strTblName, listArgs);
		
		List<String> listNew = CommonTool.getCloneList(lstNoPayOutTradeNo);
		return CommonTool.getAppendList(listNew, lstPayingOutTradeNo);
	}
	
	/**
	 * У��֧������״̬�����̡߳�
	 * @author xinwuhen
	 */
	public class ValidatePayOrderThread extends Thread {
		private ThreadPoolExecutor threadPoolExecutor = null;
		
		public ValidatePayOrderThread(ThreadPoolExecutor threadPoolExecutor) {
			this.threadPoolExecutor = threadPoolExecutor;
		}
		
		public void run() {
			// ����ִ�з���������֧����״̬ȷ�ϲ���(����Ѷ��̨��ѯ֧����״̬)
			while (true) {
				// ��֧�������ѯ״̬Ϊ��δ֧����֧���С��Ķ���ID
				List<String> listOutTradeNo = getNoOrPayingOutTradeNo("out_trade_no", "tbl_trans_order");
				System.out.println("listOutTradeNo = " + listOutTradeNo);
				
				// ����֧������ѯ�̣߳��߳������֧����״̬��ѯ������״̬���µ�֧������
				if (listOutTradeNo != null) {
					for (String strOutTradeNo : listOutTradeNo) {
						threadPoolExecutor.execute(new ValidPaymentOrderRunnable(strOutTradeNo));
					}
				}
				
				// 1����ִ��һ��
				try {
					Thread.currentThread().sleep(INQUIRY_ORDER_WAIT_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * У��֧����״̬��Runnable�ӿڡ�
	 * @author xinwuhen
	 */
	public class ValidPaymentOrderRunnable implements Runnable {
		private String strOutTradeNo = null;
		
		public ValidPaymentOrderRunnable(String strOutTradeNo) {
			this.strOutTradeNo = strOutTradeNo;
		}
		
		@Override
		public void run() {
			// ����Ѷ��̨����֧������ѯ���󣬲����Ӧ����
			Map<String, String> mapWxRespInfo = getPayOrderRespInfo(strOutTradeNo);
			
			// �������ݿ���֧������ص���Ϣ
			updateInquiryRstToTbl(mapWxRespInfo);
			
			// ��Ѷ�෵�ص�֧����״̬��ȻΪ��δ֧������֧���С�ʱ�������Ѿ�����������Чʱ��(time_expire)������ùص��ӿڣ���֧�������йر�
			closeTimeOutPayOrder(mapWxRespInfo);
		}
		
		/**
		 * ��Ѷ�෵�ص�֧����״̬��ȻΪ��δ֧������֧���С�ʱ�������Ѿ�����������Чʱ��(time_expire)������ùص��ӿڣ���֧�������йرա�
		 * @param strTradeStatus	��Ѷ�෵�ص�����֧����״̬��
		 */
		private void closeTimeOutPayOrder(Map<String, String> mapWxRespInfo) {
			String strReturnCode = mapWxRespInfo.get(PayOrderEntiry.RETURN_CODE);
			String strResultCode = mapWxRespInfo.get(PayOrderEntiry.RESULT_CODE);
			
			if (strReturnCode == null || "".equals(strReturnCode) || !strReturnCode.equals(PayOrderEntiry.SUCCESS)) {	
				return;
			} else {	// ReturnCode״̬ΪSUCESS
				Map<String, String> mapArgs = new HashMap<String, String>();
				mapArgs.put("out_trade_no", strOutTradeNo);
				String strSubMerchId = getTblFieldValue("sub_mch_id", "tbl_submch_trans_order", mapArgs);
				String strOrderTimeExpire = getTblFieldValue("time_expire", "tbl_trans_order", mapArgs);
				
				long lngOrderTimeExpire = CommonTool.getDateBaseOnChars("yyyyMMddHHmmss", strOrderTimeExpire).getTime();
				long lngCurrTime = new Date().getTime();
				System.out.println("lngCurrTime = " + lngCurrTime);
				System.out.println("lngOrderTimeExpire = " + lngOrderTimeExpire);
				
				if (strResultCode == null || "".equals(strResultCode) || !strResultCode.equals(PayOrderEntiry.SUCCESS)) {
					String strErrCode = mapWxRespInfo.get(PayOrderEntiry.ERR_CODE);
					if (strErrCode != null && strErrCode.equals(PayOrderEntiry.ORDERNOTEXIST)) {	// ���׶����Ų����ڣ���APIֻ�ܲ��ύ֧�����׷��سɹ��Ķ��������̻������Ҫ��ѯ�Ķ������Ƿ���ȷ
						if (lngCurrTime > lngOrderTimeExpire) {	// ��ǰʱ�䳬��֧������Чʱ�䣬���ùرն����ӿ�
							// ���ùص��ӿ�
							new CloseOrderServlet().sendCloseReqAndUpdateTbl(strSubMerchId, strOutTradeNo);
						}
					}
				} else {	// ResultCode״̬ΪSUCESS
					String strTradeStatus =  mapWxRespInfo.get(PayOrderEntiry.TRADE_STATE);
					if (strTradeStatus != null && (strTradeStatus.equals(PayOrderEntiry.NOTPAY) || strTradeStatus.equals(PayOrderEntiry.USERPAYING))) {
						if (lngCurrTime > lngOrderTimeExpire) {	// ��ǰʱ�䳬��֧������Чʱ�䣬���ùرն����ӿ�
							// ���ùص��ӿ�
							new CloseOrderServlet().sendCloseReqAndUpdateTbl(strSubMerchId, strOutTradeNo);
						}
					}
				}
			}
		}
	}
}
