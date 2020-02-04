package utils;

import com.google.common.collect.Lists;import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.LargeObjectException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JGitUtils {
    static int CHUNK = 400;
    public static List<String> checkedCommits = new ArrayList<>();
    public static boolean chunkSizeOverloaded = true;

    public static Git cloneRepository(String repoUrl) throws IOException {
       // File tempPath = File.createTempFile("TestRepository", "");
        String[] name = repoUrl.split("/");
        File tempPath = new File("repositories/" + name[name.length - 1]);
        if (tempPath.exists() && tempPath.isDirectory()) {
            return openJGitRepository(tempPath);
        }
//        if (!tempPath.delete()) {
//            throw new IOException("Not deletable temp file " + tempPath);
//        }
        try (Git result = Git.cloneRepository()
                .setCloneAllBranches(true)
                .setURI(repoUrl)
                .setDirectory(tempPath)
                .call()) {

            System.out.println("Repository cloned " + result.getRepository().getDirectory());
            return result;
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Git openJGitRepository(File directory) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder
                .readEnvironment()
                .findGitDir(directory)
                .build();
        return new Git(repository);
    }

    public static List<String> getListOfChangedFiles(Repository repository, Git git, String oldCommit, String newCommit)
            throws GitAPIException, IOException {
        System.out.println(oldCommit + " -> " + newCommit);

        List<DiffEntry> diffs = new ArrayList<>();
        try {
            diffs = git.diff()
                    .setOldTree(getTreeParser(repository, oldCommit))
                    .setNewTree(getTreeParser(repository, newCommit))
                    .call(); //todo here Java heap exception - out of memory
            // Missing blob c306e1647d4afecd5737d6026b256b973c184726
        } catch (OutOfMemoryError | LargeObjectException | JGitInternalException ex) {
            System.out.println("!!!!!!!!! Problems with comparing (old, new) (" + oldCommit + ", " + newCommit + ")");
            return new ArrayList<>();
        }
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
                    ObjectLoader loader = repository.open(objectId); //Missing unknown 7476a425c22d220291324a1060800d9801ca82c7

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
        Git git = JGitUtils.cloneRepository(gitUrl);
        HashMap<String, HashMap<String, String>> commitAndFilesChanged = new HashMap<>();
        Repository repository = git.getRepository();
        List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

        branches.parallelStream().forEach(branch -> {
            if (commitAndFilesChanged.size() < CHUNK) {
                String branchName = branch.getName();

                Iterable<RevCommit> commits = null;
                try {
                    commits = git.log().add(repository.resolve(branchName)).call();
                } catch (GitAPIException | IOException e) {
                    e.printStackTrace();
                }
                List<RevCommit> commitsList = Lists.newArrayList(commits.iterator());

                commitsList.parallelStream().forEach(commit -> {
                    if (commitAndFilesChanged.size() < CHUNK) {
                        if (!checkedCommits.contains(commit.getName())) {
                            List<String> changedFiles = null;
                            HashMap<String, String> fileAndItsContent = null;
                            try {
                                changedFiles = exploreCommit(repository, git, commit);
                                fileAndItsContent = accessingFilesOfCommit(commit, repository, changedFiles);
                            } catch (GitAPIException | IOException e) {
                                e.printStackTrace();
                            }
                            commitAndFilesChanged.put(commit.getName(), fileAndItsContent);
                            checkedCommits.add(commit.getName());
                            System.out.println(branchName + " -> " + commit.getName());
                            chunkSizeOverloaded = false;
                        }
                    } else {
                        chunkSizeOverloaded = true;
                    }
                });
            } else {
                chunkSizeOverloaded = true;
            }
        });
        return commitAndFilesChanged;
    }
}
