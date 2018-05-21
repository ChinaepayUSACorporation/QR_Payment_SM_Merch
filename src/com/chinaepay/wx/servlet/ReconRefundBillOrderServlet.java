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
import com.chinaepay.wx.entity.ReconPayBillOrderEntity;
import com.chinaepay.wx.entity.ReconRefundBillOrderEntity;
import com.chinaepay.wx.entity.RefundOrderEntity;

public class ReconRefundBillOrderServlet extends ReconBillOrderServlet {

	@Override
	public ReconBillOrderTask getReconBillOrderTask(ClosableTimer closableTimer, String strReconStartDay,
			String strReconEndDay) {
		return new ReconRefundBillOrderTask(closableTimer, strReconStartDay, strReconEndDay);
	}

	public class ReconRefundBillOrderTask extends ReconBillOrderTask {
		public ReconRefundBillOrderTask(ClosableTimer closableTimer, String strReconStartDay, String strReconEndDay) {
			super(closableTimer, strReconStartDay, strReconEndDay);
		}
		
		@Override
		public Map<String, Map<String, String>> getNobMoreThanWxOrderRecord(String strReconStartDay,
				String strReconEndDay) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
			PreparedStatement prst = null;
			ResultSet rs = null;
			
			Map<String, Map<String, String>> mapOuterInfo = new HashMap<String, Map<String, String>>();
			try {
				/** ��ȡNob����ڣ���Tencent�����ڵĶ��˵�������Ϣ **/
				Map<String, String> mapInnerInfo = null;
				
				String strSql = "select a.out_refund_no out_refund_no, a.refund_success_time refund_success_time, a.refund_fee refund_fee from tbl_refund_order a "
								+ " where a.out_refund_no not in (select b.out_refund_no from tbl_wx_refund_bill_info b where date_format(b.refund_success_time, '%Y%m%d')>='" + strReconStartDay + "' "
										+ " and date_format(b.refund_success_time, '%Y%m%d')<='" + strReconEndDay + "' "
										+ " and b.refund_status='" + RefundOrderEntity.SUCCESS + "') "
								+ " and date_format(a.refund_success_time, '%Y%m%d')>='" + strReconStartDay + "' "
								+ " and date_format(a.refund_success_time, '%Y%m%d')<='" + strReconEndDay + "' "
								+ " and a.refund_status='" + RefundOrderEntity.SUCCESS + "';";
				
				System.out.println(">>>--strSql = " + strSql);
				
				prst = conn.prepareStatement(strSql);
				rs = prst.executeQuery();
				while (rs.next()) {
					// У�鵱ǰ�����Ƿ����÷�Ӷģ�� 
					String strOutRefundNo = rs.getString("out_refund_no");
					if (strOutRefundNo != null && !"".equals(strOutRefundNo)) {
						boolean blnValRefTemp = this.validRefTemplet(strOutRefundNo);
						if (blnValRefTemp) {
							mapInnerInfo = new HashMap<String, String>();
							String strSrcRefundSuccTime = rs.getString("refund_success_time");
							Date srcDate = CommonTool.getDateBaseOnChars("yyyy-MM-dd HH:mm:ss", strSrcRefundSuccTime);
							String strFormatedDate = CommonTool.getFormatDateStr(srcDate, "yyyyMMddHHmmss");
							mapInnerInfo.put(ReconRefundBillOrderEntity.REFUND_SUCCESS_TIME, strFormatedDate);
							mapInnerInfo.put(ReconRefundBillOrderEntity.REFUND_FEE, rs.getString("refund_fee"));
							mapOuterInfo.put(strOutRefundNo, mapInnerInfo);
						}
					}
				}
				
				/** �����������ص���Ϣ **/
				// �������̻���
				Map<String, String> mapArgs = new HashMap<String, String>();
				String strSubMchId = null;
				
				String[] strKeys = mapOuterInfo.keySet().toArray(new String[0]);
				for (String strOutRefundNo : strKeys) {
					mapArgs.clear();
					mapArgs.put("out_refund_no", strOutRefundNo);
					String strOutTradeNo = getTblFieldValue("out_trade_no", "tbl_trans_order_refund_order", mapArgs);
					
					mapArgs.clear();
					mapArgs.put("out_trade_no", strOutTradeNo);
					strSubMchId = getTblFieldValue("sub_mch_id", "tbl_submch_trans_order", mapArgs);
					
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.SUB_MCH_ID, strSubMchId);
				}
				
