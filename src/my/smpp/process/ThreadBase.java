package my.smpp.process;

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
	protected static QueueMap liveThread = new QueueMap();

	public String getKey()
	{
		return this.getClass().getName() + this.getId();
	}

	public void addIntoLiveThread()
	{
		liveThread.enqueue(this.getKey(), this);
	}
	public void removeFromLiveThread()
	{
		liveThread.dequeue(this.getKey());
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
