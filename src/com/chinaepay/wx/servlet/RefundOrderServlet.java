package com.chinaepay.wx.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.CloseableHttpClient;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.JsonRespObj;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.entity.PayOrderEntiry;
import com.chinaepay.wx.entity.RefundOrderEntity;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * �˿�ҵ�����ࡣ
 * @author xinwuhen
 */
public class RefundOrderServlet extends TransControllerServlet {
	private final String REFUND_ORDER_URL = CommonInfo.HONG_KONG_CN_SERVER_URL + "/secapi/pay/refund";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String strOutTradeNo = request.getParameter("out_trade_no");
		String strRefundFee = request.getParameter("refund_fee");
		String strRefundDesc = CommonTool.transferCharactor(request.getParameter("refund_desc"), "ISO-8859-1", "UTF-8");
		
		System.out.println("###strRefundDesc = " + strRefundDesc);
		
		
		JsonRespObj respObj = new JsonRespObj();
		String strRefundResult = "1";
		String strReturnMsg = "";
		JSONArray jsonArray = null;
		
		// ͨ���̻������ţ���ȡ���̻�ID
		Map<String, String> mapArgs = new HashMap<String, String>();
		mapArgs.put("out_trade_no", strOutTradeNo);
		String strSubMerchId = super.getTblFieldValue("sub_mch_id", "tbl_submch_trans_order", mapArgs);
		
		boolean blnValidSumMch = super.validSubMchIsUsable(strSubMerchId);
		if (!blnValidSumMch) {	// ���̻����˿�Ȩ��
			strRefundResult = "0";
			strReturnMsg =  "�̻��޽���Ȩ��,����ϵ�̻���";
		} else {	// ���̻������˿�Ȩ��
			// ��ѯ���׵����ܽ��
			int iTotalFee = this.getPayOrderTotalFee(strOutTradeNo);
			
			// ��ѯ���׵���Ӧ�˿(���˿���˿����)�����˿���
			int iRefundTotalFee = this.getRefundOrderTotalFee(strOutTradeNo);
			
			// ������õ��˿����
			String strParsedRefundFee = CommonTool.formatNullStrToSpace(strRefundFee);
			int iRefundFee = Integer.parseInt(strParsedRefundFee.equals("") ? "0" : strParsedRefundFee);
			int iUsableBalance = iTotalFee - iRefundTotalFee;
			if (iRefundFee > iUsableBalance) {	// �˿�������˿���
				strRefundResult = "0";
				strReturnMsg =  "�������õ��˿��";
			} else {	// �����˿��������������˿����
				
				Map<String, String> mapRefundOrderInfo = new HashMap<String, String>();
				mapRefundOrderInfo.put(RefundOrderEntity.SUB_MCH_ID, CommonTool.formatNullStrToSpace(strSubMerchId));
				mapRefundOrderInfo.put(RefundOrderEntity.NONCE_STR, CommonTool.getRandomString(32));
//				Map<String, String> mapOutTradeNoArgs = new HashMap<String, String>();
//				mapOutTradeNoArgs.put("out_trade_no", strOutTradeNo);
//				mapRefundOrderInfo.put(RefundOrderEntity.TRANSACTION_ID, super.getTblFieldValue("transaction_id", "tbl_trans_order", mapOutTradeNoArgs));
				mapRefundOrderInfo.put(RefundOrderEntity.OUT_TRADE_NO, CommonTool.formatNullStrToSpace(strOutTradeNo));
				mapRefundOrderInfo.put(RefundOrderEntity.OUT_REFUND_NO, CommonTool.getOutRefundNo(new Date(), 18));
				mapRefundOrderInfo.put(RefundOrderEntity.TOTAL_FEE, String.valueOf(iTotalFee));
				mapRefundOrderInfo.put(RefundOrderEntity.REFUND_FEE, String.valueOf(iRefundFee));
				mapRefundOrderInfo.put(RefundOrderEntity.REFUND_FEE_TYPE, "USD");
				mapRefundOrderInfo.put(RefundOrderEntity.REFUND_DESC, CommonTool.formatNullStrToSpace(strRefundDesc));
				mapRefundOrderInfo = CommonTool.getAppendMap(mapRefundOrderInfo, CommonTool.getHarvestTransInfo());
				
				// ��ʼ���˿���Ϣ�����ݿ�
				boolean blnInstRst = this.insertOrderInfoToTbl(mapRefundOrderInfo);
				
				if (blnInstRst) {
					// ����Ѷ��̨�����˿����
					mapRefundOrderInfo.put(RefundOrderEntity.SIGN, CommonTool.getEntitySign(mapRefundOrderInfo));
					String strReqInfo = super.formatReqInfoToXML(mapRefundOrderInfo);
					System.out.println("strReqInfo = " + strReqInfo);
					CloseableHttpClient httpclient = CommonTool.getCertHttpClient(CommonInfo.SSL_CERT_PASSWORD);
					String strWxRespInfo = super.sendReqAndGetResp(REFUND_ORDER_URL, strReqInfo, httpclient);
					System.out.println("strWxRespInfo = " + strWxRespInfo);
					Map<String, String> mapWxRespInfo = new ParsingWXResponseXML().formatWechatXMLRespToMap(strWxRespInfo);
					System.out.println(this.getClass().getSimpleName() + " : mapWxRespInfo=" + mapWxRespInfo);
					
					// �������ݿ����˿���ص���Ϣ
					boolean blnUpRst = this.updateOrderRstToTbl(mapWxRespInfo);
					if (blnUpRst) {
						String strReturnCode = CommonTool.formatNullStrToSpace(mapWxRespInfo.get(RefundOrderEntity.RETURN_CODE));
						String strResultCode = CommonTool.formatNullStrToSpace(mapWxRespInfo.get(RefundOrderEntity.RESULT_CODE));
						System.out.println(this.getClass().getSimpleName() + " : strReturnCode=" + strReturnCode);
						System.out.println(this.getClass().getSimpleName() + " : strResultCode=" + strResultCode);
						// ReturnCode��ResultCode��ΪSUCCESSʱ���������˿�������ճɹ���У���˿��Ƿ�ɹ���������˿��ѯ�ӿ�
						if (strReturnCode.equals(RefundOrderEntity.SUCCESS) && strResultCode.equals(RefundOrderEntity.SUCCESS)) {
							strRefundResult = "1";
							strReturnMsg = "�ύ�˿�ɹ�!";
							jsonArray = new JSONArray();
							JSONObject jsonObj = new JSONObject();
							jsonObj.put(RefundOrderEntity.OUT_TRADE_NO, CommonTool.formatNullStrToSpace(strOutTradeNo));
							jsonArray.add(jsonObj);
							
							jsonObj = new JSONObject();
							jsonObj.put(RefundOrderEntity.REFUND_FEE, String.valueOf(iRefundFee));
							jsonArray.add(jsonObj);
						} else if (strReturnCode.equals(RefundOrderEntity.FAIL)) {
							strRefundResult = "0";
							strReturnMsg = mapWxRespInfo.get(PayOrderEntiry.RETURN_MSG);
						} else if (strResultCode.equalsIgnoreCase(RefundOrderEntity.FAIL)) {
							strRefundResult = "0";
							strReturnMsg =  mapWxRespInfo.get(PayOrderEntiry.ERR_CODE_DES);
						} else {
							strRefundResult = "0";
							strReturnMsg =  "������������ϵ����Ա��";
						}
					} else {
						strRefundResult = "0";
						strReturnMsg =  "�����ݿ�����˿�Ӧ����Ϣʧ�ܣ�";
					}
				} else {
					strRefundResult = "0";
					strReturnMsg =  "��ʼ���˿�������Ϣ�����ݿ�ʧ�ܣ�";
				}
			}
		}
		
