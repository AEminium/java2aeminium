package aeminium.compiler.east;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SimpleDataGroup;

public class EBlock extends EStatement implements EASTDataNode
{	
	protected final DataGroup datagroup;
	
	protected final ArrayList<EStatement> stmts;
	
	public EBlock(EAST east, Block original, EASTDataNode scope, EMethodDeclaration method)
	{
		super(east, original, scope, method);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("{}"));
		
		this.stmts = new ArrayList<EStatement>();
		for (Object stmt : original.statements())
			this.stmts.add(EStatement.create(this.east, (Statement) stmt, this, method));
	}

	/* Factory */
	public static EBlock create(EAST east, Block block, EASTDataNode scope, EMethodDeclaration method)
	{
		return new EBlock(east, block, scope, method);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public Block getOriginal()
	{
		return (Block) this.original;
	}

	@Override
	public void checkSignatures()
	{
		for (EStatement stmt : this.stmts)
			stmt.checkSignatures();
	}
	
	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		
		for (EStatement stmt : this.stmts)
			sig.addAll(stmt.getFullSignature());

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		for (EStatement stmt : this.stmts)
			stmt.checkDependencies(stack);
		
		// TODO/FIXME: add the stmts to the children?
		// Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
		
		for (EStatement stmt : this.stmts)
			sum += stmt.optimize();
		
		return sum;
	}
}
