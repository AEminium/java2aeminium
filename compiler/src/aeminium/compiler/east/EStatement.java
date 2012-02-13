package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

public abstract class EStatement extends EASTExecutableNode
{
	protected final EASTDataNode scope;
	protected final EMethodDeclaration method;
	
	public EStatement(EAST east, Statement original, EASTDataNode scope, EMethodDeclaration method)
	{
		super(east, original);

		this.scope = scope;
		this.method = method;
	}

	public static EStatement create(EAST east, Statement stmt, EASTDataNode scope, EMethodDeclaration method)
	{
		if (stmt instanceof Block)
			return EBlock.create(east, (Block) stmt, scope, method);
		
		if (stmt instanceof VariableDeclarationStatement)
			return EVariableDeclarationStatement.create(east, (VariableDeclarationStatement) stmt, scope, method);
		
		if (stmt instanceof ReturnStatement)
			return EReturnStatement.create(east, (ReturnStatement) stmt, scope, method);
		
		if (stmt instanceof IfStatement)
			return EIfStatement.create(east, (IfStatement) stmt, scope, method);

		System.err.println("Not implemented error: " + stmt.getClass().getName());

		return null;
	}

	@SuppressWarnings("unchecked")
	public List<Statement> translate(List<CompilationUnit> out)
	{
		if (this.inlineTask)
			return this.build(out);
		
		out.add(this.task.translate());
		this.task.getExecute().getBody().statements().addAll(this.build(out));
		
		AST ast = this.getAST();
		
		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName("ae_" + this.task.getName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());

		return Arrays.asList((Statement) ast.newExpressionStatement(assign));
	}
	
	public abstract List<Statement> build(List<CompilationUnit> out);
}
