package com.chinaepay.wx.entity;

public class ProcBillAndSettleOrderEntity extends CommunicateEntity {
	// ģ��ID
	public static final String POUND_FEE_TEMP_ID = "pound_fee_temp_id";
	// ����������
	public static final String DISCOUNT_FEE = "discount_fee";
	// �˿�����������
	public static final String DISCOUNT_REFUND_FEE = "discount_refund_fee";
	
	// ��Ѷ��������
	public static final String SERVICE_POUND_FEE = "service_pound_fee";
	// ��Ѷ����������
	public static final String POUND_RATE = "pound_rate";
	
	// NOB���Ƿ���ڼ�¼
	public static final String REC_NOB_EXIST = "rec_nob_exist";
	// NOB��ʵ�ʽ��
	public static final String REC_NOB_ACT_FEE = "rec_nob_act_fee";
	// Tencent���Ƿ���ڼ�¼
	public static final String REC_WX_EXIST = "rec_wx_exist";
	// Tencent��ʵ�ʽ��
	public static final String REC_WX_ACT_FEE = "rec_wx_act_fee";
	// NOB���Tencent�����ٵĽ��(��λ����)
	public static final String REC_NOB_LESS_WX_FEE = "rec_nob_less_wx_fee";
	// ���˵��˶Խ��
	public static final String REC_RESULT = "rec_result";
	// �Ƿ�ת����㵥�������
	public static final String IS_TRANSF_SETTLE = "is_transf_settle";
	// �������
	public static final String SETTLEMENT_FEE_TYPE = "settlementfee_type";
	// ���ս��㵥��
	public static final String SETTLEMENT_ORDER_ID = "settle_order_id";
	// NOB������
	public static final String NOB_POUND_FEE = "nob_pound_fee";
	// NOB����״̬
	public static final String NOB_SETTLE_STATUS = "nob_settle_status";
	// NOB���㵥ID
	public static final String NOB_SETTLE_ID = "nob_settle_id";
//	// NOB���κ�
//	public static final String NOB_SETTLE_BACH_NO = "nob_settle_bach_no";
	// Harvest������
	public static final String HAR_POUND_FEE = "har_pound_fee";
	// Harvest����״̬
	public static final String HAR_SETTLE_STATUS = "har_settle_status";
	// Harvest���㵥ID
	public static final String HAR_SETTLE_ID = "har_settle_id";
//	// Harvest���κ�
//	public static final String HAR_SETTLE_BACH_NO = "har_settle_bach_no";
	// Agent������
	public static final String AGEN_POUND_FEE = "agen_pound_fee";
	// Agent����״̬
	public static final String AGEN_SETTLE_STATUS = "agen_settle_status";
	// Agent���㵥ID
	public static final String AGEN_SETTLE_ID = "agen_settle_id";
//	// Agent���κ�
//	public static final String AGEN_SETTLE_BACH_NO = "agen_settle_bach_no";
	// SUBMCHʵ���������
	public static final String SUBMCH_SETTLE_FEE = "submch_settle_fee";
	// SUBMCH����״̬
	public static final String SUBMCH_SETTLE_STATUS = "submch_settle_status";
	// SUBMCH���㵥ID
	public static final String SUBMCH_SETTLE_ID = "submch_settle_id";
//	// SUBMCH���κ�
//	public static final String SUBMCH_SETTLE_BACH_NO = "submch_settle_bach_no";
	
	// ��֯���ͣ�sub_mch: ���̻�  agent������   harvest���׸�ͨ����  nob��NOB����
	// ���̻�
	public static final String ORG_TYPE_SUB_MCH = "Submch";
	// ����
	public static final String ORG_TYPE_AGENT = "Agent";
	// �׸�ͨ����
	public static final String ORG_TYPE_HARVEST = "Harvest";
	// NOB����
	public static final String ORG_TYPE_NOB = "Nob";
	
	// ��������
	// ֧����
	public static final String ORDER_TYPE_PAYMENT = "Payment";  
	// �˿
	public static final String ORDER_TYPE_REFUND = "Refund";
		
	/** ���ս��㵥��Ϣ **/
	// ����ID
	public static final String ORG_ID = "org_id";
	// ���㵥���κ�
	public static final String SETTLE_BATCHNO = "settle_batchno";
	// ��������, sub_mch: ���̻�  agent������   harvest���׸�ͨ����  nob��NOB����
	public static final String ORG_TYPE = "org_type";
	// ���㵥��Ӧ�Ľ�������
	public static final String SETTLE_BELONG_DATE = "settle_belong_date";
	// ���㿪ʼʱ��
	public static final String SETTLE_START_TIME = "settle_start_time";
	// �������ʱ��
	public static final String SETTLE_END_TIME = "settle_end_time";
	// ������ 
	public static final String SETTLE_FEE_AMOUNT = "settle_fee_amount";
	// �����������
	public static final String SETTLE_FEE_TYPE = "settle_fee_type";
	// ����״̬
	public static final String SETTLE_STATUS = "settle_status";
	// �̻���������
	public static final String ORG_BANK_NAME = "org_bank_name";
	// �̻������˻�
	public static final String ORG_BANK_ACCOUNT = "org_bank_account";
	
	/** ����״̬��ֵ(0��������  1��������  2���ѽ���  3������ʧ��) **/
	// ������
	public static final String SETTLE_WAITING = "0";
	// ������
	public static final String SETTLE_PROCESSING = "1";
	// �ѽ���
	public static final String SETTLE_SUCESS = "2";
	// ����ʧ��
	public static final String SETTLE_FAIL = "3";
}
