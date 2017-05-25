package my.smpp;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import com.logica.smpp.pdu.*;

import uti.MyConfig;
import uti.MyConfig.Telco;

/*
 * Chứa các config cấu hình cho chương trình
 */
public class Config
{
	public static class smpp
	{
		// config connection to SMSC
		public static String ipAddress = null;
		public static int port = 0;
		public static String systemId = null;
		public static String password = null;
		public static String systemType = "";
		public static String serviceType = "";

		public static byte srcAddrTon = 0;
		public static byte srcAddrNpi = 1;

		public static byte destAddrTon = 0;
		public static byte destAddrNpi = 1;

		public static byte dataCoding = 0;

		public static String validShortCode = ",9412";

		public static int receiveTimeout = 1000;
		public static int receiveDelay = 1000;

		/**
		 * Thời gian nghỉ cho mỗi lần rebind lại nếu có lỗi
		 */
		public static int rebindTimeout = 10000;

		/**
		 * Cho biết GW bind theo mode bất đồng bộ hay không
		 */
		public static boolean asyncMode = true;

		/**
		 * thời gian nghỉ cho mỗi lần gửi bản tin EnquireLink
		 */
		public static int checkEnquireLinkInterval = 100000;

		public static MyConfig.Telco telco = Telco.VIETTEL;

		public static AddressRange addressRange = new AddressRange();
	}

	public static class mo
	{
		public static int numberThreadSaveMo = 1;
	}

	public static class mt
	{
		public static int senderDelay = 50;
		/**
		 * Số lượng MT push sang SMSC trong 1 giây
		 */
		public static int tps = 100;

		/**
		 * Thời gian cho phép gửi lại 1 MT, nếu vượt quá thời gian này thì MT
		 * không được gửi nữa
		 */
		public static int resendDelay = 1000 * 30;

		/**
		 * Sau thời gian này, những MT không nhận được respone thì sẽ được lưu
		 * xuống log
		 */
		public static int responseTimeout = 30000;

		/**
		 * Thời gian delay cho mỗi lần check các MT đang chờ response
		 */
		public static int responseCheckInterval = 15000;

		/**
		 * Số lần retry send MT sang telco cho phép
		 */
		public static int maxRetrySendMt = 1;

		/**
		 * Chiều dài tốt đa cho 1 MT được gửi
		 */
		public static int maxLengthMt = 500;

		// Số lượng thread
		public static int numberThreadLoadMt = 1;
		public static int numberThreadResend = 1;
		public static int numberThreadResponse = 1;
		public static int numberThreadSaveMtlog = 1;

	}
	
	public static class cdr
	{
		/**
		 * Sau thời gian này, những cdr không nhận được respone từ mt thì sẽ
		 * được lưu xuống cdrQueue và hoàn tiền cho khách
		 */
		public static int waitingMtTimeout = 90 * 60 * 1000;
		/**
		 * Cho phép gateway tạo CDR hay không. VD: nếu trường hợp là đấu số dịch
		 * vụ sub thì ko cần phải CDR.
		 */
		public static boolean allowCreateCdr = true;
		/**
		 * Số lượng thread Save CDRQueue
		 */
		public static int numberThreadSaveCdr = 1;
	}
	public static class log
	{
		public static String configPath = "log4j.properties";
	}

	public static class db
	{
		public static String configPath = "hibernate.cfg.xml";
	}

	public static class app
	{
		/**
		 * Thư mục hiện tại của app, nếu để trống thì là thư mục để file chạy
		 */
		public static String currentPath = "";
		/**
		 * Nơi lưu trữ các queue nếu chương trình bị tắt
		 */
		public static String saveQueuePath = "";
	}

	/**
	 * Kiểm tra và tạo folder
	 * 
	 * @param path
	 * @return
	 */
	public static boolean checkAndCreateFolder(String folderPath)
	{
		try
		{
			File dir = new File(folderPath);
			if (!dir.exists())
			{
				if (dir.mkdir())
				{
					return true;
				}
				return false;
			}
			return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
		}
	}
	public static Properties mProp;

