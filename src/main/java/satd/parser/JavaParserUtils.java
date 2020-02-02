package satd.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.TreeVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import entities.Comment;
import satd.visitors.CodeCommentsVisitor;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class JavaParserUtils {
    public static void getAddedComments(String output) {
        String[] lines = output.split("\n");
        String singleLineAdded = "";
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("+") && !lines[i].startsWith("+++")) {
                singleLineAdded += lines[i].substring(1, lines[i].length() - 1) + "\n";
            }
        }


        CompilationUnit compilationUnit = StaticJavaParser.parse(singleLineAdded);

        VoidVisitor codeCommentsVisitor = new CodeCommentsVisitor();
        codeCommentsVisitor.visit(compilationUnit, null);
    }

    public static void getComments(String output) {
        String[] lines = output.split("\n");
        String singleLineAdded = "";
        String singleLineRemoved = "";
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("+") && !lines[i].startsWith("+++")) {
                singleLineAdded += lines[i].substring(1, lines[i].length() - 1) + "\n";
            } else if (lines[i].startsWith("-") && !lines[i].startsWith("---")) {
                singleLineRemoved += lines[i].substring(1, lines[i].length() - 1) + "\n";
            }
        }
    }
}
