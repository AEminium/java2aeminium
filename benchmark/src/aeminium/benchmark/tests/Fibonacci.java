package aeminium.benchmark.tests;

public class Fibonacci
{
	@AEminium
	public int fib(int n)
	{
		if (n < 2)
			return n;
		else
			return this.fib(n-1) + this.fib(n-2);
	}

	@AEminium
	public static int main(String[] args)
	{
		Fibonacci h = new Fibonacci();
		int a = h.fib(20);
	}
}
