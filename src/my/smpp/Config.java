package my.smpp;

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
		
		public static AddressRange addressRange  = new AddressRange();
	}

	public static class mo
	{
		public static int receiveTimeout = 0;
		public static int receiveDelay = 1000;

		public static int numberThreadSaveMo = 1;
	}

	public static class mt
	{
		public static int senderTimeout = 0;
		public static int senderDelay = 1000;
		public static int senderRequestTps = 100;

		/**
		 * Thời gian cho phép gửi lại 1 MT, nếu vượt quá thời gian này thì MT
		 * không được gửi nữa
		 */
		public static int resendTimeout = 1000 * 30;
		public static int resendDelay = 1000 * 30;

		// Số lượng thread
		public static int numberThreadLoadMt = 1;
		public static int numberThreadResend = 1;
		public static int numberThreadResponse = 1;

		/**
		 * Số lần retry send MT sang telco cho phép
		 */
		public static int maxRetrySendMt = 1;

		/**
		 * Chiều dài tốt đa cho 1 MT được gửi
		 */
		public static int maxLengthMt = 500;
		
		/**
		 * Sau thời gian này, những MT không nhận được respone thì sẽ được lưu xuống log
		 */
		public static int responseTimeout = 30000;
		
		/**
		 * Thời gian delay cho mỗi lần check các MT đang chờ response
		 */
		public static int responseCheckInterval = 15000;
		
	}
	public static class cdr
	{
		/**
		 * Sau thời gian này, những cdr không nhận được respone từ mt thì sẽ được lưu xuống cdrQueue và hoàn tiền cho khách
		 */
		public static int waitingMtTimeout = 300000;
	}
	public static class log
	{
		public static String configPath = "log4j.properties";
		public static String folderPath = ".\\LogFile\\";
	}

	public static class db
	{
		public static String configPath = "hibernate.cfg.xml";
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

			smpp.ipAddress = properties.getProperty("smpp.ipAddress", "");
			smpp.port = Integer.parseInt(properties.getProperty("smpp.port", "0"));
			smpp.systemId = properties.getProperty("smpp.systemId", "");
			smpp.password = properties.getProperty("smpp.password", "");

			mt.numberThreadLoadMt = Integer.parseInt(properties.getProperty("mt.numberThreadLoadMt", "1"));

			log.configPath = properties.getProperty("log.configPath", log.configPath);
			log.folderPath = properties.getProperty("log.folderPath", log.folderPath);
			db.configPath = properties.getProperty("db.configPath", db.configPath);

			return true;
		}
		catch (Exception e)
		{
			System.out.println(e);
			return false;
		}

	}

}
