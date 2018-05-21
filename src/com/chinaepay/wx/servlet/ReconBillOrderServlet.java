package com.chinaepay.wx.servlet;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.JsonRespObj;

import net.sf.json.JSONObject;

public abstract class ReconBillOrderServlet extends ProcBillAndSettleOrderServlet {
	public void init() {
		try {
			super.init();
			
			// ��ȡ�ȶԶ��˵�����������ָ��ʱ��
			String strHour = this.getServletContext().getInitParameter("Hour_ReconBill");
			String strMinute = this.getServletContext().getInitParameter("Minute_ReconBill");
			String strSecond = this.getServletContext().getInitParameter("Second_ReconBill");
			String strDelayTime = this.getServletContext().getInitParameter("DelayTime_ReconBill");
	
			// �����ȶԶ��˵��������߳�
			ReconBillOrderThread dbpot = new ReconBillOrderThread(strHour, strMinute, strSecond, strDelayTime);
			dbpot.start();
		} catch (ServletException e) {
			e.printStackTrace();
		}
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) {

		// ��ע�⡿��ʱ���ʽ��ȷ�����족���磺20180302
		String startDayForRecBillOrder = request.getParameter("startDayForReconBillOrder"); 
		String endDayForRecBillOrder = request.getParameter("endDayForReconBillOrder");
		
		ClosableTimer closableTimer = new ClosableTimer(true);	// ִ���������ر�Timer
		TimerTask task = this.getReconBillOrderTask(closableTimer, startDayForRecBillOrder, endDayForRecBillOrder);
        closableTimer.schedule(task, 0L);
        
        JsonRespObj respObj = new JsonRespObj();
		String strGenResult = "1";
		String strReturnMsg = "���ɶ��˵�(����֧�������˿)�����Ѿ��ύ��ִ̨�У�";
        respObj.setRespCode(strGenResult);
		respObj.setRespMsg(strReturnMsg);
		JSONObject jsonObj = JSONObject.fromObject(respObj);
		super.returnJsonObj(response, jsonObj);
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		doGet(request, response);
	}
	
	public abstract ReconBillOrderTask getReconBillOrderTask(ClosableTimer closableTimer, String strReconStartDay, String strReconEndDay);
	
	/**
	 * ���˵��ȶ�����������̡߳�
	 * @author xinwuhen
	 */
	public class ReconBillOrderThread extends Thread {
		private String strHour = null;
		private String strMinute = null;
		private String strSecond = null;
		private String strDelayTime = null;
		
		public ReconBillOrderThread(String strHour, String strMinute, String strSecond, String strDelayTime) {
			this.strHour = strHour;
			this.strMinute = strMinute;
			this.strSecond = strSecond;
			this.strDelayTime = strDelayTime;
		}
		
		public void run() {
			// ��ǰʱ��
			Date nowDate =  new Date();
			long lngNowMillSec = nowDate.getTime();
			
			// ��ȡָ������������ʱ��
			Date defineDate = getFixDateBasedOnArgs(strHour, strMinute, strSecond);
			long lngDefMillSec = defineDate.getTime();
			
			String strReconStartDay = CommonTool.getBefOrAftFormatDate(nowDate, -CommonInfo.ONE_DAY_TIME, "yyyyMMdd");
			String strReconEndDay = strReconStartDay;	// Ĭ��ִ������ȫ��Ķ��˵��˶�
			ClosableTimer closableTimer = null;
			TimerTask task = null;
			
			System.out.println("+++lngNowMillSec = " + lngNowMillSec);
			System.out.println("+++lngDefMillSec = " + lngDefMillSec);
			
			// ��ǰʱ����11��֮ǰ, ��Ҫ�ȵ�11��ʱִ�����񣬲�������24Сʱ����ѯ����ִ����ͬ����
			if (lngNowMillSec < lngDefMillSec) {
				closableTimer = new ClosableTimer(false);
		        task = getReconBillOrderTask(closableTimer, strReconStartDay, strReconEndDay);
		        closableTimer.scheduleAtFixedRate(task, defineDate, CommonInfo.ONE_DAY_TIME);
			}
			// ��ǰʱ����11��֮����Ҫ����ִ��һ�����񣬲��ڴ��յ�11�㿪ʼִ��һ�Σ��Ժ�ÿ��24Сʱ����ѯ����ִ����ͬ����
			else {
				// ִ��һ������(����ץȡ���ݵ�ʱ��)���������������ر�
				closableTimer = new ClosableTimer(true);
				task = getReconBillOrderTask(closableTimer, strReconStartDay, strReconEndDay);
				closableTimer.schedule(task, Long.parseLong(strDelayTime) * 60 * 1000);	// ������ת��Ϊ����
				
				// �ڴ���10�㿪ʼִ��һ������ �Ժ�ÿ��24Сʱ����ѯ����ִ����ͬ����
				closableTimer = new ClosableTimer(false);
				task = getReconBillOrderTask(closableTimer, strReconStartDay, strReconEndDay);
				Date nextDay = CommonTool.getBefOrAftDate(defineDate, CommonInfo.ONE_DAY_TIME);
				closableTimer.scheduleAtFixedRate(task, nextDay, CommonInfo.ONE_DAY_TIME);
			}
		}
	}
	
