package com.usac.cunoc.acelerografo2.GUI;

import com.panamahitek.ArduinoException;
import com.panamahitek.PanamaHitek_Arduino;
import com.usac.cunoc.acelerografo2.file.ManejadorArchivo;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * @author bengo
 * @refactor angelrg
 */
public class GraphApp extends Application {

    /*Arduino Connector*/
    private PanamaHitek_Arduino ino = new PanamaHitek_Arduino();

    final int WINDOW_SIZE = 20;
    final String dimAcc ="m/s^2";
    final String dimTime ="Tiempo";
    private ScheduledExecutorService scheduledExecutorService;

    /*Principal's Graph elements*/
    private final CategoryAxis xAxisPrincipal = new CategoryAxis();
    private final NumberAxis yAxisPrincipal = new NumberAxis();
    private XYChart.Series<String, Number> seriesAccXPrincipal;
    private XYChart.Series<String, Number> seriesAccYPrincipal;
    private XYChart.Series<String, Number> seriesAccZPrincipal;
    
    /**/
    private final CategoryAxis xAxisX = new CategoryAxis();
    private final NumberAxis yAxisX = new NumberAxis();
    private final CategoryAxis xAxisY = new CategoryAxis();
    private final NumberAxis yAxisY = new NumberAxis();
    private final CategoryAxis xAxisZ = new CategoryAxis();
    private final NumberAxis yAxisZ = new NumberAxis();
    private XYChart.Series<String, Number> seriesAccX;
    private XYChart.Series<String, Number> seriesAccY;
    private XYChart.Series<String, Number> seriesAccZ;
    
    /*Menu Items*/
    MenuBar menuBar = new MenuBar();
    Menu menu = new Menu("Ejecucion");
    MenuItem initMenuItem = new MenuItem("Inciar");
    MenuItem infoMenuItem = new MenuItem("Instrucciones");

    /*File control*/
    ManejadorArchivo fileManager = new ManejadorArchivo();
    private String path = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Acelerografo CUNOC");

        /*Menu Structure*/
        initMenuItem.setOnAction(e -> {
            try {
                initMenuItemAction(e, primaryStage);
            } catch (IOException ex) {
                Alert a = new Alert(AlertType.ERROR);
                a.setTitle("Error en el archivo");
                a.setHeaderText("Ha surgido un error al crear el archivo");
                a.show();
            }
        });

        infoMenuItem.setOnAction(e -> {
            infoMenuItemAction(e);
        });

        menu.getItems().add(initMenuItem);
        menu.getItems().add(infoMenuItem);

        menuBar.getMenus().add(menu);

        /*Graph elements configuration*/
        xAxisPrincipal.setLabel(dimTime);
        xAxisPrincipal.setAnimated(false);
        yAxisPrincipal.setLabel(dimAcc);
        yAxisPrincipal.setAnimated(false);

        seriesAccXPrincipal = new XYChart.Series<>();
        seriesAccXPrincipal.setName("Eje X");
        seriesAccYPrincipal = new XYChart.Series<>();
        seriesAccYPrincipal.setName("Eje Y");
        seriesAccZPrincipal = new XYChart.Series<>();
        seriesAccZPrincipal.setName("Eje Z");

        /*General Graph X,Y,Z*/
        final LineChart<String, Number> lineChart = new LineChart<>(xAxisPrincipal, yAxisPrincipal);
        lineChart.setAnimated(false);
        
        /*Self Graph's elements configuration*/
        xAxisX.setLabel(dimTime);
        xAxisX.setAnimated(false);
        yAxisX.setLabel(dimAcc);
        yAxisX.setAnimated(false);
        
        xAxisY.setLabel(dimTime);
        xAxisY.setAnimated(false);
        yAxisY.setLabel(dimAcc);
        yAxisY.setAnimated(false);
        
        xAxisZ.setLabel(dimTime);
        xAxisZ.setAnimated(false);
        yAxisZ.setLabel(dimAcc);
        yAxisZ.setAnimated(false);

        seriesAccX = new XYChart.Series<>();
        seriesAccX.setName("Eje X");
//        seriesAccX.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #FA0000;");
        seriesAccY = new XYChart.Series<>();
        seriesAccY.setName("Eje Y");
//        seriesAccY.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #00E5FA;");
        seriesAccZ = new XYChart.Series<>();
        seriesAccZ.setName("Eje Z");
//        seriesAccZ.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #E500FA;");

        /*Self Graph*/
        final LineChart<String, Number> xLineChart = new LineChart<>(xAxisX, yAxisX);
        xLineChart.setAnimated(false);
        final LineChart<String, Number> yLineChart = new LineChart<>(xAxisY, yAxisY);
        yLineChart.setAnimated(false);
        final LineChart<String, Number> zLineChart = new LineChart<>(xAxisZ, yAxisZ);
        zLineChart.setAnimated(false);

        /*Put all logic together*/
        lineChart.getData().addAll(seriesAccXPrincipal, seriesAccYPrincipal, seriesAccZPrincipal);
        xLineChart.getData().add(seriesAccX);
        yLineChart.getData().add(seriesAccY);
        zLineChart.getData().add(seriesAccZ);

