package my.smpp.process;

import java.util.Calendar;

import com.logica.smpp.*;
import com.logica.smpp.pdu.*;

import my.db.obj.MoQueue;
import my.smpp.Config;
import my.smpp.Queue;
import my.smpp.Var;
import uti.MyConfig;
import uti.MyConvert;
import uti.MyDate;

/**
 * For receiving PDUs from SMSC, in Sync mode --> Only request PDUs are
 * received. Thread chỉ chạy cho trường hợp Bind theo chế độ đồng bộ. Nếu là bất
 * đồng bộ thì ko cần.
 *
 */
public class SmscReceiver extends ThreadBase
{
	private PDU pdu = null;
	private Session session = null;
	private Queue receiveQueue = null;

	public SmscReceiver(Session session, Queue receiveQueue)
	{
		this.session = session;
		this.receiveQueue = receiveQueue;
	}
	MoQueue createMo(DeliverSM deliverSm) throws Exception
	{
		try
		{
			MoQueue moQueue = new MoQueue();
			Calendar calReceiveDate = Calendar.getInstance();
			String requestId = MyConfig.Get_DateFormat_yyyymmddhhmmssSSS().format(calReceiveDate.getTime());
			moQueue.setPhoneNumber(removePlusSign(deliverSm.getSourceAddr().getAddress()));
			moQueue.setPid(MyConvert.GetPIDByMSISDN(moQueue.getPhoneNumber(), 100));
			moQueue.setShortCode(removePlusSign(deliverSm.getDestAddr().getAddress()));
			
			moQueue.setMo(deliverSm.getShortMessage());
			moQueue.setReceiveDate(MyDate.Date2Timestamp(calReceiveDate));
			moQueue.setChannelId(MyConfig.ChannelType.SMS.GetValue());
			moQueue.setRequestId(requestId);
			moQueue.setTelcoId(Config.smpp.telco.GetValue());
			moQueue.setMoInsertDate(MyDate.Date2Timestamp(Calendar.getInstance()));
			 return moQueue;
		}
		catch(Exception ex)
		{
			throw ex;
		}
	}
	
	private String removePlusSign(String PhoneNumber)
	{
		String temp = PhoneNumber;
		if (temp.startsWith("+"))
		{
			temp = temp.substring(1);
		}
		return temp;
	}
	
	public void doRun()
	{
		while (Var.smpp.running)
		{
			if (Var.smpp.sessionBound)
			{
				try
				{
					pdu = session.receive(Config.smpp.receiveTimeout);

					// pdu = session.receive();
					if (pdu != null && pdu.isValid())
					{
						if (pdu.isRequest())
						{
							// Make default response
							Response response = ((Request) pdu).getResponse();

							// Reply with default response
							session.respond(response);

							// Add to requestQueue for further processing
							if (pdu.getCommandId() != Data.ENQUIRE_LINK)
							{
								if (pdu.getCommandId() == Data.DELIVER_SM)
								{
									 DeliverSM deliverSm = (DeliverSM) pdu;
										
										// Added on 22//2003 : VinaPhone gui ban tin DeliverReport voi
										// truong esm_class != 0x4. ==> He thong xem nhu ban tin thuong
										// sai format va gui thong bao -- report -- thong bao --> LOOP./
										// To pass over this, set:
										
										if (deliverSm.getEsmClass() == 0x04)
										{
											// this.deliveryQueue.enqueue(pdu);
											mlog.log.info("dsm.getEsmClass() == 0x04");
										}
										else
										{
											MoQueue moQueue = createMo(deliverSm);
											receiveQueue.enqueue(moQueue);
											mlog.log.info("RECEIVE MO"+deliverSm.debugString());
										}
								}
								else
								{
									mlog.log.info("Not a Deliver_SM - PDU: " + pdu.debugString());
								}
							}
						}
						else if (pdu.isResponse())
						{
							mlog.log.info(
									"received a response(?), while expect requests only. PDU: " + pdu.debugString());
						}
						else
						{
							mlog.log.info("pdu of unknown class (not request nor response) "
									+ "received; Discarding - PDU: " + pdu.debugString());
						}
					}
					else
					{
						mlog.log.info("Received an invalid pdu!");
					}
				}
				catch (Exception ex)
				{
					mlog.log.error(ex);
				}

			}
			else
			{
				mlog.log.info("Delay-receiver");

				sleep(Config.smpp.receiveDelay);

				mlog.log.info("Delay-receiver");
			}
		}
	}
}
