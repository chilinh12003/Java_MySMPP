package my.smpp;

import java.util.Calendar;

import com.logica.smpp.*;

import uti.MyDate;
import uti.MyLogger;

public class Common
{
	static MyLogger mlog = new MyLogger(Common.class.getName());

	public static String getCommandDescription(int commandStatus)
	{
		String strTemp = null;
		switch (commandStatus)
		{
			case Data.ESME_RINVMSGLEN :
				strTemp = "MESSAGE LENGTH IS INVALID";
				break;
			case Data.ESME_RINVCMDLEN :
				strTemp = "COMMAND LENGTH IS INVALID";
				break;
			case Data.ESME_RSYSERR :
				strTemp = "SYSTEM ERROR";
				break;
			case Data.ESME_RINVSRCADR :
				strTemp = "INVALID SOURCE ADDRESS";
				break;
			case Data.ESME_RINVDSTADR :
				strTemp = "INVALID DEST ADDRESS";
				break;
			case Data.ESME_RSUBMITFAIL :
				strTemp = "SUBMIT FAILED";
				break;
			case Data.ESME_RTHROTTLED :
				strTemp = "Throttling error (ESME has exceeded allowed message limits)";
				break;
			case Data.ESME_RALYBND :
				strTemp = "ESME ALREADY IN BOUND STATE";
				break;
			case Data.ESME_RBINDFAIL :
				strTemp = "BIND FAILED";
				break;
			case Data.ESME_RINVPASWD :
				strTemp = "INVALID PASSWORD";
				break;
			case Data.ESME_RINVSYSID :
				strTemp = "INVALID SYSTEM_ID";
				break;
			case Data.ESME_RINVSERTYP :
				strTemp = "INVALID SERVICE TYPE";
				break;
			case Data.ESME_RINVSRCTON :
				strTemp = "INVALID SOURCE_ADDR_TON";
				break;
			case Data.ESME_RINVSRCNPI :
				strTemp = "INVALID SOURCE_ADDR_NPI";
				break;
			case Data.ESME_RINVDSTTON :
				strTemp = "INVALID DEST_ADDR_TON";
				break;
			case Data.ESME_RINVDSTNPI :
				strTemp = "INVALID DEST_ADDR_NPI";
				break;
			case Data.ESME_RMSGQFUL :
				strTemp = "MESSAGE QUEUE FULL";
				break;
			case Data.ESME_RINVSYSTYP :
				strTemp = "INVALID SYSTEM_TYPE";
				break;
			default :
				strTemp = "ERROR";
		}
		return strTemp;
	}

	// Check resend MT
	public static int checkResendMT(int commandStatus, String MessageID)
	{
		String commandStatus_Hex = "0x" + Integer.toHexString(commandStatus).toUpperCase();
		String strTemp = null;
		int resend = 1;
		int alert = 0;
		switch (commandStatus)
		{
			case Data.ESME_RINVMSGLEN :
				strTemp = "MESSAGE LENGTH IS INVALID";
				resend = 0;
				alert = 0;
				break;
			case Data.ESME_RINVCMDLEN :
				strTemp = "COMMAND LENGTH IS INVALID";
				resend = 0;
				alert = 0;
				break;
			case Data.ESME_RSYSERR :
				strTemp = "SYSTEM ERROR";
				resend = 1;
				alert = 1;
				break;
			case Data.ESME_RINVSRCADR :
				strTemp = "INVALID SOURCE ADDRESS";
				resend = 0;
				alert = 1;
				break;
			case Data.ESME_RINVDSTADR :
				strTemp = "INVALID DEST ADDRESS";
				resend = 0;
				alert = 1;
				break;
			case Data.ESME_RSUBMITFAIL :
				strTemp = "SUBMIT FAILED";
				resend = 1;
				alert = 1;
				break;
			case Data.ESME_RTHROTTLED :
				strTemp = "Throttling error (ESME has exceeded allowed message limits)";
				resend = 1;
				alert = 0;
				break;
			case Data.ESME_RALYBND :
				strTemp = "ESME ALREADY IN BOUND STATE";
				resend = 0;
				break;
			case Data.ESME_RBINDFAIL :
				strTemp = "BIND FAILED";
				resend = 1;
				break;
			case Data.ESME_RINVPASWD :
				strTemp = "INVALID PASSWORD";
				resend = 1;
				break;
			case Data.ESME_RINVSYSID :
				strTemp = "INVALID SYSTEM_ID";
				resend = 1;
				break;
			case Data.ESME_RINVSERTYP :
				strTemp = "INVALID SERVICE TYPE";
				resend = 1;
				alert = 1;
				break;
			case Data.ESME_RINVSRCTON :
				strTemp = "INVALID SOURCE_ADDR_TON";
				resend = 1;
				alert = 1;
				break;
			case Data.ESME_RINVSRCNPI :
				strTemp = "INVALID SOURCE_ADDR_NPI";
				resend = 1;
				alert = 1;
				break;
			case Data.ESME_RINVDSTTON :
				strTemp = "INVALID DEST_ADDR_TON";
				resend = 1;
				alert = 1;
				break;
			case Data.ESME_RINVDSTNPI :
				strTemp = "INVALID DEST_ADDR_NPI";
				resend = 1;
				alert = 1;
				break;
			case Data.ESME_RMSGQFUL :
				strTemp = "MESSAGE QUEUE FULL";
				resend = 1;
				alert = 1;
				break;
			case Data.ESME_RINVSYSTYP :
				strTemp = "INVALID SYSTEM_TYPE";
				resend = 1;
				alert = 1;
				break;
			default :
				strTemp = "ERROR";
				resend = 1;
				alert = 0;
		}		

		if (alert == 1)
		{
			mlog.log.warn("{ERR=" + strTemp + ":" + commandStatus_Hex + "{Resend=" + resend + "}");
		}

		return resend;
	}

