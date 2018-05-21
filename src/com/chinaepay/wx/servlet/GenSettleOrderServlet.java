package com.chinaepay.wx.servlet;

import java.util.Date;
import java.util.Map;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chinaepay.wx.common.CommonInfo;
import com.chinaepay.wx.common.CommonTool;
import com.chinaepay.wx.common.JsonRespObj;

import net.sf.json.JSONObject;

public abstract class GenSettleOrderServlet extends ProcBillAndSettleOrderServlet {
	public void init() {
		try {
			super.init();
			
			String strHour = this.getServletContext().getInitParameter("Hour_ProcMiddleSettle");
			String strMinute = this.getServletContext().getInitParameter("Minute_ProcMiddleSettle");
			String strSecond = this.getServletContext().getInitParameter("Second_ProcMiddleSettle");
			String strDelayTime = this.getServletContext().getInitParameter("DelayTime_ProcMiddleSettle");
	
			ProcSettleOrderThread psot = new ProcSettleOrderThread(strHour, strMinute, strSecond, strDelayTime);
			psot.start();
			
		} catch (ServletException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		ClosableTimer closableTimer = new ClosableTimer(true);	// ִ���������ر�Timer
		TimerTask task = this.getProcSettleOrderTask(closableTimer);
        closableTimer.schedule(task, 0L);
        
        JsonRespObj respObj = new JsonRespObj();
		String strProResult = "1";
		String strReturnMsg = "֧�������˿��ص��м���㵥���������Ѿ��ύ��ִ̨�У�";
        respObj.setRespCode(strProResult);
		respObj.setRespMsg(strReturnMsg);
		JSONObject jsonObj = JSONObject.fromObject(respObj);
		super.returnJsonObj(response, jsonObj);
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		doGet(request, response);
	}
	
	public abstract ProcSettleOrderTask getProcSettleOrderTask(ClosableTimer closableTimer);
	
	public class ProcSettleOrderThread extends Thread {
		private String strHour = null;
		private String strMinute = null;
		private String strSecond = null;
		private String strDelayTime = null;
		
		public ProcSettleOrderThread(String strHour, String strMinute, String strSecond, String strDelayTime) {
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
			
			ClosableTimer closableTimer = null;
			TimerTask task = null;
			// ��ǰʱ����11��֮ǰ, ��Ҫ�ȵ�11��ʱִ�����񣬲�������24Сʱ����ѯ����ִ����ͬ����
			if (lngNowMillSec < lngDefMillSec) {
				closableTimer = new ClosableTimer(false);
		        task = getProcSettleOrderTask(closableTimer);
		        closableTimer.scheduleAtFixedRate(task, defineDate, CommonInfo.ONE_DAY_TIME);
			}
			// ��ǰʱ����11��֮����Ҫ����ִ��һ�����񣬲��ڴ��յ�11�㿪ʼִ��һ�Σ��Ժ�ÿ��24Сʱ����ѯ����ִ����ͬ����
			else {
				// ִ��һ������(����ץȡ���ݵ�ʱ��)���������������ر�
				closableTimer = new ClosableTimer(true);
				task = getProcSettleOrderTask(closableTimer);
				closableTimer.schedule(task, Long.parseLong(strDelayTime) * 60 * 1000);
				
				// �ڴ���10�㿪ʼִ��һ������ �Ժ�ÿ��24Сʱ����ѯ����ִ����ͬ����
				closableTimer = new ClosableTimer(false);
				task = getProcSettleOrderTask(closableTimer);
				Date nextDay = CommonTool.getBefOrAftDate(defineDate, CommonInfo.ONE_DAY_TIME);
				closableTimer.scheduleAtFixedRate(task, nextDay, CommonInfo.ONE_DAY_TIME);
			}
		}
	}
	
	
	public abstract class ProcSettleOrderTask extends TimerTask {
		private ClosableTimer closableTimer = null;
		
		public ProcSettleOrderTask(ClosableTimer closableTimer) {
			super();
			this.closableTimer = closableTimer;
		}
		
		public void run() {
			/** ��ȡ��������ֵļ����� **/
			Map<String, Map<String, String>> mapMiddleSettleInfo = this.getMiddleSettleInfo();
			
			/** ����Ҫ��������ֵļ������������ݿ� **/
			boolean blnInsRst = this.insertMiddleSettleInfoToTbl(mapMiddleSettleInfo);
			
			/** ���¶��˵��ڵ�[�Ƿ�Ǩ�Ƶ����㵥]״̬Ϊ����Ǩ�ơ� **/
			if (blnInsRst) {
				this.updateReconResultTransfStatus(mapMiddleSettleInfo);
			}
			
			/** �ж��Ƿ���Ҫ�ر�����ʱ�� **/
			if (closableTimer.isNeedClose()) {
				closableTimer.cancel();
			}
		}
		
		public abstract Map<String, Map<String, String>> getMiddleSettleInfo();
		
		public abstract boolean insertMiddleSettleInfoToTbl(Map<String, Map<String, String>> mapMiddleSettleInfo);
		
		public abstract void updateReconResultTransfStatus(Map<String, Map<String, String>> mapMiddleSettleInfo);
	}
}
