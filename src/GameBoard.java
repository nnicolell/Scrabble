import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The GameBoard class represents the game board logic in Scrabble
 *
 * This class checks the validity of the word, places words on the board,
 * adds up scores to the players’ total, prints the current state of the board to the console
 * and checks the validity of the placement of the word.
 *
 * @author Nivetha Sivasaravanan
 * @author Nicole Lim
 * @author Rimsha Atif
 */
public class GameBoard {
    private String[][] stringBoard; //used for error checking when placing a word
    private Tile[][] tileBoard; //used for displaying the board
    private int rows;
    private int cols;
    private boolean isBoardEmpty;
    private Set<String> wordsOnBoard;
    private Player currentPlayer;
    private BagOfLetters BagOfLetters;

    /**
     * Constructor to initialize the game board with the specified columns and rows. Also initializes the first player.
     *
     * @param rows The number of rows on the board
     * @param cols The number of rows on the board
     * @param currentPlayer The first player
     */
    public GameBoard (int rows, int cols, Player currentPlayer) {
        wordsOnBoard = new HashSet<>();
        this.rows = rows;
        this.cols = cols;
        isBoardEmpty = true;
        this.currentPlayer = currentPlayer;
        this.BagOfLetters = new BagOfLetters();

        stringBoard = new String[rows][cols];
        tileBoard = new Tile[rows][cols];
        //initialize places in the board
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                stringBoard[i][j] = "_";
            }
        }

        //initialize new tiles on the board
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                tileBoard[i][j] = new Tile();
            }
        }
    }

    /**
     * Check Validaty of the word by comparing it with the API
     * @param word the word entered by the player
     * @return true if the word is valid; otherwise false
     */
    public boolean checkWord (String word) {
        //testing purposes
        if (word.equals("BE"))
            return true;
        try {
            HttpURLConnection connection = null;
            URL url = new URL("https://api.dictionaryapi.dev/api/v2/entries/en/" + word);
            //We will probably use this API for future milestones since some basic words are missing
            //from the above API, but I haven't gotten the key for it yet.
//            URL url = new URL("https://api.wordnik.com/v4/word.json/" + word + "/definitions?limit=200&includeRelated=false&sourceDictionaries=all&useCanonical=false&includeTags=false&api_key=YOURAPIKEY");

            connection = (HttpURLConnection) url.openConnection();

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            rd.close();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Check the surrounding words (if any) that are created when the player makes a move
     * @return true, if the surrounding words are valid otherwise false
     */
    public boolean checkNewWords() {
        int tempRow = 0;
        int tempCol = 0;
        ArrayList<String> tempNewWords = new ArrayList<>();
        //check the row and col of the word that was just added
        String wordToCheck = "";

        //go through the board left to right and look for complete words
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (!(stringBoard[i][j]).equals("_")) {
                    wordToCheck += stringBoard[i][j];
                    tempRow = i;
                    tempCol = j;
                } else {
                    if(wordToCheck.length() > 1) { //if it's word longer than 1 letter
                        if (checkWord(wordToCheck)) { //if its a real word
                            wordToCheck += " " + tempRow + (tempCol-wordToCheck.length()+1);
                            tempNewWords.add(wordToCheck); //add to arratList bc we don't know if valid placement or not
                        } else {
                            System.out.println("Invalid placement: " + wordToCheck + " is not a valid word.");
                            for (int k = 0; k < rows; k++) {
                                for (int n = 0; n < cols; n++) {
                                    if (tileBoard[k][n].isEmpty())
                                        stringBoard[k][n] = "_";
                                    else
                                        stringBoard[k][n] = tileBoard[k][n].getLetter().getLetter() + "";
                                }
                            }
                            return false;
                        }
                    }
                    wordToCheck = "";
                }

            }
        }

        //go through the board up to down and look for complete words
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                if (!(stringBoard[j][i]).equals("_")) {
                    wordToCheck += stringBoard[j][i];
                    tempRow = j;
                    tempCol = i;
                } else {
                    if(wordToCheck.length() > 1) { //if the word is more than 1 letter
                        if (checkWord(wordToCheck)) { //if its an actual word
                            wordToCheck += " " + (tempRow-wordToCheck.length()+1) + tempCol;
                            tempNewWords.add(wordToCheck); //add to arratList bc we don't know if valid placement or not
                        } else {
                            System.out.println("Invalid placement: " + wordToCheck + " is not a valid word.");
                            for (int k = 0; k < rows; k++) {
                                for (int n = 0; n < cols; n++) {
                                    if (tileBoard[k][n].isEmpty())
                                        stringBoard[k][n] = "_";
                                    else
                                        stringBoard[k][n] = tileBoard[k][n].getLetter().getLetter() + "";
                                }
                            }
                            return false;
                        }
                    }
                    wordToCheck = "";
                }

            }
        }
        for (String s: tempNewWords) { //only add the word if the placement of the new word is valid
            if (wordsOnBoard.add(s)) {
                calculateScore(s.split(" ")[0], currentPlayer);
            }
        }
        return true;
    }

    /**
     * Place the word entered by the player p on the board
     * @param play String containing the word and its placement
     * @param p The player who entered the word
     */
    public void placeWord (String play, Player p) {
        currentPlayer = p;

        int row = 0;
        int col = 0;

        String word = play.split(" ")[0]; //gets the word
        String place = play.split(" ")[1]; //gets where the word will be placed

        if(isBoardEmpty){
            if(!checkCenterSquare(word, place)){ //if not placed on center square return
                return;
            }
        }

        if(!checkLetters(word)){
            System.out.println("You do not have the correct letters to place that word. Try again");
            return;
        }
        char commonChar = ' '; //character that is shared between word being places and existing word
        int commonCharIndex = word.indexOf('('); //index of that char in new word

        //get the index of the row and col where the word will be placed
        if (Character.isDigit(place.charAt(0))) {
            row = Character.getNumericValue(place.charAt(0)) - 1;
            col = place.toUpperCase().charAt(1) - 'A'; //cols starts at A, so we find the offset
        } else {
            col = place.toUpperCase().charAt(0) - 'A';
            row = Character.getNumericValue(place.charAt(1)) - 1;
        }
        Matcher matcher = Pattern.compile("\\((.)\\)").matcher(word);
        boolean matched = matcher.find();
        //get word and remove brackets
        if (!isBoardEmpty) {
            //find the character between the brackets (to make sure they are placing in the same spot)
            if (matched) {
                if(word.length()==3) {
                    System.out.println("Cannot place overlapping single letter.");
                    return;
                }
                commonChar = matcher.group(1).toUpperCase().charAt(0);
                if (word.charAt(0) == '(') {
                    word = commonChar + word.split("\\)")[1];
                } else if (word.charAt(word.length()-1) == ')') {
                    word = word.split("\\(")[0] + commonChar;
                } else {
                    word = word.split("\\(")[0] + commonChar + word.split("\\)")[1];
                }
            } else { //if there is no overlapping letter, check if floating
                if(isFloating(word, place)) {
                    System.out.println(word + " is floating, invalid play");
                    return;
                }
            }
        }

        if (checkWord(word)) {
            word = word.toUpperCase();
            place = place.toUpperCase();
            //if first char is a digit then we place horizontally
            if (Character.isDigit(place.charAt(0))) {
                //error check: if the word placement exceeds # of cols, then return
                if (col + word.length() > cols) {
                    System.out.println(word + ": This doesn't fit on the board");
                    return;
                }
                //check to see if the overlapping letter is in the right spot
                if((!isBoardEmpty) && (matched)) {
                    if (!(stringBoard[row][col + commonCharIndex].equals(commonChar + ""))) {
                        System.out.println(word + ": Invalid placement, overlapping char not in right spot." );
                        return;
                    }
                }

                for (int i = 0; i < word.length(); i++) {
                    if (stringBoard[row][i + col].equals("_")|| (stringBoard[row][i + col].equals(word.charAt(i)+""))) {
                        stringBoard[row][i + col] = word.charAt(i) + "";
                    } else {
                        System.out.println(word + ": This doesn't fit here.");
                        return;
                    }

                }
                if (checkNewWords()) {
                    for (int i = 0; i < word.length(); i++) {
                        tileBoard[row][i + col].placeLetter(new Letters(stringBoard[row][i + col].toUpperCase().charAt(0)));
                    }
                    updateLetters(word);
                }
            } else { //else we place vertically
                //error check: if the word placement exceeds # of rows, then return
                if (row + word.length() > rows) {
                    System.out.println(word + ": This doesn't fit on the board");
                    return;
                }

                //check to see if the overlapping letter is in the right spot if there is an overlapping letter
                if((!isBoardEmpty) && (matched)) {
                    if (!(stringBoard[row + commonCharIndex][col].equals(commonChar + ""))) {
                        System.out.println(word + ": Invalid placement, overlapping char not in right spot.");
                        return;
                    }
                }

                for (int i = 0; i < word.length(); i++) {
                    if (stringBoard[i + row][col].equals("_") || (stringBoard[i + row][col].equals(word.charAt(i) + ""))) {
                        stringBoard[i + row][col] = word.charAt(i) + "";
                    } else {
                        System.out.println(word + ": This doesn't fit here");
                        return;
                    }
                }
                if (checkNewWords()) {
                    for (int i = 0; i < word.length(); i++) {
                        tileBoard[i + row][col].placeLetter(new Letters(stringBoard[i + row][col].toUpperCase().charAt(0)));
                    }
                    updateLetters(word);
                }
            }
            printGameStatus();
            isBoardEmpty = false;
        } else {
            System.out.println(word + " is not a valid word.");
        }
    }

    public boolean isFloating(String word, String place) {
        int row, col;
        boolean horizontal;
        if (Character.isDigit(place.charAt(0))) { //horizontal
            horizontal = true;
            row = Character.getNumericValue(place.charAt(0)) - 1;
            col = place.toUpperCase().charAt(1) - 'A'; //cols starts at A, so we find the offset
        } else {
            horizontal = false;
            col = place.toUpperCase().charAt(0) - 'A';
            row = Character.getNumericValue(place.charAt(1)) - 1;
        }


        if (word.length() == 1) {
            int emptySpace = 0; //how many empty spaces there are around the letter
            int totalEmptySpace = 4; //how many empty spaces there would be if floating. decremented if edge case.
            //if top is empty for single char, then its floating
            try {
                emptySpace = (stringBoard[row - 1][col].equals("_")) ? emptySpace + 1 : emptySpace;
            } catch (ArrayIndexOutOfBoundsException e) {totalEmptySpace--;}

            //if bottom is empty for single char, then its floating
            try {
                emptySpace = (stringBoard[row + 1][col].equals("_")) ? emptySpace + 1 : emptySpace;
            } catch (ArrayIndexOutOfBoundsException ignored) {totalEmptySpace--;}

            //if left is empty for single char, then its floating
            try {
                emptySpace = (stringBoard[row][col - 1].equals("_")) ? emptySpace + 1 : emptySpace;
            } catch (ArrayIndexOutOfBoundsException ignored) {totalEmptySpace--;}

            //if right is empty for single char, then its floating
            try {
                emptySpace = (stringBoard[row][col + 1].equals("_")) ? emptySpace + 1 : emptySpace;
            } catch (ArrayIndexOutOfBoundsException ignored) {totalEmptySpace--;}

            return emptySpace == totalEmptySpace; //if 4 empty spaces, then floating
        } else {
            if (horizontal) {
                for (int i = 0; i < word.length(); i++) { //for each letter, check above below
                    //check space above the word
                    try {
                        if (!(stringBoard[row - 1][col+i].equals("_")))
                            return false;
                    } catch (ArrayIndexOutOfBoundsException ignored) {}

                    //check space below the word
                    try {
                        if (!(stringBoard[row + 1][col-1].equals("_"))) //if there is a letter, automatically return false
                            return false;
                    } catch (ArrayIndexOutOfBoundsException ignored) {}

                    //check beside first letter
                    try {
                        if (!(stringBoard[row][col-1].equals("_"))) //if there is a letter, automatically return false
                            return false;
                    } catch (ArrayIndexOutOfBoundsException ignored) {}

                    //check beside last letter
                    try {
                        if (!(stringBoard[row][col+word.length()].equals("_"))) //if there is a letter, automatically return false
                            return false;
                    } catch (ArrayIndexOutOfBoundsException ignored) {}
                }
            } else { //if vertical
                for (int i = 0; i < word.length(); i++) { //for each letter, check above below
                    //check space to the left of word
                    try {
                        if (!(stringBoard[row+i][col-1].equals("_")))
                            return false;
                    } catch (ArrayIndexOutOfBoundsException ignored) {}

                    //check space to the right of word
                    try {
                        if (!(stringBoard[row+i][col+1].equals("_"))) //if there is a letter, automatically return false
                            return false;
                    } catch (ArrayIndexOutOfBoundsException ignored) {}

                    //check above top letter
                    try {
                        if (!(stringBoard[row-1][col].equals("_"))) //if there is a letter, automatically return false
                            return false;
                    } catch (ArrayIndexOutOfBoundsException ignored) {}

                    //check below bottom letter
                    try {
                        if (!(stringBoard[row+word.length()][col].equals("_"))) //if there is a letter, automatically return false
                            return false;
                    } catch (ArrayIndexOutOfBoundsException ignored) {}
                }
            }
        }

        return true;
    }

    public boolean checkCenterSquare(String word, String place){
        int centerRow = 7;
        Character c = 'H';
        int centerCol = c - 'A';



        if (Character.isDigit(place.charAt(0))){ //check horizontal placement
            int row = Character.getNumericValue(place.charAt(0)) - 1;
            int col = place.toUpperCase().charAt(1) - 'A';

            for(int i = 0; i<word.length(); i++){
                col ++;
                if (row == centerRow && col == centerCol){
                    return true;
                }
            }
        }
        else { //check vertical placement
            int col = place.toUpperCase().charAt(0) - 'A';
            int row = Character.getNumericValue(place.charAt(1)) - 1;
           for(int i = 0; i< word.length(); i++){
               row ++;
               if(col == centerCol && row == centerRow){
                   return true;
               }
           }
        }
        System.out.println("The first word must be placed on the center square");
        return false;
    }

    /**
     * This method checks that word can be created with the letters dealt to the player
     * @param word the word entered
     * @return true if the word can be created with the player's letter, false otherwise
     */
    public boolean checkLetters(String word) {
        int i = 0;
        int n;
        boolean contains = true;

        word = word.toUpperCase();

        while (i < word.length()) {
            if(!contains){
               // i= word.length();
                break;
            }

            for (n=0; n < currentPlayer.getLetters().size(); n++){
                if(word.charAt(i) == (currentPlayer.getLetters().get(n).getLetter())){
                    currentPlayer.getLetters().get(n).setLetter(Character.toLowerCase(currentPlayer.getLetters().get(n).getLetter()));
                    contains = true;
                    break;
                }
                else{
                    contains = false;
                }
            }
            i++;
        }

        for(Letters l: currentPlayer.getLetters()){
            l.setLetter(Character.toUpperCase(l.getLetter()));
        }
        return contains;
    }


    /**
     * This method deletes the used letters from the currentPlayers Letters list
     * @param word The word entered by the currentPlayer
     */
    public void updateLetters(String word){
        int i = 0;
        int n = 0;
        while (i < word.length()) {
            for (n=0; n < currentPlayer.getLetters().size(); n++){
                if(word.charAt(i)==currentPlayer.getLetters().get(n).getLetter()){
                    currentPlayer.getLetters().remove(n);
                    System.out.println("");
                    break;
                }
            }
            i++;
        }
        currentPlayer.setLetters(deal(currentPlayer.getLetters()));
    }

    /**
     * Calculates the score of the player after their turn
     * @param word the word they entered
     * @param p the currentPlayer
     * @return the interger value of the score of the word
     */
    private int calculateScore(String word, Player p){
        int score=0;
        int i = 0;

        while(i<word.length()){
            Letters nl = new Letters(word.charAt(i));
            score += nl.getPointValue(nl.getLetter());
            i++;
        }

        p.setScore(score);
        System.out.println("Yay! You scored " + score + " points for " + word);


        return score;
    }

    /**
     * This method deals an ArrayList of random Letters. If the currentLetter already contains some letters, 7- the number of current letters are dealt
     * Otherwise, 7 random letters are dealt
     * @param currentLetters the ArrayList representing the letters the player currently has
     * @return the new list of letters, that was randomly created
     */
    public ArrayList<Letters> deal(ArrayList<Letters> currentLetters){
        Random r = new Random();
        Object[] keys = BagOfLetters.getBag().keySet().toArray();
        ArrayList<Letters> newLetters = new ArrayList<>();

        int n;  //number of letters needed to be dealt
        n= 7- currentLetters.size();;

        //copy currentLetters into the newList
        for(Letters l: currentLetters){
            newLetters.add(l);
        }

        //randomly deal n new letters to the player
        for(int i=0; i<n; i++){

            Letters newLetter = (Letters) keys[r.nextInt(keys.length)];

            while(!BagOfLetters.inBag(newLetter)){
                newLetter = (Letters) keys[r.nextInt(keys.length)];
            }

            newLetters.add(newLetter);
        }

        return newLetters;
    }

    /**
     * This method prints the board after the word is placed on it. It also prints the total score of the currentPlayer.
     */
    public void printGameStatus(){
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Tile t = tileBoard[i][j];
                if (t.isEmpty())
                    System.out.print("_ ");
                else
                    System.out.print(t.getLetter().getLetter() + " ");
                }
                System.out.println();
            }
            System.out.println();

        System.out.println(currentPlayer.getName() + " Your total score is now: " + currentPlayer.getScore());

        String s = "";
        for(Letters l: currentPlayer.getLetters()){
            s += l.getLetter() + ", ";
        }

    }


}
