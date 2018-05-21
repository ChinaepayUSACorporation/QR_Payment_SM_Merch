package com.chinaepay.wx.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import javax.servlet.ServletException;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.entity.RefundOrderEntity;

/**
 * ��ѯ�˿��Ϣ�ӿڡ�
 * @author xinwuhen
 */
public class InquiryRefundOrderServlet extends InquiryControllerServlet {
	private final String INQUIRY_REFUND_ORDER_URL = CommonInfo.HONG_KONG_CN_SERVER_URL + "/pay/refundquery";
	
	// �̳߳ض���
	private static ThreadPoolExecutor threadPoolExecutor = null;
	
	// �����̴߳�С
	private static final int iCorePoolSize = 200;
	// ����߳���
	private static final int intMaxPoolSize = 3000;
	// ��ǰ�̳߳����߳����������������̴߳�Сʱ�������߳���������ʱ��ֵ����λ���룩
	private static final int intKeepAliveTime = 1 * 60;	// 1����
	// ������е����ֵ
	private static final int intTaskQueueSize = Integer.MAX_VALUE;
	
	public void init() {
		try {
			super.init();
			
			// �����̳߳أ�ÿ���߳�����ִ���˿�Ĳ�ѯ�Լ�Ӧ���ĸ��µ����ݵĲ���
			if (threadPoolExecutor == null) {
				threadPoolExecutor = super.getThreadPoolExecutor(iCorePoolSize, intMaxPoolSize, intKeepAliveTime, intTaskQueueSize);
			}
			
			// ����У���˿״̬�����߳�
			ValidateRefundOrderThread vlidRefundOrderThread = new ValidateRefundOrderThread(threadPoolExecutor);
			vlidRefundOrderThread.start();
		} catch (ServletException e) {
			e.printStackTrace();
		}
	}
	
//	public void doGet(HttpServletRequest request, HttpServletResponse response) {
//		// ��ȡ�˿��
//		String strOutRefundNo = request.getParameter("out_refund_no");
//		
//
//		// ����Ѷ��̨�����˿��ѯ���󣬲����ݻ�õ�Ӧ���ĸ��������Ϣ
//		Map<String, String> mapWxRespInfo = this.getAndUpdateRefundOrderRespInfo(strOutRefundNo);
//		
//		/**
//		// ��������ѯ������ص������ն�
//		String strDispatcherURL = null;
//		String strReturnCode = CommonTool.formatNullStrToSpace(mapWxRespInfo.get(RefundOrderEntity.RETURN_CODE));
//		String strResultCode = CommonTool.formatNullStrToSpace(mapWxRespInfo.get(RefundOrderEntity.RESULT_CODE));
//		String strTradeStatus = CommonTool.formatNullStrToSpace(mapWxRespInfo.get(RefundOrderEntity.TRADE_STATE));
//		if (strReturnCode.equals(RefundOrderEntity.SUCCESS) 
//				&& strResultCode.equals(RefundOrderEntity.SUCCESS)
//				&& strTradeStatus.equals(RefundOrderEntity.SUCCESS)) {
//			strDispatcherURL = "../sucess.jsp?outRefundNo=" + CommonTool.formatNullStrToSpace(strOutRefundNo) 
//								+ "&refundResult=" + RefundOrderEntity.SUCCESS + "&msg=�˿�ɹ���";
//		} else if (strReturnCode.equals(RefundOrderEntity.FAIL)) {
//			strDispatcherURL = "../error.jsp?msg=" + mapWxRespInfo.get(RefundOrderEntity.RETURN_MSG);
//		} else if (strResultCode.equalsIgnoreCase(RefundOrderEntity.FAIL)) {
//			strDispatcherURL = "../error.jsp?msg=" + mapWxRespInfo.get(RefundOrderEntity.ERR_CODE_DES);
//		} else {
//			strDispatcherURL = "../error.jsp?msg=���ݴ���";
//		}
//		
//		try {
//			System.out.println("strDispatcherURL = " + strDispatcherURL);
//			request.getRequestDispatcher(strDispatcherURL).forward(request, response);
//		} catch (ServletException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		**/
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
			String strSql = "update tbl_refund_order set transaction_id='"
							+ mapArgs.get(RefundOrderEntity.TRANSACTION_ID) 
							+ "', refund_account='"
							+ mapArgs.get(RefundOrderEntity.REFUND_ACCOUNT + "_0") 
							+ "', refund_count='" 
							+ mapArgs.get(RefundOrderEntity.REFUND_COUNT) 
							+ "', refund_channel='"
							+ mapArgs.get(RefundOrderEntity.REFUND_CHANNEL + "_0") 
							+ "', refund_status='"
							+ mapArgs.get(RefundOrderEntity.REFUND_STATUS + "_0") 	// ���ڲ�ѯʱ�����������out_refund_no�����Դ˴�������Ψһ���˿��Ϣ
							+ "', refund_recv_accout='"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.REFUND_RECV_ACCOUT + "_0"))
							+ "', refund_success_time='"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.REFUND_SUCCESS_TIME + "_0"))
							+ "', rate='"
							+ mapArgs.get(RefundOrderEntity.RATE)
							+ "' where out_refund_no='"
							+ mapArgs.get(RefundOrderEntity.OUT_REFUND_NO + "_0")
							+ "';";
			System.out.println(">><<>>strSql = " + strSql);
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
	 * ����Ѷ��̨�����˿��ѯ���󣬲����ݻ�õ�Ӧ���ĸ��������Ϣ��
	 * @param strOutRefundNo
	 * @return
	 */
	private Map<String, String> getRefundOrderRespInfo(String strOutRefundNo) {
		Map<String, String> mapArgs = new HashMap<String, String>();
		mapArgs.put("out_refund_no", strOutRefundNo);
		// ��ȡ�̻�������
		String strOutTradeNo = super.getTblFieldValue("out_trade_no", "tbl_trans_order_refund_order", mapArgs);
		
		mapArgs.clear();
		mapArgs.put("out_trade_no", strOutTradeNo);
		// ��ȡ���̻���
		String strSubMerchId = super.getTblFieldValue("sub_mch_id", "tbl_submch_trans_order", mapArgs);
		
		// ����Ѷ��̨��������ǰ�������������
		Map<String, String> mapInquiryOrderArgs = new HashMap<String, String>();
		mapInquiryOrderArgs.put(RefundOrderEntity.SUB_MCH_ID, CommonTool.formatNullStrToSpace(strSubMerchId));
		mapInquiryOrderArgs.put(RefundOrderEntity.NONCE_STR, CommonTool.getRandomString(32));
		mapInquiryOrderArgs.put(RefundOrderEntity.OUT_TRADE_NO, CommonTool.formatNullStrToSpace(strOutTradeNo));
		mapInquiryOrderArgs.put(RefundOrderEntity.OUT_REFUND_NO, CommonTool.formatNullStrToSpace(strOutRefundNo));
		mapInquiryOrderArgs = CommonTool.getAppendMap(mapInquiryOrderArgs, CommonTool.getHarvestTransInfo());
		mapInquiryOrderArgs.put(RefundOrderEntity.SIGN, CommonTool.getEntitySign(mapInquiryOrderArgs));
		
		// ����Ѷ��̨����ѯ����
		String strReqInfo = super.formatReqInfoToXML(mapInquiryOrderArgs);
		System.out.println("!!!!strReqInfo = " + strReqInfo);
		String strWxRespInfo = super.sendReqAndGetResp(INQUIRY_REFUND_ORDER_URL, strReqInfo, CommonTool.getDefaultHttpClient());
		System.out.println("****strWxRespInfo = " + strWxRespInfo);
		Map<String, String> mapWxRespInfo = new ParsingWXResponseXML().formatWechatXMLRespToMap(strWxRespInfo);
		System.out.println("@@@@strWxRespInfo = " + strWxRespInfo);
		
		return mapWxRespInfo;
	}
	
	/**
	 * ���˿���ѯ״̬Ϊ���˿���С��Ķ���ID��
	 * @param strField
	 * @param strTblName
	 * @return
	 */
	private List<String> getRefundProcessingOutRefundNo(String strField, String strTblName) {
		List<String[]> listArgs = new ArrayList<String[]>();
		
		listArgs.add(new String[] {"refund_status", "=", RefundOrderEntity.PROCESSING});
		
		Date newDate =CommonTool.getBefOrAftDate(new Date(), Calendar.YEAR, -1);	// ��Ѷ����ڽ���ʱ�䳬��һ��Ķ����޷��ύ�˿
		listArgs.add(new String[] {"refund_trans_time", ">", CommonTool.getFormatDateStr(newDate, "yyyyMMddHHmmss")});	
		List<String> lstRefundNo = super.getTblFieldValueList(strField, strTblName, listArgs);
		
		return lstRefundNo;
	}
	
	/**
	 * У���˿״̬�����̡߳�
	 * @author xinwuhen
	 */
	public class ValidateRefundOrderThread extends Thread {
		private ThreadPoolExecutor threadPoolExecutor = null;
		
		public ValidateRefundOrderThread(ThreadPoolExecutor threadPoolExecutor) {
			this.threadPoolExecutor = threadPoolExecutor;
		}
		
		public void run() {
			// ����ִ�з����������˿״̬ȷ�ϲ���(����Ѷ��̨��ѯ�˿״̬)
			while (true) {
				// ���˿���ѯ״̬Ϊ���˿���С����˿ID
				List<String> listOutRefundNo = getRefundProcessingOutRefundNo("out_refund_no", "tbl_refund_order");
				System.out.println("listOutRefundNo = " + listOutRefundNo);
				
				// �����˿��ѯ�̣߳��߳�������˿״̬��ѯ������״̬���µ��˿��
				if (listOutRefundNo != null) {
					for (String strRefundNo : listOutRefundNo) {
						threadPoolExecutor.execute(new ValidRefundOrderRunnable(strRefundNo));
					}
				}
				
				// 30����һ�Σ����Ƿ��˿����
				try {
					Thread.currentThread().sleep(INQUIRY_ORDER_WAIT_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * У���˿״̬��Runnable�ӿڡ�
	 * @author xinwuhen
	 */
	public class ValidRefundOrderRunnable implements Runnable {
		private String strRefundNo = null;
		
		public ValidRefundOrderRunnable(String strRefundNo) {
			this.strRefundNo = strRefundNo;
		}
		
		@Override
		public void run() {
			// ����Ѷ��̨�����˿��ѯ���󣬲����ݻ�õ�Ӧ���ĸ��������Ϣ
			Map<String, String> mapWxRespInfo = getRefundOrderRespInfo(strRefundNo);
			
			// �������ݿ����˿��ص���Ϣ
			updateInquiryRstToTbl(mapWxRespInfo);
		}
	}
}
