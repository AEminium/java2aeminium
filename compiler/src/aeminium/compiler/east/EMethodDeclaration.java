package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EMethodDeclaration extends EBodyDeclaration
{
	MethodDeclaration origin;

	EBlock body;
	Task task;
	String name;
	boolean aeminium;

	EMethodDeclaration(EAST east, MethodDeclaration origin)
	{
		super(east);

		this.origin = origin;

		AbstractTypeDeclaration parent = (AbstractTypeDeclaration) this.origin.getParent();
		this.name = parent.getName().toString() + "_" + this.origin.getName().toString();

		if (this.getModifier("@AEminium") != null)
		{
			this.origin.modifiers().remove(this.getModifier("@AEminium"));
			this.aeminium = true;
		} else
			this.aeminium = false;

		// do something about parameters?
		// see optimize(), on a future version the read version is necessary for optimizing loops

		Block block = origin.getBody();
		assert(block != null);

		this.body = this.east.extend(block);
		this.east.putNode(this.east.resolveName(origin.resolveBinding()), this);	
	}

	public void optimize()
	{
		// TODO: calculate if any parameter is read-only:
		// a variable is read-only if there isn't any write operations in the closure of body operations

		this.body.optimize();
	}

	public MethodDeclaration translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		if (this.aeminium)
		{
			this.buildClass(cus);

			if (this.isMain())
				return this.buildMain();
		}
				
		return (MethodDeclaration) ASTNode.copySubtree(ast, this.origin);
	}

	public boolean isMain()
	{
		return (this.getModifier("static") != null) && this.origin.getName().toString().equals("main");
	}

	public void buildClass(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();
		this.task = new Task(this.east, this.name, (CompilationUnit) this.origin.getRoot(), cus);
		this.task.setMethodTask(this.origin.getReturnType2(), this.origin.parameters());

		TypeDeclaration parent = (TypeDeclaration) this.origin.getParent();

		task.setExecute(this.body.build(this.task));

		/* Create the constructor */
		MethodDeclaration constructor = this.task.createConstructor();
		this.task.addConstructor(constructor);
	}

	public MethodDeclaration buildMain()
	{
		AST ast = this.east.getAST();
		MethodDeclaration method = ast.newMethodDeclaration();
		
		method.setName((SimpleName) ASTNode.copySubtree(ast, this.origin.getName()));
		method.parameters().addAll(ASTNode.copySubtrees(ast, this.origin.parameters()));

		method.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		method.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));

		Block body = ast.newBlock();

		// AeminiumHelper.init();
		MethodInvocation init = ast.newMethodInvocation();
		init.setExpression(ast.newSimpleName("AeminiumHelper"));
		init.setName(ast.newSimpleName("init"));

		body.statements().add(ast.newExpressionStatement(init));

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newSimpleName(this.task.getType())));

		creation.arguments().add(ast.newNullLiteral());
		for (Object arg : this.origin.parameters())
			creation.arguments().add((Expression) ASTNode.copySubtree(ast, ((SingleVariableDeclaration) arg).getName()));

		body.statements().add(ast.newExpressionStatement(creation));

		// AeminiumHelper.shutdown();
		MethodInvocation shutdown = ast.newMethodInvocation();
		shutdown.setExpression(ast.newSimpleName("AeminiumHelper"));
		shutdown.setName(ast.newSimpleName("shutdown"));

		body.statements().add(ast.newExpressionStatement(shutdown));

		method.setBody(body);

		return method;
	}

	/**
	 * Gets a modifier from a list by its common name
	 * @param name The common name of the modifier (e.g.: "public", "static", "@AEminium")
	 */
	public IExtendedModifier getModifier(String name)
	{
		for (Object modifier : this.origin.modifiers())
			if (modifier.toString().equals(name))
				return (IExtendedModifier) modifier;

		return null;
	}

	public boolean isAEminium()
	{
		return this.aeminium;
	}
}
