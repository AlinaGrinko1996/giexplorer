package entities;

import java.util.Objects;

public class Comment {
    String author;
    String fileName;
    int lineBegin;
    int lineEnd;
    public String commentText;

    public Comment(String fileName, String commentText) {
        this.fileName = fileName;
        this.commentText = commentText;
    }

    public int getLineBegin() {
        return lineBegin;
    }

    public void setLineBegin(int lineBegin) {
        this.lineBegin = lineBegin;
    }

    public int getLineEnd() {
        return lineEnd;
    }

    public void setLineEnd(int lineEnd) {
        this.lineEnd = lineEnd;
    }

    @Override
    public String toString() {
        return "\n\t' Comment {" +
                "'" + commentText + "\t" +
                "fileName='" + fileName + ' ' +
                " " + lineBegin +
                " - " + lineEnd +
                "} '\n'";
    }

    //ToDo - potential bug, comments with the same text on same file will be counted as one
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return Objects.equals(fileName, comment.fileName) &&
                Objects.equals(commentText, comment.commentText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, commentText);
    }
}
