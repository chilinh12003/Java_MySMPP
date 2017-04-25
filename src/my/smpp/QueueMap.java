package my.smpp;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import uti.MyLogger;

public class QueueMap
{
	MyLogger mlog = new MyLogger(this.getClass().getName());
	
	protected HashMap<String,Object> listObj=null;
	
	public QueueMap()
	{
		listObj = new HashMap<String, Object>();
	}
	
	public HashMap<String, Object> getListObj()
	{
		return listObj;
	}

	public void setListObj(HashMap<String, Object> listObj)
	{
		this.listObj = listObj;
	}

	public Object dequeue(String key)
	{
		synchronized (listObj)
		{
			Object item = listObj.remove(key);
			return item;
		}
	}

	public void enqueue(String key, Object value)
	{
		synchronized (listObj)
		{
			listObj.put(key, value);
		}
	}
	
	public int size()
	{
		synchronized (listObj)
		{
			return listObj.size();
		}
	}

	public boolean isEmpty()
	{
		synchronized (listObj)
		{
			return listObj.isEmpty();
		}
	}
	
	/**
	 * Lưu danh sách có trong Queue xuống file
	 * @param fileName
	 */
	public void saveToFile(String fileName)
	{
		FileOutputStream fout = null;
		ObjectOutputStream objOut = null;

		if(this.listObj == null || this.size() < 1)
			return;
		try
		{
			fout = new java.io.FileOutputStream(fileName, false);
			objOut = new ObjectOutputStream(fout);

			synchronized (listObj)
			{
				for (Iterator<Entry<String, Object>> it = listObj.entrySet().iterator(); it
						.hasNext();)
				{
					HashMap.Entry<String, Object> item = (Entry<String, Object>) it.next();
					
					objOut.writeObject(item.getValue());
					objOut.flush();
					mlog.log.info("Save Queue To File:" + fileName+"-->" +MyLogger.GetLog(item.getValue()));
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
	 * Đọc từ file lên queue
	 * @param fileName: đường dẫn tới file đã lưu object
	 * @param fieldName: Tên của thuộc tính của Object sẽ lấy giá trị làm key để add vào Map
	 */
	public void loadFromFile(String fileName, String fieldName)
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
					//Vì queue là HasMap --> phải lấy giá trị của thuộc tính của Object để làm key
					Method method = obj.getClass().getDeclaredMethod(fieldName);
					if(method != null)
					{
						Object key = method.invoke(obj);
						if(key != null)
							this.enqueue(key.toString(), obj);
					}

					mlog.log.info("Load Queue From File:" + fileName+"-->" +MyLogger.GetLog(obj));
				}
				catch(EOFException ex)
				{
					flag = false;
				}
				catch (Exception ex)
				{
					mlog.log.error(ex);
					flag = false;
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
	
}
