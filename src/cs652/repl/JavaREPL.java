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
import java.util.List;
import java.util.Random;


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

	private static boolean clearDir() {
		File directory = DEFAULT_DIR.toFile();
		boolean success = true;
		// Delete all files in directory
		File[] files = directory.listFiles();
		for (File file : files)
		{
			if (!file.delete())
			{
				success = false;
			}
		}
		return success;
	}

	public static void exec(Reader r) throws IOException {
		clearDir();
		Random rand = new Random();
		String classPrefix = "Interp"+"_"+rand.nextInt(Integer.MAX_VALUE)+"_";
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
			//System.out.println("before getnewcode classNumber: "+ classNumber);
			try{
			    java = reader.getNestedString();
            }catch(EOFException eof){
			    break;//break while true loop once receive ctrl+D
            }


			if(java.startsWith(PRINT_PREFIX)){
			    java = "System.out.println(" + java.substring(PRINT_PREFIX.length(),java.length()-1) + ");";
            }
			//System.out.println("java is: " + java);

			if (java.length() == 0) continue;

			if (classNumber != 0) {
				extendSuper = classPrefix + (classNumber - 1);
				className = classPrefix + classNumber;
			} else {
				className = classPrefix + classNumber;
				extendSuper = "";
			}

			statement = "";
			declaration = "";
			if (isDeclaration(java)) {
//				System.out.println("is declare");
				declaration = java;
				newcode = getCode(className, extendSuper, declaration, statement);
			} else {
//				System.out.println("not declare");
				statement = java;
				newcode = getCode(className, extendSuper, declaration, statement);
			}


//			System.out.println("newcode is: " + newcode);
			File classFile = writeFile(className, newcode);
			boolean success = compile(classFile);

			if (success) {
//				System.out.println("before execution:");
			    exec(loader, className, "exec");
//				System.out.println("after execution:");
            }
            classNumber++;
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
		String tempFileName = "Bogus";
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
		if (className.endsWith("_0")) {
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
		if (success == false) {
			List<Diagnostic<? extends JavaFileObject>> diagnosticsList = diagnostics.getDiagnostics();
			for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticsList) {
				// read error dertails from the diagnostic object
				System.out.println("line " + (int)(diagnostic.getLineNumber()+2) + ": " + diagnostic.getMessage(null));
//				System.out.println("getSource: "+ diagnostic.getSource());
//				System.out.println("getCode: " + diagnostic.getCode());
			}
			//System.out.println(diagnostics.getDiagnostics());
		}
//		List<Diagnostic<? extends JavaFileObject>> diagnosticsList = diagnostics.getDiagnostics();
//		for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticsList) {
//			System.out.println("getSource: "+ diagnostic.getSource());
//			System.out.println("getCode: " + diagnostic.getCode());
//
//		}

		return success;
	}

	public static void exec(ClassLoader loader, String className, String methodName) {
		try {
			Class cl = loader.loadClass(className);

			Method f1 = cl.getDeclaredMethod(methodName);
			//Object o = cl.newInstance();
			f1.invoke(null, null); // call the exec() method in generated java file
		} catch (Exception e) {
			e.printStackTrace();
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
			if(file.exists()){
				boolean deleteSuccess = file.delete();
			}
			boolean createdSuccess = file.createNewFile();
			fw = new FileWriter(file, false);
			bw = new BufferedWriter(fw);
			bw.write(content);
			bw.flush();
			FileReader fd = new FileReader(file);
			StringBuilder sb = new StringBuilder();
			int c = fd.read();
			while (c!= -1) {
				sb.append((char)c);
				c=fd.read();
			}
			assert sb.toString().equals(content);
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