	public static String getErrorName(int commandStatus, String MessageID)
	{
		String commandStatus_Hex = "0x" + Integer.toHexString(commandStatus).toUpperCase();
		String strTemp = null;
		switch (commandStatus)
		{
			case Data.ESME_RINVMSGLEN :
				strTemp = "MESSAGE LENGTH IS INVALID";
				break;
			case Data.ESME_RINVCMDLEN :
				strTemp = "COMMAND LENGTH IS INVALID";
				break;
			case Data.ESME_RSYSERR :
				strTemp = "SYSTEM ERROR";
				break;
			case Data.ESME_RINVSRCADR :
				strTemp = "INVALID SOURCE ADDRESS";
				break;
			case Data.ESME_RINVDSTADR :
				strTemp = "INVALID DEST ADDRESS";
				break;
			case Data.ESME_RSUBMITFAIL :
				strTemp = "SUBMIT FAILED";
				break;
			case Data.ESME_RTHROTTLED :
				strTemp = "Throttling error (ESME has exceeded allowed message limits)";
				break;
			case Data.ESME_RALYBND :
				strTemp = "ESME ALREADY IN BOUND STATE";
				break;
			case Data.ESME_RBINDFAIL :
				strTemp = "BIND FAILED";
				break;
			case Data.ESME_RINVPASWD :
				strTemp = "INVALID PASSWORD";
				break;
			case Data.ESME_RINVSYSID :
				strTemp = "INVALID SYSTEM_ID";
				break;
			case Data.ESME_RINVSERTYP :
				strTemp = "INVALID SERVICE TYPE";
				break;
			case Data.ESME_RINVSRCTON :
				strTemp = "INVALID SOURCE_ADDR_TON";
				break;
			case Data.ESME_RINVSRCNPI :
				strTemp = "INVALID SOURCE_ADDR_NPI";
				break;
			case Data.ESME_RINVDSTTON :
				strTemp = "INVALID DEST_ADDR_TON";
				break;
			case Data.ESME_RINVDSTNPI :
				strTemp = "INVALID DEST_ADDR_NPI";
				break;
			case Data.ESME_RMSGQFUL :
				strTemp = "MESSAGE QUEUE FULL";
				break;
			case Data.ESME_RINVSYSTYP :
				strTemp = "INVALID SYSTEM_TYPE";
				break;
			default :
				strTemp = "UNKNOW ERROR";
		}
		mlog.log.info("{MessageID=" + MessageID + "}{ERR=" + strTemp + ":" + commandStatus_Hex);
		return strTemp;
	}

	public static boolean checkShortCode(String shortCode)
	{
		try
		{
			if (Config.smpp.validShortCode.indexOf("," + shortCode) >= 0)
			{
				return true;
			}
			else return false;
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			return false;
		}

	}

	
}
