package my.smpp.process;

import com.logica.smpp.*;
import com.logica.smpp.pdu.*;

import my.smpp.Config;
import my.smpp.PduMo;
import my.smpp.Queue;
import my.smpp.Var;

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

	public void doRun()
	{
		while (Var.smpp.running)
		{
			if (Var.smpp.sessionBound)
			{
				try
				{
					pdu = session.receive(Config.mo.receiveTimeout);

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
									PduMo pduMo = new PduMo(pdu);
									receiveQueue.enqueue(pduMo);
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

				sleep(Config.mo.receiveDelay);

				mlog.log.info("Delay-receiver");
			}
		}
	}
}
