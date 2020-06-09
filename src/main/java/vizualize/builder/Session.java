package vizualize.builder;

import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Хранит массив таблиц для текущего процесса синтаксического анализа
 */
class Session {
    /**
     * Объекты-таблицы текущего запуска
     */
    private ArrayList<MyTable> tables = new ArrayList<>();

    /**
     * Добавляет элемент в tables
     *
     * @param createTable объект CreateTable, из которого создается
     *                    объект типа MyTable и добавляется в в tables
     */
    void setTable(CreateTable createTable) {
        MyTable myTable = new MyTable();
        myTable.setTable(createTable.getTable());
        myTable.setColumns((ArrayList<ColumnDefinition>) createTable.getColumnDefinitions());
        setTable(myTable);
    }

    /**
     * Добавляет элемент в tables
     *
     * @param createTable объект CreateTable, из которого создается
     *                    объект типа MyTable и добавляется в в tables
     * @param relations связи с другими таблицами
     */
    void setTable(CreateTable createTable, List relations) {
        MyTable myTable = new MyTable();
        myTable.setTable(createTable.getTable());
        myTable.setColumns((ArrayList<ColumnDefinition>) createTable.getColumnDefinitions());
        myTable.setRelations(relations);
        setTable(myTable);
    }

    /**
     * Добавляет элемент в tables
     *
     * @param myTable доавляемый объект
     */
    void setTable(MyTable myTable) {
        this.tables.add(myTable);
    }


    /**
     * Getter для tables
     *
     * @return tables
     */
    ArrayList<MyTable> getTables() {
        return tables;
    }
}
