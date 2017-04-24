package my.smpp;

import java.util.Calendar;
import java.util.Vector;

import com.logica.smpp.Data;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.util.ByteBuffer;

import my.db.MtLog;
import my.db.MtQueue;
import my.db.MtQueue.ContentType;
import my.db.MtQueue.SendType;
import my.db.MtQueue.Status;
import uti.MyCheck;
import uti.MyConfig;
import uti.MyConfig.Telco;
import uti.MyDate;
import uti.MyLogger;
import uti.MyText;
import uti.define.PhoneNumberInfo;
import uti.define.PhoneNumberInfo.FormatType;

/**
 * Dùng để build SubmitSM từ MtQueue. <br>
 * Nếu có 1 bản tin MTqueue nào không biuld do không hợp lệ hoặc lỗi thì sẽ Add
 * queue để chờ insert vào MtLog
 * 
 * @author Chilinh
 *
 */
public class BuildMt
{
	MyLogger mlog = new MyLogger(this.getClass().getName());

	Queue mtLogQueue = null;
	MtQueue mtQueue = null;
	SubmitSM submitSm = null;
	Vector<SubmitSM> listSubmit = new Vector<SubmitSM>();

	MtQueue.ContentType contentType = ContentType.NoThing;
	MtQueue.SendType sendType = SendType.NoThing;
	MtQueue.Status status = Status.NoThing;

	String note = "";
	boolean isValid = false;

	// Chiều dài tối đa cho mỗi segement
	int segmentLength = 160;
	String encoding = Data.ENC_ISO8859_1;

	// Tổng độ dài của MT
	int mtLength = 0;
	int totalSegment = 1;

	public BuildMt(MtQueue mtQueue, Queue mtLogQueue)
	{
		this.mtQueue = mtQueue;
		this.mtLogQueue = mtLogQueue;
		checkValid();
	}
	void setInvalid(String note)
	{
		this.note += note;
		status = Status.MtInvalid;
		isValid = false;
	}
	/**
	 * Kiểm tra tính hợp lệ của MTQueue
	 * 
	 * @author:Chilinh
	 */
	void checkValid()
	{
		try
		{
			if (mtQueue == null)
			{
				return;
			}

			// ContentType
			contentType = MtQueue.ContentType.fromValue(mtQueue.getContentTypeId());
			sendType = MtQueue.SendType.fromValue(mtQueue.getSendTypeId());
			status = MtQueue.Status.fromValue(mtQueue.getStatusId());

			if (contentType == ContentType.NoThing || sendType == SendType.NoThing)
			{
				setInvalid("ContentType hoac SendType khong hop le");
				return;
			}

			// Lenght MT
			mtLength = mtQueue.getMt().length();
			if (mtLength > Config.mt.maxLengthMt || mtLength < 1)
			{
				setInvalid("Length Invalid");
				return;
			}

			switch (contentType)
			{
				case ShortText :
					encoding = Data.ENC_ISO8859_1;
					segmentLength = 160;
					if (mtLength > segmentLength)
					{
						setInvalid("ShortText: Length Invalid");
						return;
					}
					break;
				case ShortUnicode :
					encoding = Data.ENC_UTF16_BE;
					segmentLength = 70;
					if (mtLength > segmentLength)
					{
						setInvalid("ShortText: Length Invalid");
						return;
					}
					break;
				case LongText :
					encoding = Data.ENC_ISO8859_1;
					segmentLength = 153;

					break;
				case LongUnicode :
					encoding = Data.ENC_UTF16;
					segmentLength = 67;
					break;
				default :
					encoding = Data.ENC_ISO8859_1;
					segmentLength = 160;
					break;
			}

			// Kiểm tra hợp lệ của số điện thoại và mạng.
			if (!checkPhoneNumber())
			{
				return;
			}

			// Kiểm tra hợp lệ của ShortCode
			if (!Common.checkShortCode(mtQueue.getShoreCode()))
			{
				setInvalid("ShortCode khong ho le");
				return;
			}
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
		}

		isValid = true;
	}

	boolean checkPhoneNumber()
	{
		try
		{
			Telco telco = Telco.NOTHING;
			String phoneNumber = mtQueue.getPhoneNumber();
			PhoneNumberInfo phoneInfo = new PhoneNumberInfo(phoneNumber, telco);

			if (!phoneInfo.check(FormatType.National))
			{
				setInvalid("PhoneNumber Khong hop le");
				return false;
			}
			phoneNumber = phoneInfo.getPhoneNumber();
			telco = phoneInfo.getTelco();

			if (telco != Config.smpp.telco)
			{
				setInvalid("Telco Khong hop le");
				return false;
			}
			return true;
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			return false;
		}
	}

