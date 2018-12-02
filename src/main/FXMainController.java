package main;

import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FXMainController implements Initializable {
    @FXML
    private Label status;

    @FXML
    private Button run;

    @FXML
    private Button randomInput;

    @FXML
    private Button reloadFile;

    @FXML
    private TextArea input;

    @FXML
    private ChoiceBox schMethod;

    @FXML
    private TextField simSpeed;

    @FXML
    private TextField cs;

    private static String prevInput = "";

    private static CPU cpu;

    private static double speed;

    /**
     * Simulation Area
     */
    @FXML
    private Button backButton;

    @FXML
    private ScrollPane scroll;

    private static long lastUpdate = 0;
    private VBox subroot1;
    private AnimationTimer at;
    private ArrayList<Scheduler> lvls = new ArrayList<>();
    private Map<Integer, Integer> procBarsTable = new HashMap<>();
    private ArrayList<ProgressBar> procBars = new ArrayList<>();
    private ArrayList<Label> labels = new ArrayList<>();

    @FXML
    private void handleRunButtonAction(ActionEvent event) {

        if (validate() == "OK") {
            status.setText("OK");
            status.setTextFill(Color.DARKGREEN);
            String method = schMethod.getValue().toString();

            if (input.getText().startsWith("Random")) {
                status.setText("Error: Randomize First (press Random Input button)");
                status.setTextFill(Color.RED);
            } else {
                cpu = new CPU(input.getText(), method);
                prevInput = input.getText();
                cpu.Simulate();
                speed = Double.parseDouble(simSpeed.getText());
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("Simulation.fxml"));
                    Scene scene = new Scene(root);
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(scene);
                    stage.show();
                } catch (IOException ex) {
                    Logger.getLogger(FXMainController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            status.setText(validate());
            status.setTextFill(Color.RED);
        }
    }

    @FXML
    private void handleRandomInputButtonAction(ActionEvent event) {

        if (input.getText().startsWith("Random")) {
            status.setText("OK");
            status.setTextFill(Color.DARKGREEN);
            String[] line = input.getText().split("\n");
            String[] split = line[0].split("\\s+");
            CPU.randProc(Integer.valueOf(split[1]));
            String res = "";
            for (String string : cpu.getRandomData()) {
                res += string + "\n";
            }
            input.setText(res);
        } else {
            CPU.randProc(Integer.valueOf(5));
            String res = "";
            for (String string : cpu.getRandomData()) {
                res += string + "\n";
            }
            input.setText(res);
        }
    }

    @FXML
    private void handleReloadFileButtonAction(ActionEvent event) {

        FileChooser fileChooser = new FileChooser();

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        Stage primaryStage = new Stage();
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            String s = "", res = "";
            double burstTime = 0, delayTime = 0;
            int priority = 0, level = 0;
            try {
                BufferedReader input = new BufferedReader(new FileReader(file));
                while ((s = input.readLine()) != null) {
                    String[] split = s.split("\\s+");
                    burstTime = Double.parseDouble(split[0]);
                    delayTime = Double.parseDouble(split[1]);
                    priority = Integer.parseInt(split[2]);
                    level = Integer.parseInt(split[3]);
                    res += burstTime + " " + delayTime + " " + priority + " " + level + "\n";
                }
                this.input.setText(res);
            } catch (Exception e) {
                status.setText("Error: Bad Input File");
                status.setTextFill(Color.RED);
            }
        }
    }

    public EventHandler<KeyEvent> numericValidation(final Integer max_Lengh) {
        return e -> {
            TextField txt_TextField = (TextField) e.getSource();
            if (txt_TextField.getText().length() >= max_Lengh) {
                e.consume();
            }
            if (e.getCharacter().matches("[0-9.]")) {
                if (txt_TextField.getText().contains(".") && e.getCharacter().matches("[.]")) {
                    e.consume();
                } else if (txt_TextField.getText().length() == 0 && e.getCharacter().matches("[.]")) {
                    e.consume();
                }
            } else {
                e.consume();
            }
        };
    }

    public String validate() {

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
            int level = 0;
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

        if (Double.parseDouble(cs.getText()) < 0.4) {
            return "Error: minimum value for quantum is 0.4";
        }

        return "OK";
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        input.setText(prevInput);

        simSpeed.addEventFilter(KeyEvent.KEY_TYPED, numericValidation(2));
        cs.addEventFilter(KeyEvent.KEY_TYPED, numericValidation(5));

        schMethod.getItems().removeAll(schMethod.getItems());
        schMethod.getItems().addAll("Preemptive Priority", "Priority");
        schMethod.getSelectionModel().select("Preemptive Priority");



//        input.setPrefHeight(650);
    }

    public static CPU getCpu() {
        return cpu;
    }

    public static double getSpeed() {
        return speed;
    }

    /**
     * Simumlation Area
     */

    @FXML
    private void handleBackButtonAction(ActionEvent event) {

        at.stop();
        procBars.clear();
        procBarsTable.clear();
        labels.clear();
        lvls.clear();
        FXMainController.getCpu().resetSimData();
        FXMainController.getCpu().resetReport();
        FXMainController.getCpu().resetAll();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException ex) {
            Logger.getLogger(FXMainController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        VBox root = new VBox();
        root.setSpacing(5);
        root.setAlignment(Pos.CENTER);
        scroll.setContent(root);
        Scanner scanner = new Scanner(FXMainController.getCpu().getSimData());
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.FLOOR);

        at = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 28_000_000 * (1/FXMainController.getSpeed())){
                    ProgressBar tmp;
                    String strtmp = "";
                    if (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        String[] splt = line.split("\\s+");
                        if(procBarsTable.containsKey(Integer.valueOf(splt[0]))){
                            strtmp = "";
                            strtmp += splt[0] + ": ";
                            strtmp += "WaitTime:" + String.format("%.1f", Double.valueOf(splt[7])) + " ";
                            strtmp += "TrnATime:" + String.format("%.1f", Double.valueOf(splt[8])) + " - ";
                            strtmp += "Priority:" + splt[3] + " ";
                            strtmp += "ArivTime:" + String.format("%.1f", Double.valueOf(splt[4])) + " ";
                            strtmp += "StrtTime:" + String.format("%.1f", Double.valueOf(splt[5])) + " ";
                            strtmp += "FishTime:" + String.format("%.1f", Double.valueOf(splt[6]));
                            procBars.get(procBarsTable.get(Integer.valueOf(splt[0])))
                                    .setProgress( (Double.valueOf(df.format(Double.valueOf(splt[2])))
                                            - (Double.valueOf(df.format(Double.valueOf(splt[1])))) )
                                            / Double.valueOf(df.format(Double.valueOf(splt[2]))) );
                            labels.get(procBarsTable.get(Integer.valueOf(splt[0])))
                                    .setText(strtmp);
                            strtmp = "";
                        }else{
                            subroot1 = new VBox();
                            tmp = new ProgressBar((Double.valueOf(splt[2]) - Double.valueOf(splt[1]))
                                    / Double.valueOf(splt[2]));
                            tmp.setPrefWidth(200);
                            procBars.add(tmp);
                            procBarsTable.put(Integer.valueOf(splt[0]), procBars.size()-1);
                            subroot1.getChildren().add(procBars.get(procBars.size()-1));
                            strtmp = "";
                            strtmp += " " + splt[0] + ": ";
                            strtmp += "WaitTime:" + String.format("%.1f", Double.valueOf(splt[7])) + " ";
                            strtmp += "TrnATime:" + String.format("%.1f", Double.valueOf(splt[8])) + " - ";
                            strtmp += "Priority:" + splt[3] + " ";
                            strtmp += "ArivTime:" + String.format("%.1f", Double.valueOf(splt[4])) + " ";
                            strtmp += "StrtTime:" + String.format("%.1f", Double.valueOf(splt[5])) + " ";
                            strtmp += "FishTime:" + String.format("%.1f", Double.valueOf(splt[6]));
                            Label label = new Label(strtmp);
                            subroot1.setPadding(new Insets(8, 8, 8, 8));
                            labels.add(label);
                            subroot1.getChildren().add(labels.get(procBars.size()-1));

                            strtmp = "";
                            subroot1.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                                    null,new BorderWidths(1))));
                            subroot1.setAlignment(Pos.BASELINE_LEFT);
                            root.getChildren().add(subroot1);
                        }
                    }else{
                        subroot1 = new VBox();
                        subroot1.getChildren().add(new Label("Report:\n" + FXMainController.getCpu().getReport()));
                        root.getChildren().add(subroot1);
                        scanner.close();
                        at.stop();
                    }
                    lastUpdate = now;
                }
            }
        };
        at.start();
    }
}
