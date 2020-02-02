package satd.visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.CommentsCollection;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class CodeCommentsVisitor extends VoidVisitorAdapter<Void> {
    private String fileName;
    private String commit;

    public CodeCommentsVisitor(String fileName, String commit) {
        super();
        this.commit = commit;
        this.fileName = fileName;
    }

    @Override
    public void visit(BlockComment md, Void arg) {
        super.visit(md, arg);
        System.out.println(md + " " + fileName + commit);
    }

    @Override
    public void visit(LineComment md, Void arg) {
        super.visit(md, arg);
        System.out.println(md+ " " + fileName + commit);
    }
}