package aeminium.compiler;

import java.util.List;
import java.util.ArrayList;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.EAST;
import aeminium.compiler.east.ECompilationUnit;

public class Compiler
{
	String source;
	String target;

	/* required for AST creation */
	String[] classPath;
	String[] sourcePath;

	Compiler(String source, String target)
	{
		this.source = source;
		this.target = target;

		/* set up the environment */
		this.classPath = System.getProperty("classpath", "").split(";");
		this.sourcePath = new String[1];
		this.sourcePath[0] = source;
	}

	@SuppressWarnings("unchecked")
	public void run() throws IOException
	{
		EAST east = new EAST();
		List<String> files = this.walk(new File(this.source));

		for (String path : files)
		{
			String shortPath = path.replace(this.source + "/", "");
			String content = readFile(path);

			ASTParser parser = ASTParser.newParser(AST.JLS3);

			parser.setSource(content.toCharArray());
			parser.setEnvironment(this.classPath, this.sourcePath, null, true);
			parser.setResolveBindings(true);
			parser.setStatementsRecovery(true);
			parser.setBindingsRecovery(true);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setUnitName(shortPath);

			CompilationUnit cu = (CompilationUnit) parser.createAST(null);

			ECompilationUnit.create(east, cu);
		}

		east.checkSignatures();
		east.checkDependencies();

		int n;
		while ((n = east.optimize()) > 0)
			System.out.println("Optimized: " + n);
		
		east.preTranslate();
		
		// Save units
		for (CompilationUnit unit : east.translate())
		{
			AST ast = unit.getAST();

			ImportDeclaration helper = ast.newImportDeclaration();
			helper.setName(ast.newName("aeminium.runtime.AeminiumHelper"));
			unit.imports().add(helper);

			ImportDeclaration list = ast.newImportDeclaration();
			list.setName(ast.newName("java.util.ArrayList"));
			unit.imports().add(list);

			this.save(unit);
		}
	}

	/**
		Recursivly lists every regular file inside a given path
		@param path The base path 
	*/
	public List<String> walk(File path)
	{
		List<String> children = new ArrayList<String>();

		if (path.isDirectory())
		{
			File[] files = path.listFiles();
			for (File file : files)
				children.addAll(this.walk(file));
		} else
			children.add(path.getPath());

		return children;
	}

	private void save(CompilationUnit unit) throws IOException
	{
		BufferedWriter writer = null;
		String path = this.getCUPath(unit);

		/* build directory tree */
		String[] dirs = path.split("/");
		String curPath = "";

		for (int i = 0; i < dirs.length - 1; i++)
		{
			curPath += dirs[i] + '/';

			File dir = new File(curPath);
			if (!dir.exists())
				dir.mkdir();
		}

		try
		{
			writer = new BufferedWriter(new FileWriter(path));
			writer.write(unit.toString());
		} finally
		{
			try
			{
				if ( writer != null)
					writer.close();
			} catch ( IOException e)
			{
			}
		}
	}

	private String getCUPath(CompilationUnit cu)
	{
		String path = this.target;

		path += "/";
		path += cu.getPackage().getName().toString().replace('.', '/');
		path += "/";
		path += ((TypeDeclaration) cu.types().get(0)).getName();
		path += ".java";
		
		return path;
	}

	private static String readFile(String path) throws java.io.IOException
	{
		File file = new File(path);
	    byte[] buffer = new byte[(int) file.length()];
	    
	    BufferedInputStream f = null;
	    try
	    {
	        f = new BufferedInputStream(new FileInputStream(file));
	        f.read(buffer);
	    } finally
	    {
	        if (f != null)
	        {
	        	try
	        	{
	        		f.close();
	        	} catch (IOException ignored)
	        	{
	        	}
	    	}
	    }
	    
	    return new String(buffer);
	}

	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.err.println("java -jar Compiler source_dir target_dir");
			return;
		}

		Compiler compiler = new Compiler(args[0], args[1]);

		try
		{
			compiler.run();
		} catch (IOException e)
		{
			System.err.println("ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}
}