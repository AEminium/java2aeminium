package aeminium.benchmark.tests;

public class HelloWorld
{
	@AEminium
	public Integer method()
	{
		return 0;
	}

	@AEminium
	public Integer method2()
	{
		return 1;
	}

	@AEminium
	public static int main(String[] args)
	{
		HelloWorld m = new HelloWorld();

		//int a = m.method();
		//int b = m.method2();
	
		//int c = a+b;

		//if (c > 0)
		//	System.out.println(c);

		return 1;
	}
}
