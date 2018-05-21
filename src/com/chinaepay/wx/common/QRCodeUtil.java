package com.chinaepay.wx.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * ��ά�빤�߰���
 * 
 * @author xinwuhen
 */
public class QRCodeUtil {
	private static QRCodeUtil qrCodeUtil = null;

	private QRCodeUtil() {
	}

	public static QRCodeUtil getInstance() {
		if (qrCodeUtil == null) {
			qrCodeUtil = new QRCodeUtil();
		}

		return qrCodeUtil;
	}

	/**
	 * ���ݲ������ɶ�ά��ͼƬ��
	 * 
	 * @param imgCharactCode
	 *            �ַ�����, Ĭ��Ϊ:UTF-8.
	 * @param imgWidth
	 *            ͼƬ���, Ĭ��Ϊ: 300px
	 * @param imgHeight
	 *            ͼƬ�߶�, Ĭ��Ϊ: 300px
	 * @param strImgFileFoler
	 * 			    ͼƬ�洢Ŀ¼
	 * @param imgFileName
	 *            ͼƬ����(�磺myTestQrImg.png)
	 * @param qrContent
	 *            ��ά������
	 * @return ��ά��ͼƬ���ļ�����
	 */
	public File genQrCodeImg(String imgCharactCode, int imgWidth, int imgHeight, String strImgFileFoler, String imgFileName, String qrContent) {
		File imgFullFile = null;
		
		if (strImgFileFoler == null || "".equals(strImgFileFoler) || imgFileName == null || "".equals(imgFileName) 
				|| qrContent == null || "".equals(qrContent)) {
			return imgFullFile;
		}
		
		BitMatrix bitMatrix = null;
		try {
			// �����ά������Ĺ�ϣӳ���
			HashMap<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
			// ���뷽ʽ��֧������
			imgCharactCode = (imgCharactCode == null || "".equals(imgCharactCode) ? "UTF-8" : imgCharactCode);
			hints.put(EncodeHintType.CHARACTER_SET, imgCharactCode);
			// �ݴ�ȼ�(�ݴ�ȼ� L��M��Q��H ���� L Ϊ���, H Ϊ���)
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
			// ��ά��߾�
			hints.put(EncodeHintType.MARGIN, 1);
			
			// ���ɵ���
			imgWidth = (imgWidth <= 0 ? 300 : imgWidth);	// Ĭ��Ϊ300px
			imgHeight = (imgHeight <= 0 ? 300 : imgHeight);	// Ĭ��Ϊ300px
			
			bitMatrix = new MultiFormatWriter().encode(qrContent, BarcodeFormat.QR_CODE, imgWidth, imgHeight, hints);
			
			// ����Ŀ¼
			File fileImgFoler = new File(strImgFileFoler);
			if (!fileImgFoler.exists()) {
				fileImgFoler.mkdir();
			}
			
			// ͼƬ���ļ�����
			String strImgFullName = fileImgFoler.getPath() + "/" + imgFileName;
			imgFullFile = new File(strImgFullName);
			
			// ͼƬ��չ��(����ͼƬ��ʽ)
			Path filePath = imgFullFile.toPath();
			String imgFormat = imgFileName.substring(imgFileName.lastIndexOf(".") + 1);
			
			// ����ļ�
			MatrixToImageWriter.writeToPath(bitMatrix, imgFormat, filePath);
		} catch (WriterException | IOException e) {
			e.printStackTrace();
			imgFullFile = null;
		}
		
		return imgFullFile;
	}
}
