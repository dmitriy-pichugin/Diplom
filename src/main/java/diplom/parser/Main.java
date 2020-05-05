package diplom.parser;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        String[] paths = new String[4];
        paths[0] = "C:/Diplom/Test/CLIENTS.txt";
        paths[1] = "C:/Diplom/Test/CR_TYPE.txt";
        paths[2] = "C:/Diplom/Test/REPAYMENT.txt";
        paths[3] = "C:/Diplom/Test/CREDITS.txt";
        Parser parser = new Parser();
        parser.parse(paths);
        Session session = parser.getSession();
        System.out.println(session.getTables().isEmpty());
        Formatter formatter = new Formatter();
        formatter.addln(formatter.start_graph());
        formatter.add(formatter.toDotFormat(session));
        formatter.addln(formatter.end_graph());
        String type = "pdf";
        File out = new File("testGraph" + "." + type);
        System.out.println(formatter.getDotSource());
        formatter.writeGraphToFile( formatter.getGraph( formatter.getDotSource(), type ), out );
    }
}
