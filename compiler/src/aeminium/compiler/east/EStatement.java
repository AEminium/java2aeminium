package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

public abstract class EStatement extends EASTExecutableNode {
	protected final EASTDataNode scope;
	protected final EMethodDeclaration method;

	public EStatement(EAST east, Statement original, EASTDataNode scope,
			EMethodDeclaration method, EASTExecutableNode parent,
			EStatement base) {
		super(east, original, parent, base);

		this.scope = scope;
		this.method = method;
	}

	public static EStatement create(EAST east, Statement stmt,
			EASTDataNode scope, EMethodDeclaration method,
			EASTExecutableNode parent, EStatement base) {
		if (stmt instanceof Block)
			return EBlock.create(east, (Block) stmt, scope, method, parent,
					(EBlock) base);

		if (stmt instanceof VariableDeclarationStatement)
			return EVariableDeclarationStatement.create(east,
					(VariableDeclarationStatement) stmt, scope, method, parent,
					(EVariableDeclarationStatement) base);

		if (stmt instanceof ReturnStatement)
			return EReturnStatement.create(east, (ReturnStatement) stmt, scope,
					method, parent, (EReturnStatement) base);

		if (stmt instanceof IfStatement)
			return EIfStatement.create(east, (IfStatement) stmt, scope, method,
					parent, (EIfStatement) base);

		if (stmt instanceof ExpressionStatement)
			return EExpressionStatement.create(east,
					(ExpressionStatement) stmt, scope, method, parent,
					(EExpressionStatement) base);

		if (stmt instanceof WhileStatement)
			return EWhileStatement.create(east, (WhileStatement) stmt, scope,
					method, parent, (EWhileStatement) base);

		if (stmt instanceof BreakStatement)
			return EBreakStatement.create(east, (BreakStatement) stmt, scope,
					method, parent, (EBreakStatement) base);

		if (stmt instanceof ContinueStatement)
			return EContinueStatement.create(east, (ContinueStatement) stmt,
					scope, method, parent, (EContinueStatement) base);
		
		if (stmt instanceof ThrowStatement)
			return EThrowStatement.create(east, (ThrowStatement) stmt,
					scope, method, parent, (EThrowStatement) base);
		
		if (stmt instanceof TryStatement)
			return ETryStatement.create(east, (TryStatement) stmt,
					scope, method, parent, (ETryStatement) base);
		
		if (stmt instanceof ConstructorInvocation)
			return EConstructorInvocation.create(east, (ConstructorInvocation) stmt,
					scope, method, parent, (EConstructorInvocation) base);

		System.err.println("Not implemented error: "
				+ stmt.getClass().getName());

		return null;
	}

	@SuppressWarnings("unchecked")
	public List<Statement> translate(List<CompilationUnit> out) {
		if (this.inlineTask)
			return this.build(out);

		out.add(this.task.translate());
		this.task.getExecute().getBody().statements().addAll(this.build(out));

		AST ast = this.getAST();

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access
				.setName(ast.newSimpleName("ae_" + this.task.getFieldName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());

		this.postTranslate(this.task);

		return Arrays.asList((Statement) ast.newExpressionStatement(assign));
	}

	@Override
	public EASTDataNode getScope() {
		return this.scope;
	}

	public abstract List<Statement> build(List<CompilationUnit> out);
}
