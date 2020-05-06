package diplom.parser;

import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

import java.io.*;
import java.util.Properties;

public class Formatter {
    private StringBuilder graph = new StringBuilder();
    private final static String cfgProp = "C:/Diplom/GraphVizApi/Data/config.properties";
    private final static Properties configFile = new Properties() {
        private final static long serialVersionUID = 1L;

        {
            try {
                load(new FileInputStream(cfgProp));
            } catch (Exception e) {
            }
        }
    };
    private static String TEMP_DIR = "C:/Diplom/Tmp/GraphVizTmp";
    private static String DOT = configFile.getProperty("dotForWindows");
    private int[] dpiSizes = {46, 51, 57, 63, 70, 78, 86, 96, 106, 116, 128, 141, 155, 170, 187, 206, 226, 249};
    private int currentDpiPos = 7;

    public void add(String line) {
        this.graph.append(line);
    }

    public void addln(String line) {
        this.graph.append(line + "\n");
    }

    public void addln() {
        this.graph.append('\n');
    }

    public String start_graph() {
        return "digraph G {";
    }

    public String end_graph() {
        return "}";
    }

    public String getDotSource() {
        return this.graph.toString();
    }

    public byte[] getGraph(String dot_source, String type) {
        File dot;
        byte[] img_stream = null;
        try {
            dot = writeDotSourceToFile(dot_source);
            if (dot != null) {
                img_stream = get_img_stream(dot, type);
                if (dot.delete() == false)
                    System.err.println("Warning: " + dot.getAbsolutePath() + " could not be deleted!");
                return img_stream;
            }
            return null;
        } catch (java.io.IOException ioe) {
            return null;
        }
    }

    private File writeDotSourceToFile(String str) throws java.io.IOException {
        File temp;
        try {
            temp = File.createTempFile("dorrr", ".dot", new File(TEMP_DIR));
            FileWriter fout = new FileWriter(temp);
            fout.write(str);
            BufferedWriter br = new BufferedWriter(new FileWriter("dotsource.dot"));
            br.write(str);
            br.flush();
            br.close();
            fout.close();
        } catch (Exception e) {
            System.err.println("Error: I/O error while writing the dot source to temp file!");
            return null;
        }
        return temp;
    }

    public int writeGraphToFile(byte[] img, File to) {
        try {
            FileOutputStream fos = new FileOutputStream(to);
            fos.write(img);
            fos.close();
        } catch (java.io.IOException ioe) {
            return -1;
        }
        return 1;
    }

    String toDotFormat(Session thisSession) {
        StringBuilder formattedTables = new StringBuilder();
        StringBuilder formattedRelations = new StringBuilder();
        for (MyTable table : thisSession.getTables()) {
            formattedTables.append(tableToDotFormat(table));
            try {
                formattedRelations.append(relationToDotFormat(table));
            } catch (NullPointerException e) {
            }
        }
        return formattedTables.append(formattedRelations).toString();
    }

    String tableToDotFormat(MyTable table) {
        String fullTableName = table.getTable().getWholeTableName();
        StringBuilder tableInDot = new StringBuilder();
        tableInDot.append(fullTableName.replace(".", ""))
                .append(" [style=filled, fillcolor=\"#BFC9CA\", shape=none, margin=0, label=<" + "<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"4\">" + "<TR><TD COLSPAN=\"2\">")
                .append(fullTableName)
                .append("</TD></TR>");
        for (ColumnDefinition col : table.getColumns()) {
            tableInDot.append("<TR><TD>").append(col.getColumnName()).append("</TD><TD>").append(col.getColDataType()).append("</TD></TR>");
        }
        tableInDot.append("</TABLE>>];\n");
        return tableInDot.toString();
    }

    String relationToDotFormat(MyTable table) {
        String fullTableName = table.getTable().getWholeTableName().replace(".", "");
        StringBuilder relationInDot = new StringBuilder();
        for (String relation : table.getRelations()) {
            relationInDot.append(fullTableName).append("->").append(relation.replace(".", "")).append(";");
        }
        return relationInDot.toString();
    }

    private byte[] get_img_stream(File dot, String type) {
        File img;
        byte[] img_stream = null;

        try {
            img = File.createTempFile("graph_", "." + type, new File(TEMP_DIR));
            Runtime rt = Runtime.getRuntime();

            String[] args = {DOT, "-T" + type, "-Gdpi=" + dpiSizes[this.currentDpiPos], dot.getAbsolutePath(), "-o", img.getAbsolutePath()};
            Process p = rt.exec(args);

            p.waitFor();

            FileInputStream in = new FileInputStream(img.getAbsolutePath());
            img_stream = new byte[in.available()];
            in.read(img_stream);
            // Close it if we need to
            if (in != null) in.close();

            if (!img.delete())
                System.err.println("Warning: " + img.getAbsolutePath() + " could not be deleted!");
        } catch (java.io.IOException ioe) {
            System.err.println("Error:    in I/O processing of tempfile in dir " + TEMP_DIR + "\n");
            System.err.println("       or in calling external command");
            ioe.printStackTrace();
        } catch (java.lang.InterruptedException ie) {
            System.err.println("Error: the execution of the external program was interrupted");
            ie.printStackTrace();
        }

        return img_stream;
    }

}
