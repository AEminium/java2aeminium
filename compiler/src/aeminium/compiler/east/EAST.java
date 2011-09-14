package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EAST
{
	public static ECompilationUnit extend(CompilationUnit unit)
	{
		return new ECompilationUnit(unit);
	}

	public static EAbstractTypeDeclaration extend(AbstractTypeDeclaration decl)
	{
		if (decl instanceof TypeDeclaration)
			return new ETypeDeclaration((TypeDeclaration) decl); 
	
		System.err.println("Invalid AbstractTypeDeclaration");
		return null;
	}

	public static EExpression extend(Expression expr)
	{
		/* TODO */
		return null;
	}

	public static EReturnStatement extend(ReturnStatement ret)
	{
		return new EReturnStatement(ret);
	}

	public static EFieldDeclaration extend(FieldDeclaration field)
	{
		return new EFieldDeclaration(field);
	}

	public static EMethodDeclaration extend(MethodDeclaration method)
	{
		return new EMethodDeclaration(method);
	}

	public static EStatement extend(Statement stmt)
	{
		if (stmt instanceof Block)
			return new EBlock((Block) stmt);

		if (stmt instanceof ReturnStatement)
			return new EReturnStatement((ReturnStatement) stmt);

		System.err.println("Invalid Statement");
		return null;
	}

	public static EBlock extend(Block block)
	{
		return new EBlock(block);
	}
}
