package com.chinaepay.wx.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;

import net.sf.json.JSONObject;

public class ExtendsHttpServlet extends HttpServlet {
	/**
	 * ��ȡ�ϴ��ļ��������Ŀ¼��
	 * @return
	 */
	public String getAbsolutFilePath (String strAppendFolder) {
		String strWebAppPaht = CommonTool.getAbsolutWebAppPath(this.getClass(), System.getProperty("file.separator"));
    	String strWebAppName = CommonTool.getWebAppName(this.getServletContext());
    	String strFullFileFolder = strWebAppPaht.substring(0, strWebAppPaht.indexOf(strWebAppName)) + strAppendFolder;
    	return strFullFileFolder;
	}
	
	/**
	 * ����Ѷ��̨���������ģ����õ�Ӧ���ġ�
	 * @param strURL
	 * @param strRequestXML
	 * @param httpclient
	 * @return
	 */
	public String sendReqAndGetResp(String strURL, String strRequestXML, CloseableHttpClient httpclient) {
		String jsonStr = "";
		if (httpclient != null) {
			CloseableHttpResponse response = null;
			HttpPost httpost = null;
			try {
				httpost = new HttpPost(strURL); // ������Ӧͷ��Ϣ
				httpost.addHeader("Connection", "keep-alive");
				httpost.setEntity(new StringEntity(strRequestXML, "UTF-8"));
				response = httpclient.execute(httpost);
				HttpEntity entity = response.getEntity();
				jsonStr = EntityUtils.toString(entity, "UTF-8");
				EntityUtils.consume(entity);
			} catch(IOException ioe) {
				ioe.printStackTrace();
			} finally {
				if (response != null) {
					try {
						response.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if (httpost != null) {
					httpost.releaseConnection();
				}
				
				if (httpclient != null) {
					try {
						httpclient.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		return jsonStr;
	}
	
	/**
	 * ���ݲ�ѯ�����б���ָ�����л�ȡָ���ֶ���ֵ��
	 * @param strOutTradeNo
	 * @return
	 */
	public String getTblFieldValue(String strField, String strTblName, Map<String, String> mapConditions) {
		String strFieldValue = "";
		
		Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
		PreparedStatement prst = null;
		ResultSet rs = null;
		try {
			String strSql = "select * from " + strTblName;
			
			if (mapConditions != null) {
				int iSize = mapConditions.size();
				if (iSize > 0) {
					strSql = strSql.concat(" where ");
					String[] strKeys = mapConditions.keySet().toArray(new String[iSize]);
					for (int i = 0; i < iSize; i++) {
						String strkey = CommonTool.formatNullStrToSpace(strKeys[i]);
						String strValue = CommonTool.formatNullStrToSpace(mapConditions.get(strkey));
						strSql = strSql.concat(strkey).concat("='").concat(strValue).concat("'");
						if (i != iSize - 1) {
							strSql = strSql.concat(" and ");
						}
					}
				}
			}
			
			System.out.println("#*#*sql = " + strSql);
			
			prst = conn.prepareStatement(strSql);
			rs = prst.executeQuery();
			if (rs.next()) {
				strFieldValue = rs.getString(strField);
			}
		} catch(SQLException se) {
			se.printStackTrace();
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
		}
		
		return strFieldValue;
	}
	
	/**
	 * ����������ȡָ������ָ���ֶε��б�
	 * @param strField
	 * @param strTblName
	 * @param listArgs
	 * @return
	 */
	public List<String> getTblFieldValueList(String strField, String strTblName, List<String[]> listArgs) {
		List<String> listTblFieldValue = new ArrayList<String>();
		
		Connection conn = MysqlConnectionPool.getInstance().getConnection(true);
		PreparedStatement prst = null;
		ResultSet rs = null;
		try {
			String strSql = "select * from " + strTblName;
			
			if (listArgs != null) {
				int iSize = listArgs.size();
				if (iSize > 0) {
					strSql = strSql.concat(" where ");
					for (int i = 0; i < iSize; i++) {
						String[] strArgs = listArgs.get(i);
						strSql = strSql.concat(strArgs[0] + strArgs[1] + "'" + strArgs[2] + "'");
						if (i != iSize - 1) {
							strSql = strSql.concat(" and ");
						}
					}
				}
			}
			
			System.out.println("strSql = " + strSql);
			
			prst = conn.prepareStatement(strSql);
			rs = prst.executeQuery();
			while (rs.next()) {
				listTblFieldValue.add(rs.getString(strField));
			}
		} catch(SQLException se) {
			se.printStackTrace();
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(rs, prst, conn);
		}
		
		return listTblFieldValue;
	}
	
	/**
	 * ������ͻ��˷���Json��ʽ��Ӧ�����
	 * @param response
	 * @param jsonObj
	 */
	public void returnJsonObj(HttpServletResponse response, JSONObject jsonObj) {
		response.setCharacterEncoding("UTF-8");  
		response.setHeader("Cache-Control", "no-store");
	    response.setContentType("application/json; charset=utf-8"); 
	    PrintWriter pw = null;
		try {
			pw = response.getWriter();
			pw.write(jsonObj.toString());
			pw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}
}
