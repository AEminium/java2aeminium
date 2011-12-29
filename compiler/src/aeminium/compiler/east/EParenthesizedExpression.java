package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.Task;

public class EParenthesizedExpression extends EExpression
{
	private final ParenthesizedExpression origin;
	private final EExpression expr;

	private Object constant;
	private ITypeBinding binding;

	EParenthesizedExpression(EAST east, ParenthesizedExpression origin)
	{
		super(east);

		this.origin = origin;
	
		this.expr = this.east.extend(origin.getExpression());
	}

	@Override
	public void analyse()
	{
		super.analyse();

		// TODO: allow constant resolving
		this.constant = this.origin.resolveConstantExpressionValue();
		this.binding = this.origin.resolveTypeBinding();

		this.expr.analyse();
		
		this.datagroup = this.expr.getDataGroup();
		this.signature.addFrom(this.expr.getSignature());
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
		
		sum += this.expr.optimize();
		
		return sum;
	}
	
	public void preTranslate(Task parent)
	{
		if (this.isRoot())
			this.task = parent.newStrongDependency("paren");
		else
			this.task = parent;
		
		this.expr.preTranslate(this.task);
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

	public Expression build(List<CompilationUnit> cus)
	{
		return this.expr.translate(cus);
	}
}
