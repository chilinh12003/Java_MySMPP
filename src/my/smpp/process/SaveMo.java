package my.smpp.process;
import my.db.dao.DaoMoQueue;
import my.db.obj.CdrQueue;
import my.db.obj.MoQueue;
import my.smpp.*;
import uti.MyLogger;

/**
 * Lấy các MO đang queue rồi lưu xuống table MoQueue trong DB
 * 
 * @author Administrator
 *
 */
public class SaveMo extends ThreadBase
{
	/**
	 * Chứa danh sách các MT nhận được từ SMSC
	 */
	private Queue receiveQueue = null;
	private QueueMap cdrQueueWaiting = null;
	MoQueue moQueue = null;

	DaoMoQueue dao =null;
	
	public SaveMo(Queue receiveQueue, QueueMap cdrQueueWaiting)
	{
		// contains only request PDUs.
		this.receiveQueue = receiveQueue;
		this.cdrQueueWaiting = cdrQueueWaiting;
		dao = new DaoMoQueue();
	}

	public void doRun()
	{
		while (Var.smpp.running)
		{
			try
			{
				moQueue = (MoQueue) receiveQueue.dequeue();
				if (dao.add(moQueue))
				{
					mlog.log.info("RECEIVE MO:" + MyLogger.GetLog(moQueue));
					if (Config.cdr.allowCreateCdr)
					{
						CdrQueue cdrQueue = new CdrQueue(moQueue);
						// Add vào queue để chờ save xuống db
						cdrQueueWaiting.enqueue(moQueue.getRequestId(), cdrQueue);
					}
				}
				else
				{
					mlog.log.info("NOT SAVE DB MoQueue:" + MyLogger.GetLog(moQueue));
				}
			}
			catch (Exception ex)
			{
				mlog.log.error(ex);
			}

			sleep(50);
		}
	}

}