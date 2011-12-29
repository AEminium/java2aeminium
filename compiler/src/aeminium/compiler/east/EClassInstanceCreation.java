package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.Task;
import aeminium.compiler.datagroup.SignatureItem;
import aeminium.compiler.datagroup.SignatureItemRead;
import aeminium.compiler.datagroup.SignatureItemWrite;
import aeminium.compiler.datagroup.TemporaryDataGroup;

public class EClassInstanceCreation extends EExpression
{
	ClassInstanceCreation origin;
	List<EExpression> args;

	EClassInstanceCreation(EAST east, ClassInstanceCreation origin)
	{
		super(east);

		this.origin = origin;
		this.args = new ArrayList<EExpression>();

		for (Object arg : origin.arguments())
			this.args.add(this.east.extend((Expression) arg));
	}

	@Override
	public void analyse()
	{
		super.analyse();

		for (EExpression arg : this.args)
			arg.analyse();

		// TODO check if the constructor being used has @AEminium
		// if not, this call must be serialized, or at least run a serial version in a task that is paralell.

		this.datagroup = new TemporaryDataGroup(this);
		
		for (EExpression arg : this.args)
		{
			this.signature.addFrom(arg.getSignature());
			this.signature.add(new SignatureItemRead(arg.getDataGroup()));
		}
		
		this.signature.add(new SignatureItemWrite(this.getDataGroup()));
 	}

	@Override
	public void preTranslate(Task parent)
	{
		if (this.isRoot())
			this.task = parent.newStrongDependency("class");
		else
			this.task = parent;
		
		for (EExpression arg : this.args)
			arg.preTranslate(this.task);
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
		this.task.addField((Type) ASTNode.copySubtree(ast, this.origin.getType()), "ae_ret", true);

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

	@SuppressWarnings("unchecked")
	public Expression build(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		ClassInstanceCreation create = ast.newClassInstanceCreation();
		create.setType((Type) ASTNode.copySubtree(ast, this.origin.getType()));

		// TODO calculate constructor read/write operations on parameters and "globals" (statics)
		for (EExpression arg: this.args)
			create.arguments().add(arg.translate(cus));

		return create;
	}
}
