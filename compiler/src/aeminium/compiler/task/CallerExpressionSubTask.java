package aeminium.compiler.task;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;

import aeminium.compiler.east.EDeferredExpression;
import aeminium.compiler.east.EExpression;

public class CallerExpressionSubTask extends ExpressionSubTask
{
	protected CallerExpressionSubTask(EExpression node, String name, Task parent)
	{
		super(node, name, parent);
	}

	public static CallerExpressionSubTask create(EExpression node, String name, Task parent)
	{
		return new CallerExpressionSubTask(node, name, parent);
	}
	
	@Override
	public Expression undefer(Expression path)
	{
		AST ast = this.node.getAST();
		
		if (!this.getNode().isAeminium())
			return path;
		
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(path);
		access.setName(ast.newSimpleName("ae_deferred"));
				
		return access;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive, ArrayList<Task> overrideTasks)
	{
		AST ast = this.node.getAST();

		if (this.getNode().isAeminium())
		{
			MethodTask methodTask = this.getNode().getMethod().getTask();

			Type functionType = ast.newSimpleType(ast.newSimpleName(methodTask.getName()));
			this.addField(functionType, "ae_deferred", false);

			Assignment assign = ast.newAssignment();

			FieldAccess function = ast.newFieldAccess();
			function.setExpression(ast.newThisExpression());
			function.setName(ast.newSimpleName("ae_deferred"));
	
			assign.setLeftHandSide(function);
			
			ClassInstanceCreation create = ast.newClassInstanceCreation();
			create.setType(functionType);
			
			assign.setRightHandSide(create);
			
			body.statements().add(ast.newExpressionStatement(assign));
		}
		
		super.fillConstructor(constructor, body, recursive, overrideTasks);
	}
	
	@Override 
	public EDeferredExpression getNode()
	{
		return (EDeferredExpression) this.node;
	}
}
