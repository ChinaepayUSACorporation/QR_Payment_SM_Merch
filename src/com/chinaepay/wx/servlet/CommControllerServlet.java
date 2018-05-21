package com.chinaepay.wx.servlet;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.entity.CommunicateEntity;

public abstract class CommControllerServlet extends ExtendsHttpServlet {
	
	/**
	 * У���̻��Ƿ���Ч��
	 * @param strSubMerchId
	 * @return
	 */
	public abstract boolean validSubMchIsUsable(String strSubMerchId);
	
	/**
	 * �ڷ���΢�ź�̨���д���ǰ��������ʵ���ʽ�������ļ�ΪXML��ʽ��
	 * @param orderEntity
	 * @return
	 */
	public String formatReqInfoToXML(Map<String, String> mapRequestInfo) {
		String strXML = "";
		
		String[] strKeys = mapRequestInfo.keySet().toArray(new String[0]);
		if (strKeys.length > 0) {
			strXML = strXML.concat("<xml>");
			
			StringBuffer sb = new StringBuffer();
			for (String strKey : strKeys) {
				if (strKey != null && !"".equals(strKey) && !strKey.equals(CommunicateEntity.APP_KEY) /*&& !strKey.equals(CommunicateEntity.AGENT_ID)*/) {
					String strValue = mapRequestInfo.get(strKey);
					if (strValue != null /*&& !"".equals(strValue)*/) {
						sb.setLength(0);
						sb.append("<").append(strKey).append(">").append(strValue).append("</").append(strKey).append(">");
						strXML = strXML.concat(sb.toString());
					}
				}
			}
			
			strXML = strXML.concat("</xml>");
		}
		
		return strXML;
	}
	
	/**
	 * ����HttpPost���󣬲���ȡӦ���ġ�
	 * @param strURI
	 * @param lstNameValuePair
	 * @return
	 */
	public CloseableHttpResponse sendAndGetHttpPostRst(String strURI, List<BasicNameValuePair> lstNameValuePair) {
		CloseableHttpResponse response = null;
		// ����httpclient����
		CloseableHttpClient client = CommonTool.getDefaultHttpClient();
		// ����post��ʽ�������
		HttpPost httpPost = new HttpPost(strURI); // ������Ӧͷ��Ϣ
		// ���ò��������������
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(lstNameValuePair, "UTF-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return response;
		}

		// ����header��Ϣ
		// ָ������ͷ��Content-type������User-Agent��
		httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
		httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
		
		// ִ��������������õ������ͬ��������
		try {
			response = client.execute(httpPost);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return response;
	}
	
	/**
	 * ͨ��ָ�����ꡢ�¡��գ���ȡ���Ӧ�����ڡ�
	 * @param strHour
	 * @param strMinute
	 * @param strSecond
	 * @return
	 */
	public Date getFixDateBasedOnArgs(String strHour, String strMinute, String strSecond) {
		Map<Integer, Integer> mapCalArgs = new HashMap<Integer, Integer>();
		mapCalArgs.put(Calendar.HOUR_OF_DAY, Integer.valueOf(CommonTool.formatNullStrToZero(strHour)));
		mapCalArgs.put(Calendar.MINUTE, Integer.valueOf(CommonTool.formatNullStrToZero(strMinute)));
		mapCalArgs.put(Calendar.SECOND, Integer.valueOf(CommonTool.formatNullStrToZero(strSecond)));
		
		return CommonTool.getDefineDateBaseOnYMDHMS(mapCalArgs);
	}
	
	
	/**
	 * ����������ִ��������ͣ��ʱ���ࡣ
	 * @author xinwuhen
	 */
	public class ClosableTimer extends Timer {
		private boolean blnNeedCloseTimer = false;
		
		public ClosableTimer(boolean blnNeedCloseTimer) {
			super();
			this.blnNeedCloseTimer = blnNeedCloseTimer;
		}
		
		public boolean isNeedClose() {
			return blnNeedCloseTimer;
		}
	}
	
	
	/**
	 * ��Ϊһ���ڲ��࣬���ڽ���΢�Ŷ˺�̨���ص�XMLӦ�����ݡ�
	 * @author xinwuhen
	 *
	 */
	public class ParsingWXResponseXML {
		private Map<String, String> mapWXRespResult = new HashMap<String, String>();

		/**
		 * ����XML��������Map�С�
		 * @param strWxResponseResult
		 * @return
		 * @throws ParserConfigurationException 
		 * @throws IOException 
		 * @throws SAXException 
		 */
		public Map<String, String> formatWechatXMLRespToMap(String strWxResponseResult) {
			if (strWxResponseResult == null || "".equals(strWxResponseResult)) {
				return null;
			}
			
			if (strWxResponseResult.toLowerCase().startsWith("<xml>")) {
				DocumentBuilderFactory docBuilderFact = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = null;
				Document document = null;
				
				try {
					docBuilder = docBuilderFact.newDocumentBuilder();
					document = docBuilder.parse(new InputSource(new StringReader(strWxResponseResult)));
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// ����XML��ʽ���ַ����������ַ������ԡ���-ֵ���Ե���ʽ��ӵ�MAP�С�
				if (document != null) {
					appendElementNameAndValue(document);
				}
			}
			
			return mapWXRespResult;
		}
		
		/**
		 * ȡ��Ԫ�ؽڵ�Ľڵ������ڵ�ֵ��
		 * @param node
		 * @return
		 */
		private void appendElementNameAndValue(Node node) {
			if (node != null) {  // �жϽڵ��Ƿ�Ϊ��
				if (node.hasChildNodes()) {	// ��Ԫ�ؽڵ��»����ӽڵ�
					NodeList nodeList = node.getChildNodes();
					for (int i = 0; i < nodeList.getLength(); i++) {
						Node childNode = nodeList.item(i);
						appendElementNameAndValue(childNode);
					}
				} else {	// ��Ԫ�ؽڵ����Ѿ�û���ӽڵ�
					Node nodeParent = null;
					if ((nodeParent = node.getParentNode()) != null) {
						mapWXRespResult.put(nodeParent.getNodeName(), node.getNodeValue());
					}
				}
			}
		}
	}
}
