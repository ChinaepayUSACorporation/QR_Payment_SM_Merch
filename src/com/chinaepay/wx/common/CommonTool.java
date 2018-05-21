/**
 * @author xinwuhen
 */
package com.chinaepay.wx.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.chinaepay.wx.entity.CommunicateEntity;
import com.chinaepay.wx.entity.PayOrderEntiry;

/**
 * @author xinwuhen ���׹�������������ļ��㹤���ࡣ
 */
public class CommonTool {
	/**
	 * ������ԡ�Ԫ����λת��Ϊ���֡���
	 * @param strAmount
	 * @return
	 */
	public static String formatYuanToCent(String strAmount) {
		strAmount = (strAmount == null || "".equals(strAmount)) ? "0" : strAmount;
		double dblAmount = Double.parseDouble(strAmount);
		return String.valueOf((int) (dblAmount * 100));
	}
	
	/**
	 * ����λ���֡�ת��Ϊ��Ԫ����
	 * @param strAmount
	 * @return
	 */
	public static String formatCentToYuan(String strAmount, int iPointNum) {
		strAmount = (strAmount == null || "".equals(strAmount)) ? "0" : strAmount;
		double dblYuan = Double.parseDouble(strAmount) / 100d;
		
		return CommonTool.formatDoubleToHalfUp(dblYuan, iPointNum, iPointNum);
	}
	
	/**
	 * ��ԭ�ַ���ǰ�߲���ָ�����ַ�������֤���ַ������ȡ�
	 * @param strSrcChars
	 * @param strPreChar
	 * @param iTotalLength
	 * @return
	 */
	public static String getFixLenStr(String strSrcChars, String strPreChar, int iTotalLength) {
		if (strSrcChars == null || strPreChar == null || iTotalLength <= 0) {
			return "";
		}
		
		String strNew = "";
		if (strSrcChars.length() >= iTotalLength) {
			return strSrcChars;
		} else {
			int iNeedApndSize = iTotalLength - strSrcChars.length();
			strNew = strSrcChars;
			for (int i = 0; i < iNeedApndSize; i++) {
				strNew = strPreChar.concat(strNew);
			}
		}
		
		return strNew;
	}
	
	/**
	 * ȡ���׸�ͨ��Ϊ����ģʽʱ������Ļ���������Ϣ��
	 * @return
	 */
	public static Map<String, String> getHarvestTransInfo() {
		Map<String, String> mapHarvestTransInfo = new HashMap<String, String>();
		mapHarvestTransInfo.put(PayOrderEntiry.APPID, CommonInfo.NOB_APP_ID);
		mapHarvestTransInfo.put(PayOrderEntiry.MCH_ID, CommonInfo.NOB_MCH_ID);
		mapHarvestTransInfo.put(PayOrderEntiry.APP_KEY, CommonInfo.NOB_KEY);
		return mapHarvestTransInfo;
	}
	
	/**
	 * ��NULL��ʽ���ַ���ת��Ϊ""��
	 * @param strSrc
	 * @return
	 */
	public static String formatNullStrToSpace(String strSrc) {
		return strSrc == null ? "" : strSrc;
	}
	
	/**
	 * ���ַ�������ָ���ı����ʽ����ת����
	 * @param strSrc
	 * @param strSrcStrType
	 * @param strDestStrType
	 * @return
	 */
	public static String transferCharactor(String strSrc, String strSrcStrType, String strDestStrType) {
		if (strSrc == null || strSrcStrType == null || strDestStrType == null) {
			return null;
		}
		
		try {
			return new String(strSrc.getBytes(strSrcStrType), strDestStrType);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return strSrc;
		}
	}
	
	/**
	 * ��NULL��""��ʽ���ַ���ת��Ϊ"0"��
	 * @param strSrc
	 * @return
	 */
	public static String formatNullStrToZero(String strSrc) {
		return strSrc == null || "".equals(strSrc) ? "0" : strSrc;
	}
	
