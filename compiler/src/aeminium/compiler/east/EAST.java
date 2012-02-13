package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jdt.core.dom.*;

public class EAST
{
	private final HashMap<String, EASTNode> namedNodes;
	private final ArrayList<ECompilationUnit> originalCUs;
	
	public EAST()
	{
		this.namedNodes = new HashMap<String, EASTNode>();
		this.originalCUs = new ArrayList<ECompilationUnit>();
	}
	
	public void addOriginalCU(ECompilationUnit cu)
	{
		this.originalCUs.add(cu);
	}
	
	public void addNode(String name, EASTNode node)
	{
		this.namedNodes.put(name, node);
	}
	
	public EASTNode getNode(String name)
	{
		return this.namedNodes.get(name);
	}
	
	public void addNode(IBinding binding, EASTNode node)
	{
		this.namedNodes.put(binding.getKey(), node);
	}
	
	public EASTNode getNode(IBinding binding)
	{
		return this.getNode(binding.getKey());
	}

	public void checkSignatures()
	{
		for (ECompilationUnit cu : this.originalCUs)
			cu.checkSignatures();
	}

	public void checkDependencies()
	{
		for (ECompilationUnit cu : this.originalCUs)
			cu.checkDependencies();
	}

	public int optimize()
	{
		int sum = 0;
		
		for (ECompilationUnit cu : this.originalCUs)
			sum += cu.optimize();
		
		return sum;
	}

	public void preTranslate()
	{
		for (ECompilationUnit cu : this.originalCUs)
			cu.preTranslate();
	}

	public ArrayList<CompilationUnit> translate()
	{
		ArrayList<CompilationUnit> out = new ArrayList<CompilationUnit>();

		for (ECompilationUnit cu : this.originalCUs)
			cu.translate(out);

		return out;
	}
}