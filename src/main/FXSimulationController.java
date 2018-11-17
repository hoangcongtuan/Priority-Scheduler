/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package main;

import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FXML Controller class
 *
 * @author soheilchangizi
 */
public class FXSimulationController implements Initializable {
    
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
                            subroot1.setBorder(new Border(new BorderStroke(Color.BLACK,BorderStrokeStyle.SOLID,
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
