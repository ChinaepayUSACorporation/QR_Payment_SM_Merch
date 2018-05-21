/**
 * @author xinwuhen
 */
package com.chinaepay.wx.common;

/**
 * @author xinwuhen
 * ������Ҫ�洢�̶������ò�����Ϣ���磺΢��֧��URL��JDBC���������������˺�ID��
 */
public class CommonInfo {
	/** ��Ѷ������������ַ **/
	// �й����
	public static final String HONG_KONG_CN_SERVER_URL = "https://apihk.mch.weixin.qq.com";
	// �й���½
	public static final String MAIN_LAND_CN_SERVER_URL = "https://api.mch.weixin.qq.com";
	
	// �û�ɨ���ά��, �ڻ�ȡ�û���OpenId֮ǰ, ���ȡCODE��
	public static final String GET_WX_AUTH_CODE_URL = "payment/getWxAuthCodeServlet";
	// ��ȡ΢�ŵ�OpenId��������Ҫ�û�����֧������ҳ��
	public static final String GET_WX_OPEN_ID_URL = "payment/getWxOpenIdServlet";
	
	// ����֧����Ļص�URL(Servlet���ƣ������web.xml��������)
	public static final String NOTIFY_URL_SERVLET = "payment/notifyURLServlet";
	
	/** �ļ����Ŀ¼ **/
	// ��ά���ļ�Ŀ¼
	public static final String QR_IMG_FOLDER = "/upload/harvpay/qr_img";
	// NOB�ϴ��Ķ��˵������ļ����Ŀ¼
	public static final String SETTLE_FILE_FOLDER_FOR_NOB_UP = "/upload/harvpay/stl_nob_up";
	// ΪNOB���ض����ɵĶ��˵��ļ����Ŀ¼
	public static final String SETTLE_FILE_FOLDER_FOR_NOB_DOWN = "/upload/harvpay/stl_nob_down";
	
	/** NOB������صĻ�����Ϣ(����Ѷ����) **/
	// �����˺�ID
	public static final String NOB_APP_ID = "wxd1be3a5544867c03";
	// ���ں���Կ
	public static final String NOB_APP_SECRET = "680228e663f997100af02a70813cabf5";
	// �̻���
	public static final String NOB_MCH_ID = "1494500362";
	// ��Կ
	public static final String NOB_KEY = "ChinaepayUSA17029371969000000000";
	// �̻�֤�����루����Ĭ��Ϊ�����̻�ID��
	public static String SSL_CERT_PASSWORD = "1494500362";
	// ���ն��̻�������Ϣ֪ͨʱ����Ϣģ��ID
	public static String PAYMENT_NOTICE_TEMPLATE_ID = "k9fBz37IC-jfDx4RGFboVL61i8s9ORxfBZ4seQVAGfA";
	
	/** HarvestΨһ��ID���˴�Ϊ������õ����� **/
	public static final String HARVEST_ID = "HARVEST";
	
	// 24Сʱ����Ӧ�ĺ�����
	public static final long ONE_DAY_TIME = 24 * 60 * 60 * 1000L;
}
