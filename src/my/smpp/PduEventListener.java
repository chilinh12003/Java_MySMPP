package my.smpp;

import com.logica.smpp.*;
import com.logica.smpp.pdu.*;

import uti.MyCheck;
import uti.MyLogger;
/**
 * An implementation of a PDU listener which handles PDUs received from SMSC, in
 * <b>Asynchronous</b> mode. It puts the received requests into a queue and
 * discards all received responses. Requests then can be fetched (should be)
 * from the queue by calling to the method <code>getRequestEvent</code>.
 * <p>Note: Đây là lớp dành cho bind Async (bất đồng bộ).  </p>
 */
public class PduEventListener extends SmppObject implements ServerPDUEventListener
{
	MyLogger mLog = new MyLogger(PduEventListener.class.getName());

	private Queue receiveQueue = null;
	private Queue responseQueue = null;
	private Queue sendQueue = null;

	private DeliverSM deliverSM = null;

	public PduEventListener(Queue receiveQueue, Queue responseQueue,  Queue sendQueue)
	{
		this.receiveQueue = receiveQueue;
		this.responseQueue = responseQueue;
		this.sendQueue = sendQueue;
	}

	/**
	 * Means to process PDUs received from the SMSC. This method is called by
	 * the <code>Receiver</code> whenever a PDU is received from the SMSC.
	 * 
	 * @param request
	 *            the request received from the SMSC.
	 */
	public void handleEvent(ServerPDUEvent event)
	{
		PDU pdu = event.getPDU();
		if (pdu.isValid())
		{
			//Nếu là 1 MO do SMSC gửi sang
			if (pdu.isRequest())
			{
				
				//Khi nhận được 1 MO thì phải trả về cho SMSC 1 response
				// Make default response
				Response response = ((Request) pdu).getResponse();
				this.sendQueue.enqueue(response);
				if (pdu.getCommandId() != Data.ENQUIRE_LINK)
				{
					this.processRequest(pdu);
				}
			}
			//Nếu là 1 response - SMSC phản hồi về kết quả nhận MT từ CP
			else if (pdu.isResponse())
			{
				if (pdu.getCommandId() != Data.ENQUIRE_LINK_RESP)
					this.responseQueue.enqueue(pdu);
			}
			else
			{
				mLog.log.warn(
						"pdu of unknown class (not request nor response received; Discarding: " + pdu.debugString());
			}
		}
		else
		{
			mLog.log.warn( "Received an invalid pdu:" +pdu.debugString());
		}
	}

	private void processRequest(PDU pdu)
	{
		try
		{
			switch (pdu.getCommandId())
			{
				case Data.DELIVER_SM :
					deliverSM = (DeliverSM) pdu;
					
					// Added on 22//2003 : VinaPhone gui ban tin DeliverReport voi
					// truong esm_class != 0x4. ==> He thong xem nhu ban tin thuong
					// sai format va gui thong bao -- report -- thong bao --> LOOP./
					// To pass over this, set:
					
					if (deliverSM.getEsmClass() == 0x04)
					{
						// this.deliveryQueue.enqueue(pdu);
						mLog.log.info("dsm.getEsmClass() == 0x04");
					}
					else
					{
						PduMo pduMo = new PduMo(pdu);
						
						this.receiveQueue.enqueue(pduMo);
						String userid = deliverSM.getSourceAddr().getAddress();

						deliverSM.setSourceAddr(MyCheck.ValidPhoneNumber(userid, "84"));
						
						mLog.log.info("RECEIVE MO"+deliverSM.debugString());
					}
					break;
				case Data.DATA_SM :
					mLog.log.warn("  Data_SM --> Not processed.");
					break;
				case Data.UNBIND :
					mLog.log.info(" Data.UNBIND --> Not processed.");
					PduMo pduMo = new PduMo(pdu);
					this.receiveQueue.enqueue(pduMo);
					break;
				default :
					mLog.log.warn("processRequest: Unspecified SM " + pdu.debugString());

			}
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}

}
