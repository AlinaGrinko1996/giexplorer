package utils;

import entities.Comment;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileUtils {
    private static String SATD_FILENAME = "src/main/resources/SATDKeywords";
    private static String REPOS_FILENAME = "src/main/resources/repositories";

    public static List<String> getSATDKeywords() {
        return readFile(SATD_FILENAME);
    }

    public static List<String> getRepositories() {
        return readFile(REPOS_FILENAME);
    }

    private static List<String> readFile(String filename) {
        List<String> result = new ArrayList<>();
        try {
            FileInputStream fileInputStream = new FileInputStream(filename);
            Scanner scanner = new Scanner(fileInputStream);
            while (scanner.hasNextLine()) {
                result.add(scanner.nextLine());
            }
            scanner.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

//    public static void printReport(int allComments, List<Comment> SATDComments) {
//        return readFile(REPOS_FILENAME);
//    }
}
