package my.smpp.process;

import java.sql.Timestamp;
import java.util.Calendar;
import my.db.MtQueue;
import my.smpp.*;
import uti.MyDate;
import uti.MyLogger;
import com.logica.smpp.*;
import com.logica.smpp.pdu.*;

/**
 * Xử lý các response trả về khi gửi MT sang Telco. Nếu gửi thành công thì
 * insert vào SendLogQueue đề chờ insert xuống DB. Nếu gừi fail thì sẽ insert
 * vào ResendQueue để chờ gửi lại.
 * 
 * @author Administrator
 *
 */
public class ResponseMt extends ThreadBase
{
	private Queue responseQueue = null;
	private Queue resendQueue = null;
	private QueueMap waitSendResponse = null;
	private Queue mtLogQueue = null;

	private PDU pdu = null;
	private SubmitSMResp submiResponse = null;
	private int commandStatus = 0;

	public ResponseMt(Queue responseQueue, QueueMap waitSendResponse, Queue resendQueue, Queue mtLogQueue)
	{
		this.responseQueue = responseQueue;
		this.waitSendResponse = waitSendResponse;
		this.resendQueue = resendQueue;
		this.mtLogQueue = mtLogQueue;
	}

	public void doRun()
	{
		while (Var.smpp.running)
		{
			try
			{
				pdu = (PDU) responseQueue.dequeue();
				if (pdu.isResponse())
				{
					processResponse(pdu);
				}
			}
			catch (Exception ex)
			{
				mlog.log.error(ex);
			}

			try
			{
				Thread.sleep(50);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	

	private void processResponse(PDU pdu) throws Exception
	{
		int resend = 1;
		String errorName = "";
		Timestamp time = new Timestamp(System.currentTimeMillis());

		// nếu là response do telco trả về khi gửi MT sang
		if (pdu.getCommandId() == Data.SUBMIT_SM_RESP)
		{
			submiResponse = (SubmitSMResp) pdu;
			commandStatus = submiResponse.getCommandStatus();

			//Gửi MT sang thành công
			if (commandStatus == Data.ESME_ROK)
			{
				// lấy trong log chờ response và đưa và log SendLogQueue để lưu
				// xuống db
				MtQueue mtQueue = (MtQueue) waitSendResponse.dequeue(submiResponse.getSequenceNumber() + "");

				if (mtQueue == null)
				{
					return;
				}
				mtQueue.setDoneDate(MyDate.Date2Timestamp(Calendar.getInstance()));
				mtQueue.setStatusId(MtQueue.Status.SendSuccess.getValue());

				// Add queue để chờ lưu xuống db
				mtLogQueue.enqueue(mtQueue);
			}
			else
			{
				resend = Common.checkResendMT(commandStatus, submiResponse.getMessageId());
				errorName = Common.getErrorName(commandStatus, submiResponse.getMessageId());
				MtQueue mtQueue = (MtQueue) waitSendResponse.dequeue(submiResponse.getSequenceNumber() + "");

				if (mtQueue == null)
				{
					return;
				}
				// Mỗi lần resend sẽ công thêm lỗi vào
				mtQueue.setNote(mtQueue.getNote() + "|" + errorName);

				if ((resend == 1) && (mtQueue.getRetryCount() < Config.mt.maxRetrySendMt))
				{
					if ((time.compareTo(mtQueue.getSendDate())) > 0)
					{
						mtQueue.setRetryCount((short) (mtQueue.getRetryCount() + 1));
					}
					mlog.log.info("ADD TO RESEND:" + MyLogger.GetLog(mtQueue));
					resendQueue.enqueue(mtQueue);
				}
				else
				{
					mlog.log.info("SEND MT FAIL:" + MyLogger.GetLog(mtQueue));
					
					mtQueue.setDoneDate(MyDate.Date2Timestamp(Calendar.getInstance()));					
					mtQueue.setStatusId(MtQueue.Status.SendFail.getValue());
					
					// Add queue để chờ lưu xuống db
					mtLogQueue.enqueue(mtQueue);
				}
			}
		}
		else
		{
			mlog.log.warn("processResponse (not processed).");
		}
		// Process response
		if (pdu.isGNack())
		{
			mlog.log.warn("GENERIC_NAK (not processed).");
		}
	}
}