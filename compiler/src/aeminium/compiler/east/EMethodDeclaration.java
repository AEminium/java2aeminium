package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import aeminium.compiler.east.EAST;

import aeminium.compiler.east.ETypeDeclaration;
import aeminium.compiler.east.EMethodDeclaration;
import aeminium.compiler.east.EBlock;

public class EMethodDeclaration extends EBodyDeclaration
{
	MethodDeclaration origin;
	EBlock body;

	EMethodDeclaration(MethodDeclaration origin)
	{
		this.origin = origin;

		// do something about parameters?

		Block block = origin.getBody();

		assert(block == null);
		this.body = EAST.extend(block);		
	}

	public MethodDeclaration translate(AST ast, List<CompilationUnit> cus)
	{
		if (this.getModifier("@AEminium") != null)
		{
			this.buildClass(ast, cus);

			if (this.isMain())
				return this.buildMain(ast);
		}
				
		return (MethodDeclaration) ASTNode.copySubtree(ast, this.origin);
	}

	public boolean isMain()
	{
		return (this.getModifier("static") != null) && this.origin.getName().toString().equals("main");
	}

	public void buildClass(AST ast, List<CompilationUnit> cus)
	{
		
	}

	public MethodDeclaration buildMain(AST ast)
	{
		MethodDeclaration method = ast.newMethodDeclaration();
		
		method.setName((SimpleName) ASTNode.copySubtree(ast, this.origin.getName()));
		method.parameters().addAll(ASTNode.copySubtrees(ast, this.origin.parameters()));

		Block body = ast.newBlock();

		// AeminiumHelper.init();
		MethodInvocation init = ast.newMethodInvocation();
		init.setExpression(ast.newSimpleName("AeminiumHelper"));
		init.setName(ast.newSimpleName("init"));

		body.statements().add(ast.newExpressionStatement(init));

		// AeminiumHelper.schedule( ... );
		MethodInvocation schedule = ast.newMethodInvocation();
		schedule.setExpression(ast.newSimpleName("AeminiumHelper"));
		schedule.setName(ast.newSimpleName("schedule"));

			// AeminiumHelper.createNonBlockingTask(new AE_HelloWorld_main_body(args), AeminiumHelper.NO_HINTS),
			MethodInvocation create = ast.newMethodInvocation();
			create.setExpression(ast.newSimpleName("AeminiumHelper"));
			create.setName(ast.newSimpleName("createNonBlockingTask"));

			ClassInstanceCreation main_body = ast.newClassInstanceCreation();
			main_body.setType(ast.newSimpleType(ast.newSimpleName(this.bodyName())));

			for (Object param : this.origin.parameters())
			{
				SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
				main_body.arguments().add(ASTNode.copySubtree(ast, parameter.getName()));
			}

			create.arguments().add(main_body);

			FieldAccess no_hints = ast.newFieldAccess();
			no_hints.setExpression(ast.newSimpleName("AeminiumHelper"));
			no_hints.setName(ast.newSimpleName("NO_HINTS"));

			create.arguments().add(no_hints);

		schedule.arguments().add(create);

		FieldAccess no_parent = ast.newFieldAccess();
		no_parent.setExpression(ast.newSimpleName("AeminiumHelper"));
		no_parent.setName(ast.newSimpleName("NO_PARENT"));
		schedule.arguments().add(no_parent);

		FieldAccess no_deps = ast.newFieldAccess();
		no_deps.setExpression(ast.newSimpleName("AeminiumHelper"));
		no_deps.setName(ast.newSimpleName("NO_DEPS"));
		schedule.arguments().add(no_deps);
		
		body.statements().add(ast.newExpressionStatement(schedule));

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

	public String bodyName()
	{
		AbstractTypeDeclaration parent = (AbstractTypeDeclaration) this.origin.getParent();

		return "AE_" + parent.getName().toString() + "_" + this.origin.getName().toString();
	}
}
