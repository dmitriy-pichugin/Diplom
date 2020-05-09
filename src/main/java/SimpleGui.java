import diplom.visualize.DiagramBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

/**
 * Реализует графический интерфейс пользователя.
 */
public class SimpleGui extends JFrame {
    /**
     * Главное окно интерфейса
     */
    private JPanel mainPanel;
    /**
     * Кнопка добавления файлов
     */
    private JButton addFiles;
    /**
     * Кнопка запуска основной программы
     */
    private JButton runner;
    /**
     * Текстовое поле, в котором отображаются пути к входным файлам
     */
    private JTextArea textArea;
    /**
     * Диалоговое окно выбора файлов
     */
    private JFileChooser fileChooser;
    /**
     * Массив, хранящий пути к входных данным
     */
    private ArrayList<String> paths = new ArrayList<>();

    /**
     * Конструктор класса. Обрабатывает действия пользователя
     *
     * @param title заголовок диалогового окна
     */
    private SimpleGui(String title) {
        super(title);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        this.pack();
        /* Конфигурация диалогового окна */
        this.fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("C:/Diplom/Test"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        addFiles.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                /* При нажатии на клавишу Select Files выполняется следующий код */
                int option = fileChooser.showOpenDialog(null);
                if (option == JFileChooser.APPROVE_OPTION) {
                    /* Путь к выбранным файлам заносится в массив */
                    File[] selectedFiles = fileChooser.getSelectedFiles();
                    for (File selectedFile : selectedFiles) {
                        String newPath = selectedFile.getAbsolutePath();
                        /* Проверка, есть ли уже выбранный файл в массиве */
                        if (!paths.contains(newPath))
                            paths.add(newPath);
                    }
                    StringBuilder pathsToShow = new StringBuilder();
                    paths.forEach(path -> pathsToShow.append(path).append("\r\n"));
                    textArea.setText(pathsToShow.toString());
                }
            }
        });
        runner.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                /* При нажатии на клавишу Make Diagram выполняется основной код программы */
                try {
                    DiagramBuilder diagramBuilder = new DiagramBuilder();
                    File out = diagramBuilder.build(paths.toArray(new String[paths.size()]));
                    ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/C", "explorer " + out.getAbsolutePath());
                    processBuilder.start();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "An error occurred while the program was running\r\n" + e);
                }
            }
        });
    }

    /**
     * Точка входа программы
     *
     * @param args входной массив метода (остается пустым)
     */
    public static void main(String[] args) {
        JFrame frame = new SimpleGui("Make diagram from SQL DDL");
        frame.setVisible(true);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(10, 10, 10, 10), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setVerticalScrollBarPolicy(22);
        mainPanel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(300, 200), new Dimension(500, 400), new Dimension(1000, 800), 0, false));
        textArea = new JTextArea();
        textArea.setEditable(false);
        scrollPane1.setViewportView(textArea);
        addFiles = new JButton();
        addFiles.setText("Select files");
        mainPanel.add(addFiles, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(90, 30), new Dimension(150, 50), new Dimension(180, 60), 0, false));
        runner = new JButton();
        runner.setText("Make diagram");
        mainPanel.add(runner, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(90, 30), new Dimension(150, 50), new Dimension(180, 60), 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
