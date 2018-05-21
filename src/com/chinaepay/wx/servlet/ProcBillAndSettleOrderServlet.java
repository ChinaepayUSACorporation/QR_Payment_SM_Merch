package com.chinaepay.wx.servlet;

import com.chinaepay.wx.common.CommonTool;

public abstract class ProcBillAndSettleOrderServlet extends CommControllerServlet {

	@Override
	public boolean validSubMchIsUsable(String strSubMerchId) {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * ͨ��������ܽ��Լ����ʰٷֱȣ����������ѽ��(���������룬��λΪ����)
	 * @param strTotalFee	�ܽ���λΪ����
	 * @param strOrgRate	����(�ٷ���), �磺0.3�����0.3%�ķ��ʡ�
	 * @return
	 */
	public String getPoundFeeBaseOnRate(String strTotalFee, String strOrgRate, int iPointNum) {
		Double dblTotlFee = Double.parseDouble(CommonTool.formatNullStrToZero(strTotalFee));
		Double dblRate = Double.parseDouble(CommonTool.formatNullStrToZero(strOrgRate));
		
		double dblRst = (dblTotlFee * dblRate) / 100;
		String strFinalRst = CommonTool.formatDoubleToHalfUp(dblRst, iPointNum, iPointNum);
		System.out.println("strFinalRst = " +  strFinalRst);
		
		return strFinalRst;
	}
}
