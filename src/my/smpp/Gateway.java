package my.smpp;

import java.io.File;

import com.logica.smpp.*;
import com.logica.smpp.pdu.*;

import my.db.*;
import my.smpp.process.SendEnquireLink;
import my.smpp.process.LoadMt;
import my.smpp.process.ResendMt;
import my.smpp.process.ResponseMt;
import my.smpp.process.SaveCdr;
import my.smpp.process.SaveMo;
import my.smpp.process.SaveMtLog;
import my.smpp.process.SmscSender;
import my.smpp.process.TimeoutResponse;
import uti.MyLogger;

public class Gateway extends Thread
{
	static
	{
		String currentPath = System.getProperty("user.dir");
		File mFile = new File(currentPath + "/config.properties");

		Config.loadConfig(mFile.getAbsolutePath());

		MyLogger.log4jConfigPath = Config.log.configPath;

		HibernateSessionFactory.ConfigPath = Config.db.configPath;
		HibernateSessionFactory.init();
	}

	MyLogger mLog = new MyLogger(Gateway.class.getName());

	public static Session session = null;
	private PduEventListener pduListener = null;

	/**
	 * Danh sách MTqueue đang chờ reponse trả về từ SMSC, Lưu MTQueue
	 */
	static QueueMap waitSendResponse = null;
	/**
	 * Chứa danh sách các reponse mà SMSC trả về khi gửi MT
	 */
	static Queue responseQueue = null;
	/**
	 * Chứa danh sách MO nhận được, đang chờ để insert xuống MOQueue
	 */
	static Queue receiveQueue = null;
	/**
	 * Chứa danh sách MT (pdu) đang cần gửi sang SMSC
	 */
	static Queue sendQueue = null;
	/**
	 * Danh sách các MT gửi sang SMSC bị lỗi, và đang chờ để gửi lại, Lưu trữ
	 * MTQueue
	 */
	static Queue resendQueue = null;

	/**
	 * các MTQueue đang chờ ghi log và xử lý waitcdrQueue đang chờ
	 */
	static Queue mtLogQueue = null;

	/**
	 * Các Cdr cần insert xuống table cdrQueue
	 */
	static Queue cdrQueueSave = null;
	/**
	 * Các cdr đang chờ mt đề biết xem Tính cước hay hoàn cước
	 */
	static QueueMap cdrQueueWaiting = null;

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
	synchronized public void bindAsync()
	{
		BindRequest request = null;
		BindResponse response = null;
		Connection connection = null;

		while (!Var.smpp.sessionBound)
		{
			try
			{
				mLog.log.info("Connecting to SMSC " + Config.smpp.ipAddress + ":" + Config.smpp.port);

				connection = new TCPIPConnection(Config.smpp.ipAddress, Config.smpp.port);
				connection.setReceiveTimeout(Config.mo.receiveTimeout);
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
				mLog.log.info("Bind request " + request.debugString());
				response = session.bind(request, pduListener);
				mLog.log.info("Bind response " + response.debugString());
				if (response.getCommandStatus() == Data.ESME_ROK)
				{ // no error
					Var.smpp.sessionBound = true;
					mLog.log.info("Succesfully Bound to SMSC in " + new java.sql.Timestamp(System.currentTimeMillis())
							+ "!!!");
				}
				else
				{
					mLog.log.warn("Gateway bindASync Command Status:"
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
				mLog.log.error("Bind operation FAILT. Try later in " + (Config.smpp.rebindTimeout / 1000) + " seconds",
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

	static Gateway gateway = new Gateway();
	public static void main(String args[])
	{
		gateway = new Gateway();

		gateway.bindAsync();
		gateway.start();
	}

	public void run()
	{
		try
		{
			// Mặc định là bind theo chế độ TR - bất đồng bộ nhận và gửi

			//Gửi SubmitSM sang SMSC
			SmscSender sender = new SmscSender(sendQueue);
			sender.setPriority(MAX_PRIORITY);
			sender.start();

			//Load MT từ db và build submitSm và add vào sendQueue chờ gửi sang SMSC
			LoadMt loadMT = new LoadMt(sendQueue, waitSendResponse, mtLogQueue, 1, 0);
			loadMT.setPriority(MAX_PRIORITY);
			loadMT.start();

			//Nhận các Response từ SMSC khi gửi mt xong
			ResponseMt response = new ResponseMt(responseQueue, waitSendResponse, resendQueue, mtLogQueue);
			response.setPriority(MAX_PRIORITY);
			response.start();

			//Gửi lại các MT đang bị lỗi khi gửi sang Telco
			ResendMt resend = new ResendMt(resendQueue, sendQueue, waitSendResponse, mtLogQueue);
			resend.setPriority(MAX_PRIORITY);
			resend.start();
			
			//Insert mtlog và Insert cdrQueue
			SaveMtLog saveMtlog = new SaveMtLog(mtLogQueue, cdrQueueWaiting,cdrQueueSave);
			saveMtlog.setPriority(MAX_PRIORITY);
			saveMtlog.start();

			//Insert MoQueue và add CdrQueue đề chờ MT
			SaveMo saveMo = new SaveMo(receiveQueue, cdrQueueWaiting);
			saveMo.setPriority(MAX_PRIORITY);
			saveMo.start();

			//Insert Cdr xuống CdrQueue
			SaveCdr saveCdr = new SaveCdr(cdrQueueSave);
			saveCdr.setPriority(MAX_PRIORITY);
			saveCdr.start();
		
			
			//Kiểm tra và xóa bỏ các Queue còn sót lại
			TimeoutResponse timeoutCheck = new TimeoutResponse(waitSendResponse, mtLogQueue, cdrQueueWaiting, cdrQueueSave);
			timeoutCheck.setPriority(NORM_PRIORITY);
			timeoutCheck.start();
			
			
			//gửi bản tin EnquireLink để kiểm tra kết nối đến smsc
			SendEnquireLink checkELink = new SendEnquireLink(gateway);
			checkELink.setPriority(MAX_PRIORITY);
			checkELink.start();
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}
}