package my.smpp.process;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import my.smpp.QueueMap;
import uti.MyLogger;

/**
 * Tất cả các thread đều phải kế thừa threadBase này. Class tạo để để phục vụ
 * làm một số việc chung cho tất cả các thread
 * 
 * @author Administrator
 *
 */
public abstract class ThreadBase extends Thread
{
	MyLogger mlog = new MyLogger(this.getClass().getName());

	
	/**
	 * Lưu trữ tất cả các thread đang chạy
	 */
	protected static volatile HashMap<String, Object> liveThread = new HashMap<String, Object>();

	public String getKey()
	{
		return this.getClass().getName() + this.getId();
	}
	
	/**
	 * Số lượng thread đang chạy
	 * @return
	 */
	public static int countLiveThread()
	{
		return liveThread.size();
	}
	public void addIntoLiveThread()
	{
		liveThread.put(this.getKey(), this);
	}
	public void removeFromLiveThread()
	{
		liveThread.remove(this.getKey());
	}

	public static void stateLiveThread()
	{
		for(Iterator<Entry<String, Object>> t = liveThread.entrySet().iterator(); t.hasNext();)
		{
			Entry<String, Object> item = t.next();
			ThreadBase thread = (ThreadBase) item.getValue();
			System.out.println("Thread: "+item.getKey() +"|State:" +thread.getState().toString());
			
		}
	}
	public ThreadBase()
	{
		// TODO Auto-generated constructor stub
	}

	public final void sleep(int miliSecond)
	{
		try
		{
			Thread.sleep(miliSecond);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public final void run()
	{
		try
		{
			addIntoLiveThread();
			doRun();
		}
		catch (Exception ex)
		{
			mlog.log.error(ex);
		}
		finally
		{
			removeFromLiveThread();
		}
	}
	public abstract void doRun();

}
