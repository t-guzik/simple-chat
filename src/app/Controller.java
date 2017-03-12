package app;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import tcp.ClientTCP;

public class Controller {
    @FXML
    private TextField usersTF;
    @FXML
    private Button sendBtn;
    @FXML
    private TextField msgTF;
    @FXML
    private TextArea chatTA;
    @FXML
    private Button saveBtn;
    @FXML
    private TextField loginTF;
    @FXML
    private Label loginLbl;
    @FXML
    private Label errorsLbl;
    @FXML
    private Button logoutBtn;
    @FXML
    private Button loginBtn;

    protected ClientTCP client;
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

    public void logout(ActionEvent actionEvent) {
        client.logout();
        Stage stage = (Stage) logoutBtn.getScene().getWindow();
        stage.close();
    }

    public void login(ActionEvent actionEvent) {
        loginBtn.setDisable(true);
        msgTF.setDisable(false);
        msgTF.requestFocus();
        sendBtn.setDisable(false);
        logoutBtn.setDisable(false);
        usersTF.setVisible(true);
        usersTF.setAlignment(Pos.CENTER);

        client = new ClientTCP(chatTA, usersTF, login);
        client.login();
    }

    public void send(ActionEvent actionEvent) {
        if(!msgTF.getText().equals("")){
            client.send(msgTF.getText());
            chatTA.appendText(login + ": " + msgTF.getText() + "\n");
            msgTF.setText("");
        }
    }


    public void sendEnter(KeyEvent keyEvent) {
        if(keyEvent.getCode().equals(KeyCode.ENTER))
            send(new ActionEvent());
    }
}
