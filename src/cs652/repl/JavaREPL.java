package cs652.repl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.Buffer;
import java.util.Arrays;
import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class JavaREPL {
	public static void main(String[] args) throws IOException {
		//exec(new InputStreamReader(System.in));
		System.out.print("import java.io.*\n" + "import java.util.*\n");
	}

	public static void exec(Reader r) throws IOException {
		BufferedReader stdin = new BufferedReader(r);
		NestedReader reader = new NestedReader(stdin);
		int classNumber = 0;
		while (true) {  //while not end of file, if isdeclaration, save java file decl; else save java file statments, then exec
			System.out.print("> ");
			String java = reader.getNestedString();
			// TODO

			String className = "Interp_" + "classNumber";
			String extendSuper = "";
			String statment = "";
			if(isDeclaration(java)){
				String newcode = getCode(className,extendSuper,java,statment);
			}
			else{




			}


		}
	}


	public static boolean isDeclaration(String line) throws IOException{
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = fileManager
				.getJavaFileObjectsFromStrings(Arrays.asList(line));
		JavacTask task = (JavacTask)
				compiler.getTask(null, fileManager, diagnostics,
						null, null, compilationUnits);
		task.parse();
		return diagnostics.getDiagnostics().size() == 0;

//		getTask(Writer out, JavaFileManager fileManager, DiagnosticListener<? super JavaFileObject> diagnosticListener, Iterable<String> options, Iterable<String> classes, Iterable<? extends JavaFileObject> compilationUnits)
//		Creates a future for a compilation task with the given components and arguments.

	}
	public static String getCode(String className, String extendSuper, String defination, String statement){
		String javacode = "";
		String importing = "import java.io.*\n" + "import java.util.*\n";
		String executable = "public static void exec() {\n" + statement + "\n}\n";
		String classbody = "public static " + defination + "\n" + executable;

		javacode = importing + "public class " + className + " extends " + extendSuper + " {\n" + classbody + "\n}";

		return javacode;
	}


	/**
	 * Reference source: http://www.java2s.com/Code/Java/JDK-6/CompileaJavafilewithJavaCompiler.htm
	 * and http://www.informit.com/articles/article.aspx?p=2027052&seqNum=2
	 * Modified by XueKang
	 */
	public static Boolean compile(String fileName) throws IOException {
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = fileManager
				.getJavaFileObjectsFromStrings(Arrays.asList(fileName)); //Construct an in-memory java source file from dynamic code
		String[] compileOptions = new String[]{"-g", "-d"} ;//need to be verified
		Iterable<String> compilationOptions = Arrays.asList(compileOptions);
		JavacTask task = (JavacTask)
				compiler.getTask(null, fileManager, diagnostics,
						compilationOptions, null, compilationUnits);
		boolean success = task.call();
		fileManager.close();
		return success;
	}
//	public static CompilerControl getCompilerControlObject(String fileName){
//
//	}
	public static void exec(ClassLoader loader, String className, String methodName){

	}
	public static void writeFile(String dir, String fileName, String content){
		//SimpleJavaFileObject file = new SimpleJavaFileObject();


	}


}
