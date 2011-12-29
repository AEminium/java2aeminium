package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Task;
import aeminium.compiler.datagroup.SignatureItemRead;
import aeminium.compiler.datagroup.TemporaryDataGroup;

public class EInfixExpression extends EExpression
{
	private final InfixExpression origin;
	private final EExpression left;
	private final EExpression right;
	private final List<EExpression> extended;

	private Object constant;
	private ITypeBinding binding;
	
	EInfixExpression(EAST east, InfixExpression origin)
	{
		super(east);

		this.origin = origin;

		this.left = this.east.extend(origin.getLeftOperand());
		this.right = this.east.extend(origin.getRightOperand());

		if (origin.hasExtendedOperands())
		{
			this.extended = new ArrayList<EExpression>();
			for (Object ext : origin.extendedOperands())
				this.extended.add(this.east.extend((Expression) ext));	
		} else
			this.extended = null;
	}

	@Override
	public void analyse()
	{
		super.analyse();

		this.left.analyse();
		this.right.analyse();

		if (this.extended != null)
			for (EExpression ext : this.extended)
				ext.analyse();

		// TODO: allow constant resolving
		this.constant = this.origin.resolveConstantExpressionValue();
		this.binding = this.origin.resolveTypeBinding();
		
		this.datagroup = new TemporaryDataGroup(this);
		
		this.signature.addFrom(this.left.getSignature());
		this.signature.add(new SignatureItemRead(this.left.getDataGroup()));
		
		this.signature.addFrom(this.right.getSignature());
		this.signature.add(new SignatureItemRead(this.right.getDataGroup()));
		
		if (this.extended != null)
		{
			for (EExpression ext : this.extended)
			{
				this.signature.addFrom(ext.getSignature());
				this.signature.add(new SignatureItemRead(ext.getDataGroup()));
			}
		}
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
		
		sum += this.left.optimize();
		sum += this.right.optimize();
		
		if (this.extended != null)
			for (EExpression ext : this.extended)
				sum += ext.optimize();
	
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.isRoot())
			this.task = parent.newStrongDependency("infix");
		else
			this.task = parent;
		
		this.left.preTranslate(this.task);
		this.right.preTranslate(this.task);
		
		if (this.extended != null)
			for (EExpression ext : this.extended)
				ext.preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Expression translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(cus);

		cus.add(this.task.getCompilationUnit());
		
		/* in self task */
		this.task.addField(this.east.buildTypeFromBinding(this.binding), "ae_ret", true);

		Block execute = ast.newBlock();

		FieldAccess this_ret = ast.newFieldAccess();
		this_ret.setExpression(ast.newThisExpression());
		this_ret.setName(ast.newSimpleName("ae_ret"));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(this_ret);
		assign.setRightHandSide(this.build(cus));
		execute.statements().add(ast.newExpressionStatement(assign));

		task.setExecute(execute);

		MethodDeclaration constructor = task.createConstructor();
		task.addConstructor(constructor);

		/* in parent task */
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName(this.task.getName()));

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(access);
		ret.setName(ast.newSimpleName("ae_ret"));

		return ret;
	}

	@SuppressWarnings("unchecked")
	public Expression build(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		InfixExpression infix = ast.newInfixExpression();
		infix.setLeftOperand(this.left.translate(cus));
		infix.setRightOperand(this.right.translate(cus));
		infix.setOperator(this.origin.getOperator());

		if (this.extended != null)
			for (EExpression ext: this.extended)
				infix.extendedOperands().add(ext.translate(cus));

		return infix;
	}
}
