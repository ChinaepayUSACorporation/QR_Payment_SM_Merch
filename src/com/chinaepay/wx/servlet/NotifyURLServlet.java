package com.chinaepay.wx.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.AccessTokenUtil;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.entity.NotifyURLEntity;
import com.chinaepay.wx.entity.PayOrderEntiry;

import net.sf.json.JSONObject;

/**
 * ���׵�֧���󣬴�����Ѷ��̨���������첽֪ͨ��
 * @author xinwuhen
 */
public class NotifyURLServlet extends TransControllerServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String strReqInfo = null;
		InputStream is = null;
		try {
			is = request.getInputStream();
			strReqInfo = CommonTool.getInputStreamInfo(is, "UTF-8");
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		if (strReqInfo != null) {
			System.out.println("strReqInfo = " + strReqInfo);
			Map<String, String> mapWxRespInfo = new ParsingWXResponseXML().formatWechatXMLRespToMap(strReqInfo);
			System.out.println("mapWxRespInfo = " + mapWxRespInfo);
			
			/** У��֮ǰ�Ƿ�������ס���δ�����,��Խ��׵���ʶΪ���Ѵ�����ͬʱ����Ӧ���ĸ���Ѷ�ˣ���֪��Ѷ������ֹͣ����֧��֪ͨ�� **/
			String strRespXMLInfo = null;
			String strReturnCode = CommonTool.formatNullStrToSpace(mapWxRespInfo.get(NotifyURLEntity.RETURN_CODE));
			if (!strReturnCode.equals(NotifyURLEntity.SUCCESS)) {	// ReturnCode����Null������ֵ����Sucessʱ������֧��֪ͨ���͵��׸�֧ͨ��ƽ̨ʧ�ܣ���Ҫ�ٴεȴ�֧��֪ͨ
				strRespXMLInfo = "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
			} else {
				strRespXMLInfo = "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
				
				// У�齻�׵��Ƿ��������ص�֪ͨ��true: ������� false��δ���������
				String strOutTradeNo = mapWxRespInfo.get(NotifyURLEntity.OUT_TRADE_NO);
				if (strOutTradeNo != null) {
					boolean blnValidNotifyProcRst = validNotifyProcResult(strOutTradeNo);
					// �����׵��ص�֪ͨδ�������(false)����֧��������µ�֧��������(tbl_trans_order) 
					if (!blnValidNotifyProcRst) {
						boolean blnUpTransOrderRst = updateOrderRstToTbl(mapWxRespInfo);
						
						// ������֧��������ɹ�������󶨹��ںŵĵ�Ա���̻��ֱ��Ͷ���֧���ɹ���֪ͨ
						if (blnUpTransOrderRst) {
							String strResultCode = mapWxRespInfo.get(NotifyURLEntity.RESULT_CODE);
							if (strResultCode != null && strResultCode.equals(NotifyURLEntity.SUCCESS)) {	// ֧���ɹ�
								// ������Ϣ֪ͨ���ն��̻��ڵ�ֵ���Ա
								String strNoticeResp = new PaymentTemplateNotice().sendPaymentNotice(strOutTradeNo);
								System.out.println(">>>strNoticeResp = " + strNoticeResp);
							}
						}
					}
				}
			}
			
			PrintWriter pw = null;
			try {
				pw = response.getWriter();
				pw.write(strRespXMLInfo);
				pw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (pw !=null) {
					pw.close();
				}
			}
		}
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		this.doGet(request, response);
	}
	
	/**
	 * У�鶩���Ƿ��������ص�֪ͨ��true: ������� false��δ���������
	 * @param strOutTradeNo
	 * @return
	 */
	private boolean validNotifyProcResult(String strOutTradeNo) {
		Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
		PreparedStatement prst = null;
		ResultSet rs = null;
		boolean blnNtyPrcRst = false;
		String strInquirySql = "select * from tbl_trans_order where out_trade_no='" + strOutTradeNo + "';";
		try {
			prst = conn.prepareStatement(strInquirySql);
			rs = prst.executeQuery();
			if (rs.next()) {
				String strNtyProcRst = rs.getString("notify_proc_rst");
				if (strNtyProcRst == null || !"Y".equals(strNtyProcRst)) {
					blnNtyPrcRst = false;
				} else {
					blnNtyPrcRst = true;
				}
			}
		} catch(SQLException se) {
			se.printStackTrace();
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
		}
		
		return blnNtyPrcRst;
	}
	
	@Override
	public boolean insertOrderInfoToTbl(Map<String, String> mapArgs) {
		return true;
	}

	@Override
	public boolean updateOrderRstToTbl(Map<String, String> mapArgs) {
		boolean blnUpRst = false;
		if (mapArgs == null || mapArgs.size() == 0) {
			return blnUpRst;
		}
		
		String strReturnCode = mapArgs.get(NotifyURLEntity.RETURN_CODE);
		if (strReturnCode != null && strReturnCode.equals(NotifyURLEntity.SUCCESS)) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			PreparedStatement prst = null;
			String strResultCode = mapArgs.get(NotifyURLEntity.RESULT_CODE);
			String strStateDesc = "";
			if (strResultCode.equalsIgnoreCase(NotifyURLEntity.SUCCESS)) {
				strStateDesc = "֧���ɹ�";
			} else {
				strStateDesc = "֧��ʧ��";
			}
			String strUpdateSql = "update tbl_trans_order set notify_proc_rst='Y', transaction_id='" + CommonTool.formatNullStrToSpace(mapArgs.get(NotifyURLEntity.TRANSACTION_ID)) 
									+ "', time_end='" + CommonTool.formatNullStrToSpace(mapArgs.get(NotifyURLEntity.TIME_END))
									+ "', bank_type='" + CommonTool.formatNullStrToSpace(mapArgs.get(NotifyURLEntity.BANK_TYPE)) 
									+ "', cash_fee='" + CommonTool.formatNullStrToSpace(mapArgs.get(NotifyURLEntity.CASH_FEE)) 
									+ "', trade_state='" + strResultCode
									+ "', trade_state_desc = '" + strStateDesc
									+ "' where out_trade_no='" + CommonTool.formatNullStrToSpace(mapArgs.get(NotifyURLEntity.OUT_TRADE_NO)) + "';";
			System.out.println("strUpdateSql = " + strUpdateSql);
			try {
				prst = conn.prepareStatement(strUpdateSql);
				prst.executeUpdate();
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
	 * �˿�֧���ɹ���Harvest��̨���յ���Ѷ���صĻص�֪ͨ�����ص�֪ͨ��ǰû�б������������ٴν��д���
	 * �ص�֪ͨ������ɺ�Harvest��̨�����ݹ��ں��ڵ���Ϣģ�����ն��̻�����Ա����֧����Ϣ��
	 * @author xinwuhen
	 */
	public class PaymentTemplateNotice {
		private static final String SEND_TEMPLATE_NOTICE_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=ACCESS_TOKEN";
		
		/**
		 * ����ģ����Ϣ֪ͨ��
		 * @param strOutTradeNo
		 */
		public String sendPaymentNotice(String strOutTradeNo) {
			String strNoticeResp = null;
			if (strOutTradeNo == null || "".equals(strOutTradeNo)) {
				return strNoticeResp;
			}
			
			String strAccessToken = AccessTokenUtil.getInstance().getAccessTokenObj().getTokenValue();
			System.out.println("strAccessToken = " + strAccessToken);
			if (strAccessToken != null && !"".equals(strAccessToken)) {
				WxNoticeTemplate wxNoticeTemp = getWxNoticeTemplate(strOutTradeNo);
				System.out.println("wxNoticeTemp = " + wxNoticeTemp);
				if (wxNoticeTemp != null) {
					String strJsonNoticeInfo = JSONObject.fromObject(wxNoticeTemp).toString();
					System.out.println("strJsonNoticeInfo = " + strJsonNoticeInfo);
					
					String strSendTempNoticeURL = SEND_TEMPLATE_NOTICE_URL.replaceFirst("ACCESS_TOKEN", strAccessToken);
					System.out.println("strSendTempNoticeURL = " + strSendTempNoticeURL);
					
					// ���̻�����ģ��֪ͨ��Ϣ
					strNoticeResp = sendReqAndGetResp(strSendTempNoticeURL, strJsonNoticeInfo, CommonTool.getDefaultHttpClient());
				}
			}
			
			return strNoticeResp;
		}
		
		/**
		 * ������Ϣ֪ͨ��ģ�壬����װ��ص����ݡ�
		 * @param strOutTradeNo
		 * @return
		 */
		private WxNoticeTemplate getWxNoticeTemplate(String strOutTradeNo) {
			WxNoticeTemplate wxNoticeTemp = new WxNoticeTemplate();
			
			// ��ȡ�˽��׵�����Ӧ��ǩ����ԱID
			Map<String, String> mapArgs = new HashMap<String, String>();
			mapArgs.put("out_trade_no", strOutTradeNo);
			String strAssistantId = getTblFieldValue("assistant_id", "tbl_submch_trans_order", mapArgs);
			System.out.println("strAssistantId = " + strAssistantId);
			
			if (strAssistantId != null) {
				// ��ȡ������openid
				mapArgs.clear();
				mapArgs.put("relation_id", strAssistantId);
				mapArgs.put("user_type", "3");	// 1���� 2 �̻� 3 ��Ա
				String strAssisOpenId = getTblFieldValue("open_id", "t_wechat_user", mapArgs);
				System.out.println("strAssisOpenId = " + strAssisOpenId);
				
				// ��ȡ΢���ǳ�
				String strWxNickName = getTblFieldValue("nick_name", "t_wechat_user", mapArgs);
				System.out.println("strWxNickName = " + strWxNickName);
				
				// ��ȡ���׽��
				mapArgs.clear();
				mapArgs.put("out_trade_no", strOutTradeNo);
				String strTotalFee = getTblFieldValue("total_fee", "tbl_trans_order", mapArgs);
				strTotalFee = CommonTool.formatNullStrToZero(strTotalFee);
				strTotalFee = CommonTool.formatDoubleToHalfUp(Double.parseDouble(strTotalFee) / 100d, 2, 2) + " USD";
				System.out.println("strTotalFee = " + strTotalFee);
				
				// ��ȡ֧����ʽ
				String strPayType = getTblFieldValue("user_pay_type", "tbl_trans_order", mapArgs);
				if (strPayType != null && strPayType.equals(PayOrderEntiry.PAY_TYPE_SCAN_QR)) {
					strPayType = "ɨ��֧��";
				} else {
					strPayType = "ɨ��֧��";
				}
				
				// ��ȡ����ʱ��
				String strTimeEnd = getTblFieldValue("time_end", "tbl_trans_order", mapArgs);
				Date dateTimeEnd = CommonTool.getDateBaseOnChars("yyyyMMddHHmmss", strTimeEnd);
				strTimeEnd = CommonTool.getFormatDateStr(dateTimeEnd, "yyyy-MM-dd HH:mm:ss");
				System.out.println("strTimeEnd = " + strTimeEnd);
				
				// ��װģ������
				Map<String, TemplateData> data = new HashMap<String, TemplateData>();
				TemplateData first = new TemplateData();
				first.setValue("���ã��˿�[" + CommonTool.formatNullStrToSpace(strWxNickName) + "]���һ��֧������������:");
				TemplateData keyword1 = new TemplateData();	// ���׽��
				keyword1.setValue(strTotalFee);
				TemplateData keyword2 = new TemplateData();	// ֧����ʽ
				keyword2.setValue(strPayType);
				TemplateData keyword3 = new TemplateData();	// ����ʱ��
				keyword3.setValue(strTimeEnd);
				TemplateData keyword4 = new TemplateData();	// ���׵���(ע���˴�Ϊ�̻�������)
				keyword4.setValue(strOutTradeNo);
				TemplateData remark = new TemplateData();
				remark.setValue("��ȷ��������Ϣ�Ƿ���ȷ.");
				
				data.put("first", first);
				data.put("keyword1", keyword1);
				data.put("keyword2", keyword2);
				data.put("keyword3", keyword3);
				data.put("keyword4", keyword4);
				data.put("remark", remark);
				
				// ���������ݷ�װ��ģ����
				wxNoticeTemp.setTouser(strAssisOpenId);
				wxNoticeTemp.setTemplate_id(CommonInfo.PAYMENT_NOTICE_TEMPLATE_ID);
				wxNoticeTemp.setData(data);
			}
			
			return wxNoticeTemp;
		}
		
		/**
		 * ��Ϣģ���װ�ࡣ
		 * ΢�Źٷ��ĵ��ο�URL��https://mp.weixin.qq.com/wiki?t=resource/res_main&id=mp1433751277
		 * 1��ģ����Ϣ����ʱ��Ҫ��Ҫģ��ID��ģ���и������ĸ�ֵ���ݣ�
		 * 2��ģ���в������ݱ�����".DATA"��β��������Ϊ�����֣�
		 * 3��ģ�屣������"{{ }}"��
		 * 
		 * ����������  
		 * {{first.DATA}}
		 * ���׽�{{keyword1.DATA}}  
		 * ֧����ʽ��{{keyword2.DATA}}  
		 * ����ʱ�䣺{{keyword3.DATA}}
		 * ���׵��ţ�{{keyword4.DATA}}
		 * {{remark.DATA}}
		 * 
		 * @author xinwuhen
		 */
		public class WxNoticeTemplate {
			// ������openid
			private String touser = null;
			// ģ��ID
			private String template_id = null;
			// ģ������
			private Map<String, TemplateData> data = null;

			public String getTouser() {
				return touser;
			}

			public void setTouser(String touser) {
				this.touser = touser;
			}

			public String getTemplate_id() {
				return template_id;
			}

			public void setTemplate_id(String template_id) {
				this.template_id = template_id;
			}

			public Map<String, TemplateData> getData() {
				return data;
			}

			public void setData(Map<String, TemplateData> data) {
				this.data = data;
			}
		}
		
		/**
		 * ��Ϣģ���е����ݡ�
		 * @author xinwuhen
		 */
		public class TemplateData {
			private String value = null;
			private String color = null;

			public String getValue() {
				return value;
			}

			public void setValue(String value) {
				this.value = value;
			}

			public String getColor() {
				return color;
			}

			public void setColor(String color) {
				this.color = color;
			}
		}
	} 
}
