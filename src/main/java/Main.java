
import entities.Comment;
import org.eclipse.jgit.api.errors.GitAPIException;
import satd.parser.JavaParserUtils;
import utils.FileUtils;
import utils.JGitUtils;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String... args) throws IOException, GitAPIException {
        //String url = "https://github.com/AlinaGrinko1996/appliedprogramanalysis-2.git";
        List<String> repositories = FileUtils.getRepositories();
        AtomicInteger allComments = new AtomicInteger();
        AtomicInteger SATDComments = new AtomicInteger();

        try (OutputStreamWriter outWriter = new OutputStreamWriter(
                new FileOutputStream(
                        new File("reports/report.txt")),"utf-8")) {
            repositories.forEach(repositoryUrl -> {

                write("\nRepository: " + repositoryUrl, outWriter);
                JGitUtils.chunkSizeOverloaded = true;
                HashMap<String, HashMap<String, String>> commitFilesFilenames = null;

                while (JGitUtils.chunkSizeOverloaded) {
                    try {
                        commitFilesFilenames = JGitUtils.getAllFilesChangedInCommits(repositoryUrl);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    commitFilesFilenames.forEach((commit, fileAndContent) -> {
                        fileAndContent.forEach((file, content) -> {
                            List<Comment> comments = JavaParserUtils.getAddedComments(file, content, commit);
                            if (!comments.isEmpty()) {
                                //  System.out.println(comments);
                                allComments.getAndAdd(comments.size());
                                int commentsFiltered = JavaParserUtils.filterForSATD(comments);
                                SATDComments.getAndAdd(commentsFiltered);

                                write(comments, outWriter);
                            }
                        });
                    });
                }
            });

            write("\nAll comments -> " + allComments.get(), outWriter);
            write("\nSATD comments -> " + SATDComments.get(), outWriter);
        }
    }

    private static void write(String input, OutputStreamWriter writer) {
        try {
            writer.write(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void write(List<Comment> comments, OutputStreamWriter writer) {
        comments.forEach(comment -> {
            write("\n\n" + comment.commentText, writer);
            write("\nFile: " + comment.getFileName(), writer);
            write("\nLine: " + comment.getLineBegin(), writer);
        });
    }
}
