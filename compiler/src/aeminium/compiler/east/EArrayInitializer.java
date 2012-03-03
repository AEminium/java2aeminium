package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemMerge;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SignatureItemWrite;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class EArrayInitializer extends EExpression
{
	protected final DataGroup datagroup;
	protected final ArrayList<EExpression> exprs;
	
	public EArrayInitializer(EAST east, ArrayInitializer original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("init"));

		this.exprs = new ArrayList<EExpression>();
		for (Object expr : original.expressions())
			this.exprs.add(EExpression.create(east, (Expression) expr, scope));
	}

	/* factory */
	public static EArrayInitializer create(EAST east, ArrayInitializer original, EASTDataNode scope)
	{
		return new EArrayInitializer(east, original, scope);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public ArrayInitializer getOriginal()
	{
		return (ArrayInitializer) this.original;
	}

	@Override
	public void checkSignatures()
	{
		for (EExpression expr : this.exprs)
		{
			expr.checkSignatures();

			this.signature.addItem(new SignatureItemRead(expr.getDataGroup()));
			this.signature.addItem(new SignatureItemMerge(this.datagroup, expr.getDataGroup()));
		}
		
		this.signature.addItem(new SignatureItemWrite(this.datagroup));
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);

		for (EExpression expr : this.exprs)
			sig.addAll(expr.getFullSignature());
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		for (EExpression expr : this.exprs)
		{
			expr.checkDependencies(stack);
			this.strongDependencies.add(expr);
		}
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			if (!this.exprs.contains(node))
				this.weakDependencies.add(node);	
	}

	@Override
	public int optimize()
	{
		int sum = 0;
		
		for (EExpression expr : this.exprs)
			sum += expr.optimize();
		
		sum += super.optimize();

		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "init");
		
		for (EExpression expr : this.exprs)
			expr.preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ArrayInitializer build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();
		
		ArrayInitializer init = ast.newArrayInitializer();
		
		for (EExpression expr : this.exprs)
			init.expressions().add(expr.translate(out));
		
		return init;
	}
}
