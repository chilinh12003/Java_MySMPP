package my.smpp;
import java.util.HashMap;

public class QueueMap
{
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

}
