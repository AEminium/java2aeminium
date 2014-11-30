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
		double time = System.nanoTime();
		System.out.println(f(50));
		time = (System.nanoTime() - time) / 1000000000.0;
		System.out.println(time);
	}
}
