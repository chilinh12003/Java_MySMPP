package my.smpp;

/**
 * Khai báo các biến cho chương trình
 * @author Administrator
 *
 */
public class Var
{
	public static class smpp
	{
		public static boolean running = true;	
		public static boolean sessionBound = false;

	}
	
	public static class mt
	{
		static int sequenceNumber = 0;
		static int mtIdForUdh = 0;
		
		public static synchronized int getSequenceNumber()
		{
			sequenceNumber++;
			if (sequenceNumber > 999999)
				sequenceNumber = 1;
			return sequenceNumber;
		}
		
		public static synchronized int getMtIdForUhd()
		{
			sequenceNumber++;
			if (sequenceNumber > 200)
				sequenceNumber = 1;
			return sequenceNumber;
		}
	}

	
}
