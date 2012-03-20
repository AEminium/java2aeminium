import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.StringTokenizer;

public class Knapsack {
	public static int SIZE_LIMIT = 87;

	Random rand = new Random();
	
	int numberOfItems = 500;
	Item[] items = Knapsack.readItems("items.txt", numberOfItems);
	
	int runs = 5;
	int popSize = 100;
	int cromSize = numberOfItems;
	
	double prob_mut = 0.2;
	double prob_rec = 0.2;
	
	int numGen = 100000;

	
	
	public void run() {
		System.out.println("Running");
		for (int i=0; i<runs; i++) {
			ga();
		}
		System.out.println("Done");
	}
	
	/* Genetic Algorithm */
	public void ga() {
		
		// Initialize Population
		Indiv[] pop = new Indiv[popSize];
		for (int i=0; i< popSize; i++) {
			pop[i] = genIndiv();
			pop[i].fitness = evaluate(pop[i]);
		}
		
		for (int i=0; i< numGen; i++) {
			// Sort by lowest Fitness
			for (int j=0; j< popSize; j++) {
				pop[j].fitness = evaluate(pop[j]);
			}
			Arrays.sort(pop);
			
			// System.out.println("Best fit:" + pop[2].fitness);
			
			Indiv[] parents = tournament(pop);
			
			// Recombination
			int half = parents.length/2;
			for (int j = 0; j < half; j++) {
				if (rand.nextDouble() < prob_rec) {
					recombine(parents, j, half+j);
				}
			}
		
			// Mutation and Evaluation
			for (int j = 0; j < parents.length; j++) {
				if (rand.nextDouble() < prob_mut) {
					mutate(parents[j]);
				}
			}
			
			pop = parents;	
		}
		
		
		// Best:
		
		for (int j=0; j< popSize; j++) {
			pop[j].fitness = evaluate(pop[j]);
		}
		Arrays.sort(pop);
		Indiv indiv = pop[0];
		int value = 0;
		int weight = 0;
		for (int i=0; i< indiv.size; i++) {
			if (indiv.has[i]) {
				value += items[i].value;
				weight += items[i].weight;
			}
		}
		System.out.println("Best value/weight: " + value + ", " + weight);
		
	}
	
	private void mutate(Indiv indiv) {
		int sw = rand.nextInt(cromSize);
		indiv.has[sw] = !indiv.has[sw];
	}

	private void recombine(Indiv[] parents, int a, int b) {
		Indiv n1 = new Indiv(cromSize);
		Indiv n2 = new Indiv(cromSize);
		int cutPos = rand.nextInt(cromSize);
		for (int k = 0; k < cromSize; k++) {
			if (k < cutPos) {
				n1.add(k,parents[a].has[k]);
				n2.add(k,parents[b].has[k]);
			} else {
				n1.add(k,parents[b].has[k]);
				n2.add(k,parents[a].has[k]);
			}
			
		}
		parents[a] = n1;
		parents[b] = n2;
	}

	private Indiv[] tournament(Indiv[] pop) {
		int tSize = 50;
		Indiv[] f = new Indiv[popSize];
		for (int i = 0; i < popSize; i++) {
			Collections.shuffle(Arrays.asList(pop));
			f[i] = pop[0];
			for (int j = 1; j < tSize; j++) {
				if (pop[j].fitness < f[i].fitness) f[i] = pop[j];
			}
		}
		return f;
	}
	
	private int[] phenotype(Indiv indiv) {
		int value = 0;
		int weight = 0;
		for (int i=0; i< indiv.size; i++) {
			if (indiv.has[i]) {
				value += items[i].value;
				weight += items[i].weight;
			}
		}
		return new int[] {value, weight};
	}

	private double evaluate(Indiv indiv) {
		int[] ph = phenotype(indiv);
		int value = ph[0];
		int weight = ph[1];
		
		// Evaluation
		if (weight >= SIZE_LIMIT) {
			return 2.0;
		} else {
			return 1.0/(value); // Minimization problem.
		}
		
	}

	private Indiv genIndiv() {
		while (true) {
			int c = 0;
			Indiv e = new Indiv(cromSize);
			for(int i=0;i<cromSize;i++) {
				boolean b = ( rand.nextDouble() < 0.01 );
				e.add(i, b);
				if (b) c++;
			}
			// Avoids empty backpacks
			if ( c > 0) {
				return e;
			}
		}
	}

	public static void main(String[] args) {
		new Knapsack().run();
	}
	
	public static Item[] readItems(String fname, int n) {
		if (!new File(fname).exists())
			return null;
		
		Item[] tmp = new Item[n];
		try {
			FileInputStream fstream = new FileInputStream(fname);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int i = 0;
			while ((strLine = br.readLine()) != null) {
				StringTokenizer tok = new StringTokenizer(strLine, ",");
				tmp[i++] = new Item(tok.nextToken(), Float.parseFloat(tok.nextToken()), Float.parseFloat(tok.nextToken()));
			}
			// Close the input stream
			in.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
		return tmp;
	}

}
