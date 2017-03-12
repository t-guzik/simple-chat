package app;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.Stage;
import tcp.ClientTCP;
import udp.ChatServerUDP;
import udp.ClientUDP;

import java.io.IOException;

public class Controller {
    @FXML
    private Button sendBtn;
    @FXML
    private Button mediaBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private Button logoutBtn;
    @FXML
    private Button loginBtn;
    @FXML
    private Label loginLbl;
    @FXML
    private Label errorsLbl;
    @FXML
    private TextArea chatTA;
    @FXML
    private TextField msgTF;
    @FXML
    private TextField usersTF;
    @FXML
    private TextField loginTF;

    private ClientTCP clientTCP;
    private ClientUDP clientUDP;
    private ChatServerUDP chatServerUDP;

    private String login;

    public void saveLogin(ActionEvent actionEvent) {
        loginTF.setText(loginTF.getText().replaceAll("\\s+", ""));
        if(loginTF.getText().equals("")){
            errorsLbl.setVisible(true);
            errorsLbl.setText("Set your login!");
        }
        else{
            login = loginTF.getText();
            errorsLbl.setVisible(false);
            errorsLbl.setText("");
            saveBtn.setVisible(false);
            loginTF.setVisible(false);
            loginLbl.setVisible(false);
            loginBtn.setDisable(false);
        }
    }

    public void login(ActionEvent actionEvent) {
        loginBtn.setDisable(true);
        msgTF.setDisable(false);
        msgTF.requestFocus();
        sendBtn.setDisable(false);
        mediaBtn.setDisable(false);
        logoutBtn.setDisable(false);
        usersTF.setVisible(true);
        usersTF.setAlignment(Pos.CENTER);

        clientTCP = new ClientTCP(chatTA, usersTF, login);
        clientTCP.login();

        chatServerUDP = new ChatServerUDP();
        clientUDP = new ClientUDP();
    }

    public void send(ActionEvent actionEvent) {
        if(!msgTF.getText().equals("")){
            clientTCP.send(msgTF.getText());
            chatTA.appendText(login + ": " + msgTF.getText() + "\n");
            msgTF.setText("");
        }
    }

    public void sendEnter(KeyEvent keyEvent) {
        if(keyEvent.getCode().equals(KeyCode.ENTER))
            send(new ActionEvent());
    }

    public void sendMedia(ActionEvent event) {
        clientUDP.send();
    }

    public void logout(ActionEvent actionEvent) {
        clientTCP.logout();
        clientUDP.logout();
        chatServerUDP.shutDown();

        Stage stage = (Stage) logoutBtn.getScene().getWindow();
        stage.close();
    }
}
