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
		int a[] = new int[] { f(0), f(1), f(2), f(3)};

		System.out.println(a[0]);
		System.out.println(a[1]);
		System.out.println(a[2]);
		System.out.println(a[3]);
	}
}
