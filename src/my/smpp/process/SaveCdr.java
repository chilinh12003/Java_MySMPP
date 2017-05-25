package my.smpp.process;

import my.db.dao.DaoCdrQueue;
import my.db.obj.CdrQueue;
import my.smpp.*;
import uti.MyLogger;

/**
 * Lấy các cdr đang queue rồi lưu xuống table cdrQueue trong DB
 * 
 * @author Administrator
 *
 */
public class SaveCdr extends ThreadBase
{
	
	private Queue cdrQueueSave = null;

	CdrQueue cdrQueue = null;
	DaoCdrQueue dao = null;
	public SaveCdr(Queue cdrQueueSave)
	{
		this.cdrQueueSave = cdrQueueSave;
		dao = new DaoCdrQueue();
	}
	
	public void doRun()
	{
		while (Var.smpp.running)
		{
			try
			{
				cdrQueue = (CdrQueue) cdrQueueSave.dequeue(); 
				if (cdrQueue!=null)
				{
					saveCdrQueue(cdrQueue);
				}
				Cleaner.cleanObj(cdrQueue);
			}
			catch (Exception ex)
			{
				mlog.log.error(ex);
			}

			sleep(50);
			
		}
	}

	boolean saveCdrQueue(CdrQueue cdrQueue)
	{
		try
		{
			if (dao.add(cdrQueue))
			{
				mlog.log.info("SAVE cdrQueue -->:" + MyLogger.GetLog(cdrQueue));
				return true;
			}
			else
			{
				mlog.log.warn("FAIL SAVE cdrQueue -->:" + MyLogger.GetLog(cdrQueue));
				return false;
			}
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			mlog.log.warn("ERROR SAVE cdrQueue -->:" + MyLogger.GetLog(cdrQueue));
			return false;
		}
	}
}