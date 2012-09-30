package aeminium.compiler.task;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import aeminium.compiler.east.EWhileStatement;

public class WhileSubTask extends StatementSubTask
{
	protected WhileSubTask(EWhileStatement node, String name, Task parent, Task base)
	{
		super(node, name, parent, base);
	}

	public static WhileSubTask create(EWhileStatement node, String name, Task parent, Task base)
	{
		return new WhileSubTask(node, name, parent, base);
	}
	
	@Override
	public EWhileStatement getNode()
	{
		return (EWhileStatement) this.node;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive)
	{
		AST ast = this.node.getAST();
		
		this.base.addField(ast.newSimpleType(ast.newSimpleName(this.base.getTypeName())), "ae_previous", false);

		/* this.ae_previous = ae_parent */
		
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_previous"));
	
		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(access);
		assign.setRightHandSide(ast.newSimpleName("ae_parent"));
		
		body.statements().add(ast.newExpressionStatement(assign));
		
		super.fillConstructor(constructor, body, recursive);
	}
	
	@Override
	public CompilationUnit translate()
	{
		if (this.translated)
			return this.cu;
		
		this.fillConstructor(this.constructors.get(0), this.node.getAST().newBlock(), true);
		this.fillExecute();
		
		return this.cu;
	}
	
	@Override
	public Expression pathToParent(Expression currentPath)
	{
		AST ast = this.node.getAST();
		
		FieldAccess field = ast.newFieldAccess();
		field.setExpression(currentPath);
		field.setName(ast.newSimpleName("ae_previous"));

		return field;
	}
}
