package com.adi.merge_files.service;

import com.google.code.externalsorting.ExternalSort;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MergeFilesService {
  private final String INPUT_DIR;
  private final String OUTPUT_FILE_PATH;
  private final String CURRENT_DIR;

  public MergeFilesService(String inputDir, String outputFilePath) {
    this.INPUT_DIR = inputDir;
    this.OUTPUT_FILE_PATH = outputFilePath;
    this.CURRENT_DIR = Paths.get("").toAbsolutePath().toString();
  }

  public void mergeFiles(List<String> fileNames) throws Exception {
    for (String fileName : fileNames) {
      removeEmptyLines(getPath(INPUT_DIR, fileName));

      if (!checkIfSorted(getPath(INPUT_DIR, fileName))) {
        throw new Exception("Input file not sorted");
      }
    }

    List<String> sortedFilePaths = new ArrayList<>();
    for (String fileName : fileNames) {
      sortedFilePaths.add(getPath(INPUT_DIR, transformFileName(fileName, "sorted")));
    }

    merge(sortedFilePaths, getPath(CURRENT_DIR, "merged.txt"));
    sortFile(getPath(CURRENT_DIR, "merged.txt"), getPath(CURRENT_DIR,"merged_sorted.txt"));
    removeDuplicates(getPath(CURRENT_DIR, "merged_sorted.txt"), OUTPUT_FILE_PATH);

    for (String fileName : fileNames) {
      Files.delete(Paths.get(getPath(INPUT_DIR, transformFileName(fileName, "trimmed"))));
      Files.delete(Paths.get(getPath(INPUT_DIR, transformFileName(fileName, "sorted"))));
    }
  }

  private String getPath(String dir, String fileName) {
    return dir + fileName;
  }

  private void removeEmptyLines(String filePath) throws IOException {
    // Create output file to write trimmed output to
    String trimmedInputFileName = transformFileName(filePath, "trimmed");
    // Remove empty spaces
    BufferedWriter writer = new BufferedWriter(new FileWriter(getPath(INPUT_DIR, trimmedInputFileName), true));
    // Read file via input stream
    try (FileInputStream inputStream = new FileInputStream(filePath); Scanner scanner = new Scanner(inputStream, "UTF-8")) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        // Skip empty lines
        if (!line.isEmpty()) {
          // Write to new output file without the empty lines
          writer.write(line);
          writer.write("\n");
        }
      }

      writer.close();
      if (scanner.ioException() != null) {
        System.out.println(scanner.ioException().getMessage());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void write(String inputFilePath, String outputFilePath) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true));
    // Read file via input stream
    try (FileInputStream inputStream = new FileInputStream(inputFilePath); Scanner scanner = new Scanner(inputStream, "UTF-8")) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        writer.write(line);
        writer.write("\n");
      }

      writer.close();
      if (scanner.ioException() != null) {
        System.out.println(scanner.ioException().getMessage());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private boolean checkIfSorted(String filePath) throws IOException {
    // Externally sort file.txt into output file (file_sorted.txt)
    String sortedOutputFileName = transformFileName(filePath, "sorted");
    String trimmedInputFileName = transformFileName(filePath, "trimmed");
    try {
      String trimmedInputFileNamePath = getPath(INPUT_DIR, trimmedInputFileName);
      String sortedOutputFileNamePath = getPath(INPUT_DIR, sortedOutputFileName);
      ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(new File(trimmedInputFileNamePath)), new File(sortedOutputFileNamePath));
    } catch (IOException e) {
      e.printStackTrace();
    }

    return areContentsEqual(INPUT_DIR + trimmedInputFileName, INPUT_DIR + sortedOutputFileName);
  }

  private void merge(List<String> filePaths, String mergedFilePath) throws IOException {
    for (String filePath : filePaths) {
      write(filePath, mergedFilePath);
    }
  }

  private void sortFile(String filePath, String outputFilePath) throws IOException {
    ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(new File(filePath)), new File(outputFilePath));
  }

  private void removeDuplicates(String fileWithDuplicatesPath, String outputFilePath) throws IOException {
    // Read file via input stream
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true));
    String previousLine;

    try(Scanner fileReader = new Scanner(new File(fileWithDuplicatesPath))) {
      previousLine = fileReader.nextLine();
    }

    try (FileInputStream inputStream = new FileInputStream(fileWithDuplicatesPath); Scanner scanner = new Scanner(inputStream, "UTF-8")) {
      // Write first line
      writer.write(previousLine);
      writer.write("\n");

      // Skip first line, since we have already read and wrote it
      if (scanner.hasNextLine())
        scanner.nextLine();

      // Start from second line
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();

        // Skip empty lines
        if (line.equals(previousLine)) {
          continue;
        } else {
          // Write to new output file without the empty lines
          writer.write(line);
          writer.write("\n");
        }

        previousLine = line;
      }

      writer.close();
      if (scanner.ioException() != null) {
        System.out.println(scanner.ioException().getMessage());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private boolean areContentsEqual(String fileAPath, String fileBPath) throws IOException {
    Path fileA = Paths.get(fileAPath);
    Path fileB = Paths.get(fileBPath);

    final long size = Files.size(fileA);

    if (size != Files.size(fileB)) {
      return false;
    }

    if (size < 4096) {
      return Arrays.equals(Files.readAllBytes(fileA), Files.readAllBytes(fileB));
    }

    try (InputStream streamA = Files.newInputStream(fileA); InputStream streamB = Files.newInputStream(fileB)) {
      int data;
      while ((data = streamA.read()) != -1) {
        if (data != streamB.read()) {
          return false;
        }
      }
    }

    return true;
  }

  private String transformFileName(String filePath, String suffix) {
    String[] r = filePath.split("/");
    String fileName = r[r.length - 1].split(".txt")[0];
    return fileName + "_" + suffix + ".txt";
  }
}
