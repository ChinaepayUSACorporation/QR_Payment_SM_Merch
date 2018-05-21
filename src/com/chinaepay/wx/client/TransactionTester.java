/**
 * @author xinwuhen
 */
package com.chinaepay.wx.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.entity.InquiryPayOrderEntity;
import com.chinaepay.wx.entity.InquiryRefundOrderEntity;
import com.chinaepay.wx.entity.PayOrderEntiry;
import com.chinaepay.wx.entity.RefundOrderEntity;

/**
 * @author xinwuhen
 *	������Ҫ���ڲ��ԡ�
 */
public class TransactionTester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String strUrlWebRoot = "http://lmk.javang.cn/QRPaymentSM";
		TransactionTester transTester = new TransactionTester();
		
		/** ��ѯ�����ʽ� **/
//		try {
//			// װ�����
//			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
//			nvps.add(new BasicNameValuePair("startTimeForSettleOrder", "20180403"));
//			nvps.add(new BasicNameValuePair("endTimeForSettleOrder", "20180502"));
//			String strURL = "http://localhost:8080/QR_Payment/payment/downloadSettleOrderServlet";
//			transTester.sendReqAndGetRespInfo(strURL, nvps);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		/** ��������QRͼƬ����� **/
//		try {
//			transTester.genQRCodeAndUpToTbl();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		
		/** ����֧������ **/
//		strBizType = CommonInfo.PAYMENT_TRANSACTION_BIZ;
//		strBizReq = transTester.getPamentTransRequest();
//		exeSocketTest(strBizType + ":" + strBizReq);
		
		/** ���Բ�ѯ���� **/
//		strBizType = CommonInfo.INQUIRY_TRANSACTION_BIZ;
//		strBizReq = transTester.getInquiryTransRequest();
//		exeSocketTest(strBizType + ":" + strBizReq);
		
		/** ���Գ������� **/
//		strBizType = CommonInfo.REVERSE_TRANSACTION_BIZ;
//		strBizReq = transTester.getReverseTransRequest();
//		exeSocketTest(strBizType + ":" + strBizReq);
		
		/** �����˿�� **/
		try {
			// װ�����
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("out_trade_no", "20180517203623357257767445563118"));
			nvps.add(new BasicNameValuePair("refund_fee", "1"));
			nvps.add(new BasicNameValuePair("refund_desc", "�ҵ��˿�."));
			String strURL = strUrlWebRoot + "/payment/refundOrderServlet";
			transTester.sendReqAndGetRespInfo(strURL, nvps);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		/** ���Բ�ѯ�˿ **/
//		strBizType = CommonInfo.INQUIRY_REFUND_BIZ;
//		strBizReq = transTester.getInquiryRefundRequest();
//		exeSocketTest(strBizType + ":" + strBizReq);
		
	}

	private void sendReqAndGetRespInfo(String strUrl, List<NameValuePair> nvps) throws IOException {
		String body = "";
		// ����httpclient����
		CloseableHttpClient client = HttpClients.createDefault();
		// ����post��ʽ�������
		HttpPost httpPost = new HttpPost(strUrl); // ������Ӧͷ��Ϣ

		// ���ò��������������
		httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

		System.out.println("�����ַ��" + httpPost.getURI().toURL());
		System.out.println("���������" + nvps.toString());

		// ����header��Ϣ
		// ָ������ͷ��Content-type������User-Agent��
		httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
		httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

		// ִ��������������õ������ͬ��������
		CloseableHttpResponse response = client.execute(httpPost);
		
		int intStatusCode = response.getStatusLine().getStatusCode();
		String strReasonPhrase = response.getStatusLine().getReasonPhrase();
		
		System.out.println("intStatusCode = " + intStatusCode);
		System.out.println("strReasonPhrase = " + strReasonPhrase);
		
		if(intStatusCode == HttpStatus.SC_OK && "ok".equalsIgnoreCase(strReasonPhrase)) {
			// ��ȡ���ʵ��
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				// ��ָ������ת�����ʵ��ΪString����
				body = EntityUtils.toString(entity, "UTF-8");
			}
			EntityUtils.consume(entity);

			System.out.println("body = " + body);
		}
		
		// �ͷ�����
		response.close();
	}

	private void genQRCodeAndUpToTbl() throws IOException {

		String body = "";

		// ����httpclient����
		CloseableHttpClient client = HttpClients.createDefault();
		// ����post��ʽ�������
		HttpPost httpPost = new HttpPost("http://localhost:8080/QR_Payment/qrcode/genQrCodeSvlt"); // ������Ӧͷ��Ϣ

		// װ�����
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("sub_mch_id", "12152566"));

		// ���ò��������������
		httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

		System.out.println("�����ַ��" + httpPost.getURI().toURL());
		System.out.println("���������" + nvps.toString());

		// ����header��Ϣ
		// ָ������ͷ��Content-type������User-Agent��
		httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
		httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

		// ִ��������������õ������ͬ��������
		CloseableHttpResponse response = client.execute(httpPost);
		
		int intStatusCode = response.getStatusLine().getStatusCode();
		String strReasonPhrase = response.getStatusLine().getReasonPhrase();
		
		System.out.println("intStatusCode = " + intStatusCode);
		System.out.println("strReasonPhrase = " + strReasonPhrase);
		
		if(intStatusCode == HttpStatus.SC_OK && "ok".equalsIgnoreCase(strReasonPhrase)) {
			// ��ȡ���ʵ��
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				// ��ָ������ת�����ʵ��ΪString����
				body = EntityUtils.toString(entity, "UTF-8");
			}
			EntityUtils.consume(entity);

			System.out.println("body = " + body);
		}
		
		// �ͷ�����
		response.close();
	}
	
	/**
	 * ֧�����׶�Ӧ�������ģ���ʽ��appid=43453&mch_id=dsw342&sub_mch_id=983477232&nonce_str=aiadjsis8732487jsd8l
	 * @return
	 */
	private String getPamentTransRequest() {
		
		String strAuthCode = "135029600960750624"; // ��ά���е��û���Ȩ��
		StringBuffer sb = new StringBuffer();
		sb.append(PayOrderEntiry.AGENT_ID + "=" + "1r10s84408mdj0tgp6iov2c0k54jbps9");
		sb.append("&" + PayOrderEntiry.SUB_MCH_ID + "=" + "12152566");
		sb.append("&" + PayOrderEntiry.NONCE_STR + "=" + CommonTool.getRandomString(32));
		sb.append("&" + PayOrderEntiry.BODY + "=" + "Ipad mini  16G  ��ɫ"); // Ipad mini  16G  ��ɫ
		sb.append("&" + PayOrderEntiry.OUT_TRADE_NO + "=" + CommonTool.getOutTradeNo(new Date(), 18));
		sb.append("&" + PayOrderEntiry.TOTAL_FEE + "=" + "1");
		sb.append("&" + PayOrderEntiry.FEE_TYPE + "=" + "USD");
		sb.append("&" + PayOrderEntiry.SPBILL_CREATE_IP + "=" + CommonTool.getSpbill_Create_Ip());
		sb.append("&" + PayOrderEntiry.AUTH_CODE + "=" + strAuthCode);
//		sb.append("&" + PaymentTransactionEntity.APP_KEY + "=" + "024edfffae32c829b012c98a61686f3b");
		
		return sb.toString();
	}
	
	/**
	 * �����������ġ�
	 * @return
	 */
