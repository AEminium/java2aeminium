package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.BodyDeclaration;

import aeminium.compiler.east.EASTNode;

public abstract class EBodyDeclaration extends EASTNode
{
	public abstract BodyDeclaration translate(AST ast, List<CompilationUnit> cus);
}
