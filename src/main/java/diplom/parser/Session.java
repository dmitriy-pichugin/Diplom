package diplom.parser;

import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.ArrayList;
import java.util.List;

class Session {
    private ArrayList<MyTable> tables = new ArrayList<>();

    void setTable(CreateTable createTable) {
        MyTable myTable = new MyTable();
        myTable.setTable(createTable.getTable());
        myTable.setColumns((ArrayList<ColumnDefinition>) createTable.getColumnDefinitions());
        setTable(myTable);
    }

    void setTable(CreateTable createTable, List relations) {
        MyTable myTable = new MyTable();
        myTable.setTable(createTable.getTable());
        myTable.setColumns((ArrayList<ColumnDefinition>) createTable.getColumnDefinitions());
        myTable.setRelations(relations);
        setTable(myTable);
    }

    void setTable(MyTable myTable) {
        this.tables.add(myTable);
    }


    ArrayList<MyTable> getTables() {
        return tables;
    }
}
