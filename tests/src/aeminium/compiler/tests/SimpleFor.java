package aeminium.compiler.tests;

public class SimpleFor {
	public static void main(String[] args) {
		int[] arr = new int[20];
		for (int i=0; i<20; i++) {
			arr[i] = i * 2;
		}
		System.out.println(arr[0] + ", " + arr[1] + ", " + arr[2]);
	}
}