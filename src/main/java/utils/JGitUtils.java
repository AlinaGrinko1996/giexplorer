package utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import satd.parser.JavaParserUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JGitUtils {
    public static Repository cloneRepository(String repoUrl) throws IOException {
        File tempPath = File.createTempFile("TestRepository", "");
        if (!tempPath.delete()) {
            throw new IOException("Not deletable temp file " + tempPath);
        }
        try (Git result = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(tempPath)
                .setCloneAllBranches(true)
                .call()) {

            System.out.println("Repository cloned " + result.getRepository().getDirectory());
            return result.getRepository();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getListOfChangedFiles(Repository repository, Git git, String oldCommit, String newCommit)
            throws GitAPIException, IOException {
        final List<DiffEntry> diffs = git.diff()
                .setOldTree(getTreeParser(repository, oldCommit))
                .setNewTree(getTreeParser(repository, newCommit))
                .call();
        List<String> changedFiles = new ArrayList<>();
        for (DiffEntry diff : diffs) {
            changedFiles.add(diff.getNewPath());
        }
        return changedFiles;
    }

    private static AbstractTreeIterator getTreeParser(Repository repository, String objectId) throws IOException {
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(repository.resolve(objectId));
            RevTree tree = revWalk.parseTree(commit.getTree().getId());

            CanonicalTreeParser canonicalTreeParser = new CanonicalTreeParser();
            ObjectReader reader = repository.newObjectReader();
            canonicalTreeParser.reset(reader, tree.getId());

            revWalk.dispose();
            return canonicalTreeParser;
        }
    }

    public static HashMap<String, String> accessingFilesOfCommit(RevCommit commit, Repository repository,
                                                         List<String> changedFiles) throws IOException {
        RevTree tree = commit.getTree();
        HashMap<String, String> fileAndItsContent = new HashMap<>();

        for (int i = 0; i < changedFiles.size(); i++) {
            OutputStream stream = new ByteArrayOutputStream();
            if (!changedFiles.get(i).equals("/dev/null")) {

                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(changedFiles.get(i)));

                    if (!treeWalk.next()) {
                        throw new IllegalStateException("Did not find file " + changedFiles.get(i));
                    }

                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);

                    loader.copyTo(stream);
                }
            }
            fileAndItsContent.put(changedFiles.get(i), stream.toString());
            stream.close();
        }
        return fileAndItsContent;
    }

    public static List<String> exploreCommit(Repository repository, Git git, RevCommit commit) throws GitAPIException,
            IOException {
        int parents = commit.getParentCount();
        if (parents > 0) {
//            System.out.println("Parent of " + commit.getName() + " is "
//                    + commit.getParent(commit.getParentCount() - 1).getName());
            //todo checkAllParents
            return getListOfChangedFiles(repository, git, commit.getParent(parents - 1).getName(),
                    commit.getName());
        } //todo else = all files
        return new ArrayList<>();
    }

    public static HashMap<String, HashMap<String, String>> getAllFilesChangedInCommits(String gitUrl) throws IOException,
            GitAPIException {
        Repository repository = JGitUtils.cloneRepository(gitUrl);
        HashMap<String, HashMap<String, String>> commitAndFilesChanged = new HashMap<>();
        assert repository != null;

        Git git = new Git(repository);

        Iterable<RevCommit> commits = git.log().all().call();
        for (RevCommit commit : commits) {
            List<String> changedFiles = JGitUtils.exploreCommit(repository, git, commit);
            HashMap<String, String> fileAndItsContent = JGitUtils.accessingFilesOfCommit(commit, repository, changedFiles);
            commitAndFilesChanged.put(commit.getName(), fileAndItsContent);
        }
        return commitAndFilesChanged;
    }
}
