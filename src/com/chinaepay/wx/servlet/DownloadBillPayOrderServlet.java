package com.chinaepay.wx.servlet;

import com.chinaepay.wx.entity.DownloadBillPayOrderEntity;

/**
 * ����֧������Ӧ�Ķ��˵���Ϣ��
 * @author xinwuhen
 */
public class DownloadBillPayOrderServlet extends DownloadBillOrderServlet {
	@Override
	public DownloadBillOrderTask getNewDownloadBillOrderTask(ClosableTimer closableTimer) {
		return new DownloadBillPayOrderTask(closableTimer);
	}
	
	@Override
	public DownloadBillOrderTask getNewDownloadBillOrderTask(ClosableTimer closableTimer, String startTimeForBillOrderSucc, String endTimeForBillOrderSucc) {
		return new DownloadBillPayOrderTask(closableTimer, startTimeForBillOrderSucc, endTimeForBillOrderSucc);
	}
	
	/**
	 * ���ض��˵���֧����������
	 * @author xinwuhen
	 */
	public class DownloadBillPayOrderTask extends DownloadBillOrderTask {
		public DownloadBillPayOrderTask(ClosableTimer closableTimer) {
			super(closableTimer);
		}
		
		public DownloadBillPayOrderTask(ClosableTimer closableTimer, String startTimeForBillOrderSucc, String endTimeForBillOrderSucc) {
			super(closableTimer, startTimeForBillOrderSucc, endTimeForBillOrderSucc);
		}

		@Override
		public void run() {
			downloadAndUpdateBillInfo(DownloadBillPayOrderEntity.SUCCESS);
		}
		
		/**
		 * ���ɲ�ͬ�Ķ��˵�������Ҫ��SQL��䣨�������£���
		 * @return
		 */
		public String getUpdateBillOrderBatchSql() {
			String strSql = "replace into tbl_wx_pay_bill_info(trans_time, appid, mch_id, sub_mch_id, device_info, transaction_id, out_trade_no, "
					+ " openid, trade_type, trade_state, bank_type, fee_type, total_fee, discount_fee, goods_name, mch_data_info, service_pound_fee, "
					+ " pound_rate, cash_fee_type, cash_fee, settl_currency_type, settl_currency_amount, exchange_rate) "
					+ " values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
			
			return strSql;
		}
	}
}
