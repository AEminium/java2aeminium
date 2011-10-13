package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EVariableDeclarationStatement extends EStatement
{
	VariableDeclarationStatement origin;
	List<EVariableDeclarationFragment> frags;

	EVariableDeclarationStatement(EAST east, VariableDeclarationStatement origin)
	{
		super(east);

		this.origin = origin;
		this.frags = new ArrayList<EVariableDeclarationFragment>();

		for (Object frag : origin.fragments())
			this.frags.add(this.east.extend((VariableDeclarationFragment) frag));
	}

	@Override
	public void optimize()
	{
		for (EVariableDeclarationFragment frag : this.frags)
			frag.optimize();
	}

	@Override
	public List<Statement> translate(Task parent)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(parent);

		this.task = parent.newChild("decl");

		Block execute = ast.newBlock();
		execute.statements().addAll(this.build(task));
		task.setExecute(execute);

		MethodDeclaration constructor = task.createConstructor();
		task.addConstructor(constructor);

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName(this.task.getName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());

		return Arrays.asList((Statement) ast.newExpressionStatement(assign));
	}

	public List<Statement> build(Task task)
	{
		List<Statement> stmts = new ArrayList<Statement>();

		for (EVariableDeclarationFragment frag : this.frags)
			stmts.addAll(frag.translate(task, this.origin.getType()));

		return stmts;
	}
}