	/**
	 * ��Double������������С�������λ����������������ת�����������ַ������͡�
	 * @param dblSrc
	 * @param iMinFract
	 * @param iMaxFract
	 * @return
	 */
	public static String formatDoubleToHalfUp(double dblSrc, int iMinFract, int iMaxFract) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setRoundingMode(RoundingMode.HALF_UP);//������������
        nf.setMinimumFractionDigits(iMinFract);//������С������λС��
        nf.setMaximumFractionDigits(iMaxFract);//�����������λС��
        nf.setGroupingUsed(false);
        return nf.format(dblSrc);
	}
	
	/**
	 * ���ɹ̶�λ���������������
	 * 
	 * @return
	 */
	public static String getRandomString(int intLength) {
		char[] ch = new char[] { '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
				'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
		char[] chNew = new char[intLength];
		int iChLengh = ch.length;
		for (int i = 0; i < chNew.length; i++) {
			int index = (int) (iChLengh * Math.random());
			chNew[i] = ch[index];
		}

		return String.valueOf(chNew);
	}

	/**
	 * �����̻������š�
	 * 
	 * @param date
	 * @param intExtLength ����Ӧ<=18����Ϊ΢��Ҫ�󶩵����ܳ���Ӧ������32λ����dateת���ĳ���Ϊ14λ��
	 * @return
	 */
	public static String getOutTradeNo(Date date, int intExtLength) {
		if (date == null) {
			return null;
		}

		String strPrefix = getFormatDateStr(date, "yyyyMMddHHmmss");

		char[] ch = new char[] { '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' };
		char[] chNew = new char[intExtLength];
		int iChLengh = ch.length;
		for (int i = 0; i < chNew.length; i++) {
			int index = (int) (iChLengh * Math.random());
			chNew[i] = ch[index];
		}
		String strSufix = String.valueOf(chNew);

		return strPrefix.concat(strSufix);
	}
	
	/**
	 * �����̻��˿�ţ��˴�Ĭ��Ϊ32λ��
	 * @param date
	 * @param intExtLength ����Ӧ<=18����Ϊ΢��Ҫ�󶩵����ܳ���Ӧ������32λ����dateת���ĳ���Ϊ14λ��
	 * @return
	 */
	public static String getOutRefundNo(Date date, int intExtLength) {
		return getOutTradeNo(date, intExtLength);
	}

	/**
	 * ���ݸ�ʽ�ַ��������ڽ��и�ʽ����
	 * 
	 * @param date
	 * @param strFormat
	 * @return
	 */
	public static String getFormatDateStr(Date date, String strFormat) {
		DateFormat sdf = new SimpleDateFormat(strFormat);
		return sdf.format(date);
	}

	/**
	 * ��ȡxxʱ��֮ǰ��֮������ڸ�ʽ��
	 * @param date
	 * @param strFormat
	 * @param lngSeconds
	 * @return
	 */
	public static String getBefOrAftFormatDate(Date date, long lngMillSeconds, String strFormat) {
		Date newDate = getBefOrAftDate(date, lngMillSeconds);
		
		return getFormatDateStr(newDate, strFormat);
	}
	
	/**
	 * ��ָ����������ǰ�������ƹ̶���ʱ�䡣
	 * @param date
	 * @param lngMillSeconds
	 * @return
	 */
	public static Date getBefOrAftDate(Date date, long lngMillSeconds) {
		long lngNewTime = 0;
		long lngBaseTime = date.getTime();
		if (lngMillSeconds >= 0) {	// ��ȡָ��date����֮���ʱ��
			lngNewTime = lngBaseTime + Math.abs(lngMillSeconds);
		} else {	// ��ȡָ������֮ǰ��ʱ��
			lngNewTime = lngBaseTime - Math.abs(lngMillSeconds);
		}
		
		return new Date(lngNewTime);
	}
	
	/**
	 * ��ȡָ��ʱ����ǰ������һ��ʱ����(intTimeLen, �����ŷֱ��ʾ������ǰ)��ʱ�䵥λ(intUnit)��ʱ��ֵ��
	 * @param date
	 * @param intUnit
	 * @param intTimeLen
	 * @return
	 */
	public static Date getBefOrAftDate(Date date, int intUnit, int intTimeLen) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(intUnit, intTimeLen);
		return cal.getTime();
	}
	
	/**
	 * ���ַ���ת����ָ����ʽ�����ڡ�
	 * @param strFormat
	 * @param strSourceDate
	 * @return
	 */
	public static Date getDateBaseOnChars(String strFormat, String strSourceDate) {
		if (strFormat == null || "".equals(strFormat) || strSourceDate == null || "".equals(strSourceDate)) {
			return null;
		}
		
		DateFormat sdf = new SimpleDateFormat(strFormat);
		Date date = null;
		try {
			date = sdf.parse(strSourceDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return date;
	}
	
	/**
	 * ��ȡ��ǰ��������ֵ�����ڸ�ʽ��
	 * @param date
	 * @param mapArgs
	 * @return
	 */
	public static Date getDefineDateBaseOnYMDHMS(Map<Integer, Integer> mapArgs) {
		if (mapArgs == null || mapArgs.size() == 0) {
			return null;
		}
		
		Calendar cal = Calendar.getInstance();
		Integer[] intKeys = mapArgs.keySet().toArray(new Integer[0]);
		for (Integer intKey : intKeys) {
			cal.set(intKey, mapArgs.get(intKey));
		}

		return cal.getTime();
	}

	/**
	 * ��ȡ��ǰ�ն��豸��IP��ַ��
	 * 
	 * @return
	 * @throws UnknownHostException
	 */
	public static String getSpbill_Create_Ip() {
		InetAddress res = null;
		try {
			res = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
		return res.getHostAddress();
	}

	/**
	 * �ڷ���΢�ź�̨���д���ǰ�����ɶ�����ǩ����
	 * 
	 * @param orderEntity
	 * @param strKey
	 * @return
	 */
	public static String getEntitySign(Map<String, String> mapOrderCont) {
		String[] strContKeys = mapOrderCont.keySet().toArray(new String[0]);
		Arrays.sort(strContKeys);
		StringBuffer sb = new StringBuffer();
		for (String K : strContKeys) {
			String V = mapOrderCont.get(K);
			if (K != null && !"".equals(K) && !K.equals(CommunicateEntity.SIGN) && !K.equals(CommunicateEntity.APP_KEY) /*&& !K.equals(CommunicateEntity.AGENT_ID)*/
					&& V != null && !"".equals(V)) {
				sb.append(K.concat("=").concat(V).concat("&"));
			}
		}

		sb.append(getCorrectKey(CommunicateEntity.APP_KEY) + "=" + mapOrderCont.get(CommunicateEntity.APP_KEY));

		return new MD5Util().MD5(sb.toString(), "UTF-8").toUpperCase();
	}
	
	private static String getCorrectKey(String strFullKey) {
		if (strFullKey == null) {
			return "";
		}
		
		String strResp = "";
		if (strFullKey.contains("_")) {
			strResp = strFullKey.split("_")[1];
		}
		return strResp;
	}

	/**
	 * ��ȡһ���µ�clone��Map.
	 * 
	 * @param mapSrc
	 * @return
	 */
	public static Map<String, String> getCloneMap(Map<String, String> mapSrc) {
		if (mapSrc == null) {
			return null;
		}

		Map<String, String> newMap = new HashMap<String, String>();
		String[] strKeys = mapSrc.keySet().toArray(new String[0]);
		for (String strKey : strKeys) {
			if (strKey != null) {
				newMap.put(strKey, mapSrc.get(strKey));
			}
		}

		return newMap;
	}
	
	/**
	 * ��¡һ���б�
	 * @param listSrc
	 * @return
	 */
	public static List<String> getCloneList(List<String> listSrc) {
		if (listSrc == null) {
			return null;
		}
		
		List<String> newList = new ArrayList<String>();
		for (String strSrc : listSrc) {
			newList.add(strSrc);
		}
		
		return newList;
	}
	
	
	/**
	 * �ϲ�����Map�����ݡ�
	 * @param sourceMap
	 * @param appendMap
	 * @return
	 */
	public static Map<String, String> getAppendMap(Map<String, String> sourceMap, Map<String, String> appendMap) {
		if (sourceMap == null || appendMap == null) {
			return null;
		}
		
		String[] strKeys = appendMap.keySet().toArray(new String[0]);
		for (String strKey : strKeys) {
			if (strKey != null) {
				sourceMap.put(strKey, appendMap.get(strKey));
			}
		}

		return sourceMap;
	}
	
	/**
	 * ��ȡ����List�ϲ��������б�
	 * @param sourceList
	 * @param appendList
	 * @return
	 */
	public static List<String> getAppendList(List<String> sourceList, List<String> appendList) {
		if (sourceList == null || appendList == null) {
			return null;
		}
		
		for (String strApp : appendList) {
			sourceList.add(strApp);
		}
		
		return sourceList;
	}

	/**
	 * ��ʽ���ͻ��˵�Socket�����ַ���ΪMap.
	 * 
	 * @param strSocketRequest
	 * @return
	 */
	public static HashMap<String, String> formatStrToMap(String strSocketRequest) {
		if (strSocketRequest == null) {
			return null;
		}

		HashMap<String, String> hmOrderCont = new HashMap<String, String>();
		String[] strBig = strSocketRequest.split("&");
		for (String strTemp : strBig) {
			String[] strSmall = strTemp.split("=");
			hmOrderCont.put(strSmall[0], strSmall[1]);
		}

		return hmOrderCont;
	}

	
	/**
	 * ��ȡ��ǰWebӦ�õ�������·����
	 * @return
	 */
	public static String getAbsolutWebURL(HttpServletRequest request, boolean blnNeedPort) {
		String strAbsWebURL = request.getScheme() + "://" + request.getServerName();
		if (blnNeedPort) {
			strAbsWebURL = strAbsWebURL.concat(":" + request.getServerPort());
		}
		strAbsWebURL = strAbsWebURL.concat(request.getContextPath());
		return strAbsWebURL;
	}
	
	/**
	 * ���ݲ�ͬ�Ĳ���ϵͳ��ʽ���ļ��洢·����
	 * 
	 * @param strFileSeparator
	 * @return
	 */
	public static String getAbsolutWebAppPath(Class clazz, String strFileSeparator) {
//		String strOSWebAppPath = null;
		// WebApp�ľ���·��
		String strWebAppPath = CommonTool.urlDecodeUTF8(clazz.getClassLoader().getResource("/").getPath().replaceAll("/WEB-INF/classes/", ""));
//		if (strWebAppPath != null && strWebAppPath.startsWith("/")) {
//			strWebAppPath = strWebAppPath.substring(1);
//		}
		System.out.println(strWebAppPath);
//		String os = System.getProperty("os.name").toLowerCase();
//		if (os.startsWith("win")) { // Windows����ϵͳ
//			strOSWebAppPath = strWebAppPath.replaceAll("\\/", "\\" + strFileSeparator);
//		} else { // Linux��Unix����ϵͳ
//			strOSWebAppPath = strWebAppPath; // ��·�������滻����
//		}
//
//		return strOSWebAppPath;
		return strWebAppPath;
	}
	
	public static String getWebAppName(ServletContext svrContext) {
		return svrContext.getContextPath();
	}

	/**
	 * ���ز���SSl֤���httpClient.
	 * 
	 * @return
	 */
	public static CloseableHttpClient getDefaultHttpClient() {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		return httpclient;
	}

	/**
	 * ������ҪSSL֤���httpClient.
	 * 
	 * @param strCertPassword
	 * @return
	 */
	public static CloseableHttpClient getCertHttpClient(String strCertPassword) {
		if (strCertPassword == null || "".equals(strCertPassword)) {
			System.out.println("֤������Ϊ�գ�");
			return null;
		}

		CloseableHttpClient httpclient = null;
		/**
		 * ע��PKCS12֤�� �Ǵ�΢���̻�ƽ̨-���˻�����-�� API��ȫ �����ص�
		 */
		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance("PKCS12");
			String strCertFile = CommonTool.getAbsolutWebAppPath(CommonTool.class, System.getProperty("file.separator")) + "/conf/apiclient_cert.p12";
			System.out.println("strCertFile = " + strCertFile);
			FileInputStream instream = new FileInputStream(new File(strCertFile));// P12�ļ�Ŀ¼
			try {
				keyStore.load(instream, strCertPassword.toCharArray());
			} finally {
				instream.close();
			}

			// Trust own CA and all self-signed certs
			SSLContext sslcontext = SSLContexts.custom().loadKeyMaterial(keyStore, strCertPassword.toCharArray()).build();
			// Allow TLSv1 protocol only
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { "TLSv1" }, null, SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
			httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
		} catch (KeyStoreException | KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException
				| CertificateException | IOException e) {
			e.printStackTrace();
		}

		return httpclient;
	}
	
	/**
	 * ת��URL�ڵĲ���ΪUTF-8��ʽ��
	 * 
	 * @param str
	 * @return
	 */
	public static String urlDecodeUTF8(String str) {
		if (str == null) {
			return "";
		}

		try {
			return URLDecoder.decode(str.trim(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	/**
	 * ת��Float��Double��Ϊ2λС������ַ�����
	 * @return
	 */
	public static String formatNumToDoublePoints(double dblData) {
		return String.format("%.2f", dblData);
	}

	public static class MD5Util {
		private final String hexDigits[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

		/**
		 * MD5�����㷨��
		 * 
		 * @param sourceStr
		 * @return
		 */
		public String MD5(String sourceStr, String charsetName) {

			if (sourceStr == null || sourceStr.equals("")) {
				return null;
			}

			String resultString = null;
			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				if (charsetName == null || "".equals(charsetName)) {
					resultString = byteArrayToHexString(md.digest(sourceStr.getBytes()));
				} else {
					resultString = byteArrayToHexString(md.digest(sourceStr.getBytes(charsetName)));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				return null;
			}

			return resultString;
		}

		private String byteArrayToHexString(byte b[]) {
			StringBuffer resultSb = new StringBuffer();
			for (int i = 0; i < b.length; i++)
				resultSb.append(byteToHexString(b[i]));

			return resultSb.toString();
		}

		private String byteToHexString(byte b) {
			int n = b;
			if (n < 0)
				n += 256;
			int d1 = n / 16;
			int d2 = n % 16;
			return hexDigits[d1] + hexDigits[d2];
		}
	}
	
	/**
	 * ��ȡ�������ڵ���Ϣ��
	 * @param is
	 * @param strChrSet
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public static String getInputStreamInfo(InputStream is, String strChrSet) throws UnsupportedEncodingException, IOException {
		String strReqInfo = "";
		
		if (is != null) {
			/** ��ȡ��Ѷ�˷������Ļص���� **/
			BufferedReader bis = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			StringBuffer recieveData = new StringBuffer();
			String strLineInfo = null;
			while ((strLineInfo = bis.readLine()) != null) {
				recieveData.append(strLineInfo);
			}
			strReqInfo = recieveData.toString();
			
//			ByteArrayOutputStream outSteam = new ByteArrayOutputStream();  
//			int j = 0;
//			while ((j = is.read()) != -1) {
//				outSteam.write(j);
//			}
//			strReqInfo = outSteam.toString(strChrSet); //strReqInfo.concat(new String(byteBuffer, strChrSet));
		}
		
		return strReqInfo;
	}
}
