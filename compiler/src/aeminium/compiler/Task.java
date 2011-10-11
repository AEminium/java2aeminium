package aeminium.compiler;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.*;
import aeminium.compiler.east.*;

public class Task
{
	EAST east;
	public Task parent;
	CompilationUnit cu;
	TypeDeclaration type;
	MethodDeclaration execute;

	String name;
	int subtasks;
	int task_id;

	public Task(EAST east, String name, CompilationUnit original, List<CompilationUnit> cus)
	{
		this.east = east;
		this.parent = null;
		this.task_id = -1;
		this.name = name;
		this.subtasks = 0;

		this.build(original, cus);
	}

	private Task(EAST east, Task parent, int task_id, CompilationUnit original, List<CompilationUnit> cus)
	{
		this.east = east;
		this.parent = parent;
		this.task_id = task_id;
		this.name = parent.getName() + "_" + task_id;
		this.subtasks = 0;

		this.build(original, cus);
	}

	private void build(CompilationUnit original, List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		this.cu = ast.newCompilationUnit();
		this.cu.setPackage((PackageDeclaration) ASTNode.copySubtree(ast, original.getPackage()));
		this.cu.imports().addAll(ASTNode.copySubtrees(ast, original.imports()));

		this.type = ast.newTypeDeclaration();
		this.type.setName(ast.newSimpleName(name));
		this.type.superInterfaceTypes().add(ast.newSimpleType(ast.newName("aeminium.runtime.Body")));

		this.cu.types().add(this.type);

		cus.add(this.cu);
	}

	public String getName()
	{
		return this.name;
	}

	public String getBodyName()
	{
		if (this.parent == null)
			return "_body";

		return this.parent.getBodyName() + "_" + this.task_id;
	}

	public String getTaskName()
	{
		if (this.parent == null)
			return "_task";	

		return this.parent.getTaskName() + "_" + this.task_id;
	}

	public FieldAccess getBodyAccess()
	{
		AST ast = this.east.getAST();

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName(this.getBodyName()));
		
