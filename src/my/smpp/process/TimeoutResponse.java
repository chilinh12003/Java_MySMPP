package my.smpp.process;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import my.db.obj.CdrQueue;
import my.db.obj.MtQueue;
import my.smpp.Config;
import my.smpp.Queue;
import my.smpp.QueueMap;
import my.smpp.Var;
import uti.MyDate;
import uti.MyLogger;

/**
 * Xử lý các MT đang lưu trong waitSendResponse
 * <p>
 * Vì một lý do nào đó mà MTQueue trong waitSendResponse không được lấy ra xử
 * lý, Nếu quá thời gian cho phép thì thread này sẽ lấy ra và lưu xuống db.
 * </p>
 * 
 * @author Chilinh
 *
 */
public class TimeoutResponse extends ThreadBase
{
	QueueMap waitSendResponse = null;
	Queue mtLogQueue = null;
	QueueMap cdrQueueWaiting = null;
	Queue cdrQueueSave = null;

	public TimeoutResponse(QueueMap waitSendResponse, Queue mtLogQueue, QueueMap cdrQueueWaiting, Queue cdrQueueSave)
	{
		this.waitSendResponse = waitSendResponse;
		this.mtLogQueue = mtLogQueue;
		this.cdrQueueWaiting = cdrQueueWaiting;
		this.cdrQueueSave = cdrQueueSave;
	}

	public void doRun()
	{
		while (Var.smpp.running)
		{
			checkResponseMt();
			
			checkWaitingCdr();
			
			sleep(Config.mt.responseCheckInterval);
		}

	}
	
	void checkWaitingCdr()
	{
		try
		{
			if (cdrQueueWaiting.size() > 0)
			{
				synchronized (cdrQueueWaiting)
				{
					for (Iterator<Entry<String, Object>> it = cdrQueueWaiting.getListObj().entrySet().iterator(); it
							.hasNext();)
					{
						HashMap.Entry<String, Object> item = it.next();
						CdrQueue cdrQueue = (CdrQueue) item.getValue();

						if (isTimeOut(cdrQueue.getCdrDate(), Config.cdr.waitingMtTimeout))
						{
							addCdrQueue(cdrQueue);
							it.remove();
							mlog.log.info("TIMEOUT CDR xoa khoi queue: key:" + item.getKey() + "cdrQueueId:"
									+ cdrQueue.getCdrId());
						}
					}
				}
			}

		}

		catch (Exception ex)
		{
			mlog.log.error(ex);
		}
	}
	void checkResponseMt()
	{
		try
		{
			if (waitSendResponse.size() > 0)
			{
				synchronized (waitSendResponse)
				{
					for (Iterator<Entry<String, Object>> it = waitSendResponse.getListObj().entrySet().iterator(); it
							.hasNext();)
					{
						HashMap.Entry<String, Object> item = it.next();
						MtQueue mtQueue = (MtQueue) item.getValue();

						if (isTimeOut(mtQueue.getSendDate(), Config.mt.responseTimeout))
						{
							addMtLogQueue(mtQueue);
							it.remove();
							mlog.log.info("TIMEOUT RESPONE xoa khoi queue: key:" + item.getKey() + "MtQueueId:"
									+ mtQueue.getMtId());
						}
					}
				}
			}

		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
		}

	}
	
	public void addCdrQueue(CdrQueue cdrQueue)
	{
		try
		{
			cdrQueue.setChargeTypeId(CdrQueue.ChargeType.Refund.getValue());
			cdrQueue.setDoneDate(MyDate.Date2Timestamp(Calendar.getInstance()));

			cdrQueueSave.enqueue(cdrQueue);
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			mlog.log.warn("SAVE FAIL MTQUEUE -->:" + MyLogger.GetLog(cdrQueue));
		}
	}
	
	public void addMtLogQueue(MtQueue mtQueue)
	{
		try
		{
			// Update MtQueue
			mtQueue.setNote("Timeout: khong nhan duoc response tu smsc");
			mtQueue.setDoneDate(MyDate.Date2Timestamp(Calendar.getInstance()));
			mtLogQueue.enqueue(mtQueue);

		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			mlog.log.warn("SAVE FAIL MTQUEUE -->:" + MyLogger.GetLog(mtQueue));
		}
	}
	public boolean isTimeOut(Timestamp time, int maxTimeout)
	{
		long currTime = System.currentTimeMillis();
		if ((currTime - time.getTime()) >= maxTimeout)
		{
			return true;
		}
		return false;
	}
}
