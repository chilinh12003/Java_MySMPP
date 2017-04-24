package my.smpp.process;

import java.io.IOException;
import java.util.Calendar;

import com.logica.smpp.*;
import com.logica.smpp.pdu.*;

import my.db.CdrQueue;
import my.db.MoQueue;
import my.smpp.*;
import uti.MyConfig;
import uti.MyDate;
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
	private PduMo pduMo = null;
	private DeliverSM dsm = null;

	public SaveMo(Queue receiveQueue, QueueMap cdrQueueWaiting)
	{
		// contains only request PDUs.
		this.receiveQueue = receiveQueue;
		this.cdrQueueWaiting = cdrQueueWaiting;
	}

	public void doRun()
	{
		while (Var.smpp.running)
		{
			try
			{
				pduMo = (PduMo) receiveQueue.dequeue(); // blocks until having
				if (pduMo.getPdu().isRequest())
				{
					processRequest(pduMo);
				}
			}
			catch (Exception ex)
			{
				mlog.log.error(ex);
			}

			sleep(50);
			
		}
	}


	private void processRequest(PduMo pduMo) throws Exception, IOException
	{
		MoQueue moQueue = new MoQueue();
		try
		{
			// nếu là bản tin MO gửi từ telco sang
			if (pduMo.getPdu().getCommandId() == Data.DELIVER_SM)
			{

				dsm = (DeliverSM) pduMo.getPdu();
				moQueue.setPhoneNumber(removePlusSign(dsm.getSourceAddr().getAddress()));
				moQueue.setShortCode(removePlusSign(dsm.getDestAddr().getAddress()));
				
				moQueue.setMo(dsm.getShortMessage());
				moQueue.setReceiveDate(MyDate.Date2Timestamp(pduMo.getCalReceiveDate()));
				moQueue.setChannelId(MyConfig.ChannelType.SMS.GetValue());
				moQueue.setRequestId(pduMo.getRequestId());
				moQueue.setTelcoId(Config.smpp.telco.GetValue());
				moQueue.setMoInsertDate(MyDate.Date2Timestamp(Calendar.getInstance()));
				
				if(moQueue.Save())
				{
					mlog.log.info("RECEIVE MO:" +MyLogger.GetLog(moQueue));
					CdrQueue cdrQueue = new CdrQueue(moQueue);
					
					//Add vào queue để chờ save xuống db
					cdrQueueWaiting.enqueue(moQueue.getRequestId(),cdrQueue);
				}
				else
				{
					mlog.log.info("NOT SAVE DB MoQueue:" + MyLogger.GetLog(moQueue));		
				}
			}
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			mlog.log.error("ERROR SAVE DB MoQueue:" + MyLogger.GetLog(moQueue));
		}
	}

	// Dest_addr received from SMSC can be: +849xxx or 849xxx
	// if +849xxx then remove the plus (+) sign
	private String removePlusSign(String PhoneNumber)
	{
		String temp = PhoneNumber;
		if (temp.startsWith("+"))
		{
			temp = temp.substring(1);
		}
		return temp;
	}

}