package database;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static javafx.scene.control.Alert.AlertType;

public class MainController {

    private File database = null;
    private int checked = 0;
    private int length = 0;
    private boolean locked = false; // a semaphore just in case action handlers are in fact concurrent

    @FXML
    ComboBox<String> searchMode;
    @FXML
    TextField searchBar;
    @FXML
    TextField licenseField;
    @FXML
    TextField makeField;
    @FXML
    TextField modelField;
    @FXML
    TextField yearField;
    @FXML
    ComboBox<String> gearboxField;
    @FXML
    TextField volumeField;
    @FXML
    AnchorPane tableContainer;
    @FXML
    Button actionButton;

    private void initIO() {
        Car.initLookupTable();
    }

    // wait for the file to be unlocked, return true if timeout, false otherwise
    private boolean await() {
        long start = System.currentTimeMillis();
        int timeout = 2000; // unlock await timeout
        while(System.currentTimeMillis() - start < timeout) {
            if(!locked) return false;
        }
        return true;
    }

    private Car makeCar() {
        try {
            return new Car(licenseField.getText(),
                    makeField.getText(),
                    modelField.getText(),
                    Integer.parseInt(yearField.getText()),
                    gearboxField.getValue().equals("Да"),
                    Double.parseDouble(volumeField.getText())
                    );
        } catch (NumberFormatException e) {
            Alert failWindow = new Alert(AlertType.ERROR);
            failWindow.setTitle("Ошибка");
            failWindow.setHeaderText("Неверные данные");
            failWindow.setContentText("В поле \"Год\" должно быть целое число, в поле \"Объем\" должно быть целое или дробное число");
            failWindow.showAndWait();
            return null;
        }
    }

    private boolean addFieldsAreClear() {
        return licenseField.getText().equals("") &&
                makeField.getText().equals("") &&
                modelField.getText().equals("") &&
                yearField.getText().equals("") &&
                gearboxField.getValue().equals("") &&
                volumeField.getText().equals("");
    }

    private void clearAllFields() {
        licenseField.clear();
        makeField.clear();
        modelField.clear();
        yearField.clear();
        gearboxField.setValue("");
        volumeField.clear();
    }

    private void drawFileContents() {
        if(await()) {
            System.err.println("File unlock timeout, read failed");
            return;
        }
        locked = true;
        tableContainer.getChildren().clear();
        length = 0;
        checked = 0;
        updateButtonMode();
        Scanner reader;
        try {
            reader = new Scanner(database);
        } catch (FileNotFoundException e) {return;}
        while(reader.hasNextLine()) {
            drawCar(Car.parseCar(reader.nextLine()));
        }
        locked = false;
    }

    private void selectAndDraw(Predicate<Car> p) {
        if(await()) {
            System.err.println("File unlock timeout, read failed");
            return;
        }
        locked = true;
        tableContainer.getChildren().clear();
        length = 0;
        checked = 0;
        updateButtonMode();
        ArrayList<Car> list = new ArrayList<>();
        Scanner reader;
        try {
            reader = new Scanner(database);
        } catch (FileNotFoundException e) {return;}
        while(reader.hasNextLine()) {
            list.add(Car.parseCar(reader.nextLine()));
        }
        list = list.stream().filter(p).collect(Collectors.toCollection(ArrayList::new));
        for(Car c : list) {
            drawCar(c);
        }
        if(list.size() == 0) {
            tableContainer.getChildren().add(new Label("Ничего не найдено"));
        }
        locked = false;
    }

    public void updateButtonMode() {
        if(checked == 0) {
            actionButton.setText("Добавить");
            actionButton.setOnAction((ActionEvent event) -> addCar());
        } else if(addFieldsAreClear()) {
            actionButton.setText("Удалить");
            actionButton.setOnAction((ActionEvent event) -> removeSelected());
        } else {
            actionButton.setText("Изменить");
            actionButton.setOnAction((ActionEvent event) -> editSelected());
        }
    }

    public void newFile() {
        FileChooser dialog = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Файлы базы данных автомобилей (*.cdb)", "*.cdb");
        dialog.getExtensionFilters().add(filter);
        database = dialog.showSaveDialog(Main.stage);
        if(database != null)
            Main.stage.setTitle(database.getPath());
        initIO();
        drawFileContents();
    }

    public void openFile() {
        FileChooser dialog = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Файлы базы данных автомобилей (*.cdb)", "*.cdb");
        dialog.getExtensionFilters().add(filter);
        database = dialog.showOpenDialog(Main.stage);
        if(database != null)
            Main.stage.setTitle(database.getPath());
        initIO();
        drawFileContents();
    }

