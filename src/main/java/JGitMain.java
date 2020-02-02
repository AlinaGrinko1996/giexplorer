import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import satd.parser.JavaParserUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class JGitMain {

    public static Repository cloneJGitRepository(String gitUrl) throws IOException {
        File localPath = File.createTempFile("TestGitRepository", "");
        if(!localPath.delete()) {
            throw new IOException("Could not delete temporary file " + localPath);
        }

        // then clone
        System.out.println("Cloning from " + gitUrl + " to " + localPath);
        try (Git result = Git.cloneRepository()
                .setURI(gitUrl)
                .setDirectory(localPath)
                .setCloneAllBranches(true)
                .call()) {
            // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
            System.out.println("Having repository: " + result.getRepository().getDirectory());
            return result.getRepository();
        } catch (InvalidRemoteException e) {
            e.printStackTrace();
        } catch (TransportException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        // clean up here to not keep using more and more disk-space for these samples
        //FileUtils.deleteDirectory(localPath);
        localPath.delete();
        return openJGitRepository();
    }

    public static Repository openJGitRepository() throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
    }

    public static void listAllCommitsInRemoteRepository(String gitUrl) throws IOException {
        try (Repository repository = cloneJGitRepository(gitUrl)) {
            try (Git git = new Git(repository)) {
                // use the following instead to list commits on a specific branch
                //ObjectId branchId = repository.resolve("HEAD");
                //Iterable<RevCommit> commits = git.log().add(branchId).call();

                Iterable<RevCommit> commits = git.log().all().call();
                int count = 0;
                for (RevCommit commit : commits) {
                    System.out.println("LogCommit: " + commit);
                    //diffCommit(commit.getName(), repository);
                    List<String> changedFiles = exploreCommit(repository, git, commit);
                    accessingFiles(commit, repository, changedFiles);
                    count++;
                }
                System.out.println(count);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoHeadException e) {
                e.printStackTrace();
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> exploreCommit(Repository repository, Git git, RevCommit commit) throws GitAPIException, IOException {
        if (commit.getParentCount() > 0) {
            System.out.println("Parent of " + commit.getName()+ " is " + commit.getParent(commit.getParentCount() - 1).getName());
            return listDiff(repository, git, commit.getParent(commit.getParentCount() - 1).getName(), commit.getName());
        } //todo else = all files
        return new ArrayList<>();
    }

    private static List<String> listDiff(Repository repository, Git git, String oldCommit, String newCommit) throws GitAPIException, IOException {
        final List<DiffEntry> diffs = git.diff()
                .setOldTree(prepareTreeParser(repository, oldCommit))
                .setNewTree(prepareTreeParser(repository, newCommit))
                .call();
        List<String> changedFiles = new ArrayList<>();

        System.out.println("Found: " + diffs.size() + " differences");
        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

            diffFormatter.setRepository(repository);
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);
            for (DiffEntry diff : diffs) {
                FileHeader fileHeader = diffFormatter.toFileHeader(diff);
                System.out.println("Diff: " + diff.getChangeType() + ": " +
                        (diff.getOldPath().equals(diff.getNewPath()) ? diff.getNewPath() : diff.getOldPath() + " -> " + diff.getNewPath()));
                System.out.println("Changed: " + fileHeader.toEditList());
                changedFiles.add(diff.getNewPath());
            }
        }
        return changedFiles;
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        // from the commit we can build the tree which allows us to construct the TreeParser
        //noinspection Duplicates
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();

            return treeParser;
        }
    }

    public static void main(String... args) throws IOException {
      //  String url = "https://github.com/apache/accumulo.git";
        String url = "https://github.com/AlinaGrinko1996/appliedprogramanalysis-2.git";
       listAllCommitsInRemoteRepository(url);
    }

    private static void diffCommit(String hashID, Repository repo) throws IOException {
        //Initialize repositories.
        Git git = new Git(repo);

        //Get the commit you are looking for.
        RevCommit newCommit;
        try (RevWalk walk = new RevWalk(repo)) {
            newCommit = walk.parseCommit(repo.resolve(hashID));
        }

        System.out.println("LogCommit: " + newCommit);
        String logMessage = newCommit.getFullMessage();
        System.out.println("LogMessage: " + logMessage);
        //Print diff of the commit with the previous one.
        String temp = getDiffOfCommit(newCommit, repo, git);
        System.out.println(temp);

        JavaParserUtils.getAddedComments(temp, "");

    }
    //Helper gets the diff as a string.
    private static String getDiffOfCommit(RevCommit newCommit, Repository repo, Git git) throws IOException {

        //Get commit that is previous to the current one.
        RevCommit oldCommit = getPrevHash(newCommit, repo);
        if (oldCommit == null){
            return "Start of repo";
        }
        //Use treeIterator to diff.
        AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser(oldCommit, git);
        AbstractTreeIterator newTreeIterator = getCanonicalTreeParser(newCommit, git);
        OutputStream outputStream = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(outputStream)) {
            formatter.setRepository(git.getRepository());
            formatter.format(oldTreeIterator, newTreeIterator);
        }
        String diff = outputStream.toString();
        return diff;
    }
    //Helper function to get the previous commit.
    public static RevCommit getPrevHash(RevCommit commit, Repository repo)  throws  IOException {

        try (RevWalk walk = new RevWalk(repo)) {
            // Starting point
            walk.markStart(commit);
            int count = 0;
            for (RevCommit rev : walk) {
                // got the previous commit.
                if (count == 1) {
                    return rev;
                }
                count++;
            }
            walk.dispose();
        }
        //Reached end and no previous commits.
        return null;
    }

    //Helper function to get the tree of the changes in a commit. Written by Rüdiger Herrmann
    private static AbstractTreeIterator getCanonicalTreeParser(ObjectId commitId, Git git) throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit commit = walk.parseCommit(commitId);
            ObjectId treeId = commit.getTree().getId();
            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                return new CanonicalTreeParser(null, reader, treeId);
            }
        }
    }

    public static void commit_logs(Repository repo) throws IOException, NoHeadException, GitAPIException {
        List<String> logMessages = new ArrayList<String>();
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Git git = new Git(repo);
        Iterable<RevCommit> log = git.log().call();
        RevCommit previousCommit = null;
        for (RevCommit commit : log) {
            if (previousCommit != null) {
                AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser( previousCommit, git );
                AbstractTreeIterator newTreeIterator = getCanonicalTreeParser( commit, git );
                OutputStream outputStream = new ByteArrayOutputStream();
                try( DiffFormatter formatter = new DiffFormatter( outputStream ) ) {
                    formatter.setRepository( git.getRepository() );
                    formatter.format( oldTreeIterator, newTreeIterator );
                }
                String diff = outputStream.toString();
                System.out.println(diff);
            }
            System.out.println("LogCommit: " + commit);
            String logMessage = commit.getFullMessage();
            System.out.println("LogMessage: " + logMessage);
            logMessages.add(logMessage.trim());
            previousCommit = commit;
        }
        git.close();
    }

    public static void accessingFiles(RevCommit commit, Repository repository, List<String> changedFiles) throws IOException {
        RevTree tree = commit.getTree();
        System.out.println("Having tree: " + tree);

        for (int i = 0; i < changedFiles.size(); i++) {
            OutputStream stream = new ByteArrayOutputStream();
            if (!changedFiles.get(i).equals("/dev/null")) {
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(changedFiles.get(i)));
                    if (!treeWalk.next()) {
                        throw new IllegalStateException("Did not find expected file " + changedFiles.get(i));
                    }

                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);

                    // and then one can the loader to read the file
                    loader.copyTo(stream);
                }
            }
            JavaParserUtils.getAddedComments(stream.toString(), changedFiles.get(i));
            stream.close();
        }
      //  return stream;
    }


//    private AbstractTreeIterator getCanonicalTreeParser( ObjectId commitId ) throws IOException {
//        try( RevWalk walk = new RevWalk( git.getRepository() ) ) {
//            RevCommit commit = walk.parseCommit( commitId );
//            ObjectId treeId = commit.getTree().getId();
//            try( ObjectReader reader = git.getRepository().newObjectReader() ) {
//                return new CanonicalTreeParser( null, reader, treeId );
//            }
//        }
//    }

}
