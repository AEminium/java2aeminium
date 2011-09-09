package aeminium.benchmark.tests;

public class MethodCall
{
	public int var;

	MethodCall()
	{
		System.out.println("Constructor");
		this.var = 1;
	}

	public void test()
	{
		System.out.println("Result: " + this.var);
	}

	@AEminium
	public static void main(String[] args)
	{
		MethodCall object = new MethodCall();
		object.test();
	}
}
