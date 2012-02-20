package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EConditionalExpression extends EExpression
{
	protected final EExpression expr;
	protected final EExpression thenExpr;
	protected final EExpression elseExpr;

	protected final DataGroup datagroup;

	public EConditionalExpression(EAST east, ConditionalExpression original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("cond"));

		this.expr = EExpression.create(this.east, original.getExpression(), scope);
		this.thenExpr = EExpression.create(this.east, original.getThenExpression(), scope);
		this.elseExpr = EExpression.create(this.east, original.getElseExpression(), scope);
	}

	/* factory */
	public static EConditionalExpression create(EAST east, ConditionalExpression original, EASTDataNode scope)
	{
		return new EConditionalExpression(east, original, scope);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public ConditionalExpression getOriginal()
	{
		return (ConditionalExpression) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		this.expr.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));

		this.signature.addItem(new SignatureItemRead(this.thenExpr.getDataGroup()));
		this.signature.addItem(new SignatureItemMerge(this.datagroup, this.thenExpr.getDataGroup()));

		this.signature.addItem(new SignatureItemRead(this.elseExpr.getDataGroup()));
		this.signature.addItem(new SignatureItemMerge(this.datagroup, this.elseExpr.getDataGroup()));
		
		this.signature.addItem(new SignatureItemWrite(this.datagroup));
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
	
		sig.addAll(this.signature);
		
		sig.addAll(this.expr.getFullSignature());
		sig.addAll(this.thenExpr.getFullSignature());
		sig.addAll(this.elseExpr.getFullSignature());

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.expr.checkDependencies(stack);
		this.strongDependencies.add(this.expr);

		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);

		for (EASTExecutableNode node : deps)
			if (!node.equals(this.expr))
				this.weakDependencies.add(node);

		DependencyStack copy = stack.fork();

		this.thenExpr.checkDependencies(stack);
		this.elseExpr.checkDependencies(copy);

		stack.join(copy, this);

		this.children.add(this.thenExpr);
		this.children.add(this.elseExpr);
	}
	
	@Override
	public int optimize()
	{
		int sum = super.optimize();

		sum += this.expr.optimize();
		sum += this.elseExpr.optimize();
		sum += this.thenExpr.optimize();

		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "cond");

		this.expr.preTranslate(this.task);
	}

	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		PrefixExpression prefix = ast.newPrefixExpression();
/*		prefix.setOperand(this.expr.translate(out));
		prefix.setOperator(this.getOriginal().getOperator());
*/
		return prefix;
	}
}