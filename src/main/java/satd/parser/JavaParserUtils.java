package satd.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import entities.Comment;
import satd.visitors.CodeCommentsVisitor;

import java.util.HashSet;

public class JavaParserUtils {
    public static void getAddedComments(String filename, String content, String commit) {
        if (filename.contains(".java")) {
            try {
                CompilationUnit compilationUnit = StaticJavaParser.parse(content);

                HashSet<Comment> comments = new HashSet<>();
                //  List<Comment> comments = new ArrayList<>();
                VoidVisitor codeCommentsVisitor = new CodeCommentsVisitor(filename, commit, comments);
                codeCommentsVisitor.visit(compilationUnit, null);

                if (comments.size() > 0)
                    System.out.println(comments);
            } catch (ParseProblemException ex) {
                System.out.println("Parsing of " + filename + "performed with exception");
            }
        }
    }
}
