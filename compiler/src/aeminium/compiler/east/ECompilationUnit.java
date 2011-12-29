package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

public class ECompilationUnit extends EASTNode
{
	private final CompilationUnit origin;
	private final List<EAbstractTypeDeclaration> types;

	ECompilationUnit(EAST east, CompilationUnit origin)
	{
		super(east);

		this.origin = origin;
		this.types = new ArrayList<EAbstractTypeDeclaration>();

		for (Object type : origin.types())
			this.types.add(this.east.extend((AbstractTypeDeclaration) type));
	}
	
	@Override
	public void analyse()
	{
		for (EAbstractTypeDeclaration type : this.types)
			type.analyse();
	}

	@Override 
	public int optimize()
	{
		int sum = 0;
		
		for (EAbstractTypeDeclaration type : this.types)
			sum += type.optimize();
		
		return sum;
	}
	
	public void preTranslate()
	{
		for (EAbstractTypeDeclaration type : this.types)
			type.preTranslate();
	}
	
	@SuppressWarnings("unchecked")
	public void translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		CompilationUnit unit = ast.newCompilationUnit();
		
		unit.setPackage((PackageDeclaration) ASTNode.copySubtree(ast, this.origin.getPackage()));
		unit.imports().addAll(ASTNode.copySubtrees(ast, this.origin.imports()));

		for (EAbstractTypeDeclaration type : this.types)
			unit.types().add(type.translate(cus));

		cus.add(unit);
	}
	
	public String getQualifiedName()
	{
		String name = ((AbstractTypeDeclaration) this.origin.types().get(0)).getName().toString();
		return this.origin.getPackage().getName().toString() + "." + name;
	}
}