//	private String getReverseTransRequest() {
//		StringBuffer sb = new StringBuffer();
//		sb.append(ReverseTransactionEntity.AGENT_ID + "=" + "1r10s84408mdj0tgp6iov2c0k54jbps9");
//		sb.append("&" + ReverseTransactionEntity.SUB_MCH_ID + "=" + "12152566");
//		sb.append("&" + ReverseTransactionEntity.OUT_TRADE_NO + "=" + "20180317162341003734102708751406"); // ����ʱ�޸Ĵ��ֶ�
//		sb.append("&" + ReverseTransactionEntity.NONCE_STR + "=" + CommonTool.getRandomString(32));
////		sb.append("&" + ReverseTransactionEntity.APP_KEY + "=" + "024edfffae32c829b012c98a61686f3b");
//		
//		return sb.toString();
//	}
	
	/**
	 * �˿������ġ�
	 * @return
	 */
	private String getRefundTransRequest() {
		StringBuffer sb = new StringBuffer();
		sb.append(RefundOrderEntity.AGENT_ID + "=" + "1r10s84408mdj0tgp6iov2c0k54jbps9");
		sb.append("&" + RefundOrderEntity.SUB_MCH_ID + "=" + "12152566");
		sb.append("&" + RefundOrderEntity.NONCE_STR + "=" + CommonTool.getRandomString(32));
		sb.append("&" + RefundOrderEntity.OUT_TRADE_NO + "=" + "20180317173710924965776167374654"); // ����ʱ�޸Ĵ��ֶ�
		sb.append("&" + RefundOrderEntity.OUT_REFUND_NO + "=" + CommonTool.getOutRefundNo(new Date(), 18));	// ͬһ�˿��ʱ�ǵ��޸Ĵ��ֶ�Ϊ�̶�ֵ
		sb.append("&" + RefundOrderEntity.TOTAL_FEE + "=" + "1");
		sb.append("&" + RefundOrderEntity.REFUND_FEE + "=" + "1");
//		sb.append("&" + RefundTransactionEntity.APP_KEY + "=" + "024edfffae32c829b012c98a61686f3b");
		
		return sb.toString();
	}
	
	/**
	 * ���ɲ�ѯ��������Ĳ����б�(HashMap����).
	 * @param hmTransactionOrderCont
	 * @return
	 */
	private String getInquiryTransRequest() {
		StringBuffer sb = new StringBuffer();
		sb.append(InquiryPayOrderEntity.AGENT_ID + "=" + "1r10s84408mdj0tgp6iov2c0k54jbps9");
		sb.append("&" + InquiryPayOrderEntity.SUB_MCH_ID + "=" + "12152566");
		sb.append("&" + InquiryPayOrderEntity.OUT_TRADE_NO + "=" + "20180317173710924965776167374654");	// ����ʱ�޸Ĵ˲���
		sb.append("&" + InquiryPayOrderEntity.NONCE_STR + "=" + CommonTool.getRandomString(32));
//		sb.append("&" + InquiryTransactionEntity.APP_KEY + "=" + "024edfffae32c829b012c98a61686f3b");
		
		return sb.toString();
	}
	
	/**
	 * ��ѯ�˿��Ӧ�������ġ�
	 * @return
	 */
	private String getInquiryRefundRequest() {
		StringBuffer sb = new StringBuffer();
		sb.append(InquiryRefundOrderEntity.AGENT_ID + "=" + "1r10s84408mdj0tgp6iov2c0k54jbps9");
		sb.append("&" + InquiryRefundOrderEntity.SUB_MCH_ID + "=" + "12152566");
		sb.append("&" + InquiryRefundOrderEntity.NONCE_STR + "=" + CommonTool.getRandomString(32));
		sb.append("&" + InquiryRefundOrderEntity.OUT_TRADE_NO + "=" + "20180317173710924965776167374654");	// ����ʱ���޸Ĵ˲���
//		sb.append("&" + InquiryRefundEntity.APP_KEY + "=" + "024edfffae32c829b012c98a61686f3b");
		
		return sb.toString();
	}
}
