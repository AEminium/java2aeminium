package aeminium.benchmark.tests;

public class HelloWorld
{
	@AEminium
	public Integer method()
	{
		return 1;
	}

	@AEminium
	public static Integer main(String[] args)
	{
		HelloWorld m = new HelloWorld();
		Integer a = m.method();

		return 1;
	}
}
