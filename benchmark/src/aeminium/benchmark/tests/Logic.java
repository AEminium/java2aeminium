package aeminium.benchmark.tests;

public class Logic
{
	Logic() {}

	@AEminium
	public static String a()
	{
		return "OK";
	}

	@AEminium
	public static String b()
	{
		return "BAD";
	}
	
	@AEminium
	public static void main(String[] args)
	{
		String val;

		if (true)
			val = Logic.a();
		else
			val = Logic.b();
		
		System.out.println("Logic: " + val);
	}
}
