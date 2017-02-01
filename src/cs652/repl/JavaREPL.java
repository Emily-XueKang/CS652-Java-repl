package cs652.repl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.tools.*;
import com.sun.source.util.JavacTask;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;


public class JavaREPL {
	private static Path DEFAULT_DIR;
	private static ClassLoader loader;
	private static String PRINT_PREFIX = "print ";
	static {
	    try{
            DEFAULT_DIR = Files.createTempDirectory("temp");
            loader = new URLClassLoader(new URL[]{DEFAULT_DIR.toUri().toURL()});
        }catch (IOException e){
            System.out.println("Fail to initialize class:" + e.getStackTrace());
        }
    }

    public static void main(String[] args) throws IOException {

		exec(new InputStreamReader(System.in));
	}

	public static void exec(Reader r) throws IOException {
		BufferedReader stdin = new BufferedReader(r);
		NestedReader reader = new NestedReader(stdin);
		int classNumber = 0;
		String statement = "";
		String declaration = "";
		String newcode,java;
		String className;
		String extendSuper;

		while (true) {
			//while not end of file, if isDeclaration, save java file decl; else save java file statements, then exec
			System.out.print("> ");
			try{
			    java = reader.getNestedString();
            }catch(EOFException eof){
			    break;//break while true loop once receive ctrl+D
            }

			if(java.startsWith(PRINT_PREFIX)){
			    java = "System.out.println(" + java.substring(PRINT_PREFIX.length(),java.length()-1) + ");";
            }
			if (java.length() == 0) continue;

			if (classNumber != 0) {
				extendSuper = "Interp_" + (classNumber - 1);
				className = "Interp_" + classNumber;
			} else {
				className = "Interp_" + classNumber;
				extendSuper = "";
			}

			statement = "";
			declaration = "";
			if (isDeclaration(java)) {
				declaration = java;
				newcode = getCode(className, extendSuper, declaration, statement);
			} else {
				statement = java;
				newcode = getCode(className, extendSuper, declaration, statement);
			}

			File classFile = writeFile(className, newcode);
			boolean success = compile(classFile);
			if (success) {
			    exec(loader, className, "exec");
                classNumber++;
            }

		}

	}

	/**
	 * this method is used to compile any input lina as a declaration.
	 *
	 * @param line--need to transform to a javafile of declaration class and put it into a file.
	 * @return whether it can be compiled. see the result of "diagnostics.getDiagnostics().size()".
	 * @throws IOException
	 */
	public static boolean isDeclaration(String line) throws IOException {
		String lineTocode = "import java.io.*;\n" + "import java.util.*;\n" + "public class Bogus {\n" + "public static " + line + "\n" + "public static void exec() {\n}\n}";
		String tempFileName = "Bogus.java";
		File file = writeFile(tempFileName, lineTocode);
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		String classPath = DEFAULT_DIR.toAbsolutePath().toString();
		String[] compileOptions = new String[]{"-d", classPath, "-cp", classPath};
		Iterable<String> compilationOptions = Arrays.asList(compileOptions);
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(file);
		JavacTask task = (JavacTask)
				compiler.getTask(null, fileManager, diagnostics, compilationOptions, null, compilationUnits);
		task.parse();

		return diagnostics.getDiagnostics().size() == 0;
	}

	/**
	 * Return a class code based on the input line, with classname, declaration or statement.
	 *
	 * @param className
	 * @param extendSuper
	 * @param declaration
	 * @param statement
	 * @return
	 */
	public static String getCode(String className, String extendSuper, String declaration, String statement) {

		String javacode = "";
		String importing = "import java.io.*;\n" + "import java.util.*;\n";

		String executable = "public static void exec() {\n" + statement + "\n}\n";
		String classbody = "public static " + declaration + "\n" + executable;

		if (statement.length() != 0) {
			classbody = executable;
		}
		if (declaration.length() != 0) {
			classbody = "public static " + declaration + "\n" + "public static void exec() {\n}\n";
		}
		if (className.equals("Interp_0")) {
			javacode = importing + "public class " + className + " {\n" + classbody + "\n}";
		} else {
			javacode = importing + "public class " + className + " extends " + extendSuper + " {\n" + classbody + "\n}";
		}

		return javacode;
	}


	/**
	 * Reference source: http://www.java2s.com/Code/Java/JDK-6/CompileaJavafilewithJavaCompiler.htm
	 * and http://www.informit.com/articles/article.aspx?p=2027052&seqNum=2
	 * Modified by XueKang
	 */
	public static Boolean compile(File classFile) throws IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(classFile);
		//need to be verified,replace with actual directory and classpath
		String classPath = DEFAULT_DIR.toAbsolutePath().toString();
		String[] compileOptions = new String[]{"-d", classPath, "-cp", classPath};
		Iterable<String> compilationOptions = Arrays.asList(compileOptions);
		JavacTask task = (JavacTask)
				compiler.getTask(null, fileManager, diagnostics, compilationOptions, null, compilationUnits);

		boolean success = task.call();
		fileManager.close();
		if (diagnostics.getDiagnostics().size() != 0) {
			System.out.println(diagnostics.getDiagnostics());
		}
		return success;
	}

	public static void exec(ClassLoader loader, String className, String methodName) {
		try {
			Class cl = loader.loadClass(className);
			Method f1 = cl.getDeclaredMethod(methodName);

			Object o = cl.newInstance();
			f1.invoke(null, null);
		} catch (Exception e) {
			System.out.println(e.getStackTrace());
		}

	}

	/**
	 * Reference sourse: https://www.mkyong.com/java/how-to-write-to-file-in-java-bufferedwriter-example/
	 *
	 * @param className
	 * @param content
	 */
	public static File writeFile(String className, String content) {
		//SimpleJavaFileObject file = new SimpleJavaFileObject();
		BufferedWriter bw = null;
		FileWriter fw = null;
		String fileName = className + ".java";
		File parentDir = DEFAULT_DIR.toFile();
		File file = new File(parentDir, fileName);
		try {
			file.createNewFile();
			bw = Files.newBufferedWriter(file.toPath());
			bw.write(content);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null)
					bw.close();

				if (fw != null)
					fw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return file;
	}
}
