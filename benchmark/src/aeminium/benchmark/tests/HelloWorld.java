package aeminium.benchmark.tests;

public class HelloWorld
{
	@AEminium
	public int method()
	{
		return 1;
	}

	@AEminium
	public static void main(String[] args)
	{
		HelloWorld m = new HelloWorld();
		int a = m.method();
	}
}
