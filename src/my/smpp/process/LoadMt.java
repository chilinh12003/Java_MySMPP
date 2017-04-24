package my.smpp.process;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import my.db.MtQueue;
import my.smpp.*;
import uti.MyDate;
import uti.MyLogger;

import com.logica.smpp.pdu.*;

/**
 * Lấy MT từ MTQueue lên, và bỏ lên Queue, chờ gửi sang Telco
 * 
 * @author Administrator
 *
 */
public class LoadMt extends ThreadBase
{
	/**
	 * Danh sách các SubmitSM cần gửi sang SMSC
	 */
	private Queue sendQueue = null;
	/**
	 * Danh sách chứa các MT đang chờ request từ SMSC trả về
	 */
	private QueueMap waitSendResponse = null;
	
	private Queue mtLogQueue = null;
	int threadNumber = 1;
	int threadIndex = 0;
	int getRowCount = 10;

	MtQueue mtQueueDb = null;
	public LoadMt(Queue sendQueue, QueueMap waitSendResponse,Queue mtLogQueue, int threadNumber, int threadIndex)
	{
		this.sendQueue = sendQueue;
		this.waitSendResponse = waitSendResponse;
		this.mtLogQueue = mtLogQueue;
		this.threadNumber = threadNumber;
		this.threadIndex = threadIndex;

		mtQueueDb = new MtQueue();
	}

	public void doRun()
	{
		while (Var.smpp.running)
		{
			if (Var.smpp.sessionBound)
			{
				try
				{
					List<MtQueue> mList = mtQueueDb.GetByThread(threadNumber, threadIndex, getRowCount);
					for (MtQueue item : mList)
					{
						MtQueue mtQueue = new MtQueue(item);
						mtQueue.setSendDate(MyDate.Date2Timestamp(Calendar.getInstance()));

						BuildMt buildMt = new BuildMt(mtQueue, mtLogQueue);

						Vector<SubmitSM> listSubmit = buildMt.getSubmit();
						//
						int i = listSubmit.size();

						for (Iterator<SubmitSM> it2 = listSubmit.iterator(); it2.hasNext();)
						{
							i--;
							SubmitSM ssm = (SubmitSM) it2.next();
							this.sendQueue.enqueue(ssm);
							if (i == 0)
							{
								// Lưu MTQueue vào để chờ response từ telco trả
								// về
								this.waitSendResponse.enqueue(ssm.getSequenceNumber() + "", mtQueue);

								mlog.log.info("SEND MT: " + MyLogger.GetLog(mtQueue));
							}
						}
					}

					if (mList.size() > 0)
						mtQueueDb.Delete(mList);

					sleep(100);
				}
				catch (Exception ex)
				{
					mlog.log.error(ex);
				}
			}
			else
			{
				sleep(2000);
			}
		}
	}

}