package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class EBlock extends EStatement implements EASTDataNode
{
	protected final EASTDataNode scope;
	protected final DataGroup datagroup;
	
	protected final ArrayList<EStatement> stmts;
	
	public EBlock(EAST east, Block original, EASTDataNode scope, EMethodDeclaration method, EBlock base)
	{
		super(east, original, scope, method, base);
		
		this.scope = scope;
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("{}"));
		
		this.stmts = new ArrayList<EStatement>();

		for (int i = 0; i < original.statements().size(); i++)
		{
			this.stmts.add
			(
				EStatement.create
				(
					this.east,
					(Statement) original.statements().get(i),
					this,
					method,
					base == null ? null: base.stmts.get(i)
				)
			);
		}
	}

	/* Factory */
	public static EBlock create(EAST east, Block block, EASTDataNode scope, EMethodDeclaration method, EBlock base)
	{
		return new EBlock(east, block, scope, method, base);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}
	
	@Override
	public ETypeDeclaration getTypeDeclaration()
	{
		return this.scope.getTypeDeclaration();
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
		{
			stmt.checkDependencies(stack);

			this.strongDependencies.add(stmt);
		}
	}

	@Override
	public int optimize()
	{
		int sum = 0;
		
		for (EStatement stmt : this.stmts)
			sum += stmt.optimize();
		
		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "block", this.base == null ? null : this.base.task);
		
		for (int i = 0; i < this.stmts.size(); i++)			
			this.stmts.get(i).preTranslate(this.task);
	}
	
	@Override
	public List<Statement> build(List<CompilationUnit> out)
	{
		ArrayList<Statement> stmts = new ArrayList<Statement>();
		
		for (EStatement stmt : this.stmts)
			stmt.translate(out);

		return stmts;
	}

	@Override
	public EASTDataNode getScope()
	{
		return this.scope;
	}
}