    public void backup() {
        if(database != null) {

            if(await()) {
                System.err.println("File unlock timeout, remove failed");
                Alert failWindow = new Alert(AlertType.ERROR);
                failWindow.setTitle("Ошибка");
                failWindow.setHeaderText("Не удалось создать резервную копию - файл занят");
                failWindow.showAndWait();
                return;
            }

            locked = true;
            String path = new StringBuilder()
                    .append("C:\\Users\\user\\AppData\\Local\\Database\\")
                    .append(database.getName())
                    .append(".bkp")
                    .toString();
            try{
                if(Files.exists(Paths.get(path)))
                {
                    Files.delete(Paths.get(path));
                }
                FileWriter backupWriter = new FileWriter(new File(path));
                FileReader backupReader = new FileReader(database);
                backupReader.transferTo(backupWriter);
                backupWriter.close();
                Alert successWindow = new Alert(AlertType.CONFIRMATION);
                successWindow.setHeaderText("Резервная копия успешно создана");
                successWindow.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                Alert failWindow = new Alert(AlertType.ERROR);
                failWindow.setTitle("Ошибка");
                failWindow.setHeaderText("Не удалось создать резервную копию");
                failWindow.showAndWait();
            } finally {
                locked = false;
            }
        }
        else
        {
            Alert warnWindow = new Alert(AlertType.WARNING);
            warnWindow.setHeaderText("Пожалуйста, сначала откройте базу данных");
            warnWindow.showAndWait();
        }
    }

