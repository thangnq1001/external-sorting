import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Memory unit: Byte<br>
 * Please put ExternalSorting.class in same dir with input file<br>
 * The output file will be created in same dir also<br><br>
 * Usage example with input file: "input.txt", output file: "output.txt", memory limit: 16MB:<br>
 * javac ExternalSorting.java<br>
 * java ExternalSorting input.txt output.txt 16777216<br><br>
 * Implemented using jdk1.8.0_231
 */
public class ExternalSorting {
    // 60% is buffered for sorting, background programs,...
    private static final float BYTES_USED_PER_PART_FACTOR = 0.4f;

    public static void main(String[] args) {
        System.out.println("Sorting with params: " + Arrays.toString(args) + "...");
        // get params & set configs
        String inputFileName = args[0];
        String outputFileName = args[1];
        long memoryLimitInBytes = Long.parseLong(args[2]);
        long readLimitPerPartInBytes = (long) (memoryLimitInBytes * BYTES_USED_PER_PART_FACTOR);

        // start sorting
        try (BufferedReader inputFileReader = new BufferedReader(new FileReader(getAbsolutePath(inputFileName)));
             BufferedWriter outputWriter = new BufferedWriter(new FileWriter(outputFileName))
        ) {
            // for each part: read, sort, then write out to temp file
            boolean isEndOfFileReached = false;
            int partCount = 0;
            while (!isEndOfFileReached) {
                List<String> lines = new ArrayList<>();
                long loadedBytes = 0;
                while (loadedBytes < readLimitPerPartInBytes) {
                    String line = inputFileReader.readLine();
                    if (line == null) {
                        isEndOfFileReached = true;
                        break;
                    }
                    loadedBytes += line.getBytes(StandardCharsets.UTF_8).length;
                    lines.add(line);
                }
                if (lines.isEmpty()) {
                    continue;
                }
                partCount++;
                Collections.sort(lines);
                File partFile = new File(getPartFileName(partCount - 1));
                partFile.deleteOnExit();
                try (BufferedWriter partFileWriter = new BufferedWriter(new FileWriter(partFile))) {
                    for (String line : lines) {
                        partFileWriter.write(line);
                        partFileWriter.newLine();
                    }
                }
            }
            if (partCount == 0) {
                System.out.println("Input is empty, " + outputFileName + " will be an empty file.");
            } else {
                // merge all sorted parts using a min heap
                BufferedReader[] partFileReaders = new BufferedReader[partCount];
                PriorityQueue<MinHeapNode> minHeap = new PriorityQueue<>(Comparator.comparing(a -> a.value));
                for (int partIdx = 0; partIdx < partCount; partIdx++) {
                    partFileReaders[partIdx] = new BufferedReader(new FileReader(getAbsolutePath(getPartFileName(partIdx))));
                    minHeap.add(new MinHeapNode(partFileReaders[partIdx].readLine(), partIdx));
                }
                while (!minHeap.isEmpty()) {
                    MinHeapNode minNode = minHeap.poll();
                    outputWriter.write(minNode.value);
                    outputWriter.newLine();
                    String newLineToAddToHeap = partFileReaders[minNode.partIdx].readLine();
                    if (newLineToAddToHeap != null) {
                        minHeap.add(new MinHeapNode(newLineToAddToHeap, minNode.partIdx));
                    }
                }
                for (BufferedReader partFileReader : partFileReaders) {
                    partFileReader.close();
                }
                System.out.println("Success! Please check " + outputFileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getPartFileName(int partIdx) {
        return "thangnq.tmp.part" + partIdx + ".txt";
    }

    private static String getAbsolutePath(String fileName) {
        return new File("").getAbsolutePath() + "/" + fileName;
    }

    static class MinHeapNode {
        String value;
        int partIdx;

        MinHeapNode(String value, int partIdx) {
            this.value = value;
            this.partIdx = partIdx;
        }
    }
}
