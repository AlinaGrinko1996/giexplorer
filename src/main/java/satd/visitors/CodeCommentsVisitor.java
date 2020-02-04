package satd.visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.CommentsCollection;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import entities.Comment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class CodeCommentsVisitor extends VoidVisitorAdapter<Void> {
    //HashSet<Comment> comments;
    List<Comment> comments;
    List<Comment> singleLineComments = new ArrayList<>();

    private String fileName;
    private String commit;

    public CodeCommentsVisitor(String fileName, String commit, List<Comment> comments) {
        super();
        this.commit = commit;
        this.fileName = fileName;
        this.comments = comments;
    }

    @Override
    public void visit(BlockComment bc, Void arg) {
        super.visit(bc, arg);
        Comment comment = new Comment(fileName, bc.toString());
        bc.getRange().ifPresent(blockRange -> {
           comment.setLineBegin(blockRange.begin.line);
           comment.setLineEnd(blockRange.end.line);
        });
        if (!comments.contains(comment)) { //todo here add check for time of previous comment
            comments.add(comment);
        }
    }

    @Override
    public void visit(LineComment lc, Void arg) {
        super.visit(lc, arg);
        Comment comment = new Comment(fileName, lc.toString());
        lc.getRange().ifPresent(blockRange -> {
            comment.setLineBegin(blockRange.begin.line);
            comment.setLineEnd(blockRange.end.line);
        });
        lc.getCommentedNode().ifPresent(commentedNode -> {
            comment.commentText = getWholeComment(commentedNode.toString());
        });
        if (!comments.contains(comment)) { //todo here add check for time of previous comment
            comments.add(comment);
        }
    }

    private String getWholeComment(String node) {
        String resultingString = "";
        String[] lines = node.split("\n");
        for (int i=0; i < lines.length; i++) {
            if (lines[i].contains("//")) {
                resultingString += lines[i] + "\n";
            }
        }
        return resultingString;
    }
}