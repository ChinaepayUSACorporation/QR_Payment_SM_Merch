package com.chinaepay.wx.servlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.entity.CommunicateEntity;

public abstract class TransControllerServlet extends CommControllerServlet {
	
	/**
	 * �������ն��̻�����ĳ�ʼ��������Ϣ�����ݿ⡣
	 */
	public abstract boolean insertOrderInfoToTbl(Map<String, String> mapArgs);
	
	/**
	 * �����׸�ͨ��̨����Ѷ��̨������Ľ������Ӧ�����ݿ��
	 */
	public abstract boolean updateOrderRstToTbl(Map<String, String> mapArgs);
	
	/**
	 * У���̻��Ƿ���֧�����˿�رն�����Ȩ�ޡ�
	 */
	public boolean validSubMchIsUsable(String strSubMerchId) {
		boolean blnValSubMchIsUsable = false;
		Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
		PreparedStatement prst = null;
		ResultSet rs = null;
		try {
			String strSql = "select * from t_merchant where sub_merchant_code='" + CommonTool.formatNullStrToSpace(strSubMerchId) + "';";
			prst = conn.prepareStatement(strSql);
			rs = prst.executeQuery();
			if (rs.next()) {
				int iAuditStatus = rs.getInt("audit_status");
				int iStatus = rs.getInt("status");
				int iAccountStatus = rs.getInt("account_status");
				String strDelFlag = rs.getString("del_flag");
				
				System.out.println("strSql = " + strSql);
				System.out.println("iAuditStatus = " + iAuditStatus);
				System.out.println("iStatus = " + iStatus);
				System.out.println("iAccountStatus = " + iAccountStatus);
				System.out.println("strDelFlag = " + strDelFlag);
				
				if (iAuditStatus == Integer.valueOf(CommunicateEntity.AUDIT_STATUS_OK) // ���״̬   1 ����� 2��Ψ��� 3������� 4���ͨ��  -1��˲�ͨ��
						&& iStatus == Integer.valueOf(CommunicateEntity.MERCHANT_STATUS_OK)	// �̻�״̬ 1 ���� 2����
						&& iAccountStatus == Integer.valueOf(CommunicateEntity.ACCOUNT_STATUS_OK)	// �˻�״̬  1���� 2 ����
						&& CommonTool.formatNullStrToSpace(strDelFlag).equals(CommunicateEntity.ACCOUNT_DELETED_NG)) {	// 1:��ɾ��  0:δɾ��
					blnValSubMchIsUsable = true;
				}
			}
		} catch (SQLException se) {
			se.printStackTrace();
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
		}
		
		return blnValSubMchIsUsable;
	}
	
	/**
	 * ͨ�����̻�ID����ȡ��ǰ����ǩ��״̬�ĵ�ԱID��
	 * @param strTermMchId
	 * @return
	 */
	public String getSignedAssitantid(String strTermMchId) {
		String strSignedAssitantId = "";
		Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
		PreparedStatement prst = null;
		ResultSet rs = null;
		try {
			String strSql = "select * from t_assistant_sign where merchant_id='" + strTermMchId + "' and status=1 and del_flag='0';"; // statusΪ1�����ߣ�2�����ߡ�del_flagΪ1:��ɾ�� ; 0��δɾ���� 
			conn = MysqlConnectionPool.getInstance().getConnection(true);
			prst = conn.prepareStatement(strSql);
			rs = prst.executeQuery();
			if (rs.next()) {
				strSignedAssitantId = rs.getString("assistant_id");
			}
		} catch(SQLException se) {
			se.printStackTrace();
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
		}
		
		return strSignedAssitantId;
	}
}
