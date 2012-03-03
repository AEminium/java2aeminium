package aeminium.compiler.task;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import aeminium.compiler.east.EASTExecutableNode;
import aeminium.compiler.east.EExpression;
import aeminium.compiler.east.EStatement;

public abstract class SubTask extends Task
{
	protected SubTask(EASTExecutableNode node, String name, Task parent)
	{
		super(node, name, parent);
	}
	
	public static SubTask create(EASTExecutableNode node, String name, Task parent)
	{
		if (node instanceof EExpression)
			return ExpressionSubTask.create((EExpression) node, name, parent);
		
		if (node instanceof EStatement)
			return StatementSubTask.create((EStatement) node, name, parent);
		
		System.err.println("TODO: SubTask.create()");
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive, ArrayList<Task> overrideTasks)
	{
		AST ast = this.node.getAST();
		
		// add _parent parameter
		SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
		if (recursive)
			param.setType(ast.newSimpleType(ast.newSimpleName(this.getName())));
		else
			param.setType(ast.newSimpleType(ast.newSimpleName(this.parent.getName())));
		
		param.setName(ast.newSimpleName("ae_parent"));

		constructor.parameters().add(param);

		if (!recursive)
			this.addField(ast.newSimpleType(ast.newName(this.parent.getName())), "ae_parent", false);
		
		Assignment asgn = ast.newAssignment();

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_parent"));

		asgn.setLeftHandSide(access);
		
		if (!recursive)
		{	
			// this.ae_parent = ae_parent
			asgn.setRightHandSide(ast.newSimpleName("ae_parent"));
		} else
		{
			// this.ae_parent = ae_parent.ae_parent
			FieldAccess parent = ast.newFieldAccess();
			parent.setExpression(ast.newSimpleName("ae_parent"));
			parent.setName(ast.newSimpleName("ae_parent"));
			
			asgn.setRightHandSide(parent);
		}
		
		body.statements().add(ast.newExpressionStatement(asgn));
		
		super.fillConstructor(constructor, body, recursive, overrideTasks);
	}
}
