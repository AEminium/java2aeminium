package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public class EVariableDeclarationStatement extends EStatement implements EASTDataNode
{
	protected final Type dataType;
	protected final ArrayList<EVariableDeclarationFragment> fragments;
	
	protected final DataGroup datagroup;
	
	public EVariableDeclarationStatement(EAST east, VariableDeclarationStatement original, EASTDataNode scope, EMethodDeclaration method)
	{
		super(east, original, scope, method);

		this.datagroup = scope.getDataGroup();
		this.dataType = original.getType();
		
		this.fragments = new ArrayList<EVariableDeclarationFragment>();
		for (Object frag : original.fragments())
			this.fragments.add(EVariableDeclarationFragment.create(this.east, (VariableDeclarationFragment) frag, scope, this.dataType));
	}
	
	/* factory */
	public static EVariableDeclarationStatement create(EAST east, VariableDeclarationStatement stmt, EASTDataNode scope, EMethodDeclaration method)
	{
		return new EVariableDeclarationStatement(east, stmt, scope, method);
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
	public VariableDeclarationStatement getOriginal()
	{
		return (VariableDeclarationStatement) this.original;
	}
		
	@Override
	public void checkSignatures()
	{
		for (EVariableDeclarationFragment frag : this.fragments)
			frag.checkSignatures();
	}
	
	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		
		for (EVariableDeclarationFragment frag : this.fragments)
			sig.addAll(frag.getFullSignature());

		return sig;
	}
	
	@Override
	public void checkDependencies(DependencyStack stack)
	{
		for (EVariableDeclarationFragment frag : this.fragments)
		{
			frag.checkDependencies(stack);
			this.dependency.addStrong(frag.dependency);
		}

		Set<Dependency> deps = stack.getDependencies(this.signature);
		this.dependency.addWeak(deps);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;

		for (EVariableDeclarationFragment frag : this.fragments)
			sum += frag.optimize();

		sum += super.optimize();
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inline)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "varstmt");
		
		for (EVariableDeclarationFragment frag : this.fragments)
			frag.preTranslate(this.task);
	}
	
	@Override
	public List<Statement> build(List<CompilationUnit> out)
	{
		List<Statement> stmts = new ArrayList<Statement>();

		for (EVariableDeclarationFragment frag : this.fragments)
			stmts.addAll(frag.translate(out));

		return stmts;
	}
}