	public abstract class ReconBillOrderTask extends TimerTask {
		public ClosableTimer closableTimer = null;
		public String strReconStartDay = null;
		public String strReconEndDay = null;
		
		public ReconBillOrderTask(ClosableTimer closableTimer, String strReconStartDay, String strReconEndDay) {
			super();
			this.closableTimer = closableTimer;
			this.strReconStartDay = strReconStartDay;
			this.strReconEndDay = strReconEndDay;
		}
		
		public void run() {
			// ����NOB(Harvest)����ڣ���Tencent�಻���ڵļ�¼
			Map<String, Map<String, String>> mapOuterInfo = this.getNobMoreThanWxOrderRecord(strReconStartDay, strReconEndDay);
			System.out.println("NobMoreThanWx's mapOuterInfo = " + mapOuterInfo);
			this.insertFullRecInfoToTbl(mapOuterInfo);
			
			// ����Tencent����ڣ���NOB(Harvest)�಻���ڵļ�¼
			mapOuterInfo = this.getWxMoreThanNobOrderRecord(strReconStartDay, strReconEndDay);
			System.out.println("WxMoreThanNob's mapOuterInfo = " + mapOuterInfo);
			this.insertFullRecInfoToTbl(mapOuterInfo);
			
			// ����NOB(Harvest)����Tencent�඼���ڵļ�¼
			mapOuterInfo = this.getWxEqualNobOrderRecord(strReconStartDay, strReconEndDay);
			System.out.println("WxEqualNob's mapOuterInfo = " + mapOuterInfo);
			this.insertFullRecInfoToTbl(mapOuterInfo);
			
			// �ж��Ƿ���Ҫ�ر�����ʱ��
			System.out.println("closableTimer.isNeedClose() = " + closableTimer.isNeedClose());
			if (closableTimer.isNeedClose()) {
				closableTimer.cancel();
			}
		}
		
		/**
		 * ����NOB(Harvest)����ڣ���Tencent�಻���ڵļ�¼��
		 * @param strReconStartDay
		 * @param strReconEndDay
		 */
		public abstract Map<String, Map<String, String>> getNobMoreThanWxOrderRecord(String strReconStartDay, String strReconEndDay);
		
		/**
		 * ����Tencent����ڣ���NOB(Harvest)�಻���ڵļ�¼��
		 * @param strReconStartDay
		 * @param strReconEndDay
		 */
		public abstract Map<String, Map<String, String>> getWxMoreThanNobOrderRecord(String strReconStartDay, String strReconEndDay);
		
		/**
		 * ����NOB(Harvest)����Tencent�඼���ڵļ�¼��
		 * @param strReconStartDay
		 * @param strReconEndDay
		 */
		public abstract Map<String, Map<String, String>> getWxEqualNobOrderRecord(String strReconStartDay, String strReconEndDay);
		
		/**
		 * ���洢�������е�������Ϣ������˽�����ݿ⡣
		 * @param mapOuterInfo
		 */
		public abstract void insertFullRecInfoToTbl(Map<String, Map<String, String>> mapOuterInfo);
		
		/**
		 * У�鵱ǰ֧�����������̻��Ƿ�������Ӧ��ģ�塣
		 * @param strOrderNo
		 * @return
		 */
		public abstract boolean validRefTemplet(String strOrderNo);
		
		/**
		 * Ϊ�˲��ٸ��¶��˽�����ڶ��˳ɹ��ļ�¼����Ҫ������÷��������ݼ��޳���Щ�ɹ������ݡ�
		 * @param mapOuterInfo
		 * @param lstOutTradeNoRecOk
		 * @return
		 */
		public Map<String, Map<String, String>> getNewOuterInfo(Map<String, Map<String, String>> mapOuterInfo, List<String> lstOutTradeNoRecOk) {
			Map<String, Map<String, String>> mapNewOuterInfo = new HashMap<String, Map<String, String>>();
			
			String[] strOutTradeNos = mapOuterInfo.keySet().toArray(new String[0]);
			for (String strOutTradeNo : strOutTradeNos) {
				if (!lstOutTradeNoRecOk.contains(strOutTradeNo)) {	// ��ԭʼ�ԱȽ����ȡ���ļ�¼�����������ϴζ��˳ɹ��Ľ������
					mapNewOuterInfo.put(strOutTradeNo, mapOuterInfo.get(strOutTradeNo));
				}
			}
			
			return mapNewOuterInfo;
		}
	}
}
