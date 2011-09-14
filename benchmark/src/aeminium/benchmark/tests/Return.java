package aeminium.benchmark.tests;

public class Return
{
	Return() {}

	@AEminium
	public String message()
	{
		return "Return: OK";
	}

	@AEminium
	public static void main(String[] args)
	{
		Return object = new Return();

		System.out.println(object.message());
	}
}
