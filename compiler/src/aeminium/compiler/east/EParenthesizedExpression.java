package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EParenthesizedExpression extends EExpression
{
	protected final EExpression expr;
	
	protected final DataGroup datagroup;
	
	public EParenthesizedExpression(EAST east, ParenthesizedExpression original, EASTDataNode scope, EASTExecutableNode parent, EParenthesizedExpression base)
	{
		super(east, original, scope, parent, base);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("paren"));

		this.expr = EExpression.create(this.east, original.getExpression(), scope, this, base == null ? null : base.expr);
	}

	/* factory */
	public static EParenthesizedExpression create(EAST east, ParenthesizedExpression original, EASTDataNode scope, EASTExecutableNode parent, EParenthesizedExpression base)
	{
		return new EParenthesizedExpression(east, original, scope, parent, base);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public InfixExpression getOriginal()
	{
		return (InfixExpression) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		this.expr.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
		this.signature.addItem(new SignatureItemWrite(this.datagroup));		
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
			this.task = parent.newSubTask(this, "paren", this.base == null ? null : this.base.task);
		
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
	
	@Override
	public boolean isSimpleTask()
	{
		return EASTExecutableNode.HARD_AGGREGATION;
	}
}
