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
	
	public EPrefixExpression(EAST east, PrefixExpression original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.operator = this.getOriginal().getOperator();
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("infix " + this.operator));

		this.expr = EExpression.create(this.east, original.getOperand(), scope);
	}

	/* factory */
	public static EPrefixExpression create(EAST east, PrefixExpression original, EASTDataNode scope)
	{
		return new EPrefixExpression(east, original, scope);
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
		int sum = super.optimize();
		
		sum += this.expr.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "prefix");
		
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