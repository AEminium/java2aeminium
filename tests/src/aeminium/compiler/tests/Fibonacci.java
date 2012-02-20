package aeminium.compiler.tests;

public class Fibonacci
{
	public static int f(int n)
	{
		if (n < 2)
			return n;
		else
			return f(n-1) + f(n-2);
	}

	public static void main(String[] args)
	{
		System.out.println(f(1));
		System.out.println(f(2));
		System.out.println(f(3));
		System.out.println(f(4));
	}
}
