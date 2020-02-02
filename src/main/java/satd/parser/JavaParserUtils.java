package satd.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import satd.visitors.CodeCommentsVisitor;

public class JavaParserUtils {
    public static void getAddedComments(String filename, String content, String commit) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(content);

            VoidVisitor codeCommentsVisitor = new CodeCommentsVisitor(filename, commit);
            codeCommentsVisitor.visit(compilationUnit, null);

        } catch (ParseProblemException ex) {
            System.out.println("Parsing of " + filename + "performed with exception");
            System.out.println("Parsing of %s performed with exception");
        }
    }
}
