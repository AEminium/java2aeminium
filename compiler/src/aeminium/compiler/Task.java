package aeminium.compiler;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.*;
import aeminium.compiler.east.*;

public class Task
{
	EAST east;

	List<CompilationUnit> cus;

	CompilationUnit cu;
	MethodDeclaration execute;
	TypeDeclaration decl;

	Task parent;
	List<Task> strongDependencies;
	List<Task> weakDependencies;

	String type;
	String name;

	/* only used when this task receives additional params (e.g.: is a method task) */
	boolean isMethod;
	Type returnType;
	List<SingleVariableDeclaration> parameters;

	public Task(EAST east, String type, CompilationUnit original, List<CompilationUnit> cus)
	{
		this.east = east;

		this.parent = null;
		this.strongDependencies = new ArrayList<Task>();
		this.weakDependencies = new ArrayList<Task>();

		this.type = type;
		this.name = "ae";

		this.isMethod = false;
		this.parameters = null;
		this.returnType = null;

		this.build(original, cus);
	}

	public Task newStrongDependency(String suffix)
	{
		Task task = new Task(this.east, this.type + "_" + suffix + this.strongDependencies.size(), this.cu, this.cus);
		this.addStrongDependency(task);
		return task;
	}

	public void addStrongDependency(Task task)
	{
		this.strongDependencies.add(task);
		task.setName(this.name + "_s" + this.strongDependencies.size());
		task.parent = this;
	}

	public void addWeakDependency(Task task)
	{
		this.weakDependencies.add(task);

		task.setName(this.name + "_w" + this.weakDependencies.size());
	}

	private void build(CompilationUnit original, List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		this.cus = cus;

		this.cu = ast.newCompilationUnit();
		this.cu.setPackage((PackageDeclaration) ASTNode.copySubtree(ast, original.getPackage()));
		this.cu.imports().addAll(ASTNode.copySubtrees(ast, original.imports()));

		this.decl = ast.newTypeDeclaration();
		this.decl.setName(ast.newSimpleName(this.type));
		this.decl.superInterfaceTypes().add(ast.newSimpleType(ast.newName("aeminium.runtime.Body")));

		this.cu.types().add(this.decl);

		cus.add(this.cu);
	}

	public void setMethodTask(Type returnType, List<SingleVariableDeclaration> parameters)
	{
		this.isMethod = true;
		this.returnType = returnType;
		this.parameters = parameters;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return this.name;
	}

	public String getType()
	{
		return this.type;
	}

	public CompilationUnit getCompilationUnit()
	{
		return this.cu;
	}

	public Expression getRootBody()
	{
		AST ast = this.east.getAST();

		if (this.parent == null)
			return ast.newThisExpression();

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(this.parent.getRootBody());
		access.setName(ast.newSimpleName("ae_parent"));

		return access;
	}

	public MethodDeclaration createConstructor()
	{
		AST ast = this.east.getAST();

		MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.setName(ast.newSimpleName(this.type));
		constructor.setConstructor(true);

		Block constructor_body = ast.newBlock();

			if (this.parent != null)
			{
				// add _parent parameter
				SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
				param.setType(ast.newSimpleType(ast.newSimpleName(this.parent.getType())));
				param.setName(ast.newSimpleName("ae_parent"));

				constructor.parameters().add(param);

				// this.ae_parent = ae_parent
				this.addField(ast.newSimpleType(ast.newName(this.parent.getType())), "ae_parent");
		
				Assignment asgn = ast.newAssignment();

				FieldAccess access = ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName("ae_parent"));

				asgn.setLeftHandSide(access);
				asgn.setRightHandSide(ast.newSimpleName("ae_parent"));

				constructor_body.statements().add(ast.newExpressionStatement(asgn));
			} else 
			{
				assert (this.isMethod);

				if (!this.returnType.toString().equals("void"))
				{
					ParameterizedType caller_type = ast.newParameterizedType(ast.newSimpleType(ast.newName("aeminium.runtime.CallerBody")));
					caller_type.typeArguments().add((Type) ASTNode.copySubtree(ast, this.returnType));

					this.decl.setSuperclassType(caller_type);

					// add ae_parent parameter
					SingleVariableDeclaration param = ast.newSingleVariableDeclaration();

					param.setType((Type) ASTNode.copySubtree(ast, caller_type));
					param.setName(ast.newSimpleName("ae_parent"));

					constructor.parameters().add(param);

					// this.ae_parent = ae_parent
					this.addField((Type) ASTNode.copySubtree(ast, caller_type), "ae_parent");
		
					Assignment asgn = ast.newAssignment();

					FieldAccess access = ast.newFieldAccess();
					access.setExpression(ast.newThisExpression());
					access.setName(ast.newSimpleName("ae_parent"));

					asgn.setLeftHandSide(access);
					asgn.setRightHandSide(ast.newSimpleName("ae_parent"));

					constructor_body.statements().add(ast.newExpressionStatement(asgn));
				}

				for (SingleVariableDeclaration param : this.parameters)
				{
					// add x parameter
					constructor.parameters().add((SingleVariableDeclaration) ASTNode.copySubtree(ast, param));

					// this.x = x
					this.addField((Type) ASTNode.copySubtree(ast, param.getType()), param.getName().toString());
		
					Assignment asgn = ast.newAssignment();

					FieldAccess access = ast.newFieldAccess();
					access.setExpression(ast.newThisExpression());
					access.setName((SimpleName) ASTNode.copySubtree(ast, param.getName()));

					asgn.setLeftHandSide(access);
					asgn.setRightHandSide((Name) ASTNode.copySubtree(ast, param.getName()));

					constructor_body.statements().add(ast.newExpressionStatement(asgn));
				}
			}

