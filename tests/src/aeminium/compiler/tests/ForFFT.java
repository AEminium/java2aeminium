package aeminium.compiler.tests;

import java.util.Random;

public class ForFFT {
    public static Complex[] sequentialFFT(Complex[] x) {
        int N = (x).length;

        if (N == 1)
            return new Complex[]{ x[0] };

/*        if ((N % 2) != 0) {
            throw new RuntimeException("N is not a power of 2");
        }
*/
        Complex[] even = new Complex[N / 2];
        aeminium_rec_for_method_7(even ,N ,0 ,x);
        Complex[] q = ForFFT.sequentialFFT(even);
        Complex[] odd = even;
        aeminium_rec_for_method_8(N ,odd ,0 ,x);
        Complex[] r = ForFFT.sequentialFFT(odd);
        Complex[] y = new Complex[N];
        aeminium_rec_for_method_9(r ,q ,N ,0 ,y);
        return y;
    }

    /**
     *Generated from the For cycle in line 30 of the original file.
     */
    public static void aeminium_rec_for_method_9(Complex[] r, Complex[] q, int N, int k, Complex[] y) {
        if (k < (N / 2)) {
            double kth = (((-2) * k) * (Math.PI)) / N;
            Complex wk = new Complex(Math.cos(kth) , Math.sin(kth));
            y[k] = q[k].plus(wk.times(r[k]));
            y[(k + (N / 2))] = q[k].minus(wk.times(r[k]));
            ++k;
            aeminium_rec_for_method_9(r ,q ,N ,k ,y);
        }
    }

    /**
     *Generated from the For cycle in line 23 of the original file.
     */
    public static void aeminium_rec_for_method_8(int N, Complex[] odd, int k, Complex[] x) {
        if (k < (N / 2)) {
            odd[k] = x[((2 * k) + 1)];
            ++k;
            aeminium_rec_for_method_8(N ,odd ,k ,x);
        }
    }

    /**
     *Generated from the For cycle in line 16 of the original file.
     */
    public static void aeminium_rec_for_method_7(Complex[] even, int N, int k, Complex[] x) {
        if (k < (N / 2)) {
            even[k] = x[(2 * k)];
            ++k;
            aeminium_rec_for_method_7(even ,N ,k ,x);
        }
    }

    /**
     *Generated from the While cycle in line 60 of the original file.
     */
    public static void aeminium_rec_while_method_5(Complex[] output, int i) {
        if (i < ((output).length)) {
            System.out.println(output[i]);
            ++i;
            aeminium_rec_while_method_5(output ,i);
        }
    }

    /**
     *Generated from the While cycle in line 45 of the original file.
     */
    public static void aeminium_rec_while_method_4(Random r, int n, Complex[] x, int i) {
        if (i < n) {
            x[i] = new Complex(((2 * (r.nextDouble())) - 1) , 0);
            ++i;
            aeminium_rec_while_method_4(r ,n ,x ,i);
        }
    }

    public static Complex[] createRandomComplexArray(int n, long seed) {
        Random r = new Random(seed);
        Complex[] x = new Complex[n];
        int i = 0;
        aeminium_rec_while_method_4(r ,n ,x ,i);
        return x;
    }

    public static void main(String[] args) {
        Complex[] input = ForFFT.createRandomComplexArray(1024 ,524288);
        Complex[] output = ForFFT.sequentialFFT(input);
        int i = 0;
        aeminium_rec_while_method_5(output ,i);
    }

}
