package aeminium.compiler.tests;

public class ForFibonacci
{
	public static int f(int n)
	{
		if (n < 2)
			return n;
		return f(n-1) + f(n-2);
	}

	public static void main(String[] args)
	{
		int i = 0;
		while ( i < 30)
		{
			System.out.println("calling " + i);
			System.out.println(f(i));
			i++;
		}
	}
}
