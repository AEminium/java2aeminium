package aeminium.compiler.tests;

public class While
{
	public static void main(String[] args)
	{
		int a = 0;
		int b = 0;

		while (a < 10)
		{
			System.out.println(a++);
			System.out.println(b++);
		}
	}
}
