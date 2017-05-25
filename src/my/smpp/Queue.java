package my.smpp;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Vector;

import com.logica.smpp.pdu.PDU;
import com.logica.smpp.util.ByteBuffer;

import uti.MyConfig;
import uti.MyLogger;

public class Queue
{
	MyLogger mlog = new MyLogger(this.getClass().getName());

	protected LinkedList<Object> queue;

	public Queue()
	{
		queue = new LinkedList<Object>();
	}

	/**
	 * This method is used by a consummer. If you attempt to remove an object
	 * from an queue is empty queue, you will be blocked (suspended) until an
	 * object becomes available to remove. A blocked thread will thus wake up.
	 * 
	 * @return the first object (the one is removed).
	 */
	public Object dequeue()
	{
		synchronized (queue)
		{
			while (queue.isEmpty())
			{ // Threads are blocked
				try
				{ // if the queue is empty.
					queue.wait(); // wait until other thread call notify().
					// queue.wait(100); //

				}
				catch (InterruptedException ex)
				{
				}
			}
			Object item = queue.removeFirst();
			return item;
		}
	}

	public void enqueue(Object obj)
	{
		synchronized (queue)
		{
			queue.addLast(obj);
			// queue.notify();
			queue.notifyAll();
		}
	}

	/**
	 * Nếu thread nào đang trong trình trạng wait thì wakeup để chạy tiếp
	 */
	public void wakeup()
	{
		synchronized (queue)
		{
			queue.notifyAll();
		}
	}
	public int size()
	{
		synchronized (queue)
		{
			return queue.size();
		}
	}

	public boolean isEmpty()
	{
		synchronized (queue)
		{
			return queue.isEmpty();
		}
	}

	/**
	 * Lưu danh sách có trong Queue xuống file
	 * 
	 * @param <T>
	 * @param fileName
	 */
	public void saveToFile(String fileName)
	{
		FileOutputStream fout = null;
		ObjectOutputStream objOut = null;

		if (this.queue == null || this.queue.size() < 1)
			return;

		try
		{
			fout = new java.io.FileOutputStream(fileName, false);
			objOut = new ObjectOutputStream(fout);

			while (this.queue.size() > 0)
			{
				Object obj = this.dequeue();
				objOut.writeObject(obj);
				objOut.flush();
				mlog.log.info("Save Queue To File:" + fileName + "-->" + MyLogger.GetLog(obj));
			}
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
		}
		finally
		{
			try
			{
				if (objOut != null)
					objOut.close();
				if (fout != null)
					fout.close();
			}
			catch (IOException ex)
			{
				mlog.log.error(ex);
			}
		}
	}

	/**
	 * Đọc từ file danh sách các Object đã lưu trước đó
	 * 
	 * @param fileName
	 */
	public void loadFromFile(String fileName)
	{
		boolean flag = true;
		FileInputStream fin = null;
		ObjectInputStream objIn = null;
		FileOutputStream fout = null;
		try
		{
			File mFile = new File(fileName);
			if (!mFile.exists())
				return;

			fin = new java.io.FileInputStream(fileName);

			if (fin.available() <= 0)
			{
				return;
			}

			objIn = new ObjectInputStream(fin);

			while (flag)
			{
				try
				{
					Object obj = objIn.readObject();
					this.enqueue(obj);
					mlog.log.info("Load Queue From File:" + fileName + "-->" + MyLogger.GetLog(obj));
				}
				catch (EOFException ex)
				{
					flag = false;
				}
				catch (Exception ex)
				{
					flag = false;
					mlog.log.error(ex);
				}
			}
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
		}
		finally
		{
			try
			{
				if (fin != null)
					fin.close();
				fout = new java.io.FileOutputStream(fileName, false);
				fout.close();
			}
			catch (Exception ex)
			{
				mlog.log.error(ex);
			}
		}
	}

	private void writePdu(String folder, PDU pdu)
	{
		FileOutputStream fout = null;
		try
		{
			String fileName = MyConfig.Get_DateFormat_yyyymmddhhmmssSSS().format(Calendar.getInstance().getTime());
			fout = new FileOutputStream(folder + fileName + ".pdu");
			byte[] buf = pdu.getData().getBuffer();
			fout.write(buf);
			fout.flush();
			mlog.log.info("Save PDUQueue To File:" + fileName + "-->" + pdu.debugString());
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
		}
		finally
		{
			if (fout != null)
			{
				try
				{
					fout.close();
				}
				catch (IOException ex)
				{
					mlog.log.error(ex);
				}
			}
		}
	}

	PDU readPdu(File filePdu)
	{
		PDU pdu = null;
		byte[] buffer = null;
		FileInputStream fin = null;
		try
		{
			if (filePdu.getName().toLowerCase().endsWith(".pdu"))
			{
				fin = new FileInputStream(filePdu);
				buffer = new byte[fin.available()];
				fin.read(buffer);
				ByteBuffer bf = new ByteBuffer();
				bf.appendBytes(buffer);
				pdu = PDU.createPDU(bf);
				fin.close();
				mlog.log.info("Load PDUQueue From File:" + filePdu.getName() + "-->" + pdu.debugString());

				if (!filePdu.delete())
				{
					mlog.log.warn("FAIL delete file:" + filePdu.getName());
				}
			}
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
		}
		finally
		{
			if (fin != null)
			{
				try
				{
					fin.close();
				}
				catch (IOException ex)
				{
					mlog.log.error(ex);
				}
			}
		}
		return pdu;
	}

	/**
	 * Đối với những Queue chứa dạng PDU (SubmitSM, SubmitSM Response,
	 * DeliverySM...). Thì cần phải lưu dạng buffer tới 1 file riêng
	 * 
	 * @param folder
	 */
	public void savePdu(String folder)
	{
		try
		{

			while (queue != null && queue.size() > 0)
			{
				PDU pdu = (PDU) this.dequeue();
				writePdu(folder, pdu);
			}
		}
		catch (Exception ex)
		{
			mlog.log.error(folder, ex);
		}
	}

	/**
	 * Load cho những Queue là dạng PDU
	 * 
	 * @param folder
	 * @param extension
	 */
	public void loadPdu(String folder)
	{
		try
		{
			File file = new File(folder);
			File[] listFile = file.listFiles();
			for (int i = 0; i < listFile.length; i++)
			{
				PDU pdu = readPdu(listFile[i]);
				if (pdu != null)
					enqueue(pdu);
			}
		}
		catch (Exception ex)
		{
			mlog.log.error(folder, ex);
		}
	}
}
