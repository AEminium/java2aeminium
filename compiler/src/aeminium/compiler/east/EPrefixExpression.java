package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.PrefixExpression.Operator;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EPrefixExpression extends EExpression
{
	protected final EExpression expr;
	protected final Operator operator;
	
	protected final DataGroup datagroup;
	
	public EPrefixExpression(EAST east, PrefixExpression original, EASTDataNode scope, EPrefixExpression base)
	{
		super(east, original, scope, base);
		
		this.operator = this.getOriginal().getOperator();
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("infix " + this.operator));

		this.expr = EExpression.create(this.east, original.getOperand(), scope, base == null ? null : base.expr);
	}

	/* factory */
	public static EPrefixExpression create(EAST east, PrefixExpression original, EASTDataNode scope, EPrefixExpression base)
	{
		return new EPrefixExpression(east, original, scope, base);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public PrefixExpression getOriginal()
	{
		return (PrefixExpression) this.original;
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
		this.strongDependencies.add(this.expr);
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			if (!this.expr.equals(node))
				this.weakDependencies.add(node);
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
			this.task = parent.newSubTask(this, "prefix", this.base == null ? null : this.base.task);
		
		this.expr.preTranslate(this.task);
	}

	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		PrefixExpression prefix = ast.newPrefixExpression();
		prefix.setOperand(this.expr.translate(out));
		prefix.setOperator(this.getOriginal().getOperator());

		return prefix;
	}
}