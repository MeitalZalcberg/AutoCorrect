import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Stream;

public class AutoCorrect {
    List<String> words;
    final int numChars = 26;
    final int asciiCoding = 97;
    List<int[][]> confusionMaps = new ArrayList<>();
    int transIdx = 0;
    int subIdx = 1;
    int insIdx = 2;
    int delIdx = 3;
    int spaceCount = 3000;
    int[] charsCount = new int[numChars + 1];
    int[][] chars2way = new int[numChars + 1][numChars + 1];
    Map<String, Double> wordprobs;
    List<String> candidatesOneEdit;
    List<String> candidatesTwoEdit;
    List<String> candidatesThreeEdit;
    final private int ERRORCODE = -1;
    final private int SUCCESSCODE = 1;


    /**
     * constructor for the auto corrector
     *
     * @param confusionPaths paths to the four matrix's of confusion
     * @param corpusPath     path to corpus
     * @throws FileNotFoundException
     */
    public AutoCorrect(String[] confusionPaths, String corpusPath) throws FileNotFoundException {
        uploadConfusions(confusionPaths);
        storeProbabilities(corpusPath);
    }

    /**
     * uploads the confusion matrix to the arrray of matrix's
     *
     * @param paths to the confusion files
     * @return error code if file wasn't found
     */
    public int uploadConfusions(String[] paths) {
        for (int mapIndex = 0; mapIndex < paths.length; mapIndex++) {
            File matrixFile = new File(paths[mapIndex]);
            if (mapIndex < 2) {
                confusionMaps.add(new int[numChars][numChars]);
            }
            // deletion and insertion have the null char as well
            else {
                confusionMaps.add(new int[numChars][numChars + 1]);
            }
            try {
                Scanner sc = new Scanner(matrixFile);
                for (int i = 0; i < numChars; i++) {
                    for (int j = 0; j < numChars; j++) {
                        if (!sc.hasNextInt()) {
                            //check if file is corrupted
                            //TODO deal with errors
                            System.out.println("not enough chars");
                        }
                        confusionMaps.get(mapIndex)[i][j] = sc.nextInt();

                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return ERRORCODE;
            }
            System.out.println("uploaded confuion matrix number: " + mapIndex);
        }
        return SUCCESSCODE;
    }

    /**
     * This Methods calculates the levenshtein distance between two strings.
     * This implementation uses dynamic programming to perform faster.
     * <p>
     * for more information see: https://en.wikipedia.org/wiki/Levenshtein_distance
     * https://en.wikipedia.org/wiki/Damerau%E2%80%93Levenshtein_distance
     *
     * @param a the original word
     * @param b the second, optional correction word
     * @return the distance between the two
     */
    public int levenshteinDist(String a, String b) {
        int[][] distanceMatrix = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            distanceMatrix[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            distanceMatrix[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                //check the minimum between the possible operations:
                // deletion, insertion, substitution and transposition.

                distanceMatrix[i][j] =
                        min(distanceMatrix[i - 1][j - 1] + cost(a.charAt(i - 1), b.charAt(j - 1)), //substitution
                                distanceMatrix[i - 1][j] + 1, //deletion
                                distanceMatrix[i][j - 1] + 1); // insertion
                //transposition
                if (((i > 1) && (j > 1)) && (a.charAt(i - 1) ==
                        b.charAt(j - 2)) && (a.charAt(i - 2) == b.charAt(j - 1))) {
                    distanceMatrix[i][j] = min(distanceMatrix[i][j], distanceMatrix[i - 2][j - 2] +
                            cost(a.charAt(i - 1), b.charAt(j - 1)));
                }
            }
        }
        return distanceMatrix[a.length()][b.length()];
    }


    /**
     * This Methods calculates the "cost" of switching two characters.
     * If they are the same we get a 0 cost, else 1.
     *
     * @param a first char
     * @param b second char
     * @return cost
     */
    public static int cost(char a, char b) {
        return a == b ? 0 : 1;
    }


    /**
     * checks for the minimal int in the given array
     *
     * @param numbers array of ints
     * @return minimum value in the aray
     */
    public static int min(int... numbers) {
        return Arrays.stream(numbers)
                .min().orElse(Integer.MAX_VALUE);
    }

    public int getBestLevensteins(String word) {
        candidatesOneEdit = new ArrayList<>();
        candidatesTwoEdit = new ArrayList<>();
        candidatesThreeEdit = new ArrayList<>();
        for (Map.Entry<String, Double> entry : wordprobs.entrySet()) {
            if (levenshteinDist(word, entry.getKey()) == 1) {
                candidatesOneEdit.add(entry.getKey());
            } else if (levenshteinDist(word, entry.getKey()) == 2) {
                candidatesTwoEdit.add(entry.getKey());
            }
            // TODO decide whether to keep three edits as well
            else if (levenshteinDist(word, entry.getKey()) == 3) {
                candidatesThreeEdit.add(entry.getKey());
            }
        }
        if (candidatesOneEdit.size() == 0 && candidatesTwoEdit.size() == 0) {
            return ERRORCODE;
        }
        return SUCCESSCODE;
    }


    /**
     * This is the runner of the class, given a typo word, it will go over the
     * possible candidates from "words" and score them.
     *
     * @param typo the typed word
     */
    public void printOptions(String typo) {
        //edit distance 1
        Map<String, Double> finalOptions = new HashMap<>();
        getBestLevensteins(typo);
        ArrayList<String> probs = new ArrayList<>();
        double prob;
        double sum = 0;
        // if there aren't enough words in the candidate list with errir one
        if (candidatesOneEdit.size() < 3) {
            for (int i = 0; i < min(candidatesTwoEdit.size(), 7); i++) {
                candidatesOneEdit.add(candidatesTwoEdit.get(i));
            }
        }
        for (int i = 0; i < candidatesOneEdit.size(); i++) {
            prob = getCondProb(typo, candidatesOneEdit.get(i)) * wordprobs.get(candidatesOneEdit.get(i));
            finalOptions.put(candidatesOneEdit.get(i), prob);
            sum += prob;
        }
        for (Map.Entry<String, Double> entry : finalOptions.entrySet()) {
            //normalization of probabilities
            finalOptions.put(entry.getKey(), entry.getValue() / sum * 100);
        }
        Stream<Map.Entry<String, Double>> sorted =
                finalOptions.entrySet().stream().sorted(Map.Entry.comparingByValue());
        sorted.forEach(item -> probs.add(String.valueOf(item)));
        Collections.reverse(probs);
        // if word in dictionary:
        if (wordprobs.containsKey(typo)) {
            System.out.println(typo + " " + 100);
        }
        for (String item : probs) {
            item = item.replace('=', ' ');
            System.out.println(item);
        }
    }


    /**
     * This method creates the wordProbs - the word probability frequencies
     * the frequency of the words is computed from the file in path
     * if there is no corpus for prior distribution of the words, the file given
     * should contain a list of the words that would be the dictionary.
     *
     * @param path path for corpus or list of wrds
     * @return error code if file wasn't found, success code else
     */
    public int storeProbabilities(String path) {
        File wordsFile = new File(path);
        wordprobs = new HashMap<>();
        int N = 0;
        for (int i = 0; i <= numChars; i++) {
            if (i == numChars) {
                charsCount[i] = spaceCount;
            } else charsCount[i] = 0;
            for (int j = 0; j <= numChars; j++) {
                if (i == numChars || j == numChars) {
                    chars2way[i][j] = spaceCount;
                } else {
                    chars2way[i][j] = 0;
                }
            }
        }
        try {
            Scanner sc = new Scanner(wordsFile);
            String word;
            int letterIdx;
            words = new ArrayList<>();
            while (sc.hasNext()) {
                word = sc.next();
                // calculate the char probabilities
                for (int i = 0; i < word.length(); i++) {
                    letterIdx = word.charAt(i) - asciiCoding;
                    charsCount[letterIdx]++;
                    if (i < word.length() - 1) {
                        chars2way[letterIdx][word.charAt(i + 1) - asciiCoding]++;
                    }
                }
                N++;
                if (wordprobs.containsKey(word)) {
                    wordprobs.put(word, wordprobs.get(word) + 1);
                } else {
                    wordprobs.put(word, (double) 1);
                }
            }
            for (Map.Entry<String, Double> entry : wordprobs.entrySet()) {
                wordprobs.put(entry.getKey(), entry.getValue() / N);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ERRORCODE;
        }
        System.out.println("ceated corpus and probabilities");
        return SUCCESSCODE;
    }


    /**
     * this function returns the probability values for the word option to be the right auto correct for the typo
     *
     * @param typo   typed word that is a mistake
     * @param option candidate for correction
     * @return the probability, error code if no probability was found
     */
    private double getCondProb(String typo, String option) {
        // todo test cases for the null char
        double prtc;
        double denominator;
        int x;
        int y;
        for (int i = 0; i < typo.length(); i++) {
            //check transposition
            if (typo.length() == option.length()) {
                if (i < typo.length() - 1) {
                    if (typo.charAt(i) == option.charAt(i + 1) && typo.charAt(i + 1) == option.charAt(i)) {
                        x = option.charAt(i) - asciiCoding;
                        y = option.charAt(i + 1) - asciiCoding;
                        prtc = confusionMaps.get(transIdx)[x][y];
                        denominator = chars2way[x][y];
                        //todo check zero
                        return prtc / denominator;
                    }
                }
                //if substitution
                if (typo.charAt(i) != option.charAt(i)) {
                    x = typo.charAt(i) - asciiCoding;
                    y = option.charAt(i) - asciiCoding;
                    prtc = confusionMaps.get(subIdx)[x][y];
                    denominator = charsCount[y];
                    return prtc / denominator;
                }
            }
            // check insertion
            if (typo.length() > option.length()) {
                if (i == typo.length() - 1) {
                    x = option.charAt(i - 1) - asciiCoding;
                    y = typo.charAt(i) - asciiCoding;
                    prtc = confusionMaps.get(insIdx)[x][y];
                    denominator = charsCount[x];
                    return prtc / denominator;
                } else if (typo.charAt(i) != option.charAt(i)) {
                    if (i == 0) {
                        x = numChars;
                    } else {
                        x = option.charAt(i - 1) - asciiCoding;
                    }
                    y = typo.charAt(i) - asciiCoding;
                    prtc = confusionMaps.get(insIdx)[y][x];
                    denominator = charsCount[y];
                    return prtc / denominator;
                }
            }
            // check deletion
            if (typo.length() < option.length()) {
                if (typo.charAt(i) != option.charAt(i)) {
                    if (i == 0) {
                        y = numChars;
                    } else {
                        y = option.charAt(i - 1) - asciiCoding;
                    }
                    x = option.charAt(i) - asciiCoding;
                    prtc = confusionMaps.get(delIdx)[x][y];
                    denominator = chars2way[x][y];
                    return prtc / denominator;
                }
            }
        }
        return ERRORCODE;
    }


    public static void main(String[] args) throws FileNotFoundException {
        //use example:
        if (args.length < 4) {
            System.out.println("wrong use of args, please enter the correct file paths");
        }
        String corpusPath = args[4];
        String[] confusionPaths = {args[0], args[1], args[2], args[3]};
        AutoCorrect correcti = new AutoCorrect(confusionPaths, corpusPath);
        Scanner sc = new Scanner(System.in);
        String word = "hi";
        System.out.println("Welcome to the auto correct! In any time, press enter key instead of word to exit");
        while (!word.equals("")) {
            System.out.println("Please enter a word to autocorrect");
            word = sc.nextLine();
            correcti.printOptions(word);
        }
    }
}