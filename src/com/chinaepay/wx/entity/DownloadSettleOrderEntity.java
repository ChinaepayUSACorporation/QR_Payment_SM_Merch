package com.chinaepay.wx.entity;

public class DownloadSettleOrderEntity extends InquiryEntity {
	// ���㵥����״̬���������С�
	public static final String SETTLE_PROCESSING = "0";
	// ���㵥����״̬��������ɹ���
	public static final String SETTLE_PROC_SUCCESS = "1";
	// ���㵥����״̬��������ʧ�ܡ�
	public static final String SETTLE_PROC_FAIL = "2";
	
	// ����״̬
	public static final String USETAG = "usetag";
	// �ѽ����ѯ
	public static final String HAS_SETTLED_INQUIRY ="1";
	// δ�����ѯ
	public static final String No_SETTLED_INQUIRY ="2";
	
	// ������
	public static final String OFFSET = "offset";
	// ����¼��
	public static final String LIMIT = "limit";
	
	// ���㵥������ʼ����
	public static final String DATE_START = "date_start";
	// ���㵥��������
	public static final String DATE_END = "date_end"; 
	
	// ��ѯһ��ʱ����ڵĽ��㵥ʱ�����ص�������
	public static final String RECORD_NUM = "record_num";
	
	// ���㵥���κ�
	public static final String FBATCHNO = "fbatchno";
	// �������ڣ�=�����׽������ڡ���
	public static final String DATE_SETTLEMENT = "date_settlement";
	// ���˽��
	public static final String SETTLEMENT_FEE = "settlement_fee";
	// δ���˽��
	public static final String UNSETTLEMENT_FEE = "unsettlement_fee";
	// �������
	public static final String SETTLEMENTFEE_TYPE = "settlementfee_type";
	// ֧�����
	public static final String PAY_FEE = "pay_fee";
	// ֧������
	public static final String PAY_NET_FEE = "pay_net_fee";
	// �����ѽ��
	public static final String POUNDAGE_FEE = "poundage_fee";
}