				// ���������ID
				String strAgentId = null;
				for (String strOutRefundNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strAgentId = getTblFieldValue("agent_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.AGENT_ID, strAgentId);
				}
				
				// ����ģ��ID
				String strTempletID = null;
				for (String strOutRefundNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strTempletID = getTblFieldValue("servant_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.POUND_FEE_TEMP_ID, strTempletID);
				}
				
				// ���������������
				for (String strOutRefundNo : strKeys) {
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.DISCOUNT_REFUND_FEE, "0");	// ��������Ż�
				}
				
				// ������Ѷ���������ʡ���������
				for (String strOutRefundNo : strKeys) {
					// ��������
					String strTempId = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.POUND_FEE_TEMP_ID);
					mapArgs.clear();
					mapArgs.put("id", strTempId);
					String strWxRate = getTblFieldValue("wechat_rate", "t_servant", mapArgs);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.POUND_RATE, strWxRate);
					
					// ������
					String strRefundFee = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.REFUND_FEE);
					String strPoundFee = getPoundFeeBaseOnRate(strRefundFee, strWxRate, 0);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.SERVICE_POUND_FEE, strPoundFee);
				}
				
				// NOB(Harvest)���Ƿ���ڡ�NOB��ʵ�ʽ�Tecent���Ƿ���ڡ�Tencentʵ�ʷ��á�NOB��Tencent���ٵĽ����˽�����Ƿ�ת�Ƶ����㵥����
				for (String strOutRefundNo : strKeys) {
					// NOB���Ƿ����
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_NOB_EXIST, "1");
					
					// NOB�����Ĵ�����ʵ�ʽ��(�۳���Ѷ�������Ѻ�Ľ��)
					String strRefundFee = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.REFUND_FEE);
					String strServPoundFee = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.SERVICE_POUND_FEE);
					String strNobActFee = CommonTool.formatDoubleToHalfUp(Double.parseDouble(strRefundFee) - Double.parseDouble(strServPoundFee), 0, 0);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_NOB_ACT_FEE, strNobActFee);
					
					// Tecent���Ƿ����
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_WX_EXIST, "0");
					
					// Tencentʵ�ʷ���
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_WX_ACT_FEE, "0");
					
					// NOB��Tencent���ٵĽ��
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_NOB_LESS_WX_FEE, String.valueOf(0 - Integer.parseInt(strNobActFee)));
					
					// ���˽��(1: �ɹ�  0��ʧ��)
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_RESULT, "0");
					
					// �Ƿ�ת�Ƶ����㵥����
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.IS_TRANSF_SETTLE, "0");
				}
			} catch(SQLException se) {
				se.printStackTrace();
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
			}
			
			return mapOuterInfo;
		}

		@Override
		public Map<String, Map<String, String>> getWxMoreThanNobOrderRecord(String strReconStartDay,
				String strReconEndDay) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
			PreparedStatement prst = null;
			ResultSet rs = null;
			
			Map<String, Map<String, String>> mapOuterInfo = new HashMap<String, Map<String, String>>();
			try {
				/** ��ȡTencent����ڣ���NOB�����ڵĶ��˵�������Ϣ **/
				Map<String, String> mapInnerInfo = null;
				
				String strSql = "select a.out_refund_no out_refund_no, a.sub_mch_id sub_mch_id, a.refund_success_time refund_success_time, "
								+ " a.refund_fee refund_fee, a.discount_refund_fee discount_refund_fee, a.service_pound_fee service_pound_fee, "
								+ " a.pound_rate pound_rate from tbl_wx_refund_bill_info a"
								+ " where a.out_refund_no not in (select b.out_refund_no from tbl_refund_order b where date_format(b.refund_success_time, '%Y%m%d')>='" + strReconStartDay + "' "
										+ " and date_format(b.refund_success_time, '%Y%m%d')<='" + strReconEndDay + "' "
										+ " and b.refund_status='" + RefundOrderEntity.SUCCESS + "') "
								+ " and date_format(a.refund_success_time, '%Y%m%d')>='" + strReconStartDay + "' "
								+ " and date_format(a.refund_success_time, '%Y%m%d')<='" + strReconEndDay + "' "
								+ " and a.refund_status='" + RefundOrderEntity.SUCCESS + "';";
				
				System.out.println(">>>++--strSql = " + strSql);
				
				prst = conn.prepareStatement(strSql);
				rs = prst.executeQuery();
				while (rs.next()) {
					// У�鵱ǰ�����Ƿ����÷�Ӷģ�� 
					String strOutRefundNo = rs.getString("out_refund_no");
					if (strOutRefundNo != null && !"".equals(strOutRefundNo)) {
						boolean blnValRefTemp = this.validRefTemplet(strOutRefundNo);
						if (blnValRefTemp) {
							mapInnerInfo = new HashMap<String, String>();
							mapInnerInfo.put(ReconRefundBillOrderEntity.SUB_MCH_ID, rs.getString("sub_mch_id"));
							String strSrcRefundSuccTime = rs.getString("refund_success_time");
							Date srcDate = CommonTool.getDateBaseOnChars("yyyy-MM-dd HH:mm:ss", strSrcRefundSuccTime);
							String strFormatedDate = CommonTool.getFormatDateStr(srcDate, "yyyyMMddHHmmss");
							mapInnerInfo.put(ReconRefundBillOrderEntity.REFUND_SUCCESS_TIME, strFormatedDate);
							String strRefundFee = CommonTool.formatNullStrToZero(rs.getString("refund_fee"));
							Double dblRefundFee = Double.parseDouble(strRefundFee) * 100D;	// ����Ѷ���¼�Ľ�Ԫ��ת��Ϊ���֡���
							mapInnerInfo.put(ReconRefundBillOrderEntity.REFUND_FEE, CommonTool.formatDoubleToHalfUp(dblRefundFee, 0, 0));
							String strDiscountRefundFee = CommonTool.formatNullStrToZero(rs.getString("discount_refund_fee"));
							Double dblDiscountRefundFee = Double.parseDouble(strDiscountRefundFee) * 100D;	// ����Ѷ���¼�Ľ�Ԫ��ת��Ϊ���֡���
							mapInnerInfo.put(ReconRefundBillOrderEntity.DISCOUNT_REFUND_FEE, CommonTool.formatDoubleToHalfUp(dblDiscountRefundFee, 0, 0));
							String strServPoundFee = CommonTool.formatNullStrToZero(rs.getString("service_pound_fee"));
							Double dblServPoundFee = Math.abs(Double.parseDouble(strServPoundFee) * 100D);	// ����Ѷ���¼�Ľ�Ԫ��ת��Ϊ���֡���
																									// ���⣬������Ѷ���¼�ġ��˿������ѡ�Ϊ����������Ϊ�˱��ڼ��㣬�˴�ͳһ�޸�Ϊ����������
							mapInnerInfo.put(ReconRefundBillOrderEntity.SERVICE_POUND_FEE, CommonTool.formatDoubleToHalfUp(dblServPoundFee, 0, 0));
							String strPoundRate = rs.getString("pound_rate");
							strPoundRate = CommonTool.formatNullStrToSpace(strPoundRate).replace("%", "");	// ȥ����Ѷ�����ѵİٷֱ�(%)	
							mapInnerInfo.put(ReconRefundBillOrderEntity.POUND_RATE, strPoundRate);
							mapOuterInfo.put(strOutRefundNo, mapInnerInfo);
						}
					}
				}
				
				/** �����������ص���Ϣ **/
				Map<String, String> mapArgs = new HashMap<String, String>();
				String[] strKeys = mapOuterInfo.keySet().toArray(new String[0]);
				String strSubMchId = null;
				
				// ���������ID
				String strAgentId = null;
				for (String strOutRefundNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strAgentId = getTblFieldValue("agent_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.AGENT_ID, strAgentId);
				}
				
				// ����ģ��ID
				String strTempletID = null;
				for (String strOutRefundNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strTempletID = getTblFieldValue("servant_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.POUND_FEE_TEMP_ID, strTempletID);
				}
				
				// NOB(Harvest)���Ƿ���ڡ�NOB��ʵ�ʽ�Tecent���Ƿ���ڡ�Tencentʵ�ʷ��á�NOB��Tencent���ٵĽ����˽�����Ƿ�ת�Ƶ����㵥����
				for (String strOutRefundNo : strKeys) {
					// NOB���Ƿ����
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_NOB_EXIST, "0");
					
					// NOB��ʵ�ʽ��
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_NOB_ACT_FEE, "0");
					
					// Tecent���Ƿ����
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_WX_EXIST, "1");
					
					// Tencent�����Ĵ�����ʵ�ʷ���(�۳���Ѷ�����Ѻ�Ľ���λΪ����)
					String strRefundFee = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.REFUND_FEE);
					String strServicePoundFee = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.SERVICE_POUND_FEE);
					Double dblWxActFee = Double.parseDouble(strRefundFee) - Double.parseDouble(strServicePoundFee);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_WX_ACT_FEE, CommonTool.formatDoubleToHalfUp(dblWxActFee, 0, 0));
					
					// NOB��Tencent���ٵĽ��
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_NOB_LESS_WX_FEE, CommonTool.formatDoubleToHalfUp(dblWxActFee, 0, 0));
					
					// ���˽��(1: �ɹ�  0��ʧ��)
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_RESULT, "0");
					
					// �Ƿ�ת�Ƶ����㵥����
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.IS_TRANSF_SETTLE, "0");
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
				
				String strSql = "select a.out_refund_no out_refund_no, a.sub_mch_id sub_mch_id, a.refund_success_time refund_success_time, "
								+ " a.refund_fee refund_fee, a.discount_refund_fee discount_refund_fee, a.service_pound_fee service_pound_fee, "
								+ " a.pound_rate pound_rate from tbl_wx_refund_bill_info a, tbl_refund_order b "
								+ " where a.out_refund_no=b.out_refund_no and date_format(a.refund_success_time, '%Y%m%d')>='" + strReconStartDay + "' "
								+ " and date_format(a.refund_success_time, '%Y%m%d')<='" + strReconEndDay + "' "
								+ " and a.refund_status='" + RefundOrderEntity.SUCCESS + "' "
								+ " and date_format(b.refund_success_time, '%Y%m%d')>='" + strReconStartDay + "' "
								+ " and date_format(b.refund_success_time, '%Y%m%d')<='" + strReconEndDay + "' "
								+ " and b.refund_status='" + RefundOrderEntity.SUCCESS + "';";
				
				System.out.println("++>>>++--strSql = " + strSql);
				
				prst = conn.prepareStatement(strSql);
				rs = prst.executeQuery();
				while (rs.next()) {
					// У�鵱ǰ�����Ƿ����÷�Ӷģ�� 
					String strOutRefundNo = rs.getString("out_refund_no");
					if (strOutRefundNo != null && !"".equals(strOutRefundNo)) {
						boolean blnValRefTemp = this.validRefTemplet(strOutRefundNo);
						if (blnValRefTemp) {
							mapInnerInfo = new HashMap<String, String>();
							mapInnerInfo.put(ReconRefundBillOrderEntity.SUB_MCH_ID, rs.getString("sub_mch_id"));
							String strSrcRefundSuccTime = rs.getString("refund_success_time");
							Date srcDate = CommonTool.getDateBaseOnChars("yyyy-MM-dd HH:mm:ss", strSrcRefundSuccTime);
							String strFormatedDate = CommonTool.getFormatDateStr(srcDate, "yyyyMMddHHmmss");
							mapInnerInfo.put(ReconRefundBillOrderEntity.REFUND_SUCCESS_TIME, strFormatedDate);
							String strRefundFee = CommonTool.formatNullStrToZero(rs.getString("refund_fee"));
							Double dblRefundFee = Double.parseDouble(strRefundFee) * 100D;	// ����Ѷ���¼�Ľ��ɵ�λ��Ԫ��ת��Ϊ���֡�
							mapInnerInfo.put(ReconRefundBillOrderEntity.REFUND_FEE, CommonTool.formatDoubleToHalfUp(dblRefundFee, 0, 0));
							String strDiscountRefundFee = CommonTool.formatNullStrToZero(rs.getString("discount_refund_fee"));
							Double dblDiscountRefundFee = Double.parseDouble(strDiscountRefundFee) * 100D;	// ����Ѷ���¼�Ľ��ɵ�λ��Ԫ��ת��Ϊ���֡�
							mapInnerInfo.put(ReconRefundBillOrderEntity.DISCOUNT_REFUND_FEE, CommonTool.formatDoubleToHalfUp(dblDiscountRefundFee, 0, 0));
							String strServPoundFee = CommonTool.formatNullStrToZero(rs.getString("service_pound_fee"));
							Double dblServPoundFee = Math.abs(Double.parseDouble(strServPoundFee) * 100D);	// ����Ѷ���¼�Ľ��ɵ�λ��Ԫ��ת��Ϊ���֡�
																									// ���⣬������Ѷ���¼�ġ��˿������ѡ�Ϊ����������Ϊ�˱��ڼ��㣬�˴�ͳһ�޸�Ϊ����������
							mapInnerInfo.put(ReconRefundBillOrderEntity.SERVICE_POUND_FEE, CommonTool.formatDoubleToHalfUp(dblServPoundFee, 0, 0));
							String strPoundRate = rs.getString("pound_rate");
							strPoundRate = CommonTool.formatNullStrToSpace(strPoundRate).replace("%", "");	// ȥ����Ѷ�����ѵİٷֱ�(%)	
							mapInnerInfo.put(ReconRefundBillOrderEntity.POUND_RATE, strPoundRate);
							mapOuterInfo.put(strOutRefundNo, mapInnerInfo);
						}
					}
				}
				
				/** �����������ص���Ϣ **/
				Map<String, String> mapArgs = new HashMap<String, String>();
				String[] strKeys = mapOuterInfo.keySet().toArray(new String[0]);
				String strSubMchId = null;
				
				// ���������ID
				String strAgentId = null;
				for (String strOutRefundNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strAgentId = getTblFieldValue("agent_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.AGENT_ID, strAgentId);
				}
				
				// ����ģ��ID
				String strTempletID = null;
				for (String strOutRefundNo : strKeys) {
					strSubMchId = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.SUB_MCH_ID);
					mapArgs.clear();
					mapArgs.put("sub_merchant_code", strSubMchId);
					strTempletID = getTblFieldValue("servant_id", "t_merchant", mapArgs);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.POUND_FEE_TEMP_ID, strTempletID);
				}
				
				// NOB(Harvest)���Ƿ���ڡ�NOB��ʵ�ʽ�Tecent���Ƿ���ڡ�Tencentʵ�ʷ��á�NOB��Tencent���ٵĽ����˽�����Ƿ�ת�Ƶ����㵥����
				for (String strOutRefundNo : strKeys) {
					// NOB���Ƿ����
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_NOB_EXIST, "1");
					
					// NOB�����Ŀ۳���Ѷ�����Ѻ�������ʵ�ʽ��(��λΪ����)
					mapArgs.clear();
					mapArgs.put("out_refund_no", strOutRefundNo);
					String strRefundFee = getTblFieldValue("refund_fee", "tbl_refund_order", mapArgs);
					
					mapArgs.clear();
					mapArgs.put("id", mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.POUND_FEE_TEMP_ID));
					String strNobWxRate = getTblFieldValue("wechat_rate", "t_servant", mapArgs);	// Nob���¼����Ѷ�˷���
					String strNobSetlPoundFee = getPoundFeeBaseOnRate(strRefundFee, strNobWxRate, 0);
					String strNobActFee = CommonTool.formatDoubleToHalfUp(Double.parseDouble(strRefundFee) - Double.parseDouble(strNobSetlPoundFee), 0, 0);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_NOB_ACT_FEE, strNobActFee);
					
					// Tecent���Ƿ����
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_WX_EXIST, "1");
					
					// Tencent�����Ŀ۳���Ѷ�����Ѻ�������ʵ�ʷ���(��λΪ����)
					String strWxRefundFee = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.REFUND_FEE);
					String strServicePoundFee = mapOuterInfo.get(strOutRefundNo).get(ReconRefundBillOrderEntity.SERVICE_POUND_FEE);
					Double dblWxActFee = Double.parseDouble(strWxRefundFee) - Double.parseDouble(strServicePoundFee);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_WX_ACT_FEE, CommonTool.formatDoubleToHalfUp(dblWxActFee, 0, 0));
					
					// NOB��Tencent���ٵĽ��
					Double dblNobLessWxFee = dblWxActFee - Double.parseDouble(strNobActFee);
					String strNobLessWxFee = CommonTool.formatDoubleToHalfUp(dblNobLessWxFee, 0, 0);
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_NOB_LESS_WX_FEE, strNobLessWxFee);
					
					// ���˽��(1: �ɹ�  0��ʧ��)
					String strRecRst = null;
					String strWxPoundRate = mapOuterInfo.get(strOutRefundNo).get(ReconPayBillOrderEntity.POUND_RATE);
					double dblWxPoundRate = Double.parseDouble(strWxPoundRate);
					double dblNobPoundRate = Double.parseDouble(strNobWxRate);
					System.out.println("strOutRefundNo = " + strOutRefundNo);
					System.out.println("dblWxPoundRate = " + dblWxPoundRate);
					System.out.println("dblNobPoundRate = " + dblNobPoundRate);
					if ("0".equals(CommonTool.formatNullStrToZero(strNobLessWxFee))	// NOB��Tencent��ʵ�ʷ��ò��Ϊ0
							&& 	dblNobPoundRate == dblWxPoundRate	// ��Ѷ��������Ѷ����������NOB��ģ�嶨�����Ѷ�����������
							) {	
						strRecRst = "1";
					} else {
						strRecRst = "0";
					}
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.REC_RESULT, strRecRst);
					
					// �Ƿ�ת�Ƶ����㵥����
					mapOuterInfo.get(strOutRefundNo).put(ReconRefundBillOrderEntity.IS_TRANSF_SETTLE, "0");
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
			listArgs.add(new String[] {"date_format(refund_success_time, '%Y%m%d')", ">=", strReconStartDay});
			listArgs.add(new String[] {"date_format(refund_success_time, '%Y%m%d')", "<=", strReconEndDay});
			List<String> lstOutRefundNoRecOk = getTblFieldValueList("out_refund_no", "tbl_refund_order_recon_result", listArgs);
			
			// Ϊ�˲��ٸ��¶��˽�����ڶ��˳ɹ��ļ�¼����Ҫ������÷��������ݼ��޳���Щ�ɹ�������
			Map<String, Map<String, String>> mapNewOuterInfo = super.getNewOuterInfo(mapOuterInfo, lstOutRefundNoRecOk);
			
			try {
				String strSql = "replace into tbl_refund_order_recon_result(out_refund_no, sub_mch_id, agent_id, pound_fee_temp_id, "
								+ " refund_success_time, refund_fee, discount_refund_fee, service_pound_fee, pound_rate, rec_nob_exist, "
								+ " rec_nob_act_fee, rec_wx_exist, rec_wx_act_fee, rec_nob_less_wx_fee, rec_result, is_transf_settle) "
								+ " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
				prst = conn.prepareStatement(strSql);

				String[] strOutRefundNoS = mapNewOuterInfo.keySet().toArray(new String[0]);
				int iOutRefundNoSize = strOutRefundNoS.length; 
				for (int i = 0; i < iOutRefundNoSize; i++) {
					String strOutRefundNo = strOutRefundNoS[i];
					Map<String, String> mapInner = mapNewOuterInfo.get(strOutRefundNo);

					prst.setString(1, strOutRefundNo);
					prst.setString(2, mapInner.get(ReconRefundBillOrderEntity.SUB_MCH_ID));
					prst.setString(3, mapInner.get(ReconRefundBillOrderEntity.AGENT_ID));
					prst.setString(4, mapInner.get(ReconRefundBillOrderEntity.POUND_FEE_TEMP_ID));
					prst.setString(5, mapInner.get(ReconRefundBillOrderEntity.REFUND_SUCCESS_TIME));
					prst.setString(6, mapInner.get(ReconRefundBillOrderEntity.REFUND_FEE));
					prst.setString(7, mapInner.get(ReconRefundBillOrderEntity.DISCOUNT_REFUND_FEE));
					prst.setString(8, mapInner.get(ReconRefundBillOrderEntity.SERVICE_POUND_FEE));
					prst.setString(9, mapInner.get(ReconRefundBillOrderEntity.POUND_RATE));
					prst.setString(10, mapInner.get(ReconRefundBillOrderEntity.REC_NOB_EXIST));
					prst.setString(11, mapInner.get(ReconRefundBillOrderEntity.REC_NOB_ACT_FEE));
					prst.setString(12, mapInner.get(ReconRefundBillOrderEntity.REC_WX_EXIST));
					prst.setString(13, mapInner.get(ReconRefundBillOrderEntity.REC_WX_ACT_FEE));
					prst.setString(14, mapInner.get(ReconRefundBillOrderEntity.REC_NOB_LESS_WX_FEE));
					prst.setString(15, mapInner.get(ReconRefundBillOrderEntity.REC_RESULT));
					prst.setString(16, mapInner.get(ReconRefundBillOrderEntity.IS_TRANSF_SETTLE));

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
		 * У�鵱ǰ�˿�������̻��Ƿ�������Ӧ��ģ�塣
		 * @param strOrderNo
		 * @return
		 */
		@Override
		public boolean validRefTemplet(String strOrderNo) {
			boolean blnRefTemplet = false;
			Map<String, String> mapArgs = new HashMap<String, String>();
			mapArgs.put("out_refund_no", strOrderNo);
			String strOutTradeNo = getTblFieldValue("out_trade_no", "tbl_trans_order_refund_order", mapArgs);
			if (strOutTradeNo != null && !"".equals(strOutTradeNo)) {
				mapArgs.clear();
				mapArgs.put("out_trade_no", strOutTradeNo);
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
			}
			
			return blnRefTemplet;
		}
	}
}
