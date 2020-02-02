package satd.visitors;

import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class CodeCommentsVisitor extends VoidVisitorAdapter<Void> {

    @Override
    public void visit(BlockComment md, Void arg) {
        super.visit(md, arg);
        System.out.println(md);
    }

    @Override
    public void visit(LineComment md, Void arg) {
        super.visit(md, arg);
        System.out.println(md);
    }
}