package my.smpp.process;

import java.io.*;

import com.logica.smpp.*;
import com.logica.smpp.pdu.*;

import my.smpp.Config;
import my.smpp.Gateway;
import my.smpp.Queue;
import my.smpp.Var;

public class SmscSender extends ThreadBase
{
	private Queue sendQueue = null;
	private PDU pdu = null;

	public SmscSender(Queue sendQueue)
	{
		this.sendQueue = sendQueue;
	}

	public void doRun()
	{
		while (Var.smpp.running)
		{
			if (Var.smpp.sessionBound)
			{
				try
				{
					pdu = (PDU) sendQueue.dequeue();
					if (pdu != null)
					{
						// Nếu là bản tin response thông báo cho SMSC khi nhận
						// MO
						if (pdu.isResponse())
						{
							Gateway.session.respond((Response) pdu);
							sleep(50);
						}
						// Nếu là MT cần gửi đi
						else if (pdu.isRequest())
						{
							sendRequest(pdu);
							
							int time2sleep = (1000 / Config.mt.tps);
							sleep(time2sleep);
						}
					}
					else
					{
						mlog.log.warn("pdu is null");
						sleep(50);
					}
				}
				catch (Exception ex)
				{
					mlog.log.error(ex);
					sendQueue.enqueue(pdu);
				}
			}
			else
			{
				mlog.log.info("Delay-sender trong :" + Config.mt.senderDelay);
				sleep(Config.mt.senderDelay);
			}
		}
	}

	private void sendRequest(PDU pdu) throws IOException
	{
		if (pdu == null)
			return;
		if (pdu.getCommandId() == Data.SUBMIT_SM)
		{
			try
			{
				SubmitSM request = (SubmitSM) pdu;
				mlog.log.debug("SubmitSM Request: " + request.debugString());

				// GW chỉ chạy chế độ bất đồng bộ TR
				if (Config.smpp.asyncMode)
				{
					Gateway.session.submit(request);
				}
			}
			catch (IOException ex)
			{
				mlog.log.error(ex);
				throw ex;
			}
			catch (Exception ex)
			{
				mlog.log.error(ex);
			}

		}
	}
}
