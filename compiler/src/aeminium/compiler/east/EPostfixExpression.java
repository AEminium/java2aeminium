package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.PostfixExpression.Operator;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EPostfixExpression extends EExpression
{
	protected final EExpression expr;
	protected final Operator operator;
	
	protected final DataGroup datagroup;
	
	public EPostfixExpression(EAST east, PostfixExpression original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.operator = this.getOriginal().getOperator();
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("infix " + this.operator));

		this.expr = EExpression.create(this.east, original.getOperand(), scope);
	}

	/* factory */
	public static EPostfixExpression create(EAST east, PostfixExpression original, EASTDataNode scope)
	{
		return new EPostfixExpression(east, original, scope);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public PostfixExpression getOriginal()
	{
		return (PostfixExpression) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		this.expr.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.dependency, this.expr.getDataGroup()));

		if (this.operator == Operator.DECREMENT || this.operator == Operator.INCREMENT)
			this.signature.addItem(new SignatureItemWrite(this.dependency, this.expr.getDataGroup()));
		
		this.signature.addItem(new SignatureItemWrite(this.dependency, this.datagroup));
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
			this.task = parent.newSubTask(this, "postfix");

		this.expr.preTranslate(this.task);
	}

	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		PostfixExpression postfix = ast.newPostfixExpression();
		postfix.setOperand(this.expr.translate(out));
		postfix.setOperator(this.getOriginal().getOperator());

		return postfix;
	}
}