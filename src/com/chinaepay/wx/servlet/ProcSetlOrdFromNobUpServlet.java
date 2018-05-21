package com.chinaepay.wx.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.MysqlConnectionPool;
import com.chinaepay.wx.common.JsonRespObj;
import com.chinaepay.wx.entity.ProcSetlOrdFromNobUpEntity;

import net.sf.json.JSONObject;

/**
 * ����NOB�ϴ��Ľ��㵥�ļ����������������µ���̨���ݿ⡣
 * @author xinwuhen
 */
public class ProcSetlOrdFromNobUpServlet extends ProcSettleOrderServlet {
	private static final String SETTLE_FILE_HEADER_KEY = "HeadInfo";
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		JsonRespObj respObj = new JsonRespObj();
		String strProcResult = "1";
		String strReturnMsg = "";

		// ��ȡNOB�෵�صĽ��㵥���ݣ�������洢�ڹ̶�Ŀ¼��
		List<File> listUploadFiles = this.readAndStoreNobStlOrder(request, response);
		System.out.println("listUploadFiles = " + listUploadFiles);
		if (listUploadFiles == null || listUploadFiles.size() == 0) {
			strProcResult = "0";
			strReturnMsg = "�ϴ��ļ�ʧ��(δѡ���κ��ļ����ļ���Ϊ�ջ����ļ���СΪ0)��";
		} else {
			for (File uploadFile : listUploadFiles) {
				System.out.println("uploadFile.AbsolutePath = " + uploadFile.getAbsolutePath());
				System.out.println("uploadFile.Name = " + uploadFile.getName());
				
				// ��ȡNOB�෵�صĽ��㵥����
				Map<String, Map<String, String>> mapSetlInfo = this.getMapSetlInfos(uploadFile);
				System.out.println("mapSetlInfo = " + mapSetlInfo);
				
				// У����㵥��Ϣ�Ƿ���ȷ
				boolean blnValidSetlRst = validNobSetlOrderInfo(mapSetlInfo);
				System.out.println(">>.blnValidSetlRst = " + blnValidSetlRst);
				if (!blnValidSetlRst) {
					strProcResult = "0";
					strReturnMsg = "�ļ�ͷ�ڵ��ܽ�������ܼ�¼�������ļ����ڵ�ʵ�����ݲ�һ�£�";
				} else {
					// ���º�̨���ݿ��еĽ��㵥״̬
					boolean blnUpRst = updateNobSetlRstToTbl(mapSetlInfo);
					System.out.println(">>>.blnUpRst = " + blnUpRst);
					
					if (blnUpRst) {
						strProcResult = "1";
						strReturnMsg = "�ϴ��ļ����Ҹ��½��㵥״̬�ɹ���";
					} else {
						strProcResult = "0";
						strReturnMsg = "���㵥����ʧ�ܣ���������㵥״̬�������������Ϣ���Ƿ���ԭ�ļ�����һ�£�";
					}
				}
			}
		}
		
		System.out.println("strProcResult = " + strProcResult);
		System.out.println("strReturnMsg = " + strReturnMsg);
		
		// ���ʿ�����ʣ�ʵ�ֿͻ�������ʱ��AJAX��ȡJSON����
		response.setHeader("Access-Control-Allow-Origin", "*");
		
