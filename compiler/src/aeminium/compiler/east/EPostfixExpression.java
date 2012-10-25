package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.PostfixExpression.Operator;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EPostfixExpression extends EExpression
{
	protected final EExpression expr;
	protected final Operator operator;
	
	protected final DataGroup datagroup;
	
	public EPostfixExpression(EAST east, PostfixExpression original, EASTDataNode scope, EASTExecutableNode parent, EPostfixExpression base)
	{
		super(east, original, scope, parent, base);
		
		this.operator = this.getOriginal().getOperator();
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("postfix " + this.operator));

		this.expr = EExpression.create(this.east, original.getOperand(), scope, this, base == null ? null : base.expr);
	}

	/* factory */
	public static EPostfixExpression create(EAST east, PostfixExpression original, EASTDataNode scope, EASTExecutableNode parent, EPostfixExpression base)
	{
		return new EPostfixExpression(east, original, scope, parent, base);
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
		
		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));

		if (this.operator == Operator.DECREMENT || this.operator == Operator.INCREMENT)
			this.signature.addItem(new SignatureItemWrite(this.expr.getDataGroup()));
		
		this.signature.addItem(new SignatureItemWrite(this.datagroup));
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
		this.addStrongDependency(this.expr);
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
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
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "postfix", this.base == null ? null : this.base.task);
		
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
	
	@Override
	public boolean isSimpleTask()
	{
		return EASTExecutableNode.HARD_AGGREGATION;
	}
}