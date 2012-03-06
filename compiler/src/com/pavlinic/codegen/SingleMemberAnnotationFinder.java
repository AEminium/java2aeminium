package com.pavlinic.codegen;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

public class SingleMemberAnnotationFinder extends ASTVisitor {
  private SingleMemberAnnotation node;

  @Override
  public boolean visit(SingleMemberAnnotation node) {
    this.node = node;
    return super.visit(node);
  }
  
  public SingleMemberAnnotation getAnnotation() {
    return node;
  }
}
