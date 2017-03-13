package app;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.Stage;

import java.io.IOException;

public class Controller {
    /** GUI elements */
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
    private RadioButton multicastRBtn;
    @FXML
    private TextArea chatTA;
    @FXML
    private TextField msgTF;
    @FXML
    private TextField usersTF;
    @FXML
    private TextField loginTF;

    /** Client data */
    private Client client;
    private String login;
    private boolean multicastActive = false;

    /**
     * Saving chosen login after saveBtn pressing.
     * @param actionEvent
     */
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

    /**
     * Logging to chat after loginBtn pressing.
     * @param actionEvent
     */
    public void login(ActionEvent actionEvent) throws IOException {
        loginBtn.setDisable(true);
        msgTF.setDisable(false);
        msgTF.requestFocus();
        sendBtn.setDisable(false);
        mediaBtn.setDisable(false);
        logoutBtn.setDisable(false);
        usersTF.setVisible(true);
        usersTF.setAlignment(Pos.CENTER);
        multicastRBtn.setText("Multicast group");

        if(multicastRBtn.isSelected()) {
            multicastRBtn.setVisible(true);
            multicastActive = true;
        }
        else
            multicastRBtn.setVisible(false);

        client = new Client(chatTA, usersTF, login, multicastActive);
        client.login();
    }

    /**
     * Sending text message after sendBtn pressing.
     * @param actionEvent
     */
    public void send(ActionEvent actionEvent) {
        if(!msgTF.getText().equals("")){
            client.send(msgTF.getText());
            chatTA.appendText(login + ": " + msgTF.getText() + "\n");
            msgTF.setText("");
        }
    }

    /**
     * Sending text message using EnterKey.
     * @param keyEvent
     */
    public void sendEnter(KeyEvent keyEvent) {
        if(keyEvent.getCode().equals(KeyCode.ENTER))
            send(new ActionEvent());
    }

    /**
     * Sending media message (AsciiArt) after mediaBtn pressing.
     * @param event
     * @throws IOException
     */
    public void sendMedia(ActionEvent event) throws IOException {
        if(multicastActive)
            client.sendMulticastUDP();
        else
            client.sendUDP();
    }

    public void setGroupMembership(MouseEvent mouseEvent) throws InterruptedException, IOException {
        if(multicastActive) {
            if (multicastRBtn.isSelected()) {
                client.setGroupMembershipSelected(true);
            } else {
                client.setGroupMembershipSelected(false);
            }
        }
    }

    /**
     * Logging out after logoutBtn pressing.
     * @param actionEvent
     */
    public void logout(ActionEvent actionEvent) {
        client.logout();
        Stage stage = (Stage) logoutBtn.getScene().getWindow();
        stage.close();
    }
}