    public void restore() {

        if(await()) {
            System.err.println("File unlock timeout, remove failed");
            Alert failWindow = new Alert(AlertType.ERROR);
            failWindow.setTitle("Ошибка");
            failWindow.setHeaderText("Не удалось восстановить резервную копию - файл занят");
            failWindow.showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Файлы резервных копий баз данных (*.cbd.bkp)", "*.cdb.bkp");
        chooser.setInitialDirectory(new File("C:\\Users\\user\\AppData\\Local\\Database\\"));
        chooser.getExtensionFilters().add(filter);
        Path backup = chooser.showOpenDialog(Main.stage).toPath();

        if(database == null) {
            newFile();
        }
        locked = true;
        try {
            Files.copy(backup, new FileOutputStream(database));
            drawFileContents();
        } catch (IOException e) {
            e.printStackTrace();
        }
        locked = false;
    }

    public void selectAll() {
        checked = 0;
        for(Node row : tableContainer.getChildren()) {
            ((CheckBox)(((AnchorPane)row).getChildren().get(6))).setSelected(true);
            checked++;
        }
        updateButtonMode();
    }

    public void selectOpposite() {
        checked = 0;
        for(Node row : tableContainer.getChildren()) {
            CheckBox b = ((CheckBox)(((AnchorPane)row).getChildren().get(6)));
            b.setSelected(!(b.isSelected()));
            if(b.isSelected()) checked++;
        }
        updateButtonMode();
    }

    public void selectNone() {
        checked = 0;
        for(Node row : tableContainer.getChildren()) {
            ((CheckBox)(((AnchorPane)row).getChildren().get(6))).setSelected(false);
        }
        updateButtonMode();
    }

    public void exit() {
        System.exit(0);
    }

    public void help() {
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.open(new File("README.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void about() {
        Alert aboutWindow = new Alert(AlertType.INFORMATION);
        aboutWindow.setTitle("О программе");
        aboutWindow.setHeaderText("Лабораторная работа №1 по управлению данными");
        aboutWindow.setContentText("Ястребов В.Д. 18БИ-2. JavaFX. 2020");
        aboutWindow.showAndWait();
    }

    public void search() {
        if(database == null) {
            Alert warnWindow = new Alert(AlertType.WARNING);
            warnWindow.setHeaderText("Пожалуйста, сначала откройте базу данных");
            warnWindow.showAndWait();
            return;
        }
        if(searchBar.getText().equals(""))
        {
            drawFileContents();
            updateButtonMode();
            return;
        }
        switch(searchMode.getValue()) {
            case "Номер":
                selectAndDraw(car -> car.license.equals(Car.conformLicense(searchBar.getText())));
                break;
            case "Марка":
                selectAndDraw(car -> car.make.equals(searchBar.getText()));
                break;
            case "Модель":
                selectAndDraw(car -> car.model.equals(searchBar.getText()));
                break;
            case "Год выпуска (не старше)":
                try {
                    Integer.parseInt(searchBar.getText());
                } catch (NumberFormatException e) {
                    searchBar.setText("Пожалуйста, введите целое число");
                    break;
                }
                selectAndDraw(car -> car.year <= Integer.parseInt(searchBar.getText()));
                break;
            case "Год выпуска (не младше)":
                try {
                    Integer.parseInt(searchBar.getText());
                } catch (NumberFormatException e) {
                    searchBar.setText("Пожалуйста, введите целое число");
                    break;
                }
                selectAndDraw(car -> car.year >= Integer.parseInt(searchBar.getText()));
                break;
            case "Автомат (да/нет)":
                if(!(searchBar.getText().toLowerCase().equals("да") || searchBar.getText().toLowerCase().equals("нет"))) {
                    searchBar.setText("Пожалуйста, введите \"Да\" или \"Нет\"");
                    break;
                }
                selectAndDraw(car -> car.automatic == searchBar.getText().toLowerCase().equals("да"));
                break;
            case "Объем (не менее), л":
                try {
                    Double.parseDouble(searchBar.getText());
                } catch (NumberFormatException e) {
                    searchBar.setText("Пожалуйста, введите число");
                    break;
                }
                selectAndDraw(car -> car.year >= Double.parseDouble(searchBar.getText()));
                break;
            case "Объем (не более), л":
                try {
                    Double.parseDouble(searchBar.getText());
                } catch (NumberFormatException e) {
                    searchBar.setText("Пожалуйста, введите число");
                    break;
                }
                selectAndDraw(car -> car.year <= Double.parseDouble(searchBar.getText()));
                break;
        }
        updateButtonMode();
    }

    public void addCar() {
        if(await()) {
            System.err.println("File unlock timeout, write failed");
            return;
        }
        Car car = makeCar();
        if(car == null) {
            System.err.println("Car creator failed");
            return;
        }
        for(Node row : tableContainer.getChildren()) {
            if(((Label) ((AnchorPane) row).getChildren().get(0)).getText().equals(car.license)) {
                Alert failWindow = new Alert(AlertType.ERROR);
                failWindow.setTitle("Ошибка");
                failWindow.setHeaderText("Запись с таким гос. номером уже существует");
                failWindow.setContentText("Не может существовать двух автомобилей с одинаковым гос. номером. Попробуйте изменить гос.номер или отредактировать существующий автомобиль. ");
                failWindow.showAndWait();
                clearAllFields();
                return;
            }
        }

        locked = true; // lock the file to prevent concurrent modifications
        FileWriter writer;
        try{
            writer = new FileWriter(database, true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        try {
            writer.append(car.toString()).append(String.valueOf('\n'));
            writer.flush();
            drawCar(car);
            clearAllFields();
        }
        catch (NullPointerException e) {
            Alert warnWindow = new Alert(AlertType.WARNING);
            warnWindow.setHeaderText("Пожалуйста, сначала откройте базу данных");
            warnWindow.showAndWait();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            locked = false;
        }
    }

    public void removeSelected() {
        // Basically:
        // 1. Create a temporary file
        // 2. Copy only non-selected lines to the temporary file
        // 3. Overwrite the database with the contents of the temporary file
        // 4. Delete the temporary file

        if(await()) {
            System.err.println("File unlock timeout, remove failed");
            return;
        }
        // 1.
        File temp = new File(database.getAbsolutePath() + ".tmp");
        FileWriter writer;
        Scanner reader;
        try {
            writer = new FileWriter(temp);
            reader = new Scanner(database);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        locked = true;
        // Check which lines are selected
        ArrayList<String> selected = new ArrayList<>();
        for(Node row : tableContainer.getChildren()) {
            if(((CheckBox) (((AnchorPane) row).getChildren().get(6))).isSelected()) {
               selected.add(((Label) ((AnchorPane) row).getChildren().get(0)).getText());
            }
        }
        // 2
        String line;
        Car car;
        while(reader.hasNextLine()) {
            line = reader.nextLine();
            car = Car.parseCar(line);
            if(!(selected.contains(car.license))) {
                try{
                    writer.write(line + '\n');
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        locked = false;
        try {
            // 3
            Files.copy(temp.toPath(), new FileOutputStream(database));
            // 4
            Files.deleteIfExists(Paths.get(database.getAbsolutePath() + ".tmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        drawFileContents();
    }

    public void editSelected() {
        // Basically:
        // 1. Create a temporary file.
        // 2. Copy all the lines while redacting the selected ones
        // 3. Overwrite the database with the contents of the temporary file
        // 4. Delete the temporary file

        if(await()) {
            System.err.println("File unlock timeout, remove failed");
            return;
        }

        boolean duplicate = false;
        // Check which lines are selected and whether we are trying to duplicate a license
        ArrayList<String> selected = new ArrayList<>();
        for(Node row : tableContainer.getChildren()) {
            if(((CheckBox) (((AnchorPane) row).getChildren().get(6))).isSelected()) {
                selected.add(((Label) ((AnchorPane) row).getChildren().get(0)).getText());
            }
            if(!(licenseField.getText().isEmpty())) {
                if (((Label) (((AnchorPane) row).getChildren().get(0))).getText().equals(Car.conformLicense(licenseField.getText()))) {
                    duplicate = true;
                }
            }
        }

        if(!(licenseField.getText().isEmpty()) && (checked > 1 || duplicate)) {
            Alert failWindow = new Alert(AlertType.ERROR);
            failWindow.setTitle("Ошибка");
            failWindow.setHeaderText("Попытка присвоить двум записям одинаковый гос.номер");
            failWindow.setContentText("Не может существовать двух автомобилей с одинаковым гос. номером.");
            failWindow.showAndWait();
            clearAllFields();
        }
        // 1.
        File temp = new File(database.getAbsolutePath() + ".tmp");
        FileWriter writer;
        Scanner reader;
        try {
            writer = new FileWriter(temp);
            reader = new Scanner(database);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        boolean failed = false;
        locked = true;
        // 2
        String line;
        Car car;
        while(reader.hasNextLine()) {
            line = reader.nextLine();
            car = Car.parseCar(line);
            if(selected.contains(car.license)) {
                if (!(licenseField.getText().isEmpty())) {
                    car.license = licenseField.getText();
                }
                if (!(makeField.getText().isEmpty())) {
                    car.make = makeField.getText();
                }
                if (!(modelField.getText().isEmpty())) {
                    car.model = modelField.getText();
                }
                if (!(yearField.getText().isEmpty())) {
                    car.year = Integer.parseInt(yearField.getText());
                }
                if (!(gearboxField.getValue().isEmpty())) {
                    car.automatic = gearboxField.getValue().equals("Да");
                }
                if (!(volumeField.getText().isEmpty())) {
                    car.volume = Double.parseDouble(volumeField.getText());
                }
            }
            try{

                writer.write(car.toString() + '\n');
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            } catch (NumberFormatException e) {
                Alert failWindow = new Alert(AlertType.ERROR);
                failWindow.setTitle("Ошибка");
                failWindow.setHeaderText("Неверные данные");
                failWindow.setContentText("В поле \"Год\" должно быть целое число, в поле \"Объем\" должно быть целое или дробное число");
                failWindow.showAndWait();
                failed = true;
                break;
            }
        }
        locked = false;
        try {
            // 3
            if(!failed) {
                Files.copy(temp.toPath(), new FileOutputStream(database));
            }
            // 4
            Files.deleteIfExists(Paths.get(database.getAbsolutePath() + ".tmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        drawFileContents();
        clearAllFields();
    }

    public void drawCar(Car car) {
        AnchorPane row = new AnchorPane();
        row.setLayoutX(0);
        row.setLayoutY(20 * length);

        Label license = new Label(car.license);
        license.setPrefWidth(80);
        license.setPrefHeight(20);
        license.setStyle("-fx-border-style: solid; -fx-border-width: 1; -fx-border-color: black");
        Label make = new Label(car.make);
        make.setPrefWidth(80);
        make.setPrefHeight(20);
        make.setLayoutX(80);
        make.setStyle("-fx-border-style: solid; -fx-border-width: 1; -fx-border-color: black");
        Label model = new Label(car.model);
        model.setPrefWidth(80);
        model.setPrefHeight(20);
        model.setLayoutX(160);
        model.setStyle("-fx-border-style: solid; -fx-border-width: 1; -fx-border-color: black");
        Label year = new Label(String.valueOf(car.year));
        year.setPrefWidth(80);
        year.setPrefHeight(20);
        year.setLayoutX(240);
        year.setStyle("-fx-border-style: solid; -fx-border-width: 1; -fx-border-color: black");
        Label gearbox = new Label(car.automatic ? "Да" : "Нет");
        gearbox.setPrefWidth(80);
        gearbox.setPrefHeight(20);
        gearbox.setLayoutX(320);
        gearbox.setStyle("-fx-border-style: solid; -fx-border-width: 1; -fx-border-color: black");
        Label volume = new Label(String.valueOf(car.volume));
        volume.setPrefWidth(80);
        volume.setPrefHeight(20);
        volume.setLayoutX(400);
        volume.setStyle("-fx-border-style: solid; -fx-border-width: 1; -fx-border-color: black");
        CheckBox checkbox = new CheckBox();
        checkbox.setPrefWidth(60);
        checkbox.setPrefHeight(20);
        checkbox.setLayoutX(480);
        checkbox.setOnAction((ActionEvent event) -> {
            if(checkbox.isSelected()) checked++;
            else checked--;
            updateButtonMode();
        });
        checkbox.setStyle("-fx-border-style: solid; -fx-border-width: 1; -fx-border-color: black");
        row.getChildren().addAll(license, make, model, year, gearbox, volume, checkbox);
        tableContainer.getChildren().add(row);
        length++;
    }
}
