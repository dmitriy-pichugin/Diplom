package diplom.parser;

import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.ArrayList;

class Session {
    private ArrayList<FormattedTable> tables = new ArrayList<>();

    void setTable(CreateTable createTable) {
        FormattedTable formattedTable = new FormattedTable();
        formattedTable.setTable(createTable.getTable());
        formattedTable.setColumns((ArrayList<ColumnDefinition>) createTable.getColumnDefinitions());
    }

    void setTable(FormattedTable formattedTable) {
        this.tables.add(formattedTable);
    }


    ArrayList<FormattedTable> getTables() {
        return tables;
    }
}
