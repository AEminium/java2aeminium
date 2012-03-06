package com.pavlinic.codegen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;

public class CodeGenerator {
	private final class CodeReplacer extends ASTVisitor {
    @Override
    public boolean visit(FieldDeclaration node) {
    	if (node.getType().toString().equals(CodeFragment.class.getName())) {
    	  final SingleMemberAnnotation annotation = getAnnotation(node);
        if (annotation.getTypeName().toString().equals(GeneratedBy.class.getName())) {
          final TypeLiteral generatorType = (TypeLiteral) annotation.getValue();
          final String generatorClassName = generatorType.getType().toString();
          replaceNodeWithGeneratedCode(node, generatorClassName);
        }
    	}
    	return super.visit(node);
    }

    private void replaceNodeWithGeneratedCode(ASTNode node, final String generatorClassName) {
      final FragmentGenerator generator = createGenerator(generatorClassName);
      final ASTNode generatedCode = generator.generateCode(node.getAST());
      if (node.getParent() instanceof TypeDeclaration) {
        replaceCodeInTypeDeclaration(node, generatedCode);
      } else if (node.getParent() instanceof ReturnStatement) {
        replaceCodeInReturnStatement(node, generatedCode);
      }
    }

    private void replaceCodeInReturnStatement(ASTNode node,
        final ASTNode generatedCode) {
      final ReturnStatement parent = (ReturnStatement) node.getParent();
      parent.setExpression((Expression) generatedCode);
    }
    @SuppressWarnings("unchecked")
    private void replaceCodeInTypeDeclaration(ASTNode node,
        final ASTNode generatedCode) {
      final TypeDeclaration parent = (TypeDeclaration) node.getParent();
      final int currentNodeIndex = parent.bodyDeclarations().indexOf(node);
      node.delete();
      parent.bodyDeclarations().add(currentNodeIndex, generatedCode);
    }
    
    @Override
    public boolean visit(MethodInvocation node) {
      if (node.getExpression().toString().equals(CodeFragment.class.getName())
          && node.getName().toString().equals("expression")) {
        final TypeLiteral classLiteral = (TypeLiteral) node.arguments().get(0);
        final String generatorClassName = classLiteral.getType().toString();
        replaceNodeWithGeneratedCode(node, generatorClassName);
      }
      
      return super.visit(node);
    }

    @SuppressWarnings("unchecked")
    private FragmentGenerator createGenerator(String className) {
      try {
        final Class<? extends FragmentGenerator> clazz = (Class<? extends FragmentGenerator>) Class.forName(className);
        return clazz.getConstructor().newInstance();
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private final Configuration configuration;
  private final ImportResolver importResolver;

	public CodeGenerator(Configuration configuration, ImportResolver importResolver) {
		this.configuration = configuration;
    this.importResolver = importResolver;
	}

	public ASTNode generateClass(String className, Class<?> templateClass) {
		final ASTNode syntaxTree = loadOriginalClass(templateClass);
		importResolver.transform(syntaxTree);
		replaceOriginalNameWithGeneratedName(syntaxTree, className);
		replaceCodeFragmentsWithGeneratedCode(syntaxTree);
		
		return syntaxTree;
	}

	private void replaceCodeFragmentsWithGeneratedCode(ASTNode syntaxTree) {
		final CodeReplacer codeReplacer = new CodeReplacer();
    syntaxTree.accept(codeReplacer);
	}

	private void replaceOriginalNameWithGeneratedName(ASTNode syntaxTree, final String className) {
		syntaxTree.accept(new ASTVisitor() {
			@Override
			public boolean visit(TypeDeclaration classDeclaration) {
				classDeclaration.setName(classDeclaration.getAST().newSimpleName(className));
				return super.visit(classDeclaration);
			}
		});
	}

	private ASTNode loadOriginalClass(Class<?> templateClass) {
		final String originalClassSource = getOriginalClassSource(templateClass);
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		
		parser.setSource(originalClassSource.toCharArray());
		return parser.createAST(null);
	}

	private String getOriginalClassSource(Class<?> templateClass) {
		try {
			return IOUtils.toString(new FileReader(new File(generateSourceFileName(templateClass))));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String generateSourceFileName(Class<?> templateClass) {
		return configuration.getSourcePath() + "/" 
			+ templateClass.getName().replace('.', '/')
			+ ".java";
	}

  private SingleMemberAnnotation getAnnotation(FieldDeclaration node) {
    final SingleMemberAnnotationFinder singleMemberAnnotationFinder = new SingleMemberAnnotationFinder();
    node.accept(singleMemberAnnotationFinder);
    final SingleMemberAnnotation annotation = singleMemberAnnotationFinder.getAnnotation();
    return annotation;
  }
}
