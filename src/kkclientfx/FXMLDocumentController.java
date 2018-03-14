/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kkclientfx;

import interfaces.GameInterface;
import interfaces.PlayerInterface;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.scene.control.TextField;
import javafx.scene.text.Text;

/**
 *
 * @author tomek.buslowski
 */

// This class implements PlayerInterface = This is my PLAYER
public class FXMLDocumentController extends UnicastRemoteObject implements Initializable, PlayerInterface {
    
    int i=0;
    private boolean myTurn = false;
    private String name;
    private String symbol = "brak";
    private GameInterface game;
    private boolean isInGame = false;
    private List<Button> field = new ArrayList<>();
    
    @FXML
    private Button b1;
    @FXML
    private Button b2;
    @FXML
    private Button b3;
    @FXML
    private Button b4;
    @FXML
    private Button b5;
    @FXML
    private Button b6;
    @FXML
    private Button b7;
    @FXML
    private Button b8;
    @FXML
    private Button b9;
    @FXML
    private TextField nameTextField;
    @FXML
    private Label gameInfoLabel;
    @FXML
    private Text infoText;
    
    public FXMLDocumentController() throws RemoteException { }
    
    private void ready() {
        if( game==null ) {
            infoText.setText("Brak połączenia z serwerem. Kliknij Połącz.");
            return;
        }
        try {
            setName(nameTextField.getText());
            setSymbol(game.getFreeSymbol());
            
            isInGame = game.addPlayer(this);
            if( !isInGame ) {
                infoText.setText("Za dużo graczy.");
                System.out.println("Za dużo graczy.");
                setSymbol("brak");
                return;
            }
            infoText.setText("Podłączono do gry. Twój znak to: " + this.getSymbol());
            gameInfoLabel.setText(game.getGameInfo());
            System.out.println("Podłączono do gry: " + this.getName());
            
        } catch (RemoteException ex) {
            infoText.setText("Wystąpił wyjątek podczas łączenia z serwerem.");
            System.out.println("Wystąpił wyjątek podczas łączenia z serwerem.");
            ex.printStackTrace();
        }
    }
    
    public void setGameInfo() throws RemoteException {
        Platform.runLater(() -> {
            try {
                gameInfoLabel.setText(game.getGameInfo());
            } catch (RemoteException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    
    @FXML
    private void readyOnAction() {
        if( isInGame ){
            try {
                infoText.setText("Gra już o Tobie wie. Twój znak to: " + this.getSymbol());
            } catch (RemoteException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }
        if( nameTextField.getText().length() == 0 ) {
            infoText.setText("Wpisz imie i kliknij Gotowy");
            return;
        }
        ready();
    }
    
    @FXML
    private void connectOnAction() {
        connect();
    }
    
    @FXML
    private void disconnectOnAction() {
        if( !isInGame ) {
            infoText.setText("Gracz nie jest podłączony do gry.");
            resetField();
            return;
        }
        try {
            game.removePlayer(this);
            isInGame = false;
            System.out.println("Odłączono z gry.");
            infoText.setText("Odłączono z gry.");
            gameInfoLabel.setText("-");
            
        } catch (RemoteException ex) {
            infoText.setText("Wyjątek podczas odłączania z gry.");
            System.out.println("Wyjątek podczas odłączania z gry.");
        }
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        connect();
        field.add(b1);
        field.add(b2);
        field.add(b3);
        field.add(b4);
        field.add(b5);
        field.add(b6);
        field.add(b7);
        field.add(b8);
        field.add(b9);
    }

    public void connect() {
        if( game!=null ) {
            infoText.setText("Połączenie z serwerem działa.");
            return;
        }
        infoText.setText("Łączenie....");
        // Odpalam w drugim wątku
        
        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    game = (GameInterface) Naming.lookup("rmi://localhost/Game");
                } catch(MalformedURLException | NotBoundException | RemoteException e) {
                    infoText.setText("Nie połączono z serwerem. " + i++);
                    
                    //e.printStackTrace();
                }
                if( game!=null ) infoText.setText("Połączono z serwerem.");
                return null;
            }
        };
        synchronized(this){
            new Thread(task).start();
        }
    }
    
    @Override
    public void setName(String n) throws RemoteException {
        name = n;
    }
    
    @Override
    public String getName() throws RemoteException {
        return name;
    }
    
    @Override
    public void setSymbol(String s) throws RemoteException {
        symbol = s;
    }
    
    @Override
    public String getSymbol() throws RemoteException {
        return symbol;
    }
    
    public void updateField() throws RemoteException {
        String[] f = game.getField();
        Platform.runLater(() -> {
            int i=0;
            for(Button b : field) {
                b.setText(f[i++]);
            }
        });
    }
    
    public void resetField() {
        Platform.runLater(() -> {
            for(Button b : field) {
                b.setText("?");
            }
        });
    }
    
    @FXML
    private void fieldPressedOnAction(ActionEvent e) throws RemoteException {
        if( myTurn ) {
            String index = ((Button)e.getSource()).getId();
            int buttonNumber = (int)index.charAt(1) - 48;
            
            game.setField( buttonNumber );
        }
    }

    @Override
    public void update() throws RemoteException {
        if( game.WAITING() ) {
            setGameInfo();
            resetField();
        }
        
        else if( game.PLAYING() ) {
            updateField();
            if( game.getActualPlayer().hashCode() == this.hashCode() ) {
                myTurn = true;
                Platform.runLater(() -> {
                    gameInfoLabel.setText("Twój ruch");
                });
            } else {
                myTurn = false;
                Platform.runLater(() -> {
                    gameInfoLabel.setText("Czekaj na ruch przeciwnika");
                });
            }
        }
    }

    @Override
    public void informWinner(String msg) throws RemoteException {
        Platform.runLater(() -> {
            gameInfoLabel.setText(msg);
        });
    }
    
    @Override
    public void setMyTurn(boolean b) throws RemoteException {
        myTurn = b;
    }
    
}
