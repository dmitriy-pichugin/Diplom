package diplom.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColDataType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class Parser {
    private Session session;
    private Logger logger;
    private ArrayList<String> createAsSelectPaths;

    Parser(Logger logger) {
        this.session = new Session();
        this.createAsSelectPaths = new ArrayList<>();
        this.logger = logger;

    }

    Session getSession() {
        return session;
    }

    void parse(String[] args) throws JSQLParserException {
        for (String filename : args) {
            if (checkCreateAsSelect(filename))
                continue;
            CCJSqlParserManager ccjSqlParserManager = new CCJSqlParserManager();
            try {
                Statement statement = ccjSqlParserManager.parse(new InputStreamReader(
                        new FileInputStream(filename), StandardCharsets.UTF_8));
                parse(statement);
            } catch (FileNotFoundException e) {
                logger.severe("File not found: " + filename);
                e.printStackTrace();
            }
        }
        if (!this.createAsSelectPaths.isEmpty())
            parseCreateAsSelect();
    }

    private void parse(Statement statement) {
        if (statement instanceof CreateTable) {
            if (checkExistence(((CreateTable) statement).getTable())) {
                List relations = checkForeignKeys((CreateTable) statement);

                if (!relations.isEmpty()) {
                    for (int i = 0; i < relations.size(); i++) {
                        relations.set(i, ((CreateTable) statement).getTable().getSchemaName() + relations.get(i));
                    }
                    this.session.setTable((CreateTable) statement, relations);
                } else
                    this.session.setTable((CreateTable) statement);
                logger.info("Parsed successfully: " + ((CreateTable) statement).getTable().getWholeTableName());
            }
        } else if (statement instanceof Select) {
            logger.info("anal");
        } else logger.info("Script is not DDL:" + statement.toString());
    }

    private boolean checkExistence(Table table) {
        boolean notExist = true;
        for (MyTable myTable : this.session.getTables()) {
            if (myTable.getTable().getWholeTableName().equals(table.getWholeTableName())) {
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
            Matcher asSelectMatcher = Pattern.compile("^CREATE TABLE .*(AS)? ([( ]?)SELECT").matcher(script.toUpperCase());
            while (asSelectMatcher.find()) {
                Matcher tableNameRegex = Pattern.compile("(?<=CREATE TABLE )[^\\s]+").matcher(script);
                while (tableNameRegex.find()) {
                    String createAsSelectNewFile = "C:/Diplom/Tmp/CreateAsSelect/" + tableNameRegex.group()
                            .replace(" ", "");
                    Matcher replacer = Pattern.compile("(?=SELECT).*[^);]").matcher(script);
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
                    List<String> joins = (List<String>) ((PlainSelect) ((Select) statement)
                            .getSelectBody())
                            .getJoins()
                            .stream()
                            .map(object -> Objects.toString(object, null)).collect(Collectors.toList());
                    for (int i = 0; i < joins.size(); i++) {
                        Matcher m = Pattern.compile("(?<=JOIN ).*(?= ON)").matcher(joins.get(i).toString());
                        while (m.find()) {
                            joins.set(i, m.group().replaceAll("(?= ).*", ""));
                        }
                    }
                    joins.add(((PlainSelect) ((Select) statement).getSelectBody()).getFromItem().toString().replaceAll("(?= ).*", ""));
                    Table newTable = new Table();
                    String fullTableName = query.replaceAll(".*([\\/])", "");
                    newTable.setSchemaName(fullTableName.replaceAll("[.].*", ""));
                    newTable.setName(fullTableName.replaceAll(".*[.]", ""));
                    MyTable newMyTable = new MyTable();
                    newMyTable.setTable(newTable);
                    newMyTable.setRelations(joins);
                    ArrayList<ColumnDefinition> newTablesColumns = parseColumns(joins, ((PlainSelect) ((Select) statement).getSelectBody()).getSelectItems());
                    newMyTable.setColumns(newTablesColumns);
                    this.session.setTable(newMyTable);
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

    private ArrayList<ColumnDefinition> parseColumns(List<String> relations, List selectItems) {
        ArrayList<ColumnDefinition> columnsArrayList = new ArrayList<>();
        for (Object obj : selectItems) {
            ColumnDefinition newColumn = new ColumnDefinition();
            String unparsedColumn = obj.toString();
            Matcher asMatcher = Pattern.compile("(?<=AS ).*").matcher(unparsedColumn);
            if (asMatcher.find()) {
                newColumn.setColumnName(asMatcher.group());
            } else {
                newColumn.setColumnName(unparsedColumn.replaceAll("([^.]+$)", ""));
            }
            ColDataType dataType = getColumnDatatype(relations, unparsedColumn);
            if (dataType != null)
                newColumn.setColDataType(dataType);
            columnsArrayList.add(newColumn);
        }
        return columnsArrayList;
    }

    ColDataType getColumnDatatype(List<String> relations, String column) {
        String colTableName = column.replaceAll("[.].*", "");
        MyTable relatedTable = findTable(relations, colTableName);
        System.out.println("Found table: " + relatedTable.getTable().getWholeTableName());
        ColDataType dataType = findCol(relatedTable, column.replaceAll("(.*[.])|( .*)", ""));
        System.out.println("Found column " + column + " datatype: " + dataType);
        return dataType;
    }

    MyTable findTable(List<String> relations, String colTableName) {
        MyTable table = null;
        for (String tableName : relations) {
            MyTable relatedTable = findTable(tableName);
            if (relatedTable.getTable().getName().equals(colTableName))
                table = relatedTable;
        }
        return table;
    }

    MyTable findTable(String tableName) {
        MyTable foundTable = null;
        List<MyTable> existingTables = this.session.getTables();
        for (MyTable table : existingTables) {
            if (table.getTable().getWholeTableName().equals(tableName)) {
                foundTable = table;
                break;
            }
        }
        return foundTable;
    }

    ColDataType findCol(MyTable table, String searchedColumn) {
        ColDataType dataType = null;
        for (ColumnDefinition column : table.getColumns()) {
            System.out.println("Columns in " + table.getTable().getWholeTableName() + ": " + column.getColumnName());
            if (column.getColumnName().equals(searchedColumn)) {
                System.out.println("DATATYPE " + column.getColDataType().toString());
                dataType = column.getColDataType();
                break;
            }
        }
        return dataType;
    }

    List<String> checkForeignKeys(CreateTable createTable) {
        List<String> relations = new ArrayList();
        for (Object col : createTable.getColumnDefinitions()) {
            String newRelation = checkForeignKey(col);
            try {
                if (!newRelation.isEmpty())
                    relations.add(newRelation);
            } catch (NullPointerException e) {
            }
        }
        return relations;
    }

    String checkForeignKey(Object col) {
        String fkList = "";
        String colDefinitionString = ((ColumnDefinition) col).toString();
        Matcher fkMatcher = Pattern.compile("(?<=FOREIGN KEY [(]).*").matcher(colDefinitionString);
        while (fkMatcher.find()) {
            fkList = getRelatedTable(fkMatcher.group());
        }
        return fkList;
    }

    String getRelatedTable(String columnDefinition) {
        String relatedTable = "";
        Matcher matcher = Pattern.compile("(?<=REFERENCES ).*(?= )").matcher(columnDefinition);
        while (matcher.find()) {
            relatedTable = matcher.group();
        }
        return relatedTable;
    }

}
