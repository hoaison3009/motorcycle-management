import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
/** Parse Java sources without resolving Android classes; not a replacement for an APK build. */
public class CheckJava {
    public static void main(String[] args) throws Exception {
        List<java.io.File> files;
        try(Stream<Path> paths=Files.walk(Paths.get(args[0]))) {
            files=paths.filter(p->p.toString().endsWith(".java")).map(Path::toFile).collect(Collectors.toList());
        }
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        try(StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,null,java.nio.charset.StandardCharsets.UTF_8)) {
            JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,Arrays.asList("-proc:none","--release","11"),null,manager.getJavaFileObjectsFromFiles(files));
            task.parse();
            for(Diagnostic<?> diagnostic:diagnostics.getDiagnostics())if(diagnostic.getKind()==Diagnostic.Kind.ERROR)throw new AssertionError(diagnostic);
        }
        System.out.println("Parsed "+files.size()+" Java source files successfully (syntax only).");
    }
}
