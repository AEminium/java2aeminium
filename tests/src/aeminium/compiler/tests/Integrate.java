package aeminium.compiler.tests;

/* integrate the function f(x) = (x*x+1)*x from -1000 to 1000 */
public class Integrate
{
	static double abs(double a)
	{
		/* TODO/FIXME 0-a because -a is an InfixExpression and its not supported yet */
		/* TODO/FIXME ConditionalExpression */
		if (a > 0)
			return a;
		else
			return -a;
	}

	static double eval(double l, double r, double fl, double fr, double a)
	{
        double h = (r - l) * 0.5;
        double c = l + h;
        double fc = (c * c + 1.0) * c;
        double hh = h * 0.5;
        double al = (fl + fc) * hh;
        double ar = (fr + fc) * hh;
        double alr = al + ar;

        if (Integrate.abs(alr - a) <= 1e-11)
            return alr;
        else
            return Integrate.eval(c, r, fc, fr, ar) + Integrate.eval(l, c, fl, fc, al);
	}

	static double integrate(double left, double right)
	{
		return Integrate.eval(left, right, (left * left + 1.0) * left, (right * right + 1.0) * right, 0);
	}

	public static void main(String[] args)
	{
		double area = Integrate.integrate(-10.0, 20.0);
		System.out.println(area);
	}
}
