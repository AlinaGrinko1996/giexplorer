package satd.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import entities.Comment;
import satd.visitors.CodeCommentsVisitor;
import utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaParserUtils {
    public static List<Comment> getAddedComments(String filename, String content, String commit) {
        List<Comment> comments = new ArrayList<>();
        if (filename.contains(".java")) {
            try {
                CompilationUnit compilationUnit = StaticJavaParser.parse(content);

                //  List<Comment> comments = new ArrayList<>();
                VoidVisitor codeCommentsVisitor = new CodeCommentsVisitor(filename, commit, comments);
                codeCommentsVisitor.visit(compilationUnit, null);

//                if (comments.size() > 0)
//                    System.out.println(comments);
            } catch (ParseProblemException ex) {
                System.out.println("Parsing of " + filename + "performed with exception");
            }
        }
        return comments;
    }

    public static int filterForSATD(List<Comment> input) {
//        List<Comment> result = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        int initialNumber = input.size();
        List<String> SATDKeywords = FileUtils.getSATDKeywords();
//        input.forEach(comment -> {
//            SATDKeywords.forEach(keyWord -> {
//                if (comment.commentText.contains(keyWord) && !result.contains(comment)) {
//                    result.add(comment);
//                    counter.getAndIncrement();
//                }
//            });
//        });
        input.removeIf(comment -> {
            for (String keyWord : SATDKeywords) {
                if (comment.commentText.contains(keyWord)) {
                    counter.getAndIncrement();
                    return false;
                }
            }
            return true;
        });
        return counter.get();
    }
}