		respObj.setRespCode(strRefundResult);
		respObj.setRespMsg(strReturnMsg);
		respObj.setRespObj(jsonArray);
		JSONObject jsonObj = JSONObject.fromObject(respObj);
		super.returnJsonObj(response, jsonObj);
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		this.doGet(request, response);
	}

	@Override
	public boolean insertOrderInfoToTbl(Map<String, String> mapArgs) {
		boolean blnUpRst = false;
		
		Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
		PreparedStatement prst = null;
		try {
			// ������Ϣ���˿�뽻�׵�������
			String strSql = "insert into tbl_trans_order_refund_order(out_trade_no, out_refund_no) values('"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.OUT_TRADE_NO)) 
							+ "', '"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.OUT_REFUND_NO)) 
							+ "');";
			prst = conn.prepareStatement(strSql);
			prst.execute();
			
			// �����˿��Ϣ
			Map<String, String> mapQuryArgs = new HashMap<String, String>();
			mapQuryArgs.put("sub_merchant_code", mapArgs.get(RefundOrderEntity.SUB_MCH_ID));
			String strTerminalMchId = this.getTblFieldValue("id", "t_merchant", mapQuryArgs);
			String strSignedAssitantId = super.getSignedAssitantid(strTerminalMchId);
			strSql = "insert into tbl_refund_order(out_refund_no, assistant_id, transaction_id, total_fee, "
							+ "refund_fee, refund_fee_type, refund_desc, fee_type) values('"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.OUT_REFUND_NO)) 
							+ "','"
							+ CommonTool.formatNullStrToSpace(strSignedAssitantId)
							+ "','"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.TRANSACTION_ID))
							+ "','"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.TOTAL_FEE))
							+ "','"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.REFUND_FEE))
							+ "','"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.REFUND_FEE_TYPE))
							+ "','"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.REFUND_DESC))
							+ "','"
							+ "USD');";
			prst = conn.prepareStatement(strSql);
			prst.execute();
			
			// �����ύ����
			conn.commit();
			
			blnUpRst = true;
			
		} catch (SQLException se) {
			se.printStackTrace();
			MysqlConnectionPool.getInstance().rollback(conn);
			blnUpRst = false;
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(prst, conn);
		}
		
		return blnUpRst;
	}

	@Override
	public boolean updateOrderRstToTbl(Map<String, String> mapArgs) {
		boolean blnUpRst = false;
		if (mapArgs == null || mapArgs.size() == 0) {
			return blnUpRst;
		}
		
		String strReturnCode = CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.RETURN_CODE));
		String strResultCode = CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.RESULT_CODE));
		
		// ReturnCode��ResultCode��ΪSUCCESSʱ���������˿�������ճɹ���У���˿��Ƿ�ɹ���������˿��ѯ�ӿ�
		if (strReturnCode.equals(RefundOrderEntity.SUCCESS) && strResultCode.equals(RefundOrderEntity.SUCCESS)) {	
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			PreparedStatement prst = null;
			try {
				// �����˿����Ϣ
				String strSql = "update tbl_refund_order set refund_account='" 
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.REFUND_ACCOUNT)) 
							+ "', refund_trans_time='" 
							+ CommonTool.getFormatDateStr(new Date(), "yyyyMMddHHmmss")
							+ "', refund_id='"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.REFUND_ID))
							+ "', cash_fee='"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.CASH_FEE))
							+ "', cash_fee_type='" 
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.CASH_FEE_TYPE))
							+ "', cash_refund_fee='"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.CASH_REFUND_FEE))
							+ "', cash_refund_fee_type='"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.CASH_REFUND_FEE_TYPE))
							+ "', refund_channel='"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.REFUND_CHANNEL))
							+ "', refund_status='"
							+ RefundOrderEntity.PROCESSING
							+ "' where out_refund_no='"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.OUT_REFUND_NO))
							+ "';";
				prst = conn.prepareStatement(strSql);
				prst.executeUpdate();
				prst.close();
				
				// ���½��׵��Ľ���״̬
				strSql = "update tbl_trans_order set trade_state='"
							+ PayOrderEntiry.REFUND 
							+ "' where out_trade_no='"
							+ CommonTool.formatNullStrToSpace(mapArgs.get(RefundOrderEntity.OUT_TRADE_NO))
							+ "';";
				prst = conn.prepareStatement(strSql);
				prst.executeUpdate();
				
				// �����������ݽ��������ύ
				conn.commit();
				
				blnUpRst = true;
			} catch (SQLException se) {
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
	 * ��ѯ���׵����ܽ�
	 * @return
	 */
	private int getPayOrderTotalFee(String strOutTradeNo) {
		int iPayOrderTotalFee = 0;
		Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
		PreparedStatement prst = null;
		ResultSet rs = null;
		try {
			String strSql = "select * from tbl_trans_order where out_trade_no = '" + CommonTool.formatNullStrToSpace(strOutTradeNo) 
							+ "' and (trade_state='" + PayOrderEntiry.SUCCESS 
							+ "' or trade_state='" + PayOrderEntiry.REFUND + "');";
			prst = conn.prepareStatement(strSql);
			rs = prst.executeQuery();
			if (rs.next()) {
				String strTotalFee = rs.getString("total_fee");
				String strParsedTotalFee = CommonTool.formatNullStrToSpace(strTotalFee);
				if (!"".equals(strParsedTotalFee)) {
					iPayOrderTotalFee = Integer.parseInt(strParsedTotalFee);
				}
			}
		} catch(SQLException se) {
			se.printStackTrace();
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
		}
		
		return iPayOrderTotalFee;
	}
	
	/**
	 * ��ѯ���׵���Ӧ�˿(���˿���˿����)�����˿��
	 * @param strOutTradeNo
	 * @return
	 */
	private int getRefundOrderTotalFee(String strOutTradeNo) {
		int iRefundOrderTotalFee = 0;
		Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
		PreparedStatement prst = null;
		ResultSet rs = null;
		try {
			String strSql = "select sum(b.refund_fee) as refundTotalFee from tbl_refund_order as b where b.out_refund_no in (select a.out_refund_no from tbl_trans_order_refund_order as a where a.out_trade_no='" 
							+ CommonTool.formatNullStrToSpace(strOutTradeNo) 
							+ "') and b.refund_status in ('" 
							+ RefundOrderEntity.SUCCESS 
							+ "', '" 
							+ RefundOrderEntity.PROCESSING + "');";
			prst = conn.prepareStatement(strSql);
			rs = prst.executeQuery();
			if (rs.next()) {
				String strRefundTotalFee = rs.getString("refundTotalFee");
				String strParsedRefundTotalFee = CommonTool.formatNullStrToSpace(strRefundTotalFee);
				if (!"".equals(strParsedRefundTotalFee)) {
					iRefundOrderTotalFee = Integer.parseInt(strParsedRefundTotalFee);
				}
			}
		} catch(SQLException se) {
			se.printStackTrace();
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
		}
		
		return iRefundOrderTotalFee;
	}
}
