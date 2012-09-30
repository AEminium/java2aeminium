package aeminium.compiler.task;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

import aeminium.compiler.east.EExpression;
import aeminium.compiler.east.EMethodInvocation;
import aeminium.compiler.east.EType;

public class CallerExpressionSubTask extends ExpressionSubTask
{
	@SuppressWarnings("unchecked")
	protected CallerExpressionSubTask(EExpression node, String name, Task parent, Task base)
	{
		super(node, name, parent, base);
		
		AST ast = this.node.getAST();
		
		Type returnType = this.getNode().getType();
		Type callerType;
		
		if (this.base != null)
		{
			callerType = ast.newSimpleType(ast.newSimpleName(this.base.getTypeName()));
		} else
			{
			if (this.getNode() instanceof EMethodInvocation && ((EMethodInvocation) this.getNode()).getMethod().isVoid())
				callerType = ast.newSimpleType(ast.newName("aeminium.runtime.CallerBody"));
			else
			{
				ParameterizedType paramType = ast.newParameterizedType(ast.newSimpleType(ast.newName("aeminium.runtime.CallerBodyWithReturn")));
				
				if (returnType instanceof PrimitiveType)
					returnType = EType.boxType((PrimitiveType) returnType);
				
				paramType.typeArguments().add((Type) ASTNode.copySubtree(ast, returnType));
				callerType = paramType;
			}
		}
		this.decl.setSuperclassType(callerType);
	}

	public static CallerExpressionSubTask create(EExpression node, String name, Task parent, Task base)
	{
		return new CallerExpressionSubTask(node, name, parent, base);
	}
}
