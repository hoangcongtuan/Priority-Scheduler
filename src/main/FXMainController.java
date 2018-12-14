package main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class FXMainController implements Initializable {

    @FXML
    public TableView tableProcess;
    @FXML
    public Canvas ganttCanvas;
    @FXML
    public Label lbCurrentTime;
    @FXML
    private Label status;
    @FXML
    private Button runButton;
    @FXML
    private TextArea input;
    @FXML
    private ChoiceBox schMethod;


    private static CPU cpu;
    private static double speed;

    /**
     * Simulation Area
     */
    @FXML
    private Button stopButton;
    @FXML
    private Button pauseResumeButton;
    @FXML
    private ScrollPane scroll;
    private VBox processBox;

    private static long lastUpdateTime = 0;
    private VBox subroot1;
    private SimulationCore simulationCore;
    private Map<Integer, Integer> procBarsTable = new HashMap<>();
    private ArrayList<ProgressBar> procBars = new ArrayList<>();
    private ArrayList<Label> labels = new ArrayList<>();

    @FXML
    private void handleRunButtonAction(ActionEvent event) {
        if (validate().equals("OK")) {
            status.setText("OK");
            status.setTextFill(Color.DARKGREEN);
            String method = schMethod.getValue().toString();

            if (input.getText().startsWith("Random")) {
                status.setText("Error: Randomize First (press Random Input button)");
                status.setTextFill(Color.RED);
            } else {
                GraphicsContext gContext = ganttCanvas.getGraphicsContext2D();
                gContext.clearRect(0, 0, ganttCanvas.getWidth(), ganttCanvas.getHeight());
                initGanttChart(gContext);
                cpu = new CPU(input.getText(), method);
                doSomeThingWithProcessList(cpu.getAllProcs());

                cpu.Simulate();
                speed = 1;
                startSimulation();
            }
        } else {
            status.setText(validate());
            status.setTextFill(Color.RED);
        }
    }

    private void doSomeThingWithProcessList(ArrayList<Process> allProcs) {
        ObservableList<Process> obserList = FXCollections.observableArrayList(allProcs);
        tableProcess.setItems(obserList);
    }

    @FXML
    private void handleRandomInputButtonAction(ActionEvent event) {
        if (input.getText().startsWith("Random")) {
            status.setText("OK");
            status.setTextFill(Color.DARKGREEN);
            String[] lines = input.getText().split("\n");
            String[] split = lines[0].split("\\s+");
            CPU.randProc(Integer.valueOf(split[1]));
            String res = "";
            for (String string : CPU.getRandomData()) {
                res += string + "\n";
            }
            input.setText(res);
        } else {
            CPU.randProc(5);
            String res = "";
            for (String string : cpu.getRandomData()) {
                res += string + "\n";
            }
            input.setText(res);
        }

        String method = schMethod.getValue().toString();
        cpu = new CPU(input.getText(), method);
        doSomeThingWithProcessList(cpu.getAllProcs());
    }

    private String validate() {
        String inputCheck = input.getText();
        String lines[] = inputCheck.split("\n");

        if (lines.length == 0) {
            return "Error: No Input";
        } else if (lines[0].startsWith("Random")) {
            String split[] = lines[0].split("\\s+");
            try {
                Integer.valueOf(split[1]);
            } catch (Exception e) {
                return "Error: Bad Input for Random";
            }
        } else {
            try {
                for (String line : lines) {
                    String[] split = line.split("\\s+");
                    Double.parseDouble(split[0]);
                    Double.parseDouble(split[1]);
                    Integer.parseInt(split[2]);
                }
            } catch (Exception e) {
                return "Error: Bad Input";
            }
        }

        return "OK";
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        GraphicsContext gContext = ganttCanvas.getGraphicsContext2D();
        initGanttChart(gContext);
        CPU.setGraphicsContext(gContext);

        initTableProcess();

        schMethod.getItems().removeAll(schMethod.getItems());
        schMethod.getItems().addAll("Preemptive Priority", "Priority");
        schMethod.getSelectionModel().select("Preemptive Priority");

        //disable button
        stopButton.setDisable(true);
        pauseResumeButton.setDisable(true);

        //init simualation box
        processBox = new VBox();
        processBox.setSpacing(5);
        processBox.setAlignment(Pos.CENTER);
        scroll.setContent(processBox);
        scroll.vvalueProperty().bind(processBox.heightProperty());

        simulationCore = new SimulationCore() {
            @Override
            public void handle(long now) {
                if (now - lastUpdateTime >= 28_000_000 * (1 / FXMainController.getSpeed())) {
                    ProgressBar tmp;
                    String strtmp = "";
                    if (getScanner().hasNextLine()) {
                        String line = getScanner().nextLine();
                        String[] splt = line.split("\\s+");

                        int pid = Integer.valueOf(splt[1]);
                        double burstTime = Double.valueOf(splt[2]);
                        double totalBurstTime = Double.valueOf(splt[3]);
                        int priority = Integer.valueOf(splt[4]);
                        double arrivalTime = Double.valueOf(splt[5]);
                        double startTime = Double.valueOf(splt[6]);
                        double finishTime = Double.valueOf(splt[7]);
                        double waitTime = Double.valueOf(splt[8]);
                        double turnAroundTime = Double.valueOf(splt[9]);
                        double responseTime = Double.valueOf(splt[10]);

                        if (procBarsTable.containsKey(Integer.valueOf(splt[1]))) {
                            strtmp = "";
                            strtmp += splt[1] + ": ";
                            strtmp += "WaitTime:" + String.format("%.1f", waitTime) + " ";
                            strtmp += "TrnATime:" + String.format("%.1f", turnAroundTime) + " - ";
                            strtmp += "Priority:" + priority + " ";
                            strtmp += "ArivTime:" + String.format("%.1f", arrivalTime) + " ";
                            strtmp += "StartTime:" + String.format("%.1f", startTime) + " ";
                            strtmp += "FinishTime:" + String.format("%.1f", finishTime);

                            double progress =  (totalBurstTime - burstTime)
                                    / totalBurstTime;

                            procBars.get(procBarsTable.get(pid))
                                    .setProgress(progress);
                            labels.get(procBarsTable.get(pid))
                                    .setText(strtmp);
                            lbCurrentTime.setText(splt[0]);
                            strtmp = "";
                        } else {
                            subroot1 = new VBox();
                            double progress =  (totalBurstTime - burstTime)
                                    / totalBurstTime;
                            tmp = new ProgressBar(progress);
                            tmp.setPrefWidth(400);
                            procBars.add(tmp);
                            procBarsTable.put(pid, procBars.size() - 1);
                            subroot1.getChildren().add(procBars.get(procBars.size() - 1));
                            strtmp = "";
                            strtmp += " " + pid + ": ";
                            strtmp += "WaitTime:" + String.format("%.1f", waitTime) + " ";
                            strtmp += "TrnATime:" + String.format("%.1f", turnAroundTime) + " - ";
                            strtmp += "Priority:" + priority + " ";
                            strtmp += "ArivTime:" + String.format("%.1f", arrivalTime) + " ";
                            strtmp += "StartTime:" + String.format("%.1f", startTime) + " ";
                            strtmp += "FishTime:" + String.format("%.1f", finishTime);
                            Label label = new Label(strtmp);
                            subroot1.setPadding(new Insets(8, 8, 8, 8));
                            labels.add(label);
                            subroot1.getChildren().add(labels.get(procBars.size() - 1));

                            strtmp = "";
                            subroot1.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                                    null, new BorderWidths(1))));
                            subroot1.setAlignment(Pos.BASELINE_LEFT);
                            processBox.getChildren().add(subroot1);
                        }
                    } else {
                        subroot1 = new VBox();
                        subroot1.getChildren().add(new Label("Report:\n" + FXMainController.getCpu().getReport()));
                        processBox.getChildren().add(subroot1);
                        getScanner().close();
                        simulationCore.stop();

                        //update ui
                        runButton.setDisable(false);
                        pauseResumeButton.setText("Pause");
                        pauseResumeButton.setDisable(true);
                        stopButton.setDisable(true);
                    }
                    lastUpdateTime = now;
                }
            }
        };

    }

    private void initGanttChart(GraphicsContext gContext) {
        gContext.setFill(Color.BLACK);
        gContext.setStroke(Color.BLACK);
        gContext.setLineWidth(1f);

        double width = gContext.getCanvas().getWidth();
        double height = gContext.getCanvas().getHeight();

        gContext.strokeLine(0, height / 2, width, height / 2);
    }

    private void initTableProcess() {
        TableColumn<Process, String> pidCol = new TableColumn<Process, String>("PID");
        TableColumn<Process, Float> burstCol = new TableColumn<Process, Float>("Burst Time");
        TableColumn<Process, Float> delayCol = new TableColumn<Process, Float>("Delay Time");
        TableColumn<Process, Float> arrivalCol = new TableColumn<Process, Float>("Arrival Time");
        TableColumn<Process, Integer> priorityCol = new TableColumn<Process, Integer>("Priority");


        pidCol.setCellValueFactory(new PropertyValueFactory<>("PID"));
        burstCol.setCellValueFactory(new PropertyValueFactory<>("totalBurstTime"));
        delayCol.setCellValueFactory(new PropertyValueFactory<>("delayTime"));
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priority"));

        tableProcess.getColumns().clear();
        tableProcess.getColumns().addAll(pidCol, burstCol, delayCol, arrivalCol, priorityCol);
    }

    private static CPU getCpu() {
        return cpu;
    }

    private static double getSpeed() {
        return speed;
    }

    /**
     * Simumlation Area
     */

    @FXML
    private void handleStopButtonAction(ActionEvent event) {
        simulationCore.stop();
        stopButton.setDisable(true);
        runButton.setDisable(false);
        pauseResumeButton.setDisable(true);
        pauseResumeButton.setText("Pause");
    }

    public void handlePauseResumeButtonAction(ActionEvent actionEvent) {
        if (simulationCore.getState() == SimulationCore.SimulationState.RUNNING) {
            simulationCore.pause();
            pauseResumeButton.setText("Resume");
        } else {
            simulationCore.resume();
            pauseResumeButton.setText("Pause");
        }
    }


    private void startSimulation() {
        //title
        Label lbTitle = new Label();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, hh:mm:ss aaaa");
        String strNow = sdf.format(calendar.getTime());
        lbTitle.setText(strNow);
        processBox.getChildren().add(lbTitle);

        //remove old data
        procBars.clear();
        procBarsTable.clear();
        labels.clear();
        simulationCore.setData(getCpu().getSimulationData());
        simulationCore.start();
        stopButton.setDisable(false);
        pauseResumeButton.setDisable(false);
        runButton.setDisable(true);
    }

    public void handleClearSimulationButtonAction(ActionEvent actionEvent) {
        procBars.clear();
        labels.clear();
        procBarsTable.clear();
        processBox.getChildren().clear();
    }
}
