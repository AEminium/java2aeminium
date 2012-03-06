package com.pavlinic.codegen;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

public interface FragmentGenerator {
  ASTNode generateCode(AST ast);
}
