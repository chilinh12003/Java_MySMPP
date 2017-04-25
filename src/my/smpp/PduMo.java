package my.smpp;

import java.util.Calendar;

import com.logica.smpp.pdu.PDU;

import uti.MyConfig;

/**
 * Khi nhận được MO từ bên Telco sẽ chuyển thành PduMo và lưu vào queue
 * @author Administrator
 *
 */
public class PduMo implements java.io.Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	PDU pdu;
	String requestId = "";
	Calendar calReceiveDate =null;
	public PduMo(){}
	public PduMo(PDU pdu)
	{
		this.pdu = pdu;
		calReceiveDate = Calendar.getInstance();
		requestId = MyConfig.Get_DateFormat_yyyymmddhhmmssSSS().format(calReceiveDate.getTime());
	}
	public PDU getPdu()
	{
		return pdu;
	}
	public void setPdu(PDU pdu)
	{
		this.pdu = pdu;
	}
	public String getRequestId()
	{
		return requestId;
	}
	public void setRequestId(String requestId)
	{
		this.requestId = requestId;
	}
	public Calendar getCalReceiveDate()
	{
		return calReceiveDate;
	}
	public void setCalReceiveDate(Calendar calReceiveDate)
	{
		this.calReceiveDate = calReceiveDate;
	}
	
}