		return access;
	}

	public FieldAccess getTaskAccess()
	{
		AST ast = this.east.getAST();

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName(this.getTaskName()));

		return access;
	}

	public CompilationUnit getCompilationUnit()
	{
		return this.cu;
	}

	public MethodDeclaration createDefaultConstructor(List<Task> children)
	{
		AST ast = this.east.getAST();

		MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.setName(ast.newSimpleName(this.name));
		constructor.setConstructor(true);

		// add _parent parameter
		SingleVariableDeclaration param = ast.newSingleVariableDeclaration();
		param.setType(ast.newSimpleType(ast.newSimpleName(parent.getName())));
		param.setName(ast.newSimpleName("_parent"));

		constructor.parameters().add(param);

		Block constructor_body = ast.newBlock();

			// add _parent field;
			this.addField(ast.newSimpleType(ast.newSimpleName(this.parent.getName())),"_parent");
		
			// this._parent = _parent
			Assignment asgn = ast.newAssignment();

			FieldAccess access = ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(ast.newSimpleName("_parent"));

			asgn.setLeftHandSide(access);
			asgn.setRightHandSide(ast.newSimpleName("_parent"));

			constructor_body.statements().add(ast.newExpressionStatement(asgn));

			// add child bodies
			for (Task child : children)
			{
				SimpleType type = ast.newSimpleType(ast.newSimpleName(child.getName()));
				String name = child.getBodyAccess().getName().toString();

				// add _body_X parameter
				SingleVariableDeclaration child_param = ast.newSingleVariableDeclaration();
				child_param.setType((SimpleType) ASTNode.copySubtree(ast, type));
				child_param.setName(ast.newSimpleName(name));

				constructor.parameters().add(child_param);

				this.addField(type, name);

				// this._body_X = _body_X
				Assignment child_asgn = ast.newAssignment();

				FieldAccess child_access = ast.newFieldAccess();
				child_access.setExpression(ast.newThisExpression());
				child_access.setName(ast.newSimpleName(name));

				child_asgn.setLeftHandSide(child_access);
				child_asgn.setRightHandSide(ast.newSimpleName(name));

				constructor_body.statements().add(ast.newExpressionStatement(child_asgn));
			}
			
		constructor.setBody(constructor_body);

		return constructor;
	}

	public void addConstructor(MethodDeclaration constructor)
	{
		this.type.bodyDeclarations().add(constructor);
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

		type.bodyDeclarations().add(this.execute);
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

		this.type.bodyDeclarations().add(field);
	}

	public List<Statement> schedule(Task parent, List<Expression> arguments, List<Expression> dependencies)
	{
		assert(this.task_id != -1);

		List<Statement> stmts = new ArrayList<Statement>();
		AST ast = this.east.getAST();

		parent.addField(ast.newSimpleType(ast.newSimpleName(this.getName())), this.getBodyName());
		parent.addField(ast.newSimpleType(ast.newName("aeminium.runtime.Task")), this.getTaskName());

		/* this._body_1 = new AE_XX_1(); */
		Assignment body_assign = ast.newAssignment();

		FieldAccess body_access = ast.newFieldAccess();
		body_access.setExpression(ast.newThisExpression());
		body_access.setName(ast.newSimpleName("_body_" + this.task_id)); 

		body_assign.setLeftHandSide(body_access);

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newSimpleName(this.getName())));
		creation.arguments().addAll(arguments);

		body_assign.setRightHandSide(creation);

		stmts.add(ast.newExpressionStatement(body_assign));

		/* ArrayList<Task> _deps_1= new ArrayList<Task>(); */
		VariableDeclarationFragment var_frag = ast.newVariableDeclarationFragment();
		var_frag.setName(ast.newSimpleName("_deps_" + this.task_id));
		
		ParameterizedType var_type = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("ArrayList")));
		var_type.typeArguments().add(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));

		ClassInstanceCreation var_creation = ast.newClassInstanceCreation();
		var_creation.setType(var_type);

		var_frag.setInitializer(var_creation);

		VariableDeclarationStatement var_stmt = ast.newVariableDeclarationStatement(var_frag);
		var_stmt.setType((Type) ASTNode.copySubtree(ast, var_type));

		stmts.add(var_stmt);

		/* add dependencies */
		for (Expression dep : dependencies)
		{
			MethodInvocation dep_invocation = ast.newMethodInvocation();
			dep_invocation.setExpression(ast.newSimpleName("_deps_"+this.task_id));
			dep_invocation.setName(ast.newSimpleName("add"));
			
			dep_invocation.arguments().add(dep);

			stmts.add(ast.newExpressionStatement(dep_invocation));
		}

		/* this._task_1 = AeminumHelper.createNonBlockingTask(this.body_1, NO_HINTS) */
		Assignment task_assign = ast.newAssignment();
	
		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName(this.getTaskName()));

		task_assign.setLeftHandSide(task_access);

		MethodInvocation task_creation = ast.newMethodInvocation();
		task_creation.setExpression(ast.newSimpleName("AeminiumHelper"));
		task_creation.setName(ast.newSimpleName("createNonBlockingTask"));
		
		task_creation.arguments().add((Expression) ASTNode.copySubtree(ast, body_access));

		FieldAccess no_hints = ast.newFieldAccess();
		no_hints.setExpression(ast.newSimpleName("AeminiumHelper"));
		no_hints.setName(ast.newSimpleName("NO_HINTS"));
		
		task_creation.arguments().add(no_hints);
		
		task_assign.setRightHandSide(task_creation);

		stmts.add(ast.newExpressionStatement(task_assign));

		return stmts;
	}

	public List<Statement> scheduleSubtask(Task child, List<Expression> arguments, List<Expression> dependencies)
	{
		assert(child.task_id == -1);

		child.task_id = ++this.subtasks;
		List<Statement> stmts = child.schedule(this, arguments, dependencies);
		child.task_id = -1;

		return stmts;
	}

	public Task newSubtask(List<CompilationUnit> cus)
	{
		return new Task(this.east, this, ++this.subtasks, this.cu, cus);
	}
}
