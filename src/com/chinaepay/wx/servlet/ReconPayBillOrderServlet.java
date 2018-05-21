package com.chinaepay.wx.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.entity.PayOrderEntiry;
import com.chinaepay.wx.entity.ReconPayBillOrderEntity;

public class ReconPayBillOrderServlet extends ReconBillOrderServlet {
	@Override
	public ReconBillOrderTask getReconBillOrderTask(ClosableTimer closableTimer, String strReconStartDay,
			String strReconEndDay) {
		return new ReconPayBillOrderTask(closableTimer, strReconStartDay, strReconEndDay);
	}
	
	
	public class ReconPayBillOrderTask extends ReconBillOrderTask {

		public ReconPayBillOrderTask(ClosableTimer closableTimer, String strReconStartDay, String strReconEndDay) {
			super(closableTimer, strReconStartDay, strReconEndDay);
		}

		@Override
		public Map<String, Map<String, String>> getNobMoreThanWxOrderRecord(String strReconStartDay, String strReconEndDay) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
			PreparedStatement prst = null;
			ResultSet rs = null;
			
			Map<String, Map<String, String>> mapOuterInfo = new HashMap<String, Map<String, String>>();
			try {
				/** ��ȡNob����ڣ���Tencent�����ڵĶ��˵�������Ϣ **/
				Map<String, String> mapInnerInfo = null;
				
				String strSql = "select a.out_trade_no out_trade_no, a.time_end time_end, a.total_fee total_fee from tbl_trans_order a "
								+ " where a.out_trade_no not in (select b.out_trade_no from tbl_wx_pay_bill_info b where date_format(b.trans_time, '%Y%m%d')>='" 
										+ strReconStartDay + "' and date_format(b.trans_time, '%Y%m%d')<='" + strReconEndDay 
										+ "' and (b.trade_state='" + PayOrderEntiry.SUCCESS 
										+ "' or b.trade_state='" + PayOrderEntiry.REFUND 
										+ "' or b.trade_state='" + PayOrderEntiry.REVOKED 
										+ "')) "
								+ " and date_format(a.time_end, '%Y%m%d')>='" + strReconStartDay + "' "
								+ " and date_format(a.time_end, '%Y%m%d')<='" + strReconEndDay + "' "
								+ " and (a.trade_state='"
								+ PayOrderEntiry.SUCCESS
								+ "' or a.trade_state='"
								+ PayOrderEntiry.REFUND
								+ "' or a.trade_state='"
								+ PayOrderEntiry.REVOKED
								+ "');";
				
				System.out.println("###strSql = " + strSql);
				
				prst = conn.prepareStatement(strSql);
				rs = prst.executeQuery();
				while (rs.next()) {
					// У�鵱ǰ�����Ƿ����÷�Ӷģ�� 
					String strOutTradeNo = rs.getString("out_trade_no");
					
					System.out.println("#-#strOutTradeNo = " + strOutTradeNo);
					if (strOutTradeNo != null && !"".equals(strOutTradeNo)) {
						boolean blnValRefTemp = this.validRefTemplet(strOutTradeNo);
						System.out.println("#-#blnValRefTemp = " + blnValRefTemp);
						if (blnValRefTemp) {
							mapInnerInfo = new HashMap<String, String>();
							mapInnerInfo.put(ReconPayBillOrderEntity.TIME_END, rs.getString("time_end"));
							mapInnerInfo.put(ReconPayBillOrderEntity.TOTAL_FEE, rs.getString("total_fee"));
							mapInnerInfo.put(ReconPayBillOrderEntity.DISCOUNT_FEE, "0");	// ��������Ż�
							mapOuterInfo.put(strOutTradeNo, mapInnerInfo);
						}
					}
				}
				
				/** �����������ص���Ϣ **/
				// �������̻���
				Map<String, String> mapArgs = new HashMap<String, String>();
				String strSubMchId = null;
				
				System.out.println("###mapOuterInfo = " + mapOuterInfo);
				String[] strKeys = mapOuterInfo.keySet().toArray(new String[0]);
				System.out.println("###strKeys.size = " + strKeys.length);
				for (String strOutTradeNo : strKeys) {
					mapArgs.clear();
					mapArgs.put("out_trade_no", strOutTradeNo);
					strSubMchId = getTblFieldValue("sub_mch_id", "tbl_submch_trans_order", mapArgs);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.SUB_MCH_ID, strSubMchId);
				}
				
