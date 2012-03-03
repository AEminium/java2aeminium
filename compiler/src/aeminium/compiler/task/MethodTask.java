package aeminium.compiler.task;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.EMethodDeclaration;
import aeminium.compiler.east.EMethodDeclarationParameter;
import aeminium.compiler.east.EType;

public class MethodTask extends Task
{
	protected MethodTask(EMethodDeclaration node, String name)
	{
		super(node, name, null);
		
	}

	public static MethodTask create(EMethodDeclaration node, String name)
	{
		return new MethodTask(node, name);
	}
	
	public EMethodDeclaration getNode()
	{
		return (EMethodDeclaration) this.node;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive, ArrayList<Task> overrideTasks)
	{
		EMethodDeclaration method = this.getNode();
		AST ast = this.node.getAST();
		
		Type caller_type;
		if (method.isConstructor() || method.isVoid())
			caller_type = ast.newSimpleType(ast.newName("aeminium.runtime.CallerBody"));
		else
		{
			Type returnType = method.getOriginal().getReturnType2();
			this.addField(returnType, "ae_ret", true);

			caller_type = ast.newParameterizedType(ast.newSimpleType(ast.newName("aeminium.runtime.CallerBodyWithReturn")));

			if (returnType instanceof PrimitiveType)
				((ParameterizedType) caller_type).typeArguments().add(EType.boxType((PrimitiveType) returnType));
			else
				((ParameterizedType) caller_type).typeArguments().add(ASTNode.copySubtree(ast, returnType));
		}

		// add ae_parent parameter
		{
			SingleVariableDeclaration param = ast.newSingleVariableDeclaration();

			param.setType((Type) ASTNode.copySubtree(ast, caller_type));
			param.setName(ast.newSimpleName("ae_parent"));

			constructor.parameters().add(param);

			// this.ae_parent = ae_parent
			this.addField((Type) ASTNode.copySubtree(ast, caller_type), "ae_parent", false);

			Assignment asgn = ast.newAssignment();

			FieldAccess access = ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(ast.newSimpleName("ae_parent"));

			asgn.setLeftHandSide(access);
			asgn.setRightHandSide(ast.newSimpleName("ae_parent"));

			body.statements().add(ast.newExpressionStatement(asgn));
		}

		if (!this.getNode().isStatic())
		{
			Type thisType = this.getNode().getThisType();
			this.addField(thisType, "ae_this", false);

			// add ae_this parameter
			SingleVariableDeclaration param = ast.newSingleVariableDeclaration();

			param.setType((Type) ASTNode.copySubtree(ast, thisType));
			param.setName(ast.newSimpleName("ae_this"));

			constructor.parameters().add(param);

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
			constructor.parameters().add((SingleVariableDeclaration) ASTNode.copySubtree(ast, decl));

			// this.x = x
			this.addField((Type) ASTNode.copySubtree(ast, decl.getType()), decl.getName().toString(), false);

			Assignment asgn = ast.newAssignment();

			FieldAccess access = ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName((SimpleName) ASTNode.copySubtree(ast, decl.getName()));

			asgn.setLeftHandSide(access);
			asgn.setRightHandSide((Name) ASTNode.copySubtree(ast, decl.getName()));

			body.statements().add(ast.newExpressionStatement(asgn));
		}
		
		this.addField(ast.newSimpleType(ast.newName("aeminium.runtime.Task")), "ae_task", false);

		super.fillConstructor(constructor, body, recursive, overrideTasks);
	}
}
