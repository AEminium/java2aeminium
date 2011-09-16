package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.east.EAST;

import aeminium.compiler.east.ETypeDeclaration;
import aeminium.compiler.east.EMethodDeclaration;
import aeminium.compiler.east.EBlock;

public class EMethodDeclaration extends EBodyDeclaration
{
	EAST east;
	MethodDeclaration origin;

	EBlock body;

	TypeDeclaration task;
	int subtasks;

	EMethodDeclaration(EAST east, MethodDeclaration origin)
	{
		this.east = east;
		this.origin = origin;
		this.subtasks = 0;

		// do something about parameters?
		// see optimize(), on a future versio the read version is necessary for optimizing loops

		Block block = origin.getBody();

		assert(block == null);
		this.body = this.east.extend(block);		
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

		if (this.getModifier("@AEminium") != null)
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

		/* Create the class definition */
		CompilationUnit cu = ast.newCompilationUnit();
		CompilationUnit cu_original = (CompilationUnit) this.origin.getRoot();
		TypeDeclaration parent = (TypeDeclaration) this.origin.getParent();

		cu.setPackage((PackageDeclaration) ASTNode.copySubtree(ast, cu_original.getPackage()));
		cu.imports().addAll(ASTNode.copySubtrees(ast, cu_original.imports()));

		this.task = ast.newTypeDeclaration();

		cu.types().add(this.task);
	
		this.task.setName(ast.newSimpleName(this.bodyName()));
		this.task.superInterfaceTypes().add(ast.newSimpleType(ast.newName("aeminium.runtime.Body")));
		
		/* Create the constructor */
		MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.setName(ast.newSimpleName(this.bodyName()));
		constructor.setConstructor(true);

		Block constructor_body = ast.newBlock();

		if (this.getModifier("static") == null)
		{
			// add _this to parameter list
			SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
			param.setType(ast.newSimpleType(ast.newSimpleName(parent.getName().toString())));
			param.setName(ast.newSimpleName("_this"));

			constructor.parameters().add(param);
		}

		/* Create the return placeholder */
		if (!this.origin.getReturnType2().toString().equals("void"))
		{		
			VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
			frag.setName(ast.newSimpleName("_ret"));

			FieldDeclaration field = ast.newFieldDeclaration(frag);
			field.setType((Type) ASTNode.copySubtree(ast, this.origin.getReturnType2()));
			field.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			field.modifiers().add(ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));

			this.task.bodyDeclarations().add(field);

			// add _root field;
			VariableDeclarationFragment root = ast.newVariableDeclarationFragment();
			root.setName(ast.newSimpleName("_root"));

			FieldDeclaration rootfield = ast.newFieldDeclaration(root);
			rootfield.setType(ast.newSimpleType(ast.newSimpleName(this.bodyName())));

			this.task.bodyDeclarations().add(rootfield);

			// this._root = this
			Assignment asgn = ast.newAssignment();

			FieldAccess access = ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(ast.newSimpleName("_root"));

			asgn.setLeftHandSide(access);
			asgn.setRightHandSide(ast.newThisExpression());

			constructor_body.statements().add(ast.newExpressionStatement(asgn));
		}

		/* add parameters */
		constructor.parameters().addAll(ASTNode.copySubtrees(ast, this.origin.parameters()));

		for (Object param : constructor.parameters())
		{
			SingleVariableDeclaration parameter = (SingleVariableDeclaration) param;
			
			// add field
			VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
			frag.setName((SimpleName) ASTNode.copySubtree(ast, parameter.getName()));

			FieldDeclaration field = ast.newFieldDeclaration(frag);
			field.setType((Type) ASTNode.copySubtree(ast, parameter.getType()));
			
			this.task.bodyDeclarations().add(field);

			// add assigment "this.field = field"
			Assignment asgn = ast.newAssignment();
			
			FieldAccess left = ast.newFieldAccess();
			left.setExpression(ast.newThisExpression());
			left.setName((SimpleName) ASTNode.copySubtree(ast, parameter.getName()));

			asgn.setLeftHandSide(left);
			asgn.setRightHandSide((SimpleName) ASTNode.copySubtree(ast, parameter.getName()));
			
			ExpressionStatement stmt = ast.newExpressionStatement(asgn);

			constructor_body.statements().add(stmt);
		}

		constructor.setBody(constructor_body);
		this.task.bodyDeclarations().add(constructor);

		/* Create the execute() method */

		// public void execute(aeminium.runtime.Runtime rt, aeminium.runtime.Task task) throws Exception
		MethodDeclaration execute = ast.newMethodDeclaration();
		execute.setName(ast.newSimpleName("execute"));
		execute.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		SingleVariableDeclaration runtime = ast.newSingleVariableDeclaration();
		runtime.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Runtime")));
		runtime.setName(ast.newSimpleName("rt"));

		execute.parameters().add(runtime);

		SingleVariableDeclaration task = ast.newSingleVariableDeclaration();
		task.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));
		task.setName(ast.newSimpleName("task"));

		execute.parameters().add(task);

		execute.thrownExceptions().add(ast.newSimpleName("Exception"));

		execute.setBody(this.body.build(this, cus));

		this.task.bodyDeclarations().add(execute);
		
		cus.add(cu);	
	}

	public MethodDeclaration buildMain()
	{
		AST ast = this.east.getAST();
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

	public TypeDeclaration getTaskBody()
	{
		return this.task;
	}

	public TypeDeclaration newSubTaskBody(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();
		CompilationUnit cu_original = (CompilationUnit) this.origin.getRoot();
		String taskname = this.bodyName() + "_" + this.subtasks;

		CompilationUnit cu = ast.newCompilationUnit();

		cu.setPackage((PackageDeclaration) ASTNode.copySubtree(ast, cu_original.getPackage()));
		cu.imports().addAll(ASTNode.copySubtrees(ast, cu_original.imports()));

		TypeDeclaration decl = ast.newTypeDeclaration();
		decl.setName(ast.newSimpleName(taskname));

		decl.superInterfaceTypes().add(ast.newSimpleType(ast.newName("aeminium.runtime.Body")));
	
		// constructor
		{
			MethodDeclaration constructor = ast.newMethodDeclaration();
			constructor.setName(ast.newSimpleName(taskname));
			constructor.setConstructor(true);

			// add _root parameter

			SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
			param.setType(ast.newSimpleType(ast.newSimpleName(this.bodyName())));
			param.setName(ast.newSimpleName("_root"));

			constructor.parameters().add(param);

			Block constructor_body = ast.newBlock();

				// add _root field;
				VariableDeclarationFragment root = ast.newVariableDeclarationFragment();
				root.setName(ast.newSimpleName("_root"));

				FieldDeclaration rootfield = ast.newFieldDeclaration(root);
				rootfield.setType(ast.newSimpleType(ast.newSimpleName(this.bodyName())));

				decl.bodyDeclarations().add(rootfield);

				// this._root = _root
				Assignment asgn = ast.newAssignment();

				FieldAccess access = ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName("_root"));

				asgn.setLeftHandSide(access);
				asgn.setRightHandSide(ast.newSimpleName("_root"));

				constructor_body.statements().add(ast.newExpressionStatement(asgn));
			constructor.setBody(constructor_body);

			decl.bodyDeclarations().add(constructor);
		}

		// execute



		cu.types().add(decl);
		cus.add(cu);

		this.subtasks++;

		return decl;
	}
}
