package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Assignment.Operator;

import aeminium.compiler.Dependency;
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

	public EAssignment(EAST east, Assignment original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.operator = original.getOperator();

		this.left = EExpression.create(east, original.getLeftHandSide(), scope);
		this.right = EExpression.create(east, original.getRightHandSide(), scope);
	}

	/* factory */
	public static EAssignment create(EAST east, Assignment original, EASTDataNode scope)
	{
		return new EAssignment(east, original, scope);
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
		
		this.signature.addItem(new SignatureItemRead(this.dependency, this.right.getDataGroup()));
		this.signature.addItem(new SignatureItemWrite(this.dependency, this.left.getDataGroup()));
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
		this.dependency.addStrong(this.left.dependency);
		
		this.right.checkDependencies(stack);
		this.dependency.addStrong(this.right.dependency);
		
		Set<Dependency> deps = stack.getDependencies(this.signature);
		this.dependency.addWeak(deps);
	}

	@Override
	public int optimize()
	{
		int sum = 0;

		sum += this.left.optimize();
		sum += this.left.inlineTo(this); /* Always inline the left hand side */
		sum += this.right.optimize();
		
		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inline)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "assign");
		
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
