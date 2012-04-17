package aeminium.compiler.task;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.EMethodDeclaration;
import aeminium.compiler.east.EMethodDeclarationParameter;

public class MethodTask extends Task
{
	@SuppressWarnings("unchecked")
	protected MethodTask(EMethodDeclaration node, String name)
	{
		super(node, name, null);
		
		AST ast = this.node.getAST();
		MethodDeclaration schedule = ast.newMethodDeclaration();
		schedule.setName(ast.newSimpleName("schedule"));
		
		Block body = ast.newBlock();

		SingleVariableDeclaration parent = ast.newSingleVariableDeclaration();
		parent.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));
		parent.setName(ast.newSimpleName("ae_parent"));
		schedule.parameters().add(parent);
		
		/* Invoker task as deps or NO_PARENT */
		SingleVariableDeclaration deps = ast.newSingleVariableDeclaration();
		ParameterizedType depsType = ast.newParameterizedType(ast.newSimpleType(ast.newName("java.util.Collection")));
		depsType.typeArguments().add(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));

		deps.setType(depsType);
		deps.setName(ast.newSimpleName("ae_deps"));
		
		schedule.parameters().add(deps);
		
		if (!this.getNode().isStatic())
		{
			Type thisType = this.getNode().getThisType();
			this.addField(thisType, "ae_this", true);

			// add ae_this parameter
			SingleVariableDeclaration param = ast.newSingleVariableDeclaration();

			param.setType((Type) ASTNode.copySubtree(ast, thisType));
			param.setName(ast.newSimpleName("ae_this"));

			schedule.parameters().add(param);

			// this.ae_this = ae_this
			Assignment asgn = ast.newAssignment();

			FieldAccess access = ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(ast.newSimpleName("ae_this"));

			asgn.setLeftHandSide(access);
			asgn.setRightHandSide(ast.newSimpleName("ae_this"));

			body.statements().add(ast.newExpressionStatement(asgn));
		}
		
		for (EMethodDeclarationParameter param : this.getNode().getParameters())
		{
			SingleVariableDeclaration decl = param.getOriginal();
			
			// add x parameter
			schedule.parameters().add((SingleVariableDeclaration) ASTNode.copySubtree(ast, decl));

			// this.x = x
			this.addField((Type) ASTNode.copySubtree(ast, decl.getType()), decl.getName().toString(), true);

			Assignment asgn = ast.newAssignment();

			FieldAccess access = ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName((SimpleName) ASTNode.copySubtree(ast, decl.getName()));

			asgn.setLeftHandSide(access);
			asgn.setRightHandSide((Name) ASTNode.copySubtree(ast, decl.getName()));

			body.statements().add(ast.newExpressionStatement(asgn));
		}
		
		MethodInvocation invoke = ast.newMethodInvocation();
		invoke.setExpression(ast.newSimpleName("AeminiumHelper"));
		invoke.setName(ast.newSimpleName("schedule"));
		
		FieldAccess task = ast.newFieldAccess();
		task.setExpression(ast.newThisExpression());
		task.setName(ast.newSimpleName("ae_task"));
		
		invoke.arguments().add(task);
		
		invoke.arguments().add(ast.newSimpleName("ae_parent"));
		invoke.arguments().add(ast.newSimpleName("ae_deps"));

		body.statements().add(ast.newExpressionStatement(invoke));
		
		schedule.setBody(body);

		this.decl.bodyDeclarations().add(schedule);
	}

	public static MethodTask create(EMethodDeclaration node, String name)
	{
		return new MethodTask(node, name);
	}
	
	public EMethodDeclaration getNode()
	{
		return (EMethodDeclaration) this.node;
	}
	
	@Override
	public void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive, ArrayList<Task> overrideTasks)
	{
		EMethodDeclaration method = this.getNode();
		AST ast = this.node.getAST();
		
		if (!method.isConstructor() && !method.isVoid())
		{
			Type returnType = method.getOriginal().getReturnType2();
			this.addField(returnType, "ae_ret", true);
		}
		
		this.addField(ast.newSimpleType(ast.newName("aeminium.runtime.Task")), "ae_task", false);

		super.fillConstructor(constructor, body, recursive, overrideTasks);
	}
}
