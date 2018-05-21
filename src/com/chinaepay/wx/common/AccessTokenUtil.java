package com.chinaepay.wx.common;

import java.util.Date;

import com.chinaepay.wx.servlet.ExtendsHttpServlet;

import net.sf.json.JSONObject;

/**
 * ��ȡAccessToken�࣬�ڷ���ģ����Ϣʱ��Ҫ�õ�AccessToken�� ����ģ����ϢURL:
 * https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=
 * ACCESS_TOKEN http����ʽ: POST
 * 
 * @author xinwuhen
 */
public class AccessTokenUtil extends ExtendsHttpServlet {
	private static final String WX_ACCESS_TOKEN_GET_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";
	private static AccessTokenUtil accessTokenUtil = null;
	private AccessToken accessToken = null;

	private AccessTokenUtil() {}

	public static AccessTokenUtil getInstance() {
		if (accessTokenUtil == null) {
			accessTokenUtil = new AccessTokenUtil();
		}
		return accessTokenUtil;
	}

	/**
	 * ��ȡAccessToken����
	 * 
	 * @return
	 */
	public AccessToken getAccessTokenObj() {
		// �жϵ�ǰAccessToken�����Ƿ���Ч
		boolean blnIsValiable = this.isVilableToken(accessToken);
		if (!blnIsValiable) {	// ��ǰ�洢��Token�Ѿ�ʧЧ����Ҫ���´���Ѷ��̨��ȡ
			accessToken = getTokenFromTencent();
		}
		
		return accessToken;
	}
	
	/**
	 * �жϵ�ǰAccessToken�����Ƿ���Ч��
	 * @param accessToken
	 * @return
	 */
	private boolean isVilableToken(AccessToken accessToken) {
		boolean blnValidTokenObj = false;
		
		if (accessToken != null) {
			String strTokenValue = accessToken.getTokenValue();
			long lngExpireTime = accessToken.getExpiresTime();
			long lngCurrentTime = new Date().getTime();
			
			if (strTokenValue != null && !"".equals(strTokenValue)	// У��Token�ַ����Ƿ�Ϊ��
					&& lngCurrentTime < lngExpireTime) {	// У��Token�Ƿ����
				blnValidTokenObj = true;
			}
		}
		
		return blnValidTokenObj;
	}
	
	/**
	 * ����Ѷ���ȡAccessToken����
	 * @return
	 */
	private AccessToken getTokenFromTencent() {
		AccessToken accessToken = null;
		String strAccessTokenURL = WX_ACCESS_TOKEN_GET_URL.replaceFirst("APPID", CommonInfo.NOB_APP_ID).replaceFirst("APPSECRET", CommonInfo.NOB_APP_SECRET);
		
		String strJsonRespFromWx = sendReqAndGetResp(strAccessTokenURL, "", CommonTool.getDefaultHttpClient());
		if (strJsonRespFromWx != null && !strJsonRespFromWx.contains("errcode")) {
			JSONObject jsonObj = JSONObject.fromObject(strJsonRespFromWx);
			String strAccessToken = jsonObj.getString("access_token");
			String strExpiresIn = jsonObj.getString("expires_in");
			
			if (strAccessToken != null && !"".equals(strAccessToken) && strExpiresIn != null && !"".equals(strExpiresIn)) {
				// ����ǰʱ�����ʧЧʱ�������ǰ��10���ӡ��磺 ʧЧʱ����2Сʱ�������·�ʽ�����ʧЧʱ���Ϊ��ǰʱ��֮���1Сʱ50�֡�
				long lngExpireTime = new Date().getTime() + Long.parseLong(strExpiresIn) - 10 * 60 * 1000;
				accessToken = new AccessToken(strAccessToken, lngExpireTime);
			}
		}
		
		return accessToken;
	}

	/**
	 * AccessToken��װ�ࡣ
	 * 
	 * @author xinwuhen
	 */
	public class AccessToken {
		// �ӿڷ���ƾ֤
		private String tokenValue = null;

		// ƾ֤ʧЧʱ��
		private long expiresTime = 0L;

		private AccessToken(){}
		
		public AccessToken(String tokenValue, long expiresTime) {
			this.tokenValue = tokenValue;
			this.expiresTime = expiresTime;
		}
		
		public String getTokenValue() {
			return tokenValue;
		}
		
		public long getExpiresTime() {
			return expiresTime;
		}
	}
}
