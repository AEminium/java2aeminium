package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.Task;

public class EThisExpression extends EExpression
{
	private final ThisExpression origin;
	ITypeBinding binding;
	
	EThisExpression(EAST east, ThisExpression origin)
	{
		super(east);

		this.origin = origin;
	}

	@Override
	public void analyse()
	{
		super.analyse();

		this.binding = this.origin.resolveTypeBinding();
		
		// TODO/FIXME: this datagroup
	}

	@Override
	public int optimize()
	{
		super.optimize();
		this.root = false;

		return 0;
	}
	
	public void preTranslate(Task parent)
	{
		if (this.isRoot())
			this.task = parent.newStrongDependency("this");
		else
			this.task = parent;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Expression translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

//		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(cus);

		cus.add(this.task.getCompilationUnit());
		
		/* in self task */
		this.task.addField(ast.newSimpleType(ast.newName(this.binding.getQualifiedName())), "ae_ret", true);

		Block execute = ast.newBlock();

		FieldAccess this_ret = ast.newFieldAccess();
		this_ret.setExpression(ast.newThisExpression());
		this_ret.setName(ast.newSimpleName("ae_ret"));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(this_ret);
		assign.setRightHandSide(this.build(cus));
		execute.statements().add(ast.newExpressionStatement(assign));

		this.task.setExecute(execute);

		MethodDeclaration constructor = this.task.createConstructor();
		this.task.addConstructor(constructor);

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
		AST ast = this.east.getAST();

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(this.task.getPathToRoot());
		access.setName(ast.newSimpleName("ae_this"));

		return access;
	}
}
