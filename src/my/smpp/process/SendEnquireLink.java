package my.smpp.process;
import com.logica.smpp.pdu.*;
import my.smpp.Config;
import my.smpp.Gateway;
import my.smpp.Var;

public class SendEnquireLink extends ThreadBase
{

	Gateway gateway = null;
	public SendEnquireLink(Gateway gateway)
	{
		this.gateway = gateway;
	}
	public void doRun()
	{

		while (Var.smpp.running)
		{
			sleep(Config.smpp.checkEnquireLinkInterval);

			if (Var.smpp.sessionBound)
				this.enquireLink();
		}
	}

	/**
	 * Creates a new instance of <code>EnquireSM</code> class. This PDU is used
	 * to check that application level of the other party is alive. It can be
	 * sent both by SMSC and ESME.
	 * 
	 * See "SMPP Protocol Specification 3.4, 4.11 ENQUIRE_LINK Operation."
	 * 
	 * @see Session#enquireLink(SendEnquireLink)
	 * @see SendEnquireLink
	 * @see EnquireLinkResp
	 */
	private void enquireLink()
	{
		try
		{
			EnquireLink request = new EnquireLink();

			if (Config.smpp.asyncMode)
			{
				Gateway.session.enquireLink(request);
			}

		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			if (Var.smpp.running)
			{
				Var.smpp.sessionBound = false;
				mlog.log.info("Start rebind....");
				gateway.bindAsync();
				sleep(Config.smpp.rebindTimeout);
			}
		}
	}
}