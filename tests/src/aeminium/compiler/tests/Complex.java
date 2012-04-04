


public class Complex {
    private final double re;
    
    private final double im;
    
    public Complex(double real ,double imag) {
        this.re = real;
        this.im = imag;
    }
    
    public String toString() {
        if ((this.im) == 0)
            return (this.re) + "";
        else if ((this.re) == 0)
            return (this.im) + "i";
        else if ((this.im) < 0)
            return (((this.re) + " - ") + (-(this.im))) + "i";
        else
            return (((this.re) + " + ") + (this.im)) + "i";
        
    }
    
    public double abs() {
        return java.lang.Math.hypot(this.re ,this.im);
    }
    
    public double phase() {
        return java.lang.Math.atan2(this.im ,this.re);
    }
    
    public Complex plus(Complex b) {
        double real = (this.re) + (b.re);
        double imag = (this.im) + (b.im);
        return new Complex(real , imag);
    }
    
    public Complex minus(Complex b) {
        double real = (this.re) - (b.re);
        double imag = (this.im) - (b.im);
        return new Complex(real , imag);
    }
    
    public Complex times(Complex b) {
        double real = ((this.re) * (b.re)) - ((this.im) * (b.im));
        double imag = ((this.re) * (b.im)) + ((this.im) * (b.re));
        return new Complex(real , imag);
    }
    
    public Complex conjugate() {
        return new Complex(this.re , -(this.im));
    }
    
    public Complex reciprocal() {
        double scale = ((this.re) * (this.re)) + ((this.im) * (this.im));
        return new Complex(((this.re) / scale) , ((-(this.im)) / scale));
    }
    
    public double re() {
        return this.re;
    }
    
    public double im() {
        return this.im;
    }
    
    public Complex divides(Complex b) {
        return times(b.reciprocal());
    }
    
    public Complex exp() {
        return new Complex(((java.lang.Math.exp(this.re)) * (java.lang.Math.cos(this.im))) , ((java.lang.Math.exp(this.re)) * (java.lang.Math.sin(this.im))));
    }
    
    public Complex sin() {
        return new Complex(((java.lang.Math.sin(this.re)) * (java.lang.Math.cosh(this.im))) , ((java.lang.Math.cos(this.re)) * (java.lang.Math.sinh(this.im))));
    }
    
    public Complex cos() {
        return new Complex(((java.lang.Math.cos(this.re)) * (java.lang.Math.cosh(this.im))) , ((-(java.lang.Math.sin(this.re))) * (java.lang.Math.sinh(this.im))));
    }
    
    public Complex tan() {
        return sin().divides(cos());
    }
    
}