        BorderPane secondPane = new BorderPane();
        secondPane.setLeft(xLineChart);
        secondPane.setCenter(yLineChart);
        secondPane.setRight(zLineChart);

        BorderPane bPane = new BorderPane();
        bPane.setTop(menuBar);
        bPane.setCenter(lineChart);
        bPane.setBottom(secondPane);

        Scene scene = new Scene(bPane, 1500, 900, false);
        scene.getStylesheets().add("style.css");
        primaryStage.setScene(scene);

        primaryStage.show();

    }

    //Leer datos
    public void leerDatos() {
        try {
            ino.arduinoRX("/dev/cu.usbserial-1420", 38400, listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Metodo especifico para arduino
    private final SerialPortEventListener listener = new SerialPortEventListener() {
        @Override
        public void serialEvent(SerialPortEvent spe) {
            try {
                if (ino.isMessageAvailable()) {
                    String datos = ino.printMessage();
                    System.out.println(datos);
                    addData(LocalDateTime.now(),
                            Double.parseDouble(datos.split(",")[0]),
                            Double.parseDouble(datos.split(",")[1]),
                            Double.parseDouble(datos.split(",")[2])
                    );
                }
            } catch (SerialPortException ex) {
                Alert a = new Alert(AlertType.ERROR);
                a.setTitle("Comunicación");
                a.setHeaderText("Ha surgido un error en la comunicacion con el arduino.");
                a.show();
            } catch (ArduinoException e) {
                Alert a = new Alert(AlertType.ERROR);
                a.setTitle("Error Arduino");
                a.setHeaderText("Arduino ha fallado, verificarlo.");
                a.show();
            }
        }
    };

    /*Receive the data, set in Graph and save in file*/
    private void addData(LocalDateTime time, Double accX, Double accY, Double accZ) {
        String textTime = time.getHour() + ":" + time.getMinute() + ":" + time.getSecond() + "::" + (time.getNano() / 1000000);
        seriesAccXPrincipal.getData().add(new XYChart.Data<>(textTime, accX));
        seriesAccYPrincipal.getData().add(new XYChart.Data<>(textTime, accY));
        seriesAccZPrincipal.getData().add(new XYChart.Data<>(textTime, accZ));
        seriesAccX.getData().add(new XYChart.Data<>(textTime, accX));
        seriesAccY.getData().add(new XYChart.Data<>(textTime, accY));
        seriesAccZ.getData().add(new XYChart.Data<>(textTime, accZ));

        if (seriesAccXPrincipal.getData().size() > WINDOW_SIZE) {
            seriesAccXPrincipal.getData().remove(0);
        }
        if (seriesAccYPrincipal.getData().size() > WINDOW_SIZE) {
            seriesAccYPrincipal.getData().remove(0);
        }
        if (seriesAccZPrincipal.getData().size() > WINDOW_SIZE) {
            seriesAccZPrincipal.getData().remove(0);
        }
        if (seriesAccX.getData().size() > WINDOW_SIZE) {
            seriesAccX.getData().remove(0);
        }
        if (seriesAccY.getData().size() > WINDOW_SIZE) {
            seriesAccY.getData().remove(0);
        }
        if (seriesAccZ.getData().size() > WINDOW_SIZE) {
            seriesAccZ.getData().remove(0);
        }

        if (!path.replace(" ", "").isEmpty()) {
            fileManager.addRow(time, accX, accY, accZ, path);
        }
    }

    private void prueba(int millisecons, int range) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {

            Random random = new Random();

            Platform.runLater(() -> {
                addData(LocalDateTime.now(),
                        (random.nextInt(range) + random.nextDouble()),
                        (random.nextInt(range) + random.nextDouble()),
                        (random.nextInt(range) + random.nextDouble()));
            });
        }, 0, millisecons, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
//        scheduledExecutorService.shutdownNow();
    }

    /*Logic to start the program*/
    private void initMenuItemAction(ActionEvent e, Stage primaryStage) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Archivo");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("CSV Files", "*.csv"));
        File selectedFile = fileChooser.showSaveDialog(primaryStage);

        if (selectedFile != null) {
            path = selectedFile.getAbsolutePath();

            if (!path.contains(".csv")) {
                path = path + ".csv";
            }

            System.out.println("Path: " + path);
            Alert a = new Alert(AlertType.INFORMATION);
            a.setTitle("Información");
            a.setHeaderText("Archivo creado");
            a.show();
            fileManager.guardarArchivo(path);

            /*Comando para probar sin tener conenctado el arduino*/
//            prueba(200, 20);

            /*Comando para iniciar conexion con Arduino*/
            leerDatos();
        } else {
            Alert a = new Alert(AlertType.ERROR);
            a.setTitle("Sin archivo");
            a.setHeaderText("Debe crear o elegir un archivo donde se almacenara la información");
            a.show();
        }
    }

    private void infoMenuItemAction(ActionEvent e) {
        Alert a = new Alert(AlertType.INFORMATION);
        a.setTitle("Instrucciones");
        a.setHeaderText("Para la ejecucion de este programa\n"
                + "se requiere crear un archivo CSV,\n"
                + "de no crearse no iniciara la ejecucion\n"
                + "del programa.");
        a.show();
    }
}
