package aeminium.benchmark.tests;

public class StaticCall
{
	StaticCall() {}

	@AEminium
	public static void test()
	{
		System.out.println("StaticCall: OK");
	}

	@AEminium
	public static void main(String[] args)
	{
		StaticCall.test();
	}
}