				// ���������ID
				String strAgentId = null;
				for (String strOutTradeNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strAgentId = getTblFieldValue("agent_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.AGENT_ID, strAgentId);
				}
				
				// ����ģ��ID
				String strTempletID = null;
				for (String strOutTradeNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strTempletID = getTblFieldValue("servant_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.POUND_FEE_TEMP_ID, strTempletID);
				}
				
				// ������Ѷ���������ʡ���������
				for (String strOutTradeNo : strKeys) {
					// ��������
					String strTempId = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.POUND_FEE_TEMP_ID);
					System.out.println(">>**>strTempId = " + strTempId);
					mapArgs.clear();
					mapArgs.put("id", strTempId);
					String strWxRate = getTblFieldValue("wechat_rate", "t_servant", mapArgs);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.POUND_RATE, strWxRate);
					
					// ������
					String strTotalFee = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.TOTAL_FEE);
					String strPoundFee = getPoundFeeBaseOnRate(strTotalFee, strWxRate, 0);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.SERVICE_POUND_FEE, strPoundFee);
				}
				
				// NOB(Harvest)���Ƿ���ڡ�NOB��ʵ�ʽ�Tecent���Ƿ���ڡ�Tencentʵ�ʷ��á�NOB��Tencent���ٵĽ����˽�����Ƿ�ת�Ƶ����㵥����
				for (String strOutTradeNo : strKeys) {
					// NOB���Ƿ����
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_NOB_EXIST, "1");
					
					// NOB�����Ĵ�����ʵ�ʽ��(�۳���Ѷ�������Ѻ�Ľ��)
					String strTotalFee = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.TOTAL_FEE);
					String strServPoundFee = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.SERVICE_POUND_FEE);
					String strNobActFee = CommonTool.formatDoubleToHalfUp(Double.parseDouble(strTotalFee) - Double.parseDouble(strServPoundFee), 0, 0);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_NOB_ACT_FEE, strNobActFee);
					
					// Tecent���Ƿ����
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_WX_EXIST, "0");
					
					// Tencentʵ�ʷ���
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_WX_ACT_FEE, "0");
					
					// NOB��Tencent���ٵĽ��
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_NOB_LESS_WX_FEE, String.valueOf(0 - Integer.parseInt(strNobActFee)));
					
					// ���˽��(1: �ɹ�  0��ʧ��)
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_RESULT, "0");
					
					// �Ƿ�ת�Ƶ����㵥����
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.IS_TRANSF_SETTLE, "0");
				}
			} catch(SQLException se) {
				se.printStackTrace();
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
			}
			
			System.out.println("+mapOuterInfo = " + mapOuterInfo);
			
			return mapOuterInfo;
		}

		@Override
		public Map<String, Map<String, String>> getWxMoreThanNobOrderRecord(String strReconStartDay, String strReconEndDay) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
			PreparedStatement prst = null;
			ResultSet rs = null;
			
			Map<String, Map<String, String>> mapOuterInfo = new HashMap<String, Map<String, String>>();
			try {
				/** ��ȡTencent����ڣ���NOB�����ڵĶ��˵�������Ϣ **/
				Map<String, String> mapInnerInfo = null;
				
				String strSql = "select a.out_trade_no out_trade_no, a.sub_mch_id sub_mch_id, a.trans_time trans_time, a.total_fee total_fee, "
								+ " a.discount_fee discount_fee, a.service_pound_fee service_pound_fee, a.pound_rate pound_rate from tbl_wx_pay_bill_info a "
								+ " where a.out_trade_no not in (select b.out_trade_no from tbl_trans_order b where date_format(b.time_end, '%Y%m%d')>='" + strReconStartDay + "' "
										+ " and date_format(b.time_end, '%Y%m%d')<='" + strReconEndDay + "' and (b.trade_state='" + PayOrderEntiry.SUCCESS 
										+ "' or b.trade_state='" + PayOrderEntiry.REFUND 
										+ "' or b.trade_state='" + PayOrderEntiry.REVOKED 
										+ "')) "
								+ " and date_format(a.trans_time, '%Y%m%d')>='" + strReconStartDay + "' " 
								+ " and date_format(a.trans_time, '%Y%m%d')<='" + strReconEndDay + "' and (a.trade_state='"
								+ PayOrderEntiry.SUCCESS
								+ "' or a.trade_state='"
								+ PayOrderEntiry.REFUND
								+ "' or a.trade_state='"
								+ PayOrderEntiry.REVOKED
								+ "');";
				System.out.println(">>++strSql= " + strSql);
				prst = conn.prepareStatement(strSql);
				rs = prst.executeQuery();
				while (rs.next()) {
					// У�鵱ǰ�����Ƿ����÷�Ӷģ�� 
					String strOutTradeNo = rs.getString("out_trade_no");
					if (strOutTradeNo != null && !"".equals(strOutTradeNo)) {
						boolean blnValRefTemp = this.validRefTemplet(strOutTradeNo);
						if (blnValRefTemp) {
							mapInnerInfo = new HashMap<String, String>();
							mapInnerInfo.put(ReconPayBillOrderEntity.SUB_MCH_ID, rs.getString("sub_mch_id"));
							String strSrcTransTimeEnd = rs.getString("trans_time");
							Date srcDate = CommonTool.getDateBaseOnChars("yyyy-MM-dd HH:mm:ss", strSrcTransTimeEnd);
							String strFormatedDate = CommonTool.getFormatDateStr(srcDate, "yyyyMMddHHmmss");
							mapInnerInfo.put(ReconPayBillOrderEntity.TIME_END, strFormatedDate);
							String strTotalFee = CommonTool.formatNullStrToZero(rs.getString("total_fee"));
							Double dblTotalFee = Double.parseDouble(strTotalFee) * 100D;	// ����Ѷ���¼�Ľ�Ԫ��ת��Ϊ���֡���
							mapInnerInfo.put(ReconPayBillOrderEntity.TOTAL_FEE, CommonTool.formatDoubleToHalfUp(dblTotalFee, 0, 0));
							String strDiscountFee = CommonTool.formatNullStrToZero(rs.getString("discount_fee"));
							Double dblDiscountFee = Double.parseDouble(strDiscountFee) * 100D;	// ����Ѷ���¼�Ľ�Ԫ��ת��Ϊ���֡���
							mapInnerInfo.put(ReconPayBillOrderEntity.DISCOUNT_FEE, CommonTool.formatDoubleToHalfUp(dblDiscountFee, 0, 0));
							String strServPoundFee = CommonTool.formatNullStrToZero(rs.getString("service_pound_fee"));
							Double dblServPoundFee = Double.parseDouble(strServPoundFee) * 100D;	// ����Ѷ���¼�Ľ�Ԫ��ת��Ϊ���֡���
							mapInnerInfo.put(ReconPayBillOrderEntity.SERVICE_POUND_FEE, CommonTool.formatDoubleToHalfUp(dblServPoundFee, 0, 0));
							String strPoundRate = rs.getString("pound_rate");
							strPoundRate = CommonTool.formatNullStrToSpace(strPoundRate).replace("%", "");	// ȥ����Ѷ�����ѵİٷֱ�(%)	
							mapInnerInfo.put(ReconPayBillOrderEntity.POUND_RATE, strPoundRate);
							mapOuterInfo.put(strOutTradeNo, mapInnerInfo);
						}
					}
				}
				
				/** �����������ص���Ϣ **/
				// ���������ID
				String strAgentId = null;
				String strSubMchId = null;
				Map<String, String> mapArgs = new HashMap<String, String>();
				String[] strKeys = mapOuterInfo.keySet().toArray(new String[0]);
				for (String strOutTradeNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strAgentId = getTblFieldValue("agent_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.AGENT_ID, strAgentId);
				}
				
				// ����ģ��ID
				String strTempletID = null;
				for (String strOutTradeNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strTempletID = getTblFieldValue("servant_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.POUND_FEE_TEMP_ID, strTempletID);
				}
				
				// NOB(Harvest)���Ƿ���ڡ�NOB��ʵ�ʽ�Tecent���Ƿ���ڡ�Tencentʵ�ʷ��á�NOB��Tencent���ٵĽ����˽�����Ƿ�ת�Ƶ����㵥����
				for (String strOutTradeNo : strKeys) {
					// NOB���Ƿ����
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_NOB_EXIST, "0");
					
					// NOB��ʵ�ʽ��
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_NOB_ACT_FEE, "0");
					
					// Tecent���Ƿ����
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_WX_EXIST, "1");
					
					// Tencent�����Ĵ�����ʵ�ʷ���(�۳���Ѷ�����Ѻ�Ľ���λΪ����)
					String strTotalFee = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.TOTAL_FEE);
					String strPoundFee = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.SERVICE_POUND_FEE);
					Double dblWxActFee = Double.parseDouble(strTotalFee) - Double.parseDouble(strPoundFee);
					String strWxActFee = CommonTool.formatDoubleToHalfUp(dblWxActFee, 0, 0);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_WX_ACT_FEE, strWxActFee);
					
					// NOB��Tencent���ٵĽ��
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_NOB_LESS_WX_FEE, strWxActFee);
					
					// ���˽��(1: �ɹ�  0��ʧ��)
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_RESULT, "0");
					
					// �Ƿ�ת�Ƶ����㵥����
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.IS_TRANSF_SETTLE, "0");
				}
			} catch(SQLException se) {
				se.printStackTrace();
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
			}
			
			return mapOuterInfo;
		}

		@Override
		public Map<String, Map<String, String>> getWxEqualNobOrderRecord(String strReconStartDay, String strReconEndDay) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
			PreparedStatement prst = null;
			ResultSet rs = null;
			
			Map<String, Map<String, String>> mapOuterInfo = new HashMap<String, Map<String, String>>();
			try {
				/** ��ȡTencent����ڣ���NOB�����ڵĶ��˵�������Ϣ **/
				Map<String, String> mapInnerInfo = null;
				
				String strSql = "select a.out_trade_no out_trade_no, a.sub_mch_id sub_mch_id, a.trans_time trans_time, a.total_fee total_fee, "
								+ " a.discount_fee discount_fee, a.service_pound_fee service_pound_fee, a.pound_rate pound_rate "
								+ " from tbl_wx_pay_bill_info a, tbl_trans_order b where a.out_trade_no=b.out_trade_no "
								+ " and date_format(a.trans_time, '%Y%m%d')>='" + strReconStartDay  + "' "
								+ " and date_format(a.trans_time, '%Y%m%d')<='" + strReconEndDay + "' "
								+ " and (a.trade_state='" + PayOrderEntiry.SUCCESS + "' " 
								+ " or a.trade_state='" + PayOrderEntiry.REFUND  + "' "
								+ " or a.trade_state='" + PayOrderEntiry.REVOKED + "') "
								+ " and date_format(b.time_end, '%Y%m%d')>='" + strReconStartDay + "' "
								+ " and date_format(b.time_end, '%Y%m%d')<='" + strReconEndDay + "' "
								+ " and (b.trade_state='" + PayOrderEntiry.SUCCESS + "' " 
								+ " or b.trade_state='" + PayOrderEntiry.REFUND  + "' "
								+ " or b.trade_state='" + PayOrderEntiry.REVOKED + "');";
				
				System.out.println("++>>>>strSql= " + strSql);
				
				prst = conn.prepareStatement(strSql);
				rs = prst.executeQuery();
				while (rs.next()) {
					// У�鵱ǰ�����Ƿ����÷�Ӷģ�� 
					String strOutTradeNo = rs.getString("out_trade_no");
					System.out.println("strOutTradeNo = " + strOutTradeNo);
					if (strOutTradeNo != null && !"".equals(strOutTradeNo)) {
						boolean blnValRefTemp = this.validRefTemplet(strOutTradeNo);
						System.out.println("blnValRefTemp = " + blnValRefTemp);
						if (blnValRefTemp) {
							mapInnerInfo = new HashMap<String, String>();
							mapInnerInfo.put(ReconPayBillOrderEntity.SUB_MCH_ID, rs.getString("sub_mch_id"));
							String strSrcTransTimeEnd = rs.getString("trans_time");
							Date srcDate = CommonTool.getDateBaseOnChars("yyyy-MM-dd HH:mm:ss", strSrcTransTimeEnd);
							String strFormatedDate = CommonTool.getFormatDateStr(srcDate, "yyyyMMddHHmmss");
							mapInnerInfo.put(ReconPayBillOrderEntity.TIME_END, strFormatedDate);
							String strTotalFee = CommonTool.formatNullStrToZero(rs.getString("total_fee"));
							Double dblTotalFee = Double.parseDouble(strTotalFee) * 100D;	// ����Ѷ���¼�Ľ��ɵ�λ��Ԫ��ת��Ϊ���֡�
							mapInnerInfo.put(ReconPayBillOrderEntity.TOTAL_FEE, CommonTool.formatDoubleToHalfUp(dblTotalFee, 0, 0));
							String strDiscountFee = CommonTool.formatNullStrToZero(rs.getString("discount_fee"));
							Double dblDiscountFee = Double.parseDouble(strDiscountFee) * 100D;	// ����Ѷ���¼�Ľ��ɵ�λ��Ԫ��ת��Ϊ���֡�
							mapInnerInfo.put(ReconPayBillOrderEntity.DISCOUNT_FEE, CommonTool.formatDoubleToHalfUp(dblDiscountFee, 0, 0));
							String strServPoundFee = CommonTool.formatNullStrToZero(rs.getString("service_pound_fee"));
							Double dblServPoundFee = Double.parseDouble(strServPoundFee) * 100D;	// ����Ѷ���¼�Ľ��ɵ�λ��Ԫ��ת��Ϊ���֡�
							mapInnerInfo.put(ReconPayBillOrderEntity.SERVICE_POUND_FEE, CommonTool.formatDoubleToHalfUp(dblServPoundFee, 0, 0));
							String strPoundRate = rs.getString("pound_rate");
							strPoundRate = CommonTool.formatNullStrToSpace(strPoundRate).replace("%", "");	// ȥ����Ѷ�����ѵİٷֱ�(%)	
							mapInnerInfo.put(ReconPayBillOrderEntity.POUND_RATE, strPoundRate);
							mapOuterInfo.put(strOutTradeNo, mapInnerInfo);
						}
					}
				}
				
				/** �����������ص���Ϣ **/
				// ���������ID
				String strAgentId = null;
				String strSubMchId = null;
				Map<String, String> mapArgs = new HashMap<String, String>();
				String[] strKeys = mapOuterInfo.keySet().toArray(new String[0]);
				for (String strOutTradeNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strAgentId = getTblFieldValue("agent_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.AGENT_ID, strAgentId);
				}
				
				// ����ģ��ID
				String strTempletID = null;
				for (String strOutTradeNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strTempletID = getTblFieldValue("servant_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.POUND_FEE_TEMP_ID, strTempletID);
				}
				
				// NOB(Harvest)���Ƿ���ڡ�NOB��ʵ�ʽ�Tecent���Ƿ���ڡ�Tencentʵ�ʷ��á�NOB��Tencent���ٵĽ����˽�����Ƿ�ת�Ƶ����㵥����
				for (String strOutTradeNo : strKeys) {
					// NOB���Ƿ����
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_NOB_EXIST, "1");
					
					// NOB�����Ŀ۳���Ѷ�����Ѻ�������ʵ�ʽ��(��λΪ����)
					mapArgs.clear();
					mapArgs.put("out_trade_no", strOutTradeNo);
					String strNobTotalFee = CommonTool.formatNullStrToZero(getTblFieldValue("total_fee", "tbl_trans_order", mapArgs));	// Nob���TotalFee
					
					mapArgs.clear();
					mapArgs.put("id", mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.POUND_FEE_TEMP_ID));
					String strNobWxRate = CommonTool.formatNullStrToZero(getTblFieldValue("wechat_rate", "t_servant", mapArgs));	// Nob���¼����Ѷ�˷���
					String strNobSetlPoundFee = getPoundFeeBaseOnRate(strNobTotalFee, strNobWxRate, 0);
					String strNobActFee = CommonTool.formatDoubleToHalfUp(Double.parseDouble(strNobTotalFee) - Double.parseDouble(strNobSetlPoundFee), 0, 0);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_NOB_ACT_FEE, strNobActFee);
					
					// Tecent���Ƿ����
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_WX_EXIST, "1");
					
					// Tencent�����Ŀ۳���Ѷ�����Ѻ�������ʵ�ʷ���(��λΪ����)
					String strTotalFee = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.TOTAL_FEE);
					String strPoundFee = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.SERVICE_POUND_FEE);
					Double dblWxActFee = Double.parseDouble(strTotalFee) - Double.parseDouble(strPoundFee);
					String strWxActFee = CommonTool.formatDoubleToHalfUp(dblWxActFee, 0, 0);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_WX_ACT_FEE, strWxActFee);
					
					// NOB��Tencent���ٵĽ��
					Double dblNobLessWxFee = Double.parseDouble(strWxActFee) - Double.parseDouble(strNobActFee);
					String strNobLessWxFee = CommonTool.formatDoubleToHalfUp(dblNobLessWxFee, 0, 0);
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_NOB_LESS_WX_FEE, strNobLessWxFee);
					
					// ���˽��(1: �ɹ�  0��ʧ��)
					String strRecRst = null;
					String strWxPoundRate = mapOuterInfo.get(strOutTradeNo).get(ReconPayBillOrderEntity.POUND_RATE);
					double dblWxPoundRate = Double.parseDouble(strWxPoundRate);
					double dblNobPoundRate = Double.parseDouble(strNobWxRate);
					System.out.println("strOutTradeNo = " + strOutTradeNo);
					System.out.println("dblWxPoundRate = " + dblWxPoundRate);
					System.out.println("dblNobPoundRate = " + dblNobPoundRate);
					if ("0".equals(CommonTool.formatNullStrToZero(strNobLessWxFee))	// NOB��Tencent��ʵ�ʷ��ò��Ϊ0
							&& 	dblNobPoundRate == dblWxPoundRate	// ��Ѷ��������Ѷ����������NOB��ģ�嶨�����Ѷ�����������
							) {	
						strRecRst = "1";
					} else {
						strRecRst = "0";
					}
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.REC_RESULT, strRecRst);
					
					// �Ƿ�ת�Ƶ����㵥����
					mapOuterInfo.get(strOutTradeNo).put(ReconPayBillOrderEntity.IS_TRANSF_SETTLE, "0");
				}
			} catch(SQLException se) {
				se.printStackTrace();
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
			}
			
			return mapOuterInfo;
		}

		@Override
		public void insertFullRecInfoToTbl(Map<String, Map<String, String>> mapOuterInfo) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			PreparedStatement prst = null;

			// ȡ��ָ��ʱ����ڣ����˽�����ж��˳ɹ��Ķ�����
			List<String[]> listArgs = new ArrayList<String[]>();
			listArgs.add(new String[] {"rec_result", "=", "1"});	// 1�����˳ɹ� 0������ʧ��
			listArgs.add(new String[] {"date_format(trans_time_end, '%Y%m%d')", ">=", strReconStartDay});
			listArgs.add(new String[] {"date_format(trans_time_end, '%Y%m%d')", "<=", strReconEndDay});
			List<String> lstOutTradeNoRecOk = getTblFieldValueList("out_trade_no", "tbl_pay_order_recon_result", listArgs);
			
			// Ϊ�˲��ٸ��¶��˽�����ڶ��˳ɹ��ļ�¼����Ҫ������÷��������ݼ��޳���Щ�ɹ�������
			Map<String, Map<String, String>> mapNewOuterInfo = super.getNewOuterInfo(mapOuterInfo, lstOutTradeNoRecOk);
			
			try {
				String strSql = "replace into tbl_pay_order_recon_result(out_trade_no, sub_mch_id, agent_id, pound_fee_temp_id, "
								+ " trans_time_end, total_fee, discount_fee, service_pound_fee, pound_rate, rec_nob_exist, "
								+ " rec_nob_act_fee, rec_wx_exist, rec_wx_act_fee, rec_nob_less_wx_fee, rec_result, is_transf_settle) "
								+ " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
				
				System.out.println("---strSql = " + strSql);
				
				prst = conn.prepareStatement(strSql);

				String[] strOutTradeNoS = mapNewOuterInfo.keySet().toArray(new String[0]);
				int iOutTradeNoSize = strOutTradeNoS.length;
				for (int i = 0; i < iOutTradeNoSize; i++) {
					String strOutTradeNo = strOutTradeNoS[i];
					Map<String, String> mapInner = mapNewOuterInfo.get(strOutTradeNo);

					prst.setString(1, strOutTradeNo);
					prst.setString(2, mapInner.get(ReconPayBillOrderEntity.SUB_MCH_ID));
					prst.setString(3, mapInner.get(ReconPayBillOrderEntity.AGENT_ID));
					prst.setString(4, mapInner.get(ReconPayBillOrderEntity.POUND_FEE_TEMP_ID));
					prst.setString(5, mapInner.get(ReconPayBillOrderEntity.TIME_END));
					prst.setString(6, mapInner.get(ReconPayBillOrderEntity.TOTAL_FEE));
					prst.setString(7, mapInner.get(ReconPayBillOrderEntity.DISCOUNT_FEE));
					prst.setString(8, mapInner.get(ReconPayBillOrderEntity.SERVICE_POUND_FEE));
					prst.setString(9, mapInner.get(ReconPayBillOrderEntity.POUND_RATE));
					prst.setString(10, mapInner.get(ReconPayBillOrderEntity.REC_NOB_EXIST));
					prst.setString(11, mapInner.get(ReconPayBillOrderEntity.REC_NOB_ACT_FEE));
					prst.setString(12, mapInner.get(ReconPayBillOrderEntity.REC_WX_EXIST));
					prst.setString(13, mapInner.get(ReconPayBillOrderEntity.REC_WX_ACT_FEE));
					prst.setString(14, mapInner.get(ReconPayBillOrderEntity.REC_NOB_LESS_WX_FEE));
					prst.setString(15, mapInner.get(ReconPayBillOrderEntity.REC_RESULT));
					prst.setString(16, mapInner.get(ReconPayBillOrderEntity.IS_TRANSF_SETTLE));

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

			} catch (SQLException se) {
				se.printStackTrace();
				MysqlConnectionPool.getInstance().rollback(conn);
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(prst, conn);
			}
		}
		
		/**
		 * У�鵱ǰ֧�����������̻��Ƿ�������Ӧ��ģ�塣
		 * @param strOrderNo
		 * @return
		 */
		@Override
		public boolean validRefTemplet(String strOrderNo) {
			boolean blnRefTemplet = false;
			Map<String, String> mapArgs = new HashMap<String, String>();
			mapArgs.put("out_trade_no", strOrderNo);
			String strSubMerchId = getTblFieldValue("sub_mch_id", "tbl_submch_trans_order", mapArgs);
			if (strSubMerchId != null && !"".equals(strSubMerchId)) {
				mapArgs.clear();
				mapArgs.put("sub_merchant_code", strSubMerchId);
				String strTempletId = getTblFieldValue("servant_id", "t_merchant", mapArgs);
				if (strTempletId != null && !"".equals(strTempletId)) {
					mapArgs.clear();
					mapArgs.put("id", strTempletId);
					String strServantCode = getTblFieldValue("servant_code", "t_servant", mapArgs);
					if (strServantCode != null && !"".equals(strServantCode)) {
						blnRefTemplet = true;
					}
				}
			}
			return blnRefTemplet;
		}
	}
}