			// this.ae_task = AeminiumHelper.createNonBlockingTask(this, AeminiumHelper.NO_HINTS);
			this.addField(ast.newSimpleType(ast.newName("aeminium.runtime.Task")), "ae_task");

			Assignment task_asgn = ast.newAssignment();
	
			FieldAccess task_access = ast.newFieldAccess();
			task_access.setExpression(ast.newThisExpression());
			task_access.setName(ast.newSimpleName("ae_task"));

			task_asgn.setLeftHandSide(task_access);
			
			MethodInvocation task_create = ast.newMethodInvocation();
			task_create.setExpression(ast.newSimpleName("AeminiumHelper"));
			task_create.setName(ast.newSimpleName("createNonBlockingTask"));

			task_create.arguments().add(ast.newThisExpression());
			task_create.arguments().add(ast.newName("AeminiumHelper.NO_HINTS"));

			task_asgn.setRightHandSide(task_create);

			constructor_body.statements().add(ast.newExpressionStatement(task_asgn));

			// add child bodies
			for (Task dep : this.strongDependencies)
			{
				SimpleType type = ast.newSimpleType(ast.newSimpleName(dep.getType()));
				String name = dep.getName();

				this.addField(type, name);

				// this.ae_1 = new ....()
				Assignment child_asgn = ast.newAssignment();

				FieldAccess child_access = ast.newFieldAccess();
				child_access.setExpression(ast.newThisExpression());
				child_access.setName(ast.newSimpleName(name));

				child_asgn.setLeftHandSide(child_access);
				child_asgn.setRightHandSide(dep.create());

				constructor_body.statements().add(ast.newExpressionStatement(child_asgn));
			}

			// AeminiumHelper.schedule(this.ae_task, AeminiumHelper.NO_PARENT, Arrays.asList(this.ae_1.ae_task, this.ae_2.ae_task) );
			MethodInvocation schedule = ast.newMethodInvocation();
			schedule.setExpression(ast.newSimpleName("AeminiumHelper"));
			schedule.setName(ast.newSimpleName("schedule"));

			schedule.arguments().add((Expression) ASTNode.copySubtree(ast, task_access));
			if (this.parent == null)
				schedule.arguments().add(ast.newName("AeminiumHelper.NO_PARENT"));
			else
			{
				FieldAccess access = ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName("ae_parent"));

				FieldAccess parent_task = ast.newFieldAccess();
				parent_task.setExpression(access);
				parent_task.setName(ast.newSimpleName("ae_task"));

				schedule.arguments().add(parent_task);
			}

			if (this.strongDependencies.size() > 0 || this.weakDependencies.size() > 0)
			{
				MethodInvocation asList = ast.newMethodInvocation();
				asList.setExpression(ast.newName("java.util.Arrays"));
				asList.setName(ast.newSimpleName("asList"));

				for (Task dep : this.strongDependencies)
				{
					FieldAccess dep_access = ast.newFieldAccess();
					dep_access.setExpression(ast.newThisExpression());
					dep_access.setName(ast.newSimpleName(dep.getName()));

					FieldAccess dep_task = ast.newFieldAccess();
					dep_task.setExpression(dep_access);
					dep_task.setName(ast.newSimpleName("ae_task"));

					asList.arguments().add(dep_task);
				}

				for (Task dep : this.weakDependencies)
				{
					System.err.println("TODO: weakDependencies");
				}
				
				schedule.arguments().add(asList);
			} else
				schedule.arguments().add(ast.newName("AeminiumHelper.NO_DEPS"));

			constructor_body.statements().add(ast.newExpressionStatement(schedule));

		constructor.setBody(constructor_body);

		return constructor;
	}

	public ClassInstanceCreation create()
	{
		AST ast = this.east.getAST();

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newSimpleName(this.getType())));

		if (this.parent != null)
			creation.arguments().add(ast.newThisExpression());

		return creation;
	}

	public void addConstructor(MethodDeclaration constructor)
	{
		this.decl.bodyDeclarations().add(constructor);
	}

	public MethodDeclaration getExecute()
	{
		assert(this.execute != null);
		return this.execute;
	}

	public void setExecute(Block body)
	{
		assert(this.execute == null);

		AST ast = this.east.getAST();

		this.execute = ast.newMethodDeclaration();
		this.execute.setName(ast.newSimpleName("execute"));
		this.execute.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		SingleVariableDeclaration runtime = ast.newSingleVariableDeclaration();
		runtime.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Runtime")));
		runtime.setName(ast.newSimpleName("rt"));

		this.execute.parameters().add(runtime);

		SingleVariableDeclaration task = ast.newSingleVariableDeclaration();
		task.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));
		task.setName(ast.newSimpleName("task"));

		this.execute.parameters().add(task);

		this.execute.thrownExceptions().add(ast.newSimpleName("Exception"));
		this.execute.setBody(body);

		this.decl.bodyDeclarations().add(this.execute);
	}

	public void addField(Type type, String name)
	{
		AST ast = this.east.getAST();
		
		VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
		frag.setName(ast.newSimpleName(name));

		FieldDeclaration field = ast.newFieldDeclaration(frag);
		field.setType((Type) ASTNode.copySubtree(ast, type));
		field.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		field.modifiers().add(ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));

		this.decl.bodyDeclarations().add(field);
	}
}
