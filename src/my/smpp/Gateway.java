package my.smpp;
import java.io.File;
import com.logica.smpp.*;
import com.logica.smpp.pdu.*;

import my.db.*;
import my.db.base.ConnectBase;
import my.smpp.process.SendEnquireLink;
import my.smpp.process.CheckAndReport;
import my.smpp.process.LoadMt;
import my.smpp.process.PduEventListener;
import my.smpp.process.ResendMt;
import my.smpp.process.ResponseMt;
import my.smpp.process.SaveCdr;
import my.smpp.process.SaveMo;
import my.smpp.process.SaveMtLog;
import my.smpp.process.SmscSender;
import my.smpp.process.ThreadBase;
import my.smpp.process.TimeoutResponse;
import uti.MyLogger;

public class Gateway extends Thread
{
	static
	{
		String currentPath = System.getProperty("user.dir");
		File mFile = new File(currentPath + "/config.properties");
		Config.loadConfig(mFile.getAbsolutePath());

		if (Config.app.currentPath.equalsIgnoreCase(""))
			Config.app.currentPath = currentPath;

		if (Config.app.saveQueuePath.equalsIgnoreCase(""))
			Config.app.saveQueuePath = Config.app.currentPath + "/queue";

		Config.checkAndCreateFolder(Config.app.saveQueuePath);

		MyLogger.log4jConfigPath = Config.log.configPath;

		try
		{
			ConnectBase.defaultPoolName = "MySQL_ShortCode";
			ConnectBase.loadConfig(Config.db.configPath);
			
			//HibernateSessionFactory.ConfigPath = Config.db.configPath;
			//HibernateSessionFactory.init();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	static MyLogger mlog = new MyLogger(Gateway.class.getName());

	public static Session session = null;
	private PduEventListener pduListener = null;

	/**
	 * Danh sách MTqueue đang chờ reponse trả về từ SMSC, Lưu MTQueue
	 */
	public static QueueMap waitSendResponse = null;
	/**
	 * Chứa danh sách các (PDU)reponse mà SMSC trả về khi gửi MT
	 */
	public static Queue responseQueue = null;
	/**
	 * Chứa danh sách MO (MOQueue) nhận được, đang chờ để insert xuống MOQueue
	 */
	public static Queue receiveQueue = null;
	/**
	 * Chứa danh sách MT-SubmitSM (pdu) đang cần gửi sang SMSC
	 */
	public static Queue sendQueue = null;
	/**
	 * Danh sách các MTQueue gửi sang SMSC bị lỗi, và đang chờ để gửi lại, Lưu
	 * trữ MTQueue
	 */
	public static Queue resendQueue = null;

	/**
	 * các MTQueue đang chờ ghi log và xử lý waitcdrQueue đang chờ
	 */
	public static Queue mtLogQueue = null;

	/**
	 * Các CdrQueue cần insert xuống table cdrQueue
	 */
	public static Queue cdrQueueSave = null;
	/**
	 * Các cdrQueue đang chờ mt đề biết xem Tính cước hay hoàn cước
	 */
	public static QueueMap cdrQueueWaiting = null;

	public Gateway()
	{
		waitSendResponse = new QueueMap();
		responseQueue = new Queue();
		receiveQueue = new Queue();
		sendQueue = new Queue();
		resendQueue = new Queue();
		mtLogQueue = new Queue();
		cdrQueueSave = new Queue();
		cdrQueueWaiting = new QueueMap();
	}

	static void unBind()
	{
		try
		{
			if (Var.smpp.sessionBound)
			{
				UnbindResp response = session.unbind();
				mlog.log.info("Unbind response " + response.debugString());
				Var.smpp.sessionBound = false;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	synchronized public void bindAsync()
	{
		BindRequest request = null;
		BindResponse response = null;
		Connection connection = null;

		while (!Var.smpp.sessionBound)
		{
			try
			{
				mlog.log.info("Connecting to SMSC " + Config.smpp.ipAddress + ":" + Config.smpp.port);

				connection = new TCPIPConnection(Config.smpp.ipAddress, Config.smpp.port);
				connection.setReceiveTimeout(Config.smpp.receiveTimeout);
				session = new Session(connection);

				request = new BindTransciever();

				// set values
				request.setSystemId(Config.smpp.systemId);
				request.setPassword(Config.smpp.password);
				request.setSystemType(Config.smpp.systemType);
				request.setInterfaceVersion((byte) 0x34); // SMPPv3.4
				request.setAddressRange(Config.smpp.addressRange);

				if (pduListener == null)
				{ // Important when rebind
					// automatically
					// Note: PDUEventListener instance must be placed before
					// receiving any response.
					pduListener = new PduEventListener(receiveQueue, responseQueue, sendQueue);
				}
				// send the request
				mlog.log.info("Bind request " + request.debugString());
				response = session.bind(request, pduListener);
				mlog.log.info("Bind response " + response.debugString());
				if (response.getCommandStatus() == Data.ESME_ROK)
				{ // no error
					Var.smpp.sessionBound = true;
					mlog.log.info("Succesfully Bound to SMSC in " + new java.sql.Timestamp(System.currentTimeMillis())
							+ "!!!");
				}
				else
				{
					mlog.log.warn("Gateway bindASync Command Status:"
							+ Common.getCommandDescription(response.getCommandStatus()));
					try
					{
						sleep(Config.smpp.rebindTimeout);
					}
					catch (InterruptedException ie)
					{
					}
				}
			}
			catch (Exception ex)
			{
				mlog.log.error("Bind operation FAILT. Try later in " + (Config.smpp.rebindTimeout / 1000) + " seconds",
						ex);

				try
				{
					sleep(Config.smpp.rebindTimeout);
				}
				catch (InterruptedException ie)
				{
				}
				// Alert not bind smsc
			}
		}
	}

	static void saveQueueToFile()
	{
		waitSendResponse.saveToFile(Config.app.saveQueuePath + "/waitSendResponse.dat");
		// Save PDU

		responseQueue.savePdu(Config.app.saveQueuePath + "/responseQueue/");
		receiveQueue.saveToFile(Config.app.saveQueuePath + "/receiveQueue.dat");
		// Save PDU
		sendQueue.savePdu(Config.app.saveQueuePath + "/sendQueue/");
		resendQueue.saveToFile(Config.app.saveQueuePath + "/resendQueue.dat");
		mtLogQueue.saveToFile(Config.app.saveQueuePath + "/mtLogQueue.dat");
		cdrQueueSave.saveToFile(Config.app.saveQueuePath + "/cdrQueueSave.dat");
		cdrQueueWaiting.saveToFile(Config.app.saveQueuePath + "/cdrQueueWaiting.dat");
	}

	static void loadQueueFromFile()
	{
		waitSendResponse.loadFromFile(Config.app.saveQueuePath + "/waitSendResponse.dat", "getMtResponseId");

		// Load PDU
		Config.checkAndCreateFolder(Config.app.saveQueuePath + "/responseQueue/");
		responseQueue.loadPdu(Config.app.saveQueuePath + "/responseQueue/");
		receiveQueue.loadFromFile(Config.app.saveQueuePath + "/receiveQueue.dat");

		// Load PDU
		Config.checkAndCreateFolder(Config.app.saveQueuePath + "/sendQueue/");
		sendQueue.loadPdu(Config.app.saveQueuePath + "/sendQueue/");
		resendQueue.loadFromFile(Config.app.saveQueuePath + "/resendQueue.dat");
		mtLogQueue.loadFromFile(Config.app.saveQueuePath + "/mtLogQueue.dat");
		cdrQueueSave.loadFromFile(Config.app.saveQueuePath + "/cdrQueueSave.dat");
		cdrQueueWaiting.loadFromFile(Config.app.saveQueuePath + "/cdrQueueWaiting.dat", "getRequestId");
	}

	static void exit()
	{
		try
		{
			mlog.log.info("Stoping...");

			mlog.log.info("unbinding smsc...");
			unBind();
			mlog.log.info("unbind success smsc...");

			Var.smpp.running = false;
			Var.smpp.sessionBound = false;

			responseQueue.wakeup();
			receiveQueue.wakeup();
			sendQueue.wakeup();
			resendQueue.wakeup();
			mtLogQueue.wakeup();
			cdrQueueSave.wakeup();

			int count = 1;
			while (count < 10 && ThreadBase.countLiveThread() > 0)
			{
				mlog.log.info("Waiting for Stoping Thread...count live thread:" + ThreadBase.countLiveThread());
				// ThreadBase.stateLiveThread();
				sleep(100);
				count++;
			}

			saveQueueToFile();

			mlog.log.info("------STOPED------");

		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	

	public void run()
	{
		try
		{
			// Mặc định là bind theo chế độ TR - bất đồng bộ nhận và gửi

			// Gửi SubmitSM sang SMSC
			SmscSender sender = new SmscSender(sendQueue);
			sender.setPriority(MAX_PRIORITY);
			sender.start();

			// Load MT từ db và build submitSm và add vào sendQueue chờ gửi sang
			// SMSC

			for (int i = 0; i < Config.mt.numberThreadLoadMt; i++)
			{
				LoadMt loadMT = new LoadMt(sendQueue, waitSendResponse, mtLogQueue, Config.mt.numberThreadLoadMt, i);
				loadMT.setPriority(MAX_PRIORITY);
				loadMT.start();
			}

			for (int i = 0; i < Config.mt.numberThreadResponse; i++)
			{
				// Nhận các Response từ SMSC khi gửi mt xong
				ResponseMt response = new ResponseMt(responseQueue, waitSendResponse, resendQueue, mtLogQueue);
				response.setPriority(MAX_PRIORITY);
				response.start();
			}

			for (int i = 0; i < Config.mt.numberThreadResend; i++)
			{
				// Gửi lại các MT đang bị lỗi khi gửi sang Telco
				ResendMt resend = new ResendMt(resendQueue, sendQueue, waitSendResponse, mtLogQueue);
				resend.setPriority(MAX_PRIORITY);
				resend.start();
			}

			for (int i = 0; i < Config.mt.numberThreadSaveMtlog; i++)
			{
				// Insert mtlog và Insert cdrQueue
				SaveMtLog saveMtlog = new SaveMtLog(mtLogQueue, cdrQueueWaiting, cdrQueueSave);
				saveMtlog.setPriority(MAX_PRIORITY);
				saveMtlog.start();
			}
			for (int i = 0; i < Config.mo.numberThreadSaveMo; i++)
			{
				// Insert MoQueue và add CdrQueue đề chờ MT
				SaveMo saveMo = new SaveMo(receiveQueue, cdrQueueWaiting);
				saveMo.setPriority(MAX_PRIORITY);
				saveMo.start();
			}
			for (int i = 0; i < Config.cdr.numberThreadSaveCdr; i++)
			{
				// Insert Cdr xuống CdrQueue
				SaveCdr saveCdr = new SaveCdr(cdrQueueSave);
				saveCdr.setPriority(MAX_PRIORITY);
				saveCdr.start();
			}

			// Kiểm tra và xóa bỏ các Queue còn sót lại
			TimeoutResponse timeoutCheck = new TimeoutResponse(waitSendResponse, mtLogQueue, cdrQueueWaiting,
					cdrQueueSave);
			timeoutCheck.setPriority(NORM_PRIORITY);
			timeoutCheck.start();

			// gửi bản tin EnquireLink để kiểm tra kết nối đến smsc
			SendEnquireLink checkELink = new SendEnquireLink(gateway);
			checkELink.setPriority(MAX_PRIORITY);
			checkELink.start();

			/*
			 * Cleaner cleaner = new Cleaner();
			 * cleaner.setPriority(MIN_PRIORITY); cleaner.start();
			 */
			CheckAndReport check = new CheckAndReport();
			check.setPriority(NORM_PRIORITY);
			check.start();
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
		}
	}

	
	static Gateway gateway;
	public static void main(String args[])
	{
		mlog.log.info("------START-------");
		gateway = new Gateway();

		loadQueueFromFile();

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				exit();
			}
		});

		gateway.bindAsync();
		gateway.start();
	}

}
