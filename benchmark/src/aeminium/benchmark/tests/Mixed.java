package aeminium.benchmark.tests;

@AEminium
public class Mixed
{
	@AEminium
	public Integer f(Integer x)
	{
		return x;
	}

	@AEminium
	public Integer g(Integer x)
	{
		return 2*x;
	}

	@AEminium
	public Integer method(Integer arg)
	{
		Integer x = this.f(arg);
		Integer sum = 0;
		Integer b;

		if (x > 0)
			b = 10;
		else
			b = 20;

		for (Integer i = 0; i < b; i++)
			sum += this.g(i);

		return sum;
	}

	@AEminium
	public static main(String[] args)
	{
		Mixed object = new Mixed();

		System.out.println(object.method(10));
	}
}