	/**
	 * Add Queue chờ Lưu xuống mtlog
	 * 
	 * @author:Chilinh
	 */
	public void addMtlogQueue()
	{
		try
		{
			// Update MtQueue
			mtQueue.setTotalSegment((short) totalSegment);
			mtQueue.setNote(note);
			mtQueue.setStatusId(status.getValue());
			mtQueue.setDoneDate(MyDate.Date2Timestamp(Calendar.getInstance()));

			mtLogQueue.enqueue(mtQueue);
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			mlog.log.warn("SAVE FAIL MTQUEUE -->:" + MyLogger.GetLog(mtQueue));
		}
	}
	public Vector<SubmitSM> getSubmit()
	{
		// Nếu mtQueue không hợp lệ thì insert xuống log, và không gửi đi
		if (!isValid || sendType == SendType.NotSend)
		{
			addMtlogQueue();
			return listSubmit;
		}

		try
		{
			String[] arrMt = MyText.split(mtQueue.getMt(), segmentLength);
			totalSegment = arrMt.length;

			int mtId = Var.mt.getMtIdForUhd();
			
			int sequenceId= 1;
			
			for (int i = 0; i < totalSegment; i++)
			{
				
				
				initSubmit();
				ByteBuffer udh;
				ByteBuffer buffer;
				String mtUnicode;

				switch (contentType)
				{
					case ShortText :
						submitSm.setShortMessage(mtQueue.getMt(), encoding);
						break;
					case ShortUnicode :
						buffer = new ByteBuffer();
						buffer.appendString(mtQueue.getMt(), Data.ENC_UTF16);
						buffer.removeBytes(2);
						mtUnicode = buffer.removeString(buffer.length(), Data.ENC_ISO8859_1);
						submitSm.setEsmClass((byte) 0x03);
						submitSm.setDataCoding((byte) 0x08);
						submitSm.setShortMessage(mtUnicode, Data.ENC_ISO8859_1);
						break;
					case LongText :
						udh = createUdh(mtId, totalSegment, i + 1);
						udh.appendString(arrMt[i], encoding);
						submitSm.setShortMessageData(udh);
						break;
					case LongUnicode :

						udh = createUdh(mtId, totalSegment, i + 1);

						buffer = new ByteBuffer();
						buffer.appendString(arrMt[i], Data.ENC_UTF16);
						// Xóa bỏ các ký tự thừa
						buffer.removeBytes(2);
						mtUnicode = buffer.removeString(buffer.length(), Data.ENC_ISO8859_1);

						udh.appendString(mtUnicode, Data.ENC_ISO8859_1);
						submitSm.setDataCoding((byte) 0x08);
						submitSm.setShortMessageData(udh);
						break;
					default :
						break;
				}
				
				sequenceId = mtQueue.getMtId()+i;
				submitSm.setSequenceNumber(sequenceId);

				if (i == totalSegment - 1)
				{
					// Chỉ thiết lập nhận bản tin Delivery cho segment cuối
					// cùng
					submitSm.setRegisteredDelivery((byte) 1);
				}
				listSubmit.add(submitSm);
			}

			status = Status.WaitingResponse;
			// Update MtQueue
			mtQueue.setTotalSegment((short) totalSegment);
			mtQueue.setNote(note);
			mtQueue.setMtResponseId(sequenceId);
			mtQueue.setStatusId(status.getValue());

		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			mlog.log.warn("BUILD MT ERROR -->" + MyLogger.GetLog(mtQueue));
			setInvalid("Build Submit Error");
			status = Status.BuildSubmitFail;
			addMtlogQueue();
		}

		return listSubmit;
	}

	void initSubmit() throws Exception
	{
		try
		{
			submitSm = new SubmitSM();

			submitSm.setSourceAddr(Config.smpp.destAddrTon, Config.smpp.srcAddrNpi, mtQueue.getShoreCode());
			submitSm.setDestAddr(Config.smpp.destAddrTon, Config.smpp.destAddrNpi, mtQueue.getPhoneNumber());
			submitSm.setServiceType(Config.smpp.serviceType);
			submitSm.setEsmClass((byte) (Data.SM_UDH_GSM));
			submitSm.setDataCoding(Config.smpp.dataCoding);
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
			throw ex;
		}
	}

	ByteBuffer createUdh(int mtId, int totalSegment, int indexSegment) throws Exception
	{

		// part will be UDH (6 bytes) + length of part
		ByteBuffer udh = new ByteBuffer();
		// Field 1 (1 octet): Length of User Data Header, in this case 05.
		udh.appendByte((byte) 0x05);
		// Field 2 (1 octet): Information Element Identifier, equal to 00
		// (Concatenated short messages, 8-bit reference number)
		udh.appendByte((byte) 0x00);
		// Field 3 (1 octet): Length of the header, excluding the first two
		// fields; equal to 03
		udh.appendByte((byte) 0x03);
		// Field 4 (1 octet): 00-FF, CSMS reference number, must be same for all
		// the SMS parts in the CSMS
		udh.appendByte((byte) mtId);
		// Field 5 (1 octet): 00-FF, total number of parts. The value shall
		// remain constant for every short message which makes up the
		// concatenated short message. If the value is zero then the receiving
		// entity shall ignore the whole information element
		udh.appendByte((byte) totalSegment);
		// Field 6 (1 octet): 00-FF, this part's number in the sequence. The
		// value shall start at 1 and increment for every short message which
		// makes up the concatenated short message. If the value is zero or
		// greater than the value in Field 5 then the receiving entity shall
		// ignore the whole information element. [ETSI Specification: GSM 03.40
		// Version 5.3.0: July 1996]
		udh.appendByte((byte) indexSegment);

		return udh;
	}
}
