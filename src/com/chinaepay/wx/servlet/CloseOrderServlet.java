package com.chinaepay.wx.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.entity.CloseOrderEntity;

/**
 * �����رսӿڡ����������Ҫ���ùص��ӿڣ�
 * �̻�����֧��ʧ����Ҫ�����µ������·���֧����Ҫ��ԭ�����ŵ��ùص��������ظ�֧����ϵͳ�µ����û�֧����ʱ��ϵͳ�˳��������������û�����������ùص��ӿڡ�
 * @author xinwuhen
 *
 */
public class CloseOrderServlet extends TransControllerServlet {
	private final String CLOSE_ORDER_URL = CommonInfo.HONG_KONG_CN_SERVER_URL + "/pay/closeorder";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String strSubMerchId = request.getParameter("sub_mch_id");
		String strOutTradeNo = request.getParameter("out_trade_no");
		System.out.println("strSubMerchId = " + strSubMerchId);
		System.out.println("strOutTradeNo = " + strOutTradeNo);
		
		// ����Ѷ��̨���͹رն������󣬲�����Ӧ�����������ݿ�
		this.sendCloseReqAndUpdateTbl(strSubMerchId, strOutTradeNo);
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		doGet(request, response);
	}
	
	@Override
	public boolean insertOrderInfoToTbl(Map<String, String> mapArgs) {
		return true;
	}
	
	/**
	 * ����Ѷ��̨���͹رն������󣬲�����Ӧ�����������ݿ⡣
	 * @param strSubMerchId
	 * @param strOutTradeNo
	 */
	public void sendCloseReqAndUpdateTbl(String strSubMerchId, String strOutTradeNo) {
		/** �������������� **/
		Map<String, String> mapCloseOrderReqInfo = new HashMap<String, String>();
		mapCloseOrderReqInfo.put(CloseOrderEntity.SUB_MCH_ID, CommonTool.formatNullStrToSpace(strSubMerchId));
		mapCloseOrderReqInfo.put(CloseOrderEntity.OUT_TRADE_NO, CommonTool.formatNullStrToSpace(strOutTradeNo));
		mapCloseOrderReqInfo.put(CloseOrderEntity.NONCE_STR, CommonTool.getRandomString(32));
		mapCloseOrderReqInfo = CommonTool.getAppendMap(mapCloseOrderReqInfo, CommonTool.getHarvestTransInfo());
		mapCloseOrderReqInfo.put(CloseOrderEntity.SIGN, CommonTool.getEntitySign(mapCloseOrderReqInfo));
		
		/** ��ʽ������������ΪXML��ʽ **/
		String strReqInfo = super.formatReqInfoToXML(mapCloseOrderReqInfo);
		System.out.println("cls_strReqInfo = " + strReqInfo);
		
		/** ����Ѷ��̨���ͱ���,������Ӧ���� **/
		String strWxRespInfo = super.sendReqAndGetResp(CLOSE_ORDER_URL, strReqInfo, CommonTool.getDefaultHttpClient());
		System.out.println("cls_strWxRespInfo = " + strWxRespInfo);
		Map<String, String> mapWxRespInfo = new ParsingWXResponseXML().formatWechatXMLRespToMap(strWxRespInfo);
		System.out.println("cls_mapWxRespInfo = " + mapWxRespInfo);
		
		/** ����Ӧ���ģ������ص�������µ���̨���ݿ� **/
		mapWxRespInfo.put(CloseOrderEntity.OUT_TRADE_NO, CommonTool.formatNullStrToSpace(strOutTradeNo));
		updateOrderRstToTbl(mapWxRespInfo);
	}

	@Override
	public boolean updateOrderRstToTbl(Map<String, String> mapArgs) {
		boolean blnUpRst = false;
		String strReturnCode = mapArgs.get(CloseOrderEntity.RETURN_CODE);
		String strResultCode = mapArgs.get(CloseOrderEntity.RESULT_CODE);
		String strOutTradeNo = mapArgs.get(CloseOrderEntity.OUT_TRADE_NO);
		
		if (CommonTool.formatNullStrToSpace(strReturnCode).equals(CloseOrderEntity.SUCCESS) 
				&& CommonTool.formatNullStrToSpace(strResultCode).equals(CloseOrderEntity.SUCCESS)) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			PreparedStatement prst = null;
			String strSql = "update tbl_trans_order set trade_state='" + CloseOrderEntity.CLOSED + "', trade_state_desc='�ѹر�' "
							+ " where out_trade_no='" + CommonTool.formatNullStrToSpace(strOutTradeNo) + "';";
			System.out.println("+-=+strSql = " + strSql);
			try {
				prst = conn.prepareStatement(strSql);
				prst.execute();
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
}
