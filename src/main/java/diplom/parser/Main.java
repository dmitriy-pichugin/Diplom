package diplom.parser;

public class Main {
    public static void main(String[] args){
        String[] filenames = new String[2];
        filenames[0] = "C:/Diplom/Test/tab1.txt";
        filenames[1] = "C:/Diplom/Test/tab2.txt";
        Parser parser = new Parser();
        parser.parse(filenames);

    }
}
