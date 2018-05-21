package com.chinaepay.wx.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.common.JsonRespObj;
import com.chinaepay.wx.entity.MergPayAndRefundSettleEntity;

import net.sf.json.JSONObject;

public class MergPayAndRefundSettleServlet extends ProcBillAndSettleOrderServlet {
	public void init() {
		try {
			super.init();
			
			String strHour = this.getServletContext().getInitParameter("Hour_ProcFinalSettle");
			String strMinute = this.getServletContext().getInitParameter("Minute_ProcFinalSettle");
			String strSecond = this.getServletContext().getInitParameter("Second_ProcFinalSettle");
			String strDelayTime = this.getServletContext().getInitParameter("DelayTime_ProcFinalSettle");
	
			MergPayAndRefundSettleThread mparst = new MergPayAndRefundSettleThread(strHour, strMinute, strSecond, strDelayTime);
			mparst.start();
		} catch (ServletException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		ClosableTimer closableTimer = new ClosableTimer(true);	// ִ���������ر�Timer
		TimerTask task = new ProcFinalSettleOrderTask(closableTimer);
        closableTimer.schedule(task, 0L);
        
        JsonRespObj respObj = new JsonRespObj();
		String strProResult = "1";
		String strReturnMsg = "����ģ��������յĽ��㵥���������Ѿ��ύ��ִ̨�У�";
        respObj.setRespCode(strProResult);
		respObj.setRespMsg(strReturnMsg);
		JSONObject jsonObj = JSONObject.fromObject(respObj);
		super.returnJsonObj(response, jsonObj);
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		doGet(request, response);
	}
	
	public class MergPayAndRefundSettleThread extends Thread {
		private String strHour = null;
		private String strMinute = null;
		private String strSecond = null;
		private String strDelayTime = null;
		
		public MergPayAndRefundSettleThread(String strHour, String strMinute, String strSecond, String strDelayTime) {
			this.strHour = strHour;
			this.strMinute = strMinute;
			this.strSecond = strSecond;
			this.strDelayTime = strDelayTime;
		}
		
		public void run() {
			// ��ǰʱ��
			Date nowDate =  new Date();
			long lngNowMillSec = nowDate.getTime();
			
			// ��ȡָ������������ʱ��
			Date defineDate = getFixDateBasedOnArgs(strHour, strMinute, strSecond);
			long lngDefMillSec = defineDate.getTime();
			
			ClosableTimer closableTimer = null;
			TimerTask task = null;
			if (lngNowMillSec < lngDefMillSec) {
				closableTimer = new ClosableTimer(false);
		        task = new ProcFinalSettleOrderTask(closableTimer);
		        closableTimer.scheduleAtFixedRate(task, defineDate, CommonInfo.ONE_DAY_TIME);
			}
			else {
				// ִ��һ������(����ץȡ���ݵ�ʱ��)���������������ر�
				closableTimer = new ClosableTimer(true);
				task = new ProcFinalSettleOrderTask(closableTimer);
				closableTimer.schedule(task, Long.parseLong(strDelayTime) * 60 * 1000);
				
				// �ڴ���18�㿪ʼִ��һ������ �Ժ�ÿ��24Сʱ����ѯ����ִ����ͬ����
				closableTimer = new ClosableTimer(false);
				task = new ProcFinalSettleOrderTask(closableTimer);
				Date nextDay = CommonTool.getBefOrAftDate(defineDate, CommonInfo.ONE_DAY_TIME);
				closableTimer.scheduleAtFixedRate(task, nextDay, CommonInfo.ONE_DAY_TIME);
			}
		}
	}
	
	
	public class ProcFinalSettleOrderTask extends TimerTask {
		private ClosableTimer closableTimer = null;
		
		public ProcFinalSettleOrderTask(ClosableTimer closableTimer) {
			super();
			this.closableTimer = closableTimer;
		}
		
		public void run() {
			/** ���м���㵥(����֧��/�˿�)��Ϣ����������ά�Ȳ��뵽���ս���ʱ����ʱ�� **/
			// ����м���㵥����ʱ��
			this.clearMidleSettleTbl();
			// ����֧������Ӧ���м���㵥����Ϣ���뵽��ʱ��
			this.inserPayMidleSettleList();
			// �����˿��Ӧ���м���㵥����Ϣ���뵽��ʱ��
			this.inserRefundMidleSettleList();	// ��ע�⡿���������Ѿ����˿�Ľ��ȡ����
			
			/** ���ɸ����������ս��㵥��Ϣ��������µ����ս��㵥�������м���㵥��ĸ���������״̬ **/
			// ���ɸ����������ս��㵥��Ϣ
			List<String[]> listFinalSettleInfo = this.getFinalSettleInfo();
			
			// ���µ����ս��㵥��, �Լ������м���㵥��ĸ���������״̬
			Connection conn = null;
			try {
				conn = MysqlConnectionPool.getInstance().getConnection(false);
				
				// ���µ����ս��㵥��
				List<String[]> lstOuterInfo = this.insertFinalSettleInfo(listFinalSettleInfo, conn);
				
				// ������ʱ���ڵĽ��㵥�š��Լ�����״̬(�������Ϊ0�Ľ��㵥������״ֱ̬�Ӹ���Ϊ���ѽ���)
				this.updateTempSettleOrderNo(lstOuterInfo, conn);
				
				/* �����м���㵥��ĸ���������״̬ */
				// �����м���㵥��״̬ǰ����ȡ�����š��������͡����ս��㵥ID
				List<String[]> listMidlSetlInfo = this.getMidlSetlInfo(conn);
				System.out.println("listMidlSetlInfo.size = " + listMidlSetlInfo.size());
				
				// ����[֧�������˿]������и�������״̬���Լ����ս��㵥��
				this.updatePayAndRefDetailInfo(listMidlSetlInfo, conn);
				
				// �ύ������ر�����ݣ���������һ����
				conn.commit();
			} catch (SQLException e) {
				e.printStackTrace();
				MysqlConnectionPool.getInstance().rollback(conn);
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(conn);
			}
			
			// ����ҵ��������ٴ�����м���㵥����ʱ��
			this.clearMidleSettleTbl();
			
			/** �ж��Ƿ���Ҫ�ر�����ʱ�� **/
			if (closableTimer.isNeedClose()) {
				closableTimer.cancel();
			}
		}
		
		/**
		 * ����м���㵥����ʱ��
		 */
		private void clearMidleSettleTbl() {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			String strSql = "delete from tbl_temp_settle_order;";
			System.out.println("++$+strSql = " + strSql);
			PreparedStatement prst = null;
			try {
				prst = conn.prepareStatement(strSql);
				prst.execute();
				conn.commit();
			} catch (SQLException e) {
				e.printStackTrace();
				MysqlConnectionPool.getInstance().rollback(conn);
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(prst, conn);
			}
		}
		
		/**
		 * ����֧������Ӧ���м���㵥����Ϣ���뵽��ʱ��
		 */
		private void inserPayMidleSettleList() {
			// ��ȡNOB����
			String strSql = "select '" + CommonInfo.NOB_MCH_ID + "' org_id, '" + MergPayAndRefundSettleEntity.ORG_TYPE_NOB + "' org_type, "
							+ " out_trade_no out_order_no, '" + MergPayAndRefundSettleEntity.ORDER_TYPE_PAYMENT + "' order_type, "
							+ " date_format(trans_time_end, '%Y%m%d') settle_belong_date, nob_pound_fee settle_fee_amount "
							+ " from tbl_pay_settle_detail_order where nob_settle_status in ('" 
							+ MergPayAndRefundSettleEntity.SETTLE_WAITING + "', '" + MergPayAndRefundSettleEntity.SETTLE_FAIL + "');";
			List<String[]> listNobTempInfo = this.getOrgTempOrder(strSql);
			System.out.println("++listNobTempInfo = " + listNobTempInfo);
			this.insertOrgTempOrder(listNobTempInfo);
			
			// ��ȡHarvest����
			strSql = "select '" + CommonInfo.HARVEST_ID + "' org_id, '" + MergPayAndRefundSettleEntity.ORG_TYPE_HARVEST + "' org_type, "
							+ " out_trade_no out_order_no, '" + MergPayAndRefundSettleEntity.ORDER_TYPE_PAYMENT + "' order_type, "
							+ " date_format(trans_time_end, '%Y%m%d') settle_belong_date, har_pound_fee settle_fee_amount "
							+ " from tbl_pay_settle_detail_order where har_settle_status in ('" 
							+ MergPayAndRefundSettleEntity.SETTLE_WAITING + "', '" + MergPayAndRefundSettleEntity.SETTLE_FAIL + "');";
			List<String[]> listHarTempInfo = this.getOrgTempOrder(strSql);
			System.out.println("++listHarTempInfo = " + listHarTempInfo);
			this.insertOrgTempOrder(listHarTempInfo);
			 
			// ��ȡAgent����
			strSql = "select agent_id org_id, '" + MergPayAndRefundSettleEntity.ORG_TYPE_AGENT + "' org_type, "
							+ " out_trade_no out_order_no, '" + MergPayAndRefundSettleEntity.ORDER_TYPE_PAYMENT + "' order_type, "
							+ " date_format(trans_time_end, '%Y%m%d') settle_belong_date, agen_pound_fee settle_fee_amount "
							+ " from tbl_pay_settle_detail_order where agen_settle_status in ('" 
							+ MergPayAndRefundSettleEntity.SETTLE_WAITING + "', '" + MergPayAndRefundSettleEntity.SETTLE_FAIL + "');";
			List<String[]> listAgenTempInfo = this.getOrgTempOrder(strSql);
			System.out.println("++listAgenTempInfo = " + listAgenTempInfo);
			this.insertOrgTempOrder(listAgenTempInfo);
			
			// ��ȡSubMch����
			strSql = "select sub_mch_id org_id, '" + MergPayAndRefundSettleEntity.ORG_TYPE_SUB_MCH + "' org_type, "
							+ " out_trade_no out_order_no, '" + MergPayAndRefundSettleEntity.ORDER_TYPE_PAYMENT + "' order_type, "
							+ " date_format(trans_time_end, '%Y%m%d') settle_belong_date, submch_settle_fee settle_fee_amount "
							+ " from tbl_pay_settle_detail_order where submch_settle_status in ('" 
							+ MergPayAndRefundSettleEntity.SETTLE_WAITING + "', '" + MergPayAndRefundSettleEntity.SETTLE_FAIL + "');";
			List<String[]> listSubmchTempInfo = this.getOrgTempOrder(strSql);
			System.out.println("++listSubmchTempInfo = " + listSubmchTempInfo);
			this.insertOrgTempOrder(listSubmchTempInfo);
		}
		
		/**
		 * �����˿��Ӧ���м���㵥����Ϣ���뵽��ʱ��
		 */
		private void inserRefundMidleSettleList() {
			// ��ȡNOB����
			String strSql = "select '" + CommonInfo.NOB_MCH_ID + "' org_id, '" + MergPayAndRefundSettleEntity.ORG_TYPE_NOB + "' org_type, "
							+ " out_refund_no out_order_no, '" + MergPayAndRefundSettleEntity.ORDER_TYPE_REFUND + "' order_type, "
							+ " date_format(refund_success_time, '%Y%m%d') settle_belong_date, 0 - nob_pound_fee settle_fee_amount "
							+ " from tbl_refund_settle_detail_order where nob_settle_status in ('" 
							+ MergPayAndRefundSettleEntity.SETTLE_WAITING + "', '" + MergPayAndRefundSettleEntity.SETTLE_FAIL + "');";
			List<String[]> listNobTempInfo = this.getOrgTempOrder(strSql);
			System.out.println("##listNobTempInfo = " + listNobTempInfo);
			this.insertOrgTempOrder(listNobTempInfo);
			
			// ��ȡHarvest����
			strSql = "select '" + CommonInfo.HARVEST_ID + "' org_id, '" + MergPayAndRefundSettleEntity.ORG_TYPE_HARVEST + "' org_type, "
							+ " out_refund_no out_order_no, '" + MergPayAndRefundSettleEntity.ORDER_TYPE_REFUND + "' order_type, "
							+ " date_format(refund_success_time, '%Y%m%d') settle_belong_date, 0 - har_pound_fee settle_fee_amount "
							+ " from tbl_refund_settle_detail_order where har_settle_status in ('" 
							+ MergPayAndRefundSettleEntity.SETTLE_WAITING + "', '" + MergPayAndRefundSettleEntity.SETTLE_FAIL + "');";
			List<String[]> listHarTempInfo = this.getOrgTempOrder(strSql);
			System.out.println("##listHarTempInfo = " + listHarTempInfo);
			this.insertOrgTempOrder(listHarTempInfo);
			 
			// ��ȡAgent����
			strSql = "select agent_id org_id, '" + MergPayAndRefundSettleEntity.ORG_TYPE_AGENT + "' org_type, "
							+ " out_refund_no out_order_no, '" + MergPayAndRefundSettleEntity.ORDER_TYPE_REFUND + "' order_type, "
							+ " date_format(refund_success_time, '%Y%m%d') settle_belong_date, 0 - agen_pound_fee settle_fee_amount "
							+ " from tbl_refund_settle_detail_order where agen_settle_status in ('" 
							+ MergPayAndRefundSettleEntity.SETTLE_WAITING + "', '" + MergPayAndRefundSettleEntity.SETTLE_FAIL + "');";
			List<String[]> listAgenTempInfo = this.getOrgTempOrder(strSql);
			System.out.println("##listAgenTempInfo = " + listAgenTempInfo);
			this.insertOrgTempOrder(listAgenTempInfo);
			
			// ��ȡSubMch����
			strSql = "select sub_mch_id org_id, '" + MergPayAndRefundSettleEntity.ORG_TYPE_SUB_MCH + "' org_type, "
							+ " out_refund_no out_order_no, '" + MergPayAndRefundSettleEntity.ORDER_TYPE_REFUND + "' order_type, "
							+ " date_format(refund_success_time, '%Y%m%d') settle_belong_date, 0 - submch_settle_fee settle_fee_amount "
							+ " from tbl_refund_settle_detail_order where submch_settle_status in ('" 
							+ MergPayAndRefundSettleEntity.SETTLE_WAITING + "', '" + MergPayAndRefundSettleEntity.SETTLE_FAIL + "');";
			List<String[]> listSubmchTempInfo = this.getOrgTempOrder(strSql);
			System.out.println("##listSubmchTempInfo = " + listSubmchTempInfo);
			this.insertOrgTempOrder(listSubmchTempInfo);
		}
		
		/**
		 * ���ɸ����������ս��㵥��Ϣ��
		 * @return
		 */
		private List<String[]> getFinalSettleInfo() {
			List<String[]> listFinalSettleInfo = new ArrayList<String[]>();
			
			Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
			PreparedStatement prst = null;
			ResultSet rs = null;
			
			try {
				String strSql = "select org_id, org_type, settle_belong_date, sum(settle_fee_amount) settle_fee_amount from tbl_temp_settle_order "
								+ " group by org_id, settle_belong_date, org_type order by org_id, settle_belong_date;";
				System.out.println("###strSql = " + strSql);
				prst = conn.prepareStatement(strSql);
				rs = prst.executeQuery();
				while (rs.next()) {
					String strSetlOrderId = CommonTool.getRandomString(32);
					String strOrgId = rs.getString("org_id");
					String strOrgType = rs.getString("org_type");
					String strSetlBelongDate = rs.getString("settle_belong_date");
					String strSetlFeeAmount = rs.getString("settle_fee_amount");
					String[] strFinalSetInfo = new String[] {strSetlOrderId, strOrgId, strOrgType, strSetlBelongDate, strSetlFeeAmount};
					listFinalSettleInfo.add(strFinalSetInfo);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
			}
			return listFinalSettleInfo;
		}
		
		/**
		 * ���µ����ս��㵥��
		 * @param listFinalSettleInfo
		 * @throws SQLException 
		 */
		private List<String[]> insertFinalSettleInfo(List<String[]> listFinalSettleInfo, Connection conn) throws SQLException  {
			List<String[]> lstOuterInfo = new ArrayList<String[]>();
			
			String strSql = "replace into tbl_settlement_sum_order(settle_order_id, org_id, org_type, settle_belong_date, settle_batch_no, "
							+ " settle_start_time, settle_end_time, settle_fee_amount, settle_fee_type, settle_status) "
							+ " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
			PreparedStatement prst = conn.prepareStatement(strSql);
			
			for (int i = 0; i < listFinalSettleInfo.size(); i++) {
				String[] strFinalSetlInfo = listFinalSettleInfo.get(i);
				String strSetlOrderId = strFinalSetlInfo[0];
				String strOrgId = strFinalSetlInfo[1];
				String strOrgType = strFinalSetlInfo[2];
				String strSetlBelongDate = strFinalSetlInfo[3];
				String strSetlBatchNo = this.getPlusedBatchNo(strOrgId, strSetlBelongDate);
				String strSetlStartTime = CommonTool.getFormatDateStr(new Date(), "yyyyMMddHHmmss");
				String strSetlEndTime = "";
				String strSetlFeeAmount = strFinalSetlInfo[4];
				System.out.println("strSetlFeeAmount = " + strSetlFeeAmount);

				String strSetlFeeType = "USD";
				String strSetlStatus = null;
				// ��������>0(����¼�����)��������<0(Ϊ���̻������)�ļ�¼���
				// ���㵥�ڽ���λ�ɡ��֡�ת���ɡ�Ԫ��
				String strUSDYuan = CommonTool.formatCentToYuan(strSetlFeeAmount, 2);
				double dblUSDYuan = Double.parseDouble(strUSDYuan);
				if (dblUSDYuan == 0.00d) {
					strSetlStatus = MergPayAndRefundSettleEntity.SETTLE_SUCESS;	// ������Ϊ0ʱ������״̬Ϊ���ѽ��㡱
					strUSDYuan = String.valueOf(CommonTool.formatDoubleToHalfUp(Math.abs(dblUSDYuan), 2, 2));
					strSetlEndTime = CommonTool.getFormatDateStr(new Date(), "yyyyMMddHHmmss");
				} else {
					strSetlStatus = MergPayAndRefundSettleEntity.SETTLE_PROCESSING;	// �����Ϊ0ʱ������״̬Ϊ�������С�
				}
				prst.setString(1, strSetlOrderId);
				prst.setString(2, strOrgId);
				prst.setString(3, strOrgType);
				prst.setString(4, strSetlBelongDate);
				prst.setString(5, strSetlBatchNo);
				prst.setString(6, strSetlStartTime);
				prst.setString(7, strSetlEndTime);
				prst.setString(8, strUSDYuan);
				prst.setString(9, strSetlFeeType);
				prst.setString(10, strSetlStatus);
				
				prst.addBatch();
				
				/** ���ɸ�����ʱ����㵥�š����㵥״̬��صļ��� **/
				String[] strOutValues = new String[] {strOrgId, strSetlBelongDate, strSetlOrderId, strSetlStatus};
				lstOuterInfo.add(strOutValues);
			}
			
			prst.executeBatch();
			prst.close();
			
			return lstOuterInfo;
		}
		
		/**
		 * �����м���㵥��ʱ���ڽ��㵥ID��
		 * @param listFinalSettleInfo
		 * @param prst
		 * @throws SQLException 
		 */
		private void updateTempSettleOrderNo(List<String[]> lstOuterInfo, Connection conn) throws SQLException {
			String strSql = "update tbl_temp_settle_order set settle_order_id=?, settle_status=? where org_id=? and settle_belong_date=?;";
			PreparedStatement prst = conn.prepareStatement(strSql);
			
			for (String[] strOuterInfo : lstOuterInfo) {
				String strOrgId = strOuterInfo[0];
				String strSetlBelongDate = strOuterInfo[1];
				String strSetlOrderId = strOuterInfo[2];
				String strSetlStatus = strOuterInfo[3];
				
				prst.setString(1, strSetlOrderId);
				prst.setString(2, strSetlStatus);
				prst.setString(3, strOrgId);
				prst.setString(4, strSetlBelongDate);
				
				prst.addBatch();
			}
			
			
			prst.executeBatch();
			prst.close();
		}
		
		/**
		 * �����м���㵥��״̬ǰ������ʱ���ȡ�����š��������͡����ս��㵥ID��
		 * @param listFinalSettleInfo
		 * @return
		 * @throws SQLException 
		 */
		private List<String[]> getMidlSetlInfo(Connection conn) throws SQLException {
			List<String[]> listMidlSetlInfo = new ArrayList<String[]>();
			
			String strSql = "select order_type, out_order_no, org_type, settle_order_id, settle_status from tbl_temp_settle_order group by order_type, out_order_no, org_type, settle_order_id, settle_status;";
			System.out.println("%%%=" + strSql);
			PreparedStatement prst = conn.prepareStatement(strSql);
			ResultSet rs = prst.executeQuery();
			while (rs.next()) {
				String[] strMidlSetlInfo = new String[] {rs.getString("order_type"), rs.getString("out_order_no"), rs.getString("org_type"), rs.getString("settle_order_id"), rs.getString("settle_status")};
				listMidlSetlInfo.add(strMidlSetlInfo);
			}
			
			return listMidlSetlInfo;
		}
		
		/**
		 * ����[֧�������˿]������и�������״̬���Լ����ս��㵥�š�
		 * @param listMidlSetlInfo
		 * @param conn
		 * @return
		 * @throws SQLException 
		 */
		private void updatePayAndRefDetailInfo(List<String[]> listMidlSetlInfo, Connection conn) throws SQLException {
			
			PreparedStatement prst = null;
			for (String[] strOrderInfos : listMidlSetlInfo) {
				String strOrderType = strOrderInfos[0];
				String strOutOrderNo = strOrderInfos[1];
				String strOrgType = strOrderInfos[2];
				String strSetlOrderId = strOrderInfos[3];
				String strSetlOrderStatus = strOrderInfos[4];
				String strUpSql = "update TABLE set COLUMN_SETTLE_ORDER_ID='" + strSetlOrderId + "', COLUMN_SETTLE_STATUS='" 
									+ strSetlOrderStatus + "' where ORDER_NO='" + strOutOrderNo + "'";
				if (MergPayAndRefundSettleEntity.ORDER_TYPE_PAYMENT.equals(CommonTool.formatNullStrToSpace(strOrderType))) {
					strUpSql = strUpSql.replace("TABLE", "tbl_pay_settle_detail_order").replace("ORDER_NO", "out_trade_no");
				} else if (MergPayAndRefundSettleEntity.ORDER_TYPE_REFUND.equals(CommonTool.formatNullStrToSpace(strOrderType))) {
					strUpSql = strUpSql.replace("TABLE", "tbl_refund_settle_detail_order").replace("ORDER_NO", "out_refund_no");
				}
				
				if (MergPayAndRefundSettleEntity.ORG_TYPE_NOB.equals(CommonTool.formatNullStrToSpace(strOrgType))) {	// ��������ΪNOB
					strUpSql = strUpSql.replace("COLUMN_SETTLE_ORDER_ID", "nob_settle_order_id").replace("COLUMN_SETTLE_STATUS", "nob_settle_status");
				} else if (MergPayAndRefundSettleEntity.ORG_TYPE_HARVEST.equals(CommonTool.formatNullStrToSpace(strOrgType))) {	// ��������ΪHarvest
					strUpSql = strUpSql.replace("COLUMN_SETTLE_ORDER_ID", "har_settle_order_id").replace("COLUMN_SETTLE_STATUS", "har_settle_status");
				} else if (MergPayAndRefundSettleEntity.ORG_TYPE_AGENT.equals(CommonTool.formatNullStrToSpace(strOrgType))) {	// ��������ΪAgent
					strUpSql = strUpSql.replace("COLUMN_SETTLE_ORDER_ID", "agen_settle_order_id").replace("COLUMN_SETTLE_STATUS", "agen_settle_status");
				} else if (MergPayAndRefundSettleEntity.ORG_TYPE_SUB_MCH.equals(CommonTool.formatNullStrToSpace(strOrgType))) {	// ��������ΪSubmch
					strUpSql = strUpSql.replace("COLUMN_SETTLE_ORDER_ID", "submch_settle_order_id").replace("COLUMN_SETTLE_STATUS", "submch_settle_status");
				}
				
				System.out.println(">>->>strUpSql = " + strUpSql);
				prst = conn.prepareStatement(strUpSql);
				prst.executeUpdate();
				prst.close();
			}
		}
		
		/**
		 * ��ȡ������㵥�м������Ķ�����Ϣ��
		 * @param strSql
		 * @return
		 */
		private List<String[]> getOrgTempOrder(String strSql) {
			List<String[]> listTempInfo = new ArrayList<String[]>();
			
			Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
			PreparedStatement prst = null;
			ResultSet rs = null;
			
			try {
				System.out.println("#++-strSql = " + strSql);
				prst = conn.prepareStatement(strSql);
				rs = prst.executeQuery();
				while (rs.next()) {
					String strOrgId = rs.getString("org_id");
					String strOutOrderNo = rs.getString("out_order_no");
					String strOrgType = rs.getString("org_type");
					String strOrderType = rs.getString("order_type");
					String strSetBelongDate = rs.getString("settle_belong_date");
					String strSetFeeAmount = rs.getString("settle_fee_amount");
					
					System.out.println("strOrgId = " + strOrgId);
					System.out.println("strOutOrderNo = " + strOutOrderNo);
					System.out.println("strOrgType = " + strOrgType);
					System.out.println("strOrderType = " + strOrderType);
					System.out.println("strSetBelongDate = " + strSetBelongDate);
					System.out.println("strSetFeeAmount = " + strSetFeeAmount);
					
					String[] strOrderInfo = new String[] {strOrgId, strOutOrderNo, strOrgType, strOrderType, strSetBelongDate, strSetFeeAmount};
					System.out.println("+++--strOrderInfo = " + strOrderInfo);
					
					listTempInfo.add(strOrderInfo);
					System.out.println("+++--listTempInfo = " + listTempInfo);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
			}
			
			return listTempInfo;
		}
		
		/**
		 * ����������ȡ������ʱ���ݲ��뵽��ʱ���С�
		 * @param listOrgTempInfo
		 */
		private void insertOrgTempOrder(List<String[]> listOrgTempInfo) {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
			PreparedStatement prst = null;
			
			try {
				// ����ʱ���������
				String strSql = "replace into tbl_temp_settle_order(org_id, out_order_no, org_type, order_type, settle_belong_date, settle_fee_amount) "
								+ " values(?, ?, ?, ?, ?, ?);";
				System.out.println("++-++strSql = " + strSql);
				System.out.println("listOrgTempInfo.size = " + listOrgTempInfo.size());
				prst = conn.prepareStatement(strSql);
				for (int i = 0; i < listOrgTempInfo.size(); i++) {
					String[] strOrderInfo = listOrgTempInfo.get(i);
					prst.setString(1, strOrderInfo[0]);
					prst.setString(2, strOrderInfo[1]);
					prst.setString(3, strOrderInfo[2]);
					prst.setString(4, strOrderInfo[3]);
					prst.setString(5, strOrderInfo[4]);
					prst.setString(6, strOrderInfo[5]);
					prst.addBatch();
				}
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
		 * ��ȡĳ���������������ĵ����κţ��������1��Ȼ�󷵻ء�
		 * @param strOrgId
		 * @param strSetlBelongDate
		 * @return
		 * @throws SQLException 
		 */
		private String getPlusedBatchNo(String strOrgId, String strSetlBelongDate) throws SQLException {
			Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
			String strSql = "select (max(SUBSTRING_INDEX(settle_batch_no, '_', -1)) + 1) as plused_batch_no from tbl_settlement_sum_order where org_id='" 
							+ strOrgId + "' and settle_belong_date='" + strSetlBelongDate + "';";
			PreparedStatement prst = conn.prepareStatement(strSql);
			ResultSet rs = prst.executeQuery();
			String strPlusedBatchNo = null;
			if (rs.next()) {
				strPlusedBatchNo = rs.getString("plused_batch_no");
			}
			
			if (strPlusedBatchNo == null || "".equals(strPlusedBatchNo)) {
				strPlusedBatchNo = "0001";
			} else {
				strPlusedBatchNo = CommonTool.getFixLenStr(strPlusedBatchNo, "0", 4);
			}
			
			String strFinlRst = strSetlBelongDate + "_" + strPlusedBatchNo;
			return strFinlRst;
		}
	}
}
