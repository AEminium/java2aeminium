package com.pavlinic.codegen;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeLiteral;

public class ImportResolver {
  public void transform(ASTNode astNode) {
    final Map<String, String> typeMap = new HashMap<String, String>();
    final StringBuilder packageName = new StringBuilder();
    
    astNode.accept(new ASTVisitor() {
      @Override
      public boolean visit(ImportDeclaration node) {
        final String typeName = node.getName().toString();
        final String simpleName = typeName.substring(typeName.lastIndexOf('.') + 1);
        typeMap.put(simpleName, typeName);
        node.delete();
        return super.visit(node);
      }
      
      @Override
      public boolean visit(PackageDeclaration node) {
        packageName.append(node.getName().toString());
        return super.visit(node);
      }
    });
    
    astNode.accept(new ASTVisitor() {
      @Override
      public boolean visit(FieldDeclaration node) {
        final AST ast = node.getAST();
        final String newTypeName = resolveType(typeMap, node.getType().toString(), packageName.toString());
        node.setType(ast.newSimpleType(ast.newName(newTypeName)));
        return super.visit(node);
      }
      
      @Override
      public boolean visit(TypeLiteral node) {
        final AST ast = node.getAST();
        final String newTypeName = resolveType(typeMap, node.getType().toString(), packageName.toString());
        node.setType(ast.newSimpleType(ast.newName(newTypeName)));
        return super.visit(node);
      }
      
      @Override
      public boolean visit(SingleMemberAnnotation node) {
        final AST ast = node.getAST();
        final String newTypeName = resolveType(typeMap, node.getTypeName().toString(), packageName.toString());
        node.setTypeName(ast.newName(newTypeName));
        return super.visit(node);
      }
      @Override
      public boolean visit(MethodInvocation node) {
        final AST ast = node.getAST();
        final Expression expression = node.getExpression();
        if (expression instanceof SimpleName) {
          final SimpleName simpleName = (SimpleName) expression;
          final String newTypeName = resolveType(typeMap, simpleName.toString(), packageName.toString());
          node.setExpression(ast.newName(newTypeName));
        }
        return super.visit(node);
      }
      
      
    });
  }

  String resolveType(final Map<String, String> typeMap, String originalReference, String packageName) {
    //If the package is already specified, nothing to do
    if (originalReference.indexOf('.') >= 0) {
      return originalReference;
    }
    final String resolved = typeMap.get(originalReference);
    if (resolved == null) {
      try {
        final String className = packageName + "." + originalReference;
        Class.forName(className);
        return className;
      } catch (ClassNotFoundException e) {
        return "java.lang." + originalReference;
      }
    }
    return resolved;
  }
}
