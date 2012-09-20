package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EInfixExpression extends EExpression
{
	protected final EExpression left;
	protected final EExpression right;
	protected final ArrayList<EExpression> extended;
	protected final Operator operator;
	
	protected final DataGroup datagroup;
	
	public EInfixExpression(EAST east, InfixExpression original, EASTDataNode scope, EInfixExpression base)
	{
		super(east, original, scope, base);
		
		this.operator = this.getOriginal().getOperator();
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("infix " + this.operator));

		this.left = EExpression.create(this.east, original.getLeftOperand(), scope, base == null ? null : base.left);
		this.right = EExpression.create(this.east, original.getRightOperand(), scope, base == null ? null : base.right);
		
		this.extended = new ArrayList<EExpression>();

		if (original.extendedOperands() != null)
		{
			for (int i = 0; i < original.extendedOperands().size(); i++)
			{
				this.extended.add
				(
					EExpression.create
					(
						this.east,
						(Expression) original.extendedOperands().get(i),
						scope,
						base == null ? null : base.extended.get(i)
					)
				);
			}
		}
	}

	/* factory */
	public static EInfixExpression create(EAST east, InfixExpression original, EASTDataNode scope, EInfixExpression base)
	{
		return new EInfixExpression(east, original, scope, base);
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
		this.left.checkSignatures();
		this.right.checkSignatures();
		
		for (EExpression ext : this.extended)
			ext.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.left.getDataGroup()));
		this.signature.addItem(new SignatureItemRead(this.right.getDataGroup()));
		this.signature.addItem(new SignatureItemWrite(this.datagroup));
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
	
		sig.addAll(this.signature);
		sig.addAll(this.left.getFullSignature());
		sig.addAll(this.right.getFullSignature());

		for (EExpression ext : this.extended)
			sig.addAll(ext.getFullSignature());
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.left.checkDependencies(stack);
		this.strongDependencies.add(this.left);
		
		this.right.checkDependencies(stack);
		this.strongDependencies.add(this.right);
		
		for (EExpression ext : this.extended)
		{
			ext.checkDependencies(stack);
			this.strongDependencies.add(ext);
		}
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			if (!this.left.equals(node) && !this.right.equals(node) && !this.extended.contains(node))
				this.weakDependencies.add(node);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;
		
		sum += this.left.optimize();
		sum += this.right.optimize();
		
		for (EExpression ext : this.extended)
			sum += ext.optimize();
		
		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "infix", this.base == null ? null : this.base.task);
		
		this.left.preTranslate(this.task);
		this.right.preTranslate(this.task);
		
		for (int i = 0; i < this.extended.size(); i++)
			this.extended.get(i).preTranslate(this.task);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		InfixExpression infix = ast.newInfixExpression();
		infix.setLeftOperand(this.left.translate(out));
		infix.setRightOperand(this.right.translate(out));
		infix.setOperator(this.getOriginal().getOperator());

		for (EExpression ext: this.extended)
			infix.extendedOperands().add(ext.translate(out));

		return infix;
	}
}
