package my.smpp.process;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.Vector;

import com.logica.smpp.pdu.SubmitSM;

import my.db.MtQueue;
import my.smpp.BuildMt;
import my.smpp.Config;
import my.smpp.Queue;
import my.smpp.QueueMap;
import my.smpp.Var;
import uti.MyLogger;

public class ResendMt extends ThreadBase
{
	private Queue resendQueue = null;
	private Queue sendQueue = null;
	QueueMap waitSendResponse = null;
	private Queue mtLogQueue = null;
	MtQueue mtQueue = null;
	public ResendMt(Queue resendQueue, Queue sendQueue, QueueMap waitSendResponse, Queue mtLogQueue)
	{
		this.resendQueue = resendQueue;
		this.sendQueue = sendQueue;
		this.waitSendResponse = waitSendResponse;
		this.mtLogQueue = mtLogQueue;

	}

	public void doRun()
	{
		while (Var.smpp.running)
		{
			try
			{
				// Not over Max messages in queue
				if (resendQueue.size() > 0)
				{
					for (int i = 0; i < resendQueue.size(); i++)
					{
						mtQueue = (MtQueue) resendQueue.dequeue();
						if (mtQueue != null)
						{
							resendMT(mtQueue);
						}
						else
						{
							mlog.log.info("Can't resend sms get from resendQueue is null");

						}

						sleep(100);
					}

				}
				sleep(100);

			}
			catch (Exception ex)
			{
				mlog.log.error(ex);
			}
		}
	}

	public void resendMT(MtQueue mtQueue)
	{
		try
		{
			// nếu sau thời gian Delay (Config.smscResendDelay) thì mới được gửi
			// lại,
			// Không thì sẽ insert lại tiếp resendQueue
			if (isTimeResend(mtQueue.getSendDate()))
			{
				Timestamp time = new Timestamp(System.currentTimeMillis());
				mtQueue.setDoneDate(time);

				BuildMt buildMt = new BuildMt(mtQueue, mtLogQueue);

				Vector<SubmitSM> listSubmit = buildMt.getSubmit();

				int i = listSubmit.size();

				for (Iterator<SubmitSM> it2 = listSubmit.iterator(); it2.hasNext();)
				{
					i--;
					SubmitSM ssm = (SubmitSM) it2.next();
					this.sendQueue.enqueue(ssm);

					mlog.log.info("RESEND MT: " + MyLogger.GetLog(mtQueue));
					if (i == 0)
					{
						// Lưu MTQueue vào để chờ response từ telco trả về
						this.waitSendResponse.enqueue(ssm.getSequenceNumber() + "", mtQueue);
					}
				}
			}
			else
			{
				// Mỗi MT resend thì phải sau thời gian Config.smscResendDelay
				// giấy thì mới được gửi lại
				resendQueue.enqueue(mtQueue);
			}
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
		}
	}

	public boolean isTimeResend(Timestamp time)
	{
		long currTime = System.currentTimeMillis();
		if ((currTime - time.getTime()) > Config.mt.resendDelay)
		{
			return true;
		}
		return false;
	}

}
