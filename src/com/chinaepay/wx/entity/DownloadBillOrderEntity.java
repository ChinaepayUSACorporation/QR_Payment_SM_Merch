package com.chinaepay.wx.entity;

public class DownloadBillOrderEntity extends InquiryEntity {
	// ���˵�����Ϊ��֧������
	public static final String BILL_ORDER_PAYMENT = "bill_payment";
	// ���˵�����Ϊ���˿��
	public static final String BILL_ORDER_REFUND = "bill_refund";
	
	// ���˵�����״̬���������С�
	public static final String BILL_PROCESSING = "0";
	// ���˵�����״̬��������ɹ���
	public static final String BILL_PROC_SUCCESS = "1";
	// ���˵�����״̬��������ʧ�ܡ�
	public static final String BILL_PROC_FAIL = "2";
}
