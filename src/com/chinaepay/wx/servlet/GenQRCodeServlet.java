package com.chinaepay.wx.servlet;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.JsonRespObj;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.common.QRCodeUtil;

import net.sf.json.JSONObject;

/**
 * ���ɶ�ά�롣
 * @author xinwuhen
 */
public class GenQRCodeServlet extends ExtendsHttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		JsonRespObj respObj = new JsonRespObj();
		// ����Ӧ�𷵻ؽ��(0��ʧ��  1���ɹ�)
		String strGenResult = "0";
		String strReturnMsg = "";
		Map<String, String> mapSubMchImg = null;
		try {
			request.setCharacterEncoding("UTF-8");
			
			// �û�ɨ���ά��, �ڻ�ȡ�û���OpenId֮ǰ, ���ȡCODE��
			String strWxAuthCodeURL = CommonTool.getAbsolutWebURL(request, true) + "/" + CommonInfo.GET_WX_AUTH_CODE_URL;
			System.out.println("strWxAuthCodeURL = " + strWxAuthCodeURL);
			
			// ȡ���ն��̻�ID
			String sub_mch_id = CommonTool.urlDecodeUTF8(request.getParameter("sub_mch_id"));
			// ��ά������
			String strQRCodeContent = strWxAuthCodeURL + "?sub_mch_id=" + sub_mch_id;
			// ���ɶ�ά�벢����Ӧ��Ϣ���µ����ݱ�
			boolean blnGenRst = genQRAndUpToTbl(strQRCodeContent, sub_mch_id);
			System.out.println("blnGenRst = " + blnGenRst);
			
			// ȡ�ö�ά��洢·��
			Map<String, String> mapArgs = new HashMap<String, String>();
			mapArgs.put("sub_mch_id", sub_mch_id);
			String strQRFilePath = super.getTblFieldValue("qr_png_folder", "tbl_qrcode_info", mapArgs);
	    	String strWebAppName = CommonTool.getWebAppName(this.getServletContext());
	    	System.out.println("strWebAppName = " + strWebAppName);
	    	String strPrePath = CommonTool.getAbsolutWebAppPath(this.getClass(), System.getProperty("file.separator")).replaceAll(strWebAppName, "");
	    	System.out.println("strPrePath = " + strPrePath);
	    	String strQRPngFolder = strQRFilePath.replace(strPrePath, "");
	    	System.out.println("strQRPngFolder = " + strQRPngFolder);
			String strImgName = super.getTblFieldValue("qr_png_name", "tbl_qrcode_info", mapArgs);
			
			// ����Ӧ�𷵻ؽ��(0��ʧ��  1���ɹ�)
			if (blnGenRst) {
				strGenResult = "1";
				strReturnMsg = "���ɶ�ά��ɹ�!";
				mapSubMchImg = new HashMap<String, String>();
				mapSubMchImg.put(sub_mch_id, strQRPngFolder + "/" + strImgName);
			} else {
				strGenResult = "0";
				strReturnMsg = "���ɶ�ά��ʧ��!";
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			strGenResult = "0";
			strReturnMsg = "���ɶ�ά��ʧ��!";
		}
		
		respObj.setRespCode(strGenResult);
		respObj.setRespMsg(strReturnMsg);
		respObj.setRespObj(mapSubMchImg);
		JSONObject jsonObj = JSONObject.fromObject(respObj);
		super.returnJsonObj(response, jsonObj);
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		this.doGet(request, response);
	}
	
	/**
	 * ���ɶ�ά��ͼƬ����������ӦĿ¼��ͬʱ��ͼƬ�洢·����Ϣ���µ����ݱ�
	 * @param strQRCodeContent
	 * @return
	 */
	private boolean genQRAndUpToTbl(String strQRCodeContent, String strSubMchId) {
		if (strQRCodeContent == null || "".equals(strQRCodeContent) || strSubMchId == null || "".equals(strSubMchId)) {
			return false;
		}
		
		/** ���ݲ������ɶ�ά��ͼƬ **/
		String strQRPngFolder = super.getAbsolutFilePath(CommonInfo.QR_IMG_FOLDER);
		String strQRPngName = strSubMchId + ".png";
		File qrImgFile = QRCodeUtil.getInstance().genQrCodeImg("UTF-8", 300, 300, strQRPngFolder, strQRPngName, strQRCodeContent);
		System.out.println("qrImgFile = " + qrImgFile);
		
    	/** ��ͼƬ�洢·����Ϣ���µ����ݱ� **/
    	MysqlConnectionPool mysqlConnPool = MysqlConnectionPool.getInstance();
		Connection conn = mysqlConnPool.getConnection(false);
		PreparedStatement pstat = null;
		String strUpdateSql = "replace into tbl_qrcode_info(sub_mch_id, qr_code_content, qr_png_folder, qr_png_name, gen_time) values('" 
								+ strSubMchId + "','" 
								+ strQRCodeContent + "','" 
								+ strQRPngFolder + "','" 
								+ strQRPngName + "','"
								+ CommonTool.getFormatDateStr(new Date(), "yyyyMMddHHmmss") + "');";
		try {
			pstat = conn.prepareStatement(strUpdateSql);
			pstat.executeUpdate();
			
			conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(pstat, conn);
		}
		
    	return true;
	}
}