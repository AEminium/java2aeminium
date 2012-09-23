package aeminium.compiler.tests;

public class Fibonacci
{
	public static long f(int n)
	{
		if (n < 2)
			return n;
		return f(n-1) + f(n-2);
	}

	public static void main(String[] args)
	{
		System.out.println(f(45));
	}
}
