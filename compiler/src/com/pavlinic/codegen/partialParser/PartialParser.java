package com.pavlinic.codegen.partialParser;

import java.lang.reflect.Field;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class PartialParser {

	// By Alcides Fonseca
	public Block parseBlock(String expressionString, AST ast) {
	    final String wholeProgramString = "class X { public void m() { " + expressionString + " } }";
	    final ASTParser astParser = ASTParser.newParser(AST.JLS3);
	    astParser.setSource(wholeProgramString.toCharArray());
	    final CompilationUnit compiledCode = (CompilationUnit) astParser.createAST(null);
	    final TypeDeclaration typeDeclaration = (TypeDeclaration) compiledCode.types().get(0);
	    final MethodDeclaration methodDeclaration = (MethodDeclaration) typeDeclaration.bodyDeclarations().get(0);
	    final Block expression = (Block) methodDeclaration.getBody();
	    morphExpression(expression, ast);
	    return expression;
	  }
	
  public Expression parseExpression(String expressionString, AST ast) {
    final String wholeProgramString = "class X {int a = " + expressionString + ";}";
    final ASTParser astParser = ASTParser.newParser(AST.JLS3);
    astParser.setSource(wholeProgramString.toCharArray());
    final CompilationUnit compiledCode = (CompilationUnit) astParser.createAST(null);
    final TypeDeclaration typeDeclaration = (TypeDeclaration) compiledCode.types().get(0);
    final FieldDeclaration fieldDeclaration = (FieldDeclaration) typeDeclaration.bodyDeclarations().get(0);
    final VariableDeclarationFragment fragment = (VariableDeclarationFragment) fieldDeclaration.fragments().get(0);
    final Expression expression = fragment.getInitializer();
    morphExpression(expression, ast);
    return expression;
  }

  private void morphExpression(ASTNode exp, AST ast) {
    setAst(exp, ast);
    clearParent(exp);
  }

  private void clearParent(ASTNode exp) {
    try {
      final Field field = ASTNode.class.getDeclaredField("parent");
      field.setAccessible(true);
      field.set(exp, null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void setAst(ASTNode exp, AST ast) {
    //AST should never be null
    if (ast == null) {
      return;
    }
    try {
      final Field field = ASTNode.class.getDeclaredField("ast");
      field.setAccessible(true);
      field.set(exp, ast);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

}
