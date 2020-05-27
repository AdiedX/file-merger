package main.java.com.adi.merge_files;

import com.adi.merge_files.service.MergeFilesService;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

public class MergeFilesApp {
  private MergeFilesService mergeFilesService;

  public static void main(String... args) {
    // Grab input file names from inputDir and compile list of such names
    try (Stream<Path> paths = Files.walk(Paths.get(inputDir))) {
      paths.filter(Files::isRegularFile).forEach(System.out::println);
    } catch (IOException e) {
      e.printStackTrace();
    }

    MergeFilesService mergeFilesService = new MergeFilesService("./input_files/", "./output.txt");

    try {
      mergeFilesService.mergeFiles(Arrays.asList("a.txt", "b.txt", "c.txt"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
