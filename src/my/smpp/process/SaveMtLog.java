package my.smpp.process;

import java.util.Calendar;

import my.db.dao.DaoMtLog;
import my.db.dao.DaoMtQueue;
import my.db.obj.CdrQueue;
import my.db.obj.MtLog;
import my.db.obj.MtQueue;
import my.smpp.*;
import uti.MyDate;
import uti.MyLogger;

/**
 * Lấy các mtlog đang queue rồi lưu xuống table mtlog trong DB <br>
 * Đồng thời lưu luông cdrQueue
 * 
 * @author Administrator
 *
 */
public class SaveMtLog extends ThreadBase
{
	private Queue mtLogQueue = null;
	private QueueMap cdrQueueWaiting = null;
	private Queue cdrQueueSave = null;
	private MtQueue mtQueue = null;
	private MtLog mtlog =null;
	private DaoMtLog dao;
	public SaveMtLog(Queue mtLogQueue, QueueMap cdrQueueWaiting, Queue cdrQueueSave)
	{
		this.mtLogQueue = mtLogQueue;
		this.cdrQueueWaiting = cdrQueueWaiting;
		this.cdrQueueSave = cdrQueueSave;
		this.dao = new DaoMtLog();
	}

	public void doRun()
	{
		while (Var.smpp.running)
		{
			try
			{
				mtQueue = (MtQueue) mtLogQueue.dequeue();
				if (mtQueue != null)
				{
					saveMtLog(mtQueue);
					
					if(Config.cdr.allowCreateCdr)
						addCdrQueue(mtQueue);
					
					Cleaner.cleanObj(mtQueue);
				}				
			}
			catch (Exception ex)
			{
				mlog.log.error(ex);
			}

			sleep(50);

		}
	}

	boolean addCdrQueue(MtQueue mtQueue)
	{
		CdrQueue cdrQueue = null;
		try
		{
		
			// Cần thận khi gọi cdrQueueWaiting.dequeue(). Vì nếu
			// cdrQueueWaiting không có thì thread này sẽ bị khóa. --> ko load
			// MTnữa
			cdrQueue = (CdrQueue) cdrQueueWaiting.dequeue(mtQueue.getRequestId());

			if (cdrQueue != null)
			{
				cdrQueue.setChargeTypeId(mtQueue.getChargeTypeId());
				cdrQueue.setMtId(mtQueue.getMtId());
				cdrQueue.setDoneDate(mtQueue.getDoneDate());
				cdrQueue.setKeyword(mtQueue.getKeyword());
				cdrQueue.setStatusId(CdrQueue.Status.WaitingFtp.getValue());

				if (mtQueue.getStatusId().shortValue() != DaoMtQueue.Status.SendSuccess.getValue().shortValue())
				{
					// nếu gửi không thành công thì hoàn tiền cho khách hàng
					cdrQueue.setChargeTypeId(CdrQueue.ChargeType.Refund.getValue());
				}
				cdrQueueSave.enqueue(cdrQueue);
			}
			return false;
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			mlog.log.warn("ERROR Add CdrQueue  -->:" + MyLogger.GetLog(cdrQueue));
			return false;
		}
	}
	/**
	 * Lưu xuống MTLog
	 * 
	 * @param mtQueue
	 * @return
	 */
	boolean saveMtLog(MtQueue mtQueue)
	{
		
		try
		{
			mtlog = new MtLog(mtQueue);
			mtlog.setLogDate(MyDate.Date2Timestamp(Calendar.getInstance()));
			if (dao.add(mtlog))
			{
				mlog.log.info("SAVE MtLog -->:" + MyLogger.GetLog(mtlog));
				return true;
			}
			else
			{
				mlog.log.warn("FAIL SAVE MtLog -->:" + MyLogger.GetLog(mtlog));
				return false;
			}
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			mlog.log.warn("ERROR SAVE MtLog -->:" + MyLogger.GetLog(mtlog));
			return false;
		}
	}

}