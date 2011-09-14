package aeminium.benchmark.tests;

public class MethodCall
{
	MethodCall() {}

	@AEminium
	public void test()
	{
		System.out.println("MethodCall: OK");
	}

	@AEminium
	public static void main(String[] args)
	{
		MethodCall object = new MethodCall();
		object.test();
	}
}
