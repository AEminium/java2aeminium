package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Assignment.Operator;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemMerge;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SignatureItemWrite;
import aeminium.compiler.task.Task;

public class EAssignment extends EExpression
{
	protected final EExpression left;
	protected final EExpression right;
	protected final Operator operator;

	public EAssignment(EAST east, Assignment original, EASTDataNode scope, EAssignment base)
	{
		super(east, original, scope, base);
		
		this.operator = original.getOperator();

		if (base == null)
		{
			this.left = EExpression.create(east, original.getLeftHandSide(), scope, null);
			this.right = EExpression.create(east, original.getRightHandSide(), scope, null);
		} else
		{
			this.left = EExpression.create(east, original.getLeftHandSide(), scope, base.left);
			this.right = EExpression.create(east, original.getRightHandSide(), scope, base.right);			
		}
	}

	/* factory */
	public static EAssignment create(EAST east, Assignment original, EASTDataNode scope, EAssignment base)
	{
		return new EAssignment(east, original, scope, base);
	}
	
	@Override
	public Assignment getOriginal()
	{
		return (Assignment) this.original;
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.left.getDataGroup();
	}

	@Override
	public void checkSignatures()
	{
		this.left.checkSignatures();
		this.right.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.right.getDataGroup()));
		this.signature.addItem(new SignatureItemWrite(this.left.getDataGroup()));
		this.signature.addItem(new SignatureItemMerge(this.left.getDataGroup(), this.right.getDataGroup()));
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		sig.addAll(this.left.getFullSignature());
		sig.addAll(this.right.getFullSignature());
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.left.checkDependencies(stack);
		this.addStrongDependency(this.left);
		
		this.right.checkDependencies(stack);
		this.addStrongDependency(this.right);
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
	}

	@Override
	public int optimize()
	{
		int sum = 0;

		sum += this.left.optimize();
		sum += this.left.inline(this); /* Always inline the left hand side */
		sum += this.right.optimize();
		
		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "assign", this.base == null ? null : this.base.task);
		
		this.left.preTranslate(this.task);
		this.right.preTranslate(this.task);
	}
	
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();
		
		Assignment assign = ast.newAssignment();
		assign.setOperator(this.operator);
		assign.setLeftHandSide(this.left.translate(out));
		assign.setRightHandSide(this.right.translate(out));
		
		return assign;
	}
}
