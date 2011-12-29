package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Task;

public class EVariableDeclarationStatement extends EStatement
{
	private final VariableDeclarationStatement origin;
	private final List<EVariableDeclarationFragment> frags;

	EVariableDeclarationStatement(EAST east, VariableDeclarationStatement origin)
	{
		super(east);

		this.origin = origin;
		this.frags = new ArrayList<EVariableDeclarationFragment>();

		for (Object frag : origin.fragments())
			this.frags.add(this.east.extend((VariableDeclarationFragment) frag));
	}

	@Override
	public void analyse()
	{
		for (EVariableDeclarationFragment frag : this.frags)
			frag.analyse();
		
		for (EVariableDeclarationFragment frag : this.frags)
			this.signature.addFrom(frag.getSignature());
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();

		for (EVariableDeclarationFragment frag : this.frags)
			sum += frag.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.isRoot())
			this.task = parent.newChild("decl");
		else
			this.task = parent;
		
		for (EVariableDeclarationFragment frag : this.frags)
			frag.preTranslate(this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Statement> translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(cus);

		cus.add(this.task.getCompilationUnit());
		
		Block execute = ast.newBlock();
		execute.statements().addAll(this.build(cus));
		this.task.setExecute(execute);

		MethodDeclaration constructor = this.task.createConstructor();
		this.task.addConstructor(constructor);

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName(this.task.getName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());

		return Arrays.asList((Statement) ast.newExpressionStatement(assign));
	}

	public List<Statement> build(List<CompilationUnit> cus)
	{
		List<Statement> stmts = new ArrayList<Statement>();

		for (EVariableDeclarationFragment frag : this.frags)
			stmts.addAll(frag.translate(cus));

		return stmts;
	}

	public Type getType()
	{
		return this.origin.getType();
	}
}
