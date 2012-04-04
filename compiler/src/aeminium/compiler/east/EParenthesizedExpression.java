package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EParenthesizedExpression extends EExpression
{
	protected final EExpression expr;
	
	protected final DataGroup datagroup;
	
	public EParenthesizedExpression(EAST east, ParenthesizedExpression original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("paren"));

		this.expr = EExpression.create(this.east, original.getExpression(), scope);
	}

	/* factory */
	public static EParenthesizedExpression create(EAST east, ParenthesizedExpression original, EASTDataNode scope)
	{
		return new EParenthesizedExpression(east, original, scope);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public ParenthesizedExpression getOriginal()
	{
		return (ParenthesizedExpression) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		this.expr.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.dependency, this.expr.getDataGroup()));
		this.signature.addItem(new SignatureItemWrite(this.dependency, this.datagroup));		
		this.signature.addItem(new SignatureItemMerge(this.datagroup, this.expr.getDataGroup()));
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
	
		sig.addAll(this.signature);
		sig.addAll(this.expr.getFullSignature());
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.expr.checkDependencies(stack);
		this.dependency.addStrong(this.expr.dependency);
		
		Set<Dependency> deps = stack.getDependencies(this.signature);
		this.dependency.addWeak(deps);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;
		
		sum += this.expr.optimize();
		sum += super.optimize();

		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inline)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "paren");
		
		this.expr.preTranslate(this.task);
	}

	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		ParenthesizedExpression paren = ast.newParenthesizedExpression();
		paren.setExpression(this.expr.translate(out));
		
		return paren;
	}
}