	public static boolean loadConfig(String propFile)
	{
		Properties properties = new Properties();
		System.out.println("Reading configuration file " + propFile);
		try
		{
			FileInputStream fin = new FileInputStream(propFile);
			properties.load(fin);
			mProp = properties;
			fin.close();
			//Smpp
			{
			smpp.ipAddress = properties.getProperty("smpp.ipAddress", "");
			smpp.port = Integer.parseInt(properties.getProperty("smpp.port", "0"));
			smpp.systemId = properties.getProperty("smpp.systemId", "");
			smpp.password = properties.getProperty("smpp.password", "");
			smpp.validShortCode = properties.getProperty("smpp.validShortCode", "");
			smpp.receiveTimeout = Integer
					.parseInt(properties.getProperty("smpp.receiveTimeout", Integer.toString(smpp.receiveTimeout)));
			smpp.receiveDelay = Integer.parseInt(properties.getProperty("smpp.receiveDelay", "1"));
			smpp.checkEnquireLinkInterval = Integer.parseInt(properties.getProperty("smpp.checkEnquireLinkInterval",
					Integer.toString(smpp.checkEnquireLinkInterval)));

			mo.numberThreadSaveMo = Integer
					.parseInt(properties.getProperty("mo.numberThreadSaveMo", Integer.toString(mo.numberThreadSaveMo)));
			}
			
			//mt
			{
			mt.tps = Integer.parseInt(properties.getProperty("mt.tps", Integer.toString(mt.tps)));
			mt.resendDelay = Integer
					.parseInt(properties.getProperty("mt.resendDelay", Integer.toString(mt.resendDelay)));
			mt.responseTimeout = Integer
					.parseInt(properties.getProperty("mt.responseTimeout", Integer.toString(mt.responseTimeout)));
			mt.responseCheckInterval = Integer.parseInt(
					properties.getProperty("mt.responseCheckInterval", Integer.toString(mt.responseCheckInterval)));
			mt.maxRetrySendMt = Integer
					.parseInt(properties.getProperty("mt.maxRetrySendMt", Integer.toString(mt.maxRetrySendMt)));
			mt.maxLengthMt = Integer
					.parseInt(properties.getProperty("mt.maxLengthMt", Integer.toString(mt.maxLengthMt)));
			mt.numberThreadLoadMt = Integer
					.parseInt(properties.getProperty("mt.numberThreadLoadMt", Integer.toString(mt.numberThreadLoadMt)));
			mt.numberThreadResend = Integer
					.parseInt(properties.getProperty("mt.numberThreadResend", Integer.toString(mt.numberThreadResend)));
			mt.numberThreadResponse = Integer.parseInt(
					properties.getProperty("mt.numberThreadResponse", Integer.toString(mt.numberThreadResponse)));
			mt.numberThreadSaveMtlog = Integer.parseInt(
					properties.getProperty("mt.numberThreadSaveMtlog", Integer.toString(mt.numberThreadSaveMtlog)));
			}
			
			cdr.waitingMtTimeout = Integer.parseInt(properties.getProperty("cdr.waitingMtTimeout", Integer.toString(cdr.waitingMtTimeout)));
			cdr.allowCreateCdr = Boolean.parseBoolean(properties.getProperty("cdr.allowCreateCdr", Boolean.toString(cdr.allowCreateCdr)));
			cdr.numberThreadSaveCdr = Integer.parseInt(properties.getProperty("cdr.numberThreadSaveCdr", Integer.toString(cdr.numberThreadSaveCdr)));
			
			log.configPath = properties.getProperty("log.configPath", log.configPath);			
			
			db.configPath = properties.getProperty("db.configPath", db.configPath);
			
			app.currentPath =properties.getProperty("app.currentPath", app.currentPath);
			app.saveQueuePath =properties.getProperty("app.saveQueuePath", app.saveQueuePath);

			return true;
		}
		catch (Exception e)
		{
			System.out.println(e);
			return false;
		}

	}

}
