package wsa.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.URI;

/** Classe che lancia l'applicazione e imposta le tab.
 * I campi {@link Main#we} e {@link Main#browser} servono al WSA
 * per dare la possibilità di visualizzare le pagine incontrate nel crawling. */
public class Main extends Application {

    public static WebEngine we;
    public static Stage browser;
    private static TabPane tabs;
    private int nEsp = 1;

    /** Lancia l'applicazione. */
    public static void main(String[] args) { launch(args); }

    /** Viene chiamato da launch(args) in {@link Main#main}.
     * Setta i campi della classe, imposta il TabPane nel primaryStage e il pulsante per aggiungere
     * nuove Tab (oggetti {@link wsa.gui.TabTab}). Se è presente solo una Tab,
     * il bottone {@link wsa.gui.vboxes.VBoxPostStart#linkVersoSite}
     * (che calcola il numero di link che puntano da un sito aperto a un altro) viene disattivato,
     * altrimenti viene attivato. (tramite il metodo {@link wsa.gui.TabTab#attivaLinks(boolean)})
     * Poi setta la scena e visualizza il primaryStage. */
    @Override
    public void start(Stage primaryStage) {
        WebView wView = new WebView();
        we = wView.getEngine();
        browser = new Stage();
        Scene sceneBrowser = new Scene(wView, 800, 600);
        browser.setScene(sceneBrowser);
        browser.setTitle("Browser");

        AnchorPane root = new AnchorPane();
        tabs = new TabPane();
        Button addButton = new Button("+");
        Label label = new Label("Per avviare una nuova esplorazione, premere sul '+' in alto a destra!");
        label.setFont(new Font("Verdana", 14));

        AnchorPane.setTopAnchor(tabs, 0.0);
        AnchorPane.setLeftAnchor(tabs, 0.0);
        AnchorPane.setRightAnchor(tabs, 0.0);
        AnchorPane.setTopAnchor(addButton, 3.3);
        AnchorPane.setRightAnchor(addButton, 3.3);
        AnchorPane.setLeftAnchor(label, 10.0);
        AnchorPane.setTopAnchor(label, 10.0);

        addButton.setOnAction(e -> {
            TabTab tab = new TabTab(primaryStage, "Esplorazione " + nEsp++);
            tabs.getTabs().add(tab);
            tabs.getSelectionModel().select(tab);
        });

        tabs.getTabs().addListener((ListChangeListener<Tab>) c -> {
            if (tabs.getTabs().size() > 1) {
                tabs.getTabs().stream().forEach(tab -> {
                    TabTab tabTab = (TabTab) tab;
                    tabTab.attivaLinks(true);
                });
            } else if (tabs.getTabs().size() == 1) {
                TabTab tabTab = (TabTab) tabs.getTabs().get(0);
                tabTab.attivaLinks(false);
                label.setVisible(false);
            } else if (tabs.getTabs().size() == 0) {
                label.setVisible(true);
            }
        });

        addButton.fire(); // si inizia con una tab

        root.getChildren().addAll(tabs, addButton, label);

        Scene scene = new Scene(root, 800, 550); //width, height
        primaryStage.setTitle("Web Site Analyser");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            tabs.getTabs().stream().forEach(tab -> tab.getOnClosed().handle(event));
            tabs.getTabs().clear();
            System.exit(0);
        });
    }

    /** @return un ObservableList<URI> contenente tutti i domini delle esplorazioni
     *  che vengono visualizzate in ogni Tab. */
    public static ObservableList<URI> getDoms(){
        ObservableList<URI> doms = FXCollections.observableArrayList();
        tabs.getTabs().stream().forEach(tab -> {
            TabTab tabTab = (TabTab) tab;
            if (tabTab.getDom() != null) doms.add(tabTab.getDom());
        });
        return doms;
    }
}
