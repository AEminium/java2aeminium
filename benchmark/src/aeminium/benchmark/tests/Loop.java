package aeminium.benchmark.tests;

public class Loop
{
	Loop() {}
	
	@AEminium
	public static Integer val()
	{
		return 1;
	}

	@AEminium
	public static void main(String[] args)
	{
		Integer sum = 0;

		for (int i = 0; i < 1000; i++)
		{
			sum += Loop.val();
		}
		
		if (sum == 1000)
			System.out.println("Loop: OK");
		else
			System.out.println("Loop: BAD");		
	}
}
