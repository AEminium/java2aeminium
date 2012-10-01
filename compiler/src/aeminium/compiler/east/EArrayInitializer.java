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
	
	public EArrayInitializer(EAST east, ArrayInitializer original, EASTDataNode scope, EArrayInitializer base)
	{
		super(east, original, scope, base);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("init"));

		this.exprs = new ArrayList<EExpression>();

		for (int i = 0; i < original.expressions().size(); i++)
		{
			this.exprs.add
			(
				EExpression.create
				(
					east,
					(Expression) original.expressions().get(i),
					scope,
					base == null ? base : base.exprs.get(i)
				)
			);
		}
	}

	/* factory */
	public static EArrayInitializer create(EAST east, ArrayInitializer original, EASTDataNode scope, EArrayInitializer base)
	{
		return new EArrayInitializer(east, original, scope, base);
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
			this.addStrongDependency(expr);
		}
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
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
			this.task = parent.newSubTask(this, "init", this.base == null ? null : this.base.task);

		for (int i = 0; i < this.exprs.size(); i++)
			this.exprs.get(i).preTranslate(this.task);
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
