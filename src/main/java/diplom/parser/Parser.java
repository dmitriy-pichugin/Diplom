package diplom.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.select.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Parser {
    private Session session;
    private Logger logger;
    private ArrayList<String> createAsSelectPaths;

    Parser() {
        this.session = new Session();
        this.createAsSelectPaths = new ArrayList<>();
        this.logger = Logger.getLogger("DIPLOM");
        this.logger.setLevel(Level.INFO);
        try {
            DateFormat dateFormat = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss");
            Date currentDate = new Date();
            FileHandler fh = new FileHandler("C:/Diplom/Log/" + dateFormat.format(currentDate) + ".log");
            fh.setFormatter(new SimpleFormatter());
            this.logger.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void parse(String[] args) {
        for (String filename : args) {
            if (checkCreateAsSelect(filename))
                continue;
            CCJSqlParserManager ccjSqlParserManager = new CCJSqlParserManager();
            try {
                Statement statement = ccjSqlParserManager.parse(new InputStreamReader(
                        new FileInputStream(filename), StandardCharsets.UTF_8));
                checkClass(statement);
            } catch (FileNotFoundException e) {
                logger.severe("File not found: " + filename);
                e.printStackTrace();
            } catch (JSQLParserException e) {
                logger.severe("Can't parse " + filename);
                e.printStackTrace();
            }
        }
        if (!this.createAsSelectPaths.isEmpty())
            parseCreateAsSelect();
    }

    private void checkClass(Statement statement) {
        if (statement instanceof CreateTable) {
            if (checkExistence(((CreateTable) statement).getTable()))
                this.session.setTable((CreateTable) statement);
            logger.info("Parsed successfully: " + ((CreateTable) statement).getTable().getWholeTableName());
        } else if (statement instanceof Select) {
            logger.info("anal");
        } else logger.info("Script is not DDL:" + statement.toString());
    }

    private boolean checkExistence(Table table) {
        boolean notExist = true;
        for (FormattedTable formattedTable : this.session.getTables()) {
            if (formattedTable.getTable().getWholeTableName().equals(table.getWholeTableName())) {
                notExist = false;
                break;
            }
        }
        return notExist;
    }

    private boolean checkCreateAsSelect(String path) {
        try {
            String script = new String(Files.readAllBytes(Paths.get(path)))
                    .replace("\r", "").replace("\n", "");
            Matcher asSelectMatcher = Pattern.compile("^CREATE TABLE .*(AS)? SELECT").matcher(script.toUpperCase());
            while (asSelectMatcher.find()) {
                Matcher tableNameRegex = Pattern.compile("(?<=CREATE TABLE ).*(?=(AS)? SELECT)").matcher(script);
                while (tableNameRegex.find()) {
                    String createAsSelectNewFile = "C:/Diplom/Tmp/CreateAsSelect/" + tableNameRegex.group()
                            .replace(" ", "");
                    Matcher replacer = Pattern.compile("(?=SELECT).*").matcher(script);
                    while (replacer.find())
                        Files.write(Paths.get(createAsSelectNewFile), replacer.group().getBytes());
                    this.createAsSelectPaths.add(createAsSelectNewFile);
                    logger.info("Found CREATE AS SELECT in " + createAsSelectNewFile + ". Copied to temporary directory.");
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void parseCreateAsSelect() {
        for (String query : createAsSelectPaths) {
            CCJSqlParserManager ccjSqlParserManager = new CCJSqlParserManager();
            try {
                Statement statement = ccjSqlParserManager.parse(new InputStreamReader(
                        new FileInputStream(query), StandardCharsets.UTF_8));
                if (statement instanceof Select) {
                    List joins = ((PlainSelect) ((Select) statement).getSelectBody()).getJoins();
                    for (int i = 0; i < joins.size(); i++) {
                        Matcher m = Pattern.compile("(?<=JOIN ).*(?= ON)").matcher(joins.get(i).toString());
                        while (m.find()) {
                            joins.set(i, m.group());
                        }
                    }
                    joins.add(((PlainSelect) ((Select) statement).getSelectBody()).getFromItem());
                    Table newTable = new Table();
                    String fullTableName = query.replaceAll(".*([\\/])", "");
                    newTable.setSchemaName(fullTableName.replaceAll("[.].*", ""));
                    newTable.setName(fullTableName.replaceAll(".*[.]", ""));
                    FormattedTable newFormattedTable = new FormattedTable();
                    newFormattedTable.setTable(newTable);
                    newFormattedTable.setRelations(joins);
                    ArrayList<ColumnDefinition> newTablesColumns = parseColumns(((PlainSelect) ((Select) statement).getSelectBody()).getSelectItems());
                    newFormattedTable.setColumns(newTablesColumns);
                    this.session.setTable(newFormattedTable);
                    logger.info("Parsed successfully: " + fullTableName);
                }
            } catch (JSQLParserException e) {
                logger.severe("Can't parse " + query);
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                logger.severe("File not found: " + query);
                e.printStackTrace();
            }
        }
    }

    // TODO: 01.05.2020 DON'T CATCH DATA TYPE
    private ArrayList<ColumnDefinition> parseColumns(List selectItems){
        ArrayList<ColumnDefinition> columnsArrayList = new ArrayList<>();
        for (Object obj : selectItems){
            ColumnDefinition newColumn = new ColumnDefinition();
            String unparsedColumn = obj.toString();
            Matcher asMatcher = Pattern.compile("(?<=AS ).*").matcher(unparsedColumn);
            if (asMatcher.find()){
                newColumn.setColumnName(asMatcher.group());
            } else {
                newColumn.setColumnName(unparsedColumn.replaceAll("([^.]+$)",""));
            }
            columnsArrayList.add(newColumn);
        }
        return columnsArrayList;
    }



}