		respObj.setRespCode(strProcResult);
		respObj.setRespMsg(strReturnMsg);
		JSONObject jsonObj = JSONObject.fromObject(respObj);
		super.returnJsonObj(response, jsonObj);
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		doGet(request, response);
	}
	
	/**
	 * ��ȡNOB�෵�صĽ��㵥���ݣ�������洢�ڹ̶�Ŀ¼�¡�
	 * @param request
	 * @param response
	 * @return
	 */
	private List<File> readAndStoreNobStlOrder(HttpServletRequest request, HttpServletResponse response) {
		List<File> listUploadFiles = new ArrayList<File>();
		try {
			// ���㵥�ļ�����Ŀ¼
			String strSetlFilePath = super.getAbsolutFilePath(CommonInfo.SETTLE_FILE_FOLDER_FOR_NOB_UP);
			
			DiskFileItemFactory diskFactory = new DiskFileItemFactory();
            // threshold ���ޡ��ٽ�ֵ����Ӳ�̻��� 1M
            diskFactory.setSizeThreshold(10 * 1024);
            // repository �����ң�����ʱ�ļ�Ŀ¼
            diskFactory.setRepository(new File(strSetlFilePath));
            ServletFileUpload upload = new ServletFileUpload(diskFactory);
            //��ֹ����
            upload.setHeaderEncoding("UTF-8");
            // ���������ϴ�������ļ���С 4M
            upload.setSizeMax(4 * 1024 * 1024);
            // ����HTTP������Ϣͷ
            List<FileItem> fileItems = upload.parseRequest(request);
            
            for (FileItem fileItem : fileItems) {
            	if (fileItem.isFormField()) {	// �Ǳ��ڵ�����
            		processFormField(fileItem);	//���������
            	} else {	// ���ϴ����ļ�
            		File fileUploaded = processUploadFile(strSetlFilePath, fileItem);	//�����ϴ����ļ�
            		if (fileUploaded != null) {
            			listUploadFiles.add(fileUploaded);
            		}
            	}
            }
		} catch (FileUploadException e) {
			e.printStackTrace();
		} finally {
			
		}
		
		return listUploadFiles;
	}
	
	/**
	 * ������ڵ�����ֵ��
	 * @param item
	 * @param pw
	 */
	private void processFormField(FileItem item) {
//		String name = item.getFieldName();
//		if (name.equals("stuid")) {
//			studentid = item.getString();
//		} else if (name.equals("workid")) {
//			workid = item.getString();
//		}
	}
	
	private File processUploadFile(String strSetlFilePath, FileItem item) {
		File fileUploaded = null;
		
		// �ͻ����ϴ�ʱ���ļ���·������
		String strFileFullName = item.getName();
		System.out.println("strFileFullName = " + strFileFullName);
		int iFileIndex = strFileFullName.lastIndexOf("\\");
		String strFileName = strFileFullName.substring(iFileIndex + 1, strFileFullName.length());
		System.out.println("strFileName = " + strFileName);
		long lngFileSize = item.getSize();
		if ("".equals(strFileName) || lngFileSize == 0) {
			System.out.println("�ļ���Ϊ�ջ����ļ���СΪ0.");
			fileUploaded = null;
		} else {
			String strServerFileFullName =  strSetlFilePath + "/" + strFileName;
			try {
				File uploadFile = new File(strServerFileFullName);
				if (uploadFile.exists()) {	// ɾ���ɵ��ļ�
					uploadFile.delete();
				}
				
				item.write(uploadFile);
				fileUploaded = uploadFile;
			} catch (Exception e) {
				e.printStackTrace();
				fileUploaded = null;
			}
		}
		
		return fileUploaded;
	}
	
	/**
	 * ��ȡNO�෵�صĽ��㵥���ݡ�
	 * @param nobSetlFile
	 * @return	Map<String, Map<String, String>> �ṹ�а���ͷ����Ϣ�Լ�������Ϣ������������£�
	 * {HeadInfo:{File_Gen_Time:20180514135123, Total_Records:3, Total_Settle_Amount:0.5, Settle_Fee_Type:USD}}
	 * {a565fg4vcsw25s3g2tiqgpn2jehycgwh:{org_id:212322593, org_type:Submch, settle_belong_date:20180509, settle_batch_no:20180509_0001, 
	 * settle_start_time:20180513234455, settle_fee_amount:0.03, settle_fee_type:USD, settle_bank_type:NOB, settle_bank_account:683733456336536, 
	 * settle_status:1}}
	 */
	private Map<String, Map<String, String>> getMapSetlInfos(File nobSetlFile) {
		Map<String, Map<String, String>> mapSetlInfo = new HashMap<String, Map<String, String>>();
		if (nobSetlFile == null) {
			return mapSetlInfo;
		}
		
		FileReader reader = null;
        BufferedReader buffReader = null;
		try {
			reader = new FileReader(nobSetlFile);
			buffReader = new BufferedReader(reader);
			
			String strLine = null;
			while ((strLine = buffReader.readLine()) != null) {
				if (strLine.toLowerCase().startsWith("File_Gen_Time".toLowerCase())) {	// ��ȡ�ļ�ͷ��Ϣ
					Map<String, String> mapHeadInfo = new HashMap<String, String>();
					String[] strHeadNames = strLine.split(",");	// �ļ�ͷ�����е��ֶ�������Ϣ
					
					strLine = buffReader.readLine(); 	// �ļ�ͷ�����е��ֶ�ֵ��Ϣ
					if (strLine != null) {
						String[] strHeadValues = strLine.split(",");
						for (int i = 0; i < strHeadNames.length; i++) {
							String strHeadValue = "";
							if (i < strHeadValues.length) {
								strHeadValue = strHeadValues[i];
							}
							mapHeadInfo.put(strHeadNames[i], CommonTool.formatNullStrToSpace(strHeadValue));
						}
					}
					mapSetlInfo.put(SETTLE_FILE_HEADER_KEY, mapHeadInfo);		
				} else if (strLine.toLowerCase().startsWith("settle_order_id".toLowerCase())) {	// ��ȡ�ļ�����Ϣ
					Map<String, String> mapBodyInfo = null;
					String[] strBodyNames = strLine.split(",");	// �ļ��������е��ֶ�������Ϣ
					
					while ((strLine = buffReader.readLine()) != null && !strLine.startsWith("EOF")) {	// �ļ��������е��ֶ�ֵ��Ϣ
						mapBodyInfo = new HashMap<String, String>();
						String[] strBodyValues = strLine.split(",");
						String strSetlOrdId = null;
						for (int j = 0; j < strBodyValues.length; j++) {
							if (j == 0) {
								strSetlOrdId = strBodyValues[j];
							} else {
								mapBodyInfo.put(strBodyNames[j], strBodyValues[j]);
							}
						}
						mapSetlInfo.put(strSetlOrdId, mapBodyInfo);
					}
				} else if (strLine.startsWith("EOF")) {
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (buffReader != null) {
				try {
					buffReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return mapSetlInfo;
	}
	
	/**
	 * У����㵥��Ϣ�Ƿ���ȷ��
	 * @param mapSetlInfo
	 * @return
	 */
	private boolean validNobSetlOrderInfo(Map<String, Map<String, String>> mapSetlInfo) {
		boolean blnValdSetlRst = false;
		
		Map<String, String> mapHeader = mapSetlInfo.get(SETTLE_FILE_HEADER_KEY);
		if (mapHeader != null) {
			String strTotalRecords = mapHeader.get("Total_Records");
			String strTotalSetlAmount = mapHeader.get("Total_Settle_Amount");
			System.out.println("Double.parseDouble(strTotalSetlAmount) = " + Double.parseDouble(strTotalSetlAmount));
			
			String[] strKeys = mapSetlInfo.keySet().toArray(new String[0]);
			double dblSetlFeeAmount = 0d;
			for (String strKey : strKeys) {
				if (!SETTLE_FILE_HEADER_KEY.equals(strKey)) {
					String strSetlFeeAmt = mapSetlInfo.get(strKey).get("settle_fee_amount");
					System.out.println("strSetlFeeAmt = " + strSetlFeeAmt);
					
					dblSetlFeeAmount = dblSetlFeeAmount + Double.parseDouble(strSetlFeeAmt);
				}
			}
			
			
			System.out.println("dblSetlFeeAmount = " + dblSetlFeeAmount);
			System.out.println("strTotalRecords = " + strTotalRecords);
			
			if (Double.parseDouble(strTotalSetlAmount) == Double.parseDouble(CommonTool.formatDoubleToHalfUp(dblSetlFeeAmount, 2, 2)) && Integer.parseInt(strTotalRecords) == (mapSetlInfo.size() - 1)) {
				blnValdSetlRst = true;
			}
		}
		
		return blnValdSetlRst;
	}
	
	/**
	 * ���º�̨���ݿ��еĽ��㵥״̬��
	 * @param mapSetlInfo
	 */
	private boolean updateNobSetlRstToTbl(Map<String, Map<String, String>> mapSetlInfo) {
		boolean blnUpAllInfoRst = false;
		Connection conn = MysqlConnectionPool.getInstance().getConnection(false);
		
		try {
			// 1.�������ս��㵥��
			boolean blnUpFinalSetlRst = this.updateFinalSettleOrderInfo(mapSetlInfo, conn);
			System.out.println("blnUpFinalSetlRst = " + blnUpFinalSetlRst);
			
			// 2.�����м�/��ϸ���㵥��(��֧����ϸ���˿���ϸ��)
			if (blnUpFinalSetlRst) {
				// 2.1 ����֧���������м��
				this.updatePaySetlOrderInfo(mapSetlInfo, conn, "tbl_pay_settle_detail_order");
				
				// 2.2 �����˿�����м��
				this.updatePaySetlOrderInfo(mapSetlInfo, conn, "tbl_refund_settle_detail_order");
				
				// ���е�У��ͨ���������������ύ
				conn.commit();
				blnUpAllInfoRst = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			blnUpAllInfoRst = false;
			try {
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} finally {
			MysqlConnectionPool.getInstance().releaseConnInfo(conn);
		}
		
		return blnUpAllInfoRst;
	}
	
	/**
	 * ����NOB�ϴ��Ľ��㵥�ļ���У���ļ������еļ�¼�Ƿ���������ս��㵥��һ�£��������㵥ID���������Ƿ�һ�£���
	 * @param mapSetlInfo
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	private boolean updateFinalSettleOrderInfo(Map<String, Map<String, String>> mapSetlInfo, Connection conn) throws SQLException {
		boolean blnUpFinalSetlRst = true;
		String strSql = "update tbl_settlement_sum_order set settle_status=?, settle_end_time=? "
						+ " where settle_order_id=?;";
		PreparedStatement prst = conn.prepareStatement(strSql);
		
		String[] strKeys = mapSetlInfo.keySet().toArray(new String[0]);
		for (String strKey : strKeys) {
			if (!SETTLE_FILE_HEADER_KEY.equals(strKey)) {
				String strSetlOrderId = strKey;
				Map<String, String> mapSetlBodyValues = mapSetlInfo.get(strSetlOrderId);
				
				// У�����ݿ���Ƿ�����������㵥��ȷ�������㵥�š�������һ�£�
				Map<String, String> mapArgs = new HashMap<String, String>();
				mapArgs.put("settle_order_id", strSetlOrderId);
				mapArgs.put("settle_fee_amount", CommonTool.formatNullStrToSpace(mapSetlBodyValues.get(ProcSetlOrdFromNobUpEntity.SETTLE_FEE_AMOUNT)));
				String strTblSetlOrderId = getTblFieldValue("settle_order_id", "tbl_settlement_sum_order", mapArgs);
				System.out.println("strTblSetlOrderId = " + strTblSetlOrderId);
				
				if (strTblSetlOrderId == null || "".equals(strTblSetlOrderId)) {
					blnUpFinalSetlRst = false;
					break;
				} else {
					prst.setString(1, CommonTool.formatNullStrToSpace(mapSetlBodyValues.get(ProcSetlOrdFromNobUpEntity.SETTLE_STATUS)));
					prst.setString(2, CommonTool.getFormatDateStr(new Date(), "yyyyMMddHHmmss"));
					prst.setString(3, strSetlOrderId);
					prst.addBatch();
				}
			}
		}
		
		System.out.println("blnUpFinalSetlRst = " + blnUpFinalSetlRst);
		if (blnUpFinalSetlRst) {	// NOB�ϴ��Ľ��㵥�ļ��ڵļ�¼��ȫ��У��ͨ��
			prst.executeBatch();
		}
		
		return blnUpFinalSetlRst;
	}
	
	/**
	 * ����֧�������˿�����м���еĽ���״̬��
	 * @param mapSetlInfo
	 * @param conn
	 * @return
	 * @throws SQLException 
	 */
	private void updatePaySetlOrderInfo(Map<String, Map<String, String>> mapSetlInfo, Connection conn, String strTblName) throws SQLException {
		String[] strKeys = mapSetlInfo.keySet().toArray(new String[0]);
		PreparedStatement prst = null;
		
		for (String strKey : strKeys) {
			if (!SETTLE_FILE_HEADER_KEY.equals(strKey)) {
				String strSetlOrderId = strKey;
				Map<String, String> mapSetlBodyValues = mapSetlInfo.get(strSetlOrderId);
				String strSql = "update " + strTblName + " set COLUMN_SETTLE_STATUS='" 
							+ mapSetlBodyValues.get(ProcSetlOrdFromNobUpEntity.SETTLE_STATUS) 
							+ "' where COLUMN_SETTLE_ORDER_ID='" 
							+ strSetlOrderId
							+ "';";
				
				System.out.println("$$$strSql = " + strSql);
				
				String strOrgType = mapSetlBodyValues.get(ProcSetlOrdFromNobUpEntity.ORG_TYPE);
				if (ProcSetlOrdFromNobUpEntity.ORG_TYPE_NOB.equals(CommonTool.formatNullStrToSpace(strOrgType))) {	// ��������ΪNOB
					strSql = strSql.replace("COLUMN_SETTLE_STATUS", "nob_settle_status").replace("COLUMN_SETTLE_ORDER_ID", "nob_settle_order_id");
				} else if (ProcSetlOrdFromNobUpEntity.ORG_TYPE_HARVEST.equals(CommonTool.formatNullStrToSpace(strOrgType))) {	// ��������ΪHarvest
					strSql = strSql.replace("COLUMN_SETTLE_STATUS", "har_settle_status").replace("COLUMN_SETTLE_ORDER_ID", "har_settle_order_id");
				} else if (ProcSetlOrdFromNobUpEntity.ORG_TYPE_AGENT.equals(CommonTool.formatNullStrToSpace(strOrgType))) {	// ��������ΪAgent
					strSql = strSql.replace("COLUMN_SETTLE_STATUS", "agen_settle_status").replace("COLUMN_SETTLE_ORDER_ID", "agen_settle_order_id");
				} else if (ProcSetlOrdFromNobUpEntity.ORG_TYPE_SUB_MCH.equals(CommonTool.formatNullStrToSpace(strOrgType))) {	// ��������ΪSubmch
					strSql = strSql.replace("COLUMN_SETTLE_STATUS", "submch_settle_status").replace("COLUMN_SETTLE_ORDER_ID", "submch_settle_order_id");
				}
				
				prst = conn.prepareStatement(strSql);
				prst.executeUpdate();
				prst.close();
			}
		}
	}
}
