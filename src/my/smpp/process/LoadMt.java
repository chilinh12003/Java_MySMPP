package my.smpp.process;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import my.db.dao.DaoMtQueue;
import my.db.obj.MtQueue;
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

	DaoMtQueue daoMtQueue = null;
	List<MtQueue> listMt = null;
	
	LinkedList<SubmitSM> listSubmit = null;
	public LoadMt(Queue sendQueue, QueueMap waitSendResponse, Queue mtLogQueue, int threadNumber, int threadIndex)
	{
		this.sendQueue = sendQueue;
		this.waitSendResponse = waitSendResponse;
		this.mtLogQueue = mtLogQueue;
		this.threadNumber = threadNumber;
		this.threadIndex = threadIndex;

		daoMtQueue = new DaoMtQueue();
		//listMt = new ArrayList<MtQueue>();
		//listSubmit = new LinkedList<SubmitSM>();
	}

	public void doRun()
	{
		while (Var.smpp.running)
		{
			if (Var.smpp.sessionBound)
			{
				try
				{
					listMt = daoMtQueue.GetByThread(threadNumber, threadIndex, getRowCount);
					for (MtQueue mtQueue : listMt)
					{
						mtQueue.setSendDate(MyDate.Date2Timestamp(Calendar.getInstance()));
						BuildMt buildMt=new BuildMt(mtQueue, this.mtLogQueue);
												
						this.listSubmit = buildMt.getSubmit();
						
						int i = listSubmit.size();
						while(listSubmit.size() > 0)
						{
							i--;
							SubmitSM ssm = (SubmitSM) listSubmit.removeFirst();
							this.sendQueue.enqueue(ssm);
							if (i == 0)
							{
								// Lưu MTQueue vào để chờ response từ telco trả
								// về
								mtQueue.setMtResponseId(ssm.getSequenceNumber());
								this.waitSendResponse.enqueue(ssm.getSequenceNumber() + "", mtQueue);

								mlog.log.info("SEND MT: " + MyLogger.GetLog(mtQueue));
							}
						}						
					}

					if (listMt.size() > 0)
					{
						daoMtQueue.delete(listMt);
					}
					Cleaner.cleanObj(listMt);

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