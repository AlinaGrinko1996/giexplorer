
import org.eclipse.jgit.api.errors.GitAPIException;
import satd.parser.JavaParserUtils;
import utils.JGitUtils;

import java.io.IOException;
import java.util.HashMap;

public class Main {

    public static void main(String... args) throws IOException, GitAPIException {
        String url = "https://github.com/AlinaGrinko1996/appliedprogramanalysis-2.git";
        HashMap<String, HashMap<String, String>> commitFilesFilenames = JGitUtils.getAllFilesChangedInCommits(url);
        commitFilesFilenames.forEach((commit, fileAndContent) -> {
            fileAndContent.forEach((file, content) -> {
                JavaParserUtils.getAddedComments(file, content, commit);
            });
        });
    }
}
