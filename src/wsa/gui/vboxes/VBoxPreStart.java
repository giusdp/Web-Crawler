package wsa.gui.vboxes;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import wsa.web.SiteCrawler;
import wsa.web.TabCrawler;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Classe che rappresenta la schermata iniziale. Un oggetto di questa classe
 * ha sempre associato il medesimo VBox {@link VBoxPreStart#vBox}, contenente
 * gli stessi nodi */
public class VBoxPreStart extends VBoxes {

    private DirectoryChooser dirChooser;
    private Stage stage;

    private Button start, carica, nuovo, cancellaDati, visualDati; // TASTI PRE START
    private TextField dominio, uriSeedPreStart;
    private Text notificaDavide, notifDominio, notifDirectory;
    private Tooltip aiutoStart;


    /** Costruttore. Crea tutti i Node di {@link VBoxPreStart#vBox} e ne imposta le azioni.
     * @param tabCrawler un TabCrawler che viene passato al costruttore di {@link VBoxes}
     * @param stage uno Stage utile a {@link VBoxPreStart#dirChooser}, oggetto che permette
     * di visualizzare il file system e scegliere una directory */
    public VBoxPreStart(TabCrawler tabCrawler, Stage stage) {
        super(tabCrawler);
        this.stage = stage;
        dirChooser = new DirectoryChooser();

        createVBox();
        setActions();
    }


    @Override
    protected void createVBox() {
        vBox.setMinHeight(500);

        VBox informations = new VBox(10);
        Text textDominioPreStart = new Text("Dominio: ");
        Text textDirectoryPreStart = new Text("Directory: ");
        notifDominio = new Text("Nessun dominio inserito.");
        notifDirectory = new Text("Nessuna directory scelta.");
        textDominioPreStart.setFont(Font.font(14));
        textDirectoryPreStart.setFont(Font.font(14));
        informations.getChildren().addAll(textDominioPreStart, notifDominio, textDirectoryPreStart, notifDirectory);

        Tooltip aiutoCarica = new Tooltip("Carica un'esplorazione archiviata." + "\n" + "Potrai riprenderla o visualizzarne i dati.");
        Tooltip aiutoNuovo = new Tooltip("Scegli una nuova directory dove salvare i dati riguardanti l'esplorazione.");

        carica = new Button("Carica archivio");
        Tooltip.install(carica, aiutoCarica);

        nuovo = new Button("Nuovo archivio");
        HBox hbox = new HBox(carica, nuovo);
        hbox.setSpacing(50);
        Tooltip.install(nuovo, aiutoNuovo);

        VBox textsfieldss = new VBox(10);
        dominio = new TextField();
        dominio.promptTextProperty().setValue("Dominio (Invio per aggiungere)");
        dominio.setMinWidth(100);
        dominio.setMaxWidth(400);

        uriSeedPreStart = new TextField();
        uriSeedPreStart.promptTextProperty().setValue("Seed (Invio per aggiungere)");
        uriSeedPreStart.setMinWidth(100);
        uriSeedPreStart.setMaxWidth(400);
        disableNode(uriSeedPreStart);
        textsfieldss.getChildren().addAll(dominio, uriSeedPreStart);

        notificaDavide = new Text("Inserire dati per iniziare l'esplorazione.");
        notificaDavide.setFont(Font.font(12.5));

        aiutoStart = new Tooltip("Inizia l'esplorazione.");
        Tooltip aiutoVisualDati = new Tooltip("Visualizza i dati relativi all'esplorazione caricata.");
        Tooltip aiutoCancellaDati = new Tooltip("Cancella dati immessi.");

        HBox startCancel = new HBox(20);
        start = new Button("Start");
        disableNode(start);
        start.setMaxWidth(220);
        Tooltip.install(start, aiutoStart);

        visualDati = new Button("Visualizza Dati");
        disableNode(visualDati);
        Tooltip.install(visualDati, aiutoVisualDati);

        cancellaDati = new Button("Cancella");
        disableNode(cancellaDati);
        cancellaDati.setMaxWidth(100);
        startCancel.getChildren().addAll(visualDati, start, cancellaDati);
        Tooltip.install(cancellaDati, aiutoCancellaDati);

        informations.setAlignment(Pos.CENTER);
        hbox.setAlignment(Pos.CENTER);
        textsfieldss.setAlignment(Pos.CENTER);
        startCancel.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(informations, hbox, textsfieldss, notificaDavide, startCancel);
    }

    @Override
    protected void setActions(){
    //Cancella tutti dati immessi dall'utente.
        cancellaDati.setOnAction(event -> {
            tabCrawler.setDir(null);
            tabCrawler.setDom(null);
            tabCrawler.cancelSeeds();

            notificaDavide.setText("Inserire dati per iniziare l'esplorazione.");
            notifDominio.setText("Nessun dominio inserito.");
            notifDirectory.setText("Nessuna directory scelta.");

            enableNode(dominio);
            disableNode(start);
            disableNode(cancellaDati);
            disableNode(uriSeedPreStart);
            disableNode(visualDati);
            start.setText("Start");
            aiutoStart.setText("inizia l'esplorazione.");
        });


        //Se l'utente preme Invio, imposta il parametro dom con il contenuto di domminio (immesso dall'utente).
        //Inoltre sar� abilitata la TextField per inserire gli uri seed, il bottone start e il pulsante cacellaDati.
        dominio.setOnAction(event -> {
            try {
                dom = new URI(dominio.getText());
                if (!SiteCrawler.checkDomain(dom)) throw new IllegalArgumentException();
                tabCrawler.setDom(dom);

                printURIOnGUI(notifDominio, "", dom);

                if (tabCrawler.getDir() == null){
                    notificaDavide.setText("Scegli una directory o inizia crawling senza salvare.");
                }
                else {
                    if (tabCrawler.getSeeds().size() == 0) notificaDavide.setText("Aggiungi, ora o dopo, seeds e inizia crawling.");
                    else notificaDavide.setText("Inizia crawling.");
                }
                dominio.clear();
                enableNode(uriSeedPreStart);
                enableNode(start);
                enableNode(cancellaDati);
            } catch (Exception e) {
                notificaDavide.setText("Dominio errato.");
            }
        });

        //Se l'utente preme Invio, aggiunge l' URI seed immesso al tabCrawler.
        uriSeedPreStart.setOnAction(event -> {
            try {
                URI seed = new URI(uriSeedPreStart.getText());
                tabCrawler.addSeed(seed);
                uriSeedPreStart.clear();
                notificaDavide.setText("Seed aggiunti: " + tabCrawler.getSeeds().size());
            } catch (Exception e) {
                notificaDavide.setText("Seed errato!");
            }
        });

        //Carica una vecchia esplorazione. L'utente pu� sceglierla grazie a un oggetto DirectoryChooser.
        //Inoltre abilita il bottone start, cancellaDati, la TextField degli URI seed, il bottone per visualizzare
        //i dati relativi all'esplorazione e disabilita la TextField per l'inserimento del dominio.
        carica.setOnAction(event -> {
            File file = dirChooser.showDialog(stage);
            Path dir = null;
            if (file != null) {
                dir = Paths.get(file.getPath());
                try {
                    dom = loadDomFromArchive(Paths.get(dir.toString() + "/Dominio.ser")); }
                catch (
                    IllegalArgumentException e){notificaDavide.setText(e.getMessage());
                    return;
                }
                notificaDavide.setText("Visualizza dati, aggiungi seeds e riprendi o cancella dati.");
                enableNode(start);
                enableNode(cancellaDati);
                enableNode(uriSeedPreStart);
                enableNode(visualDati);
                disableNode(dominio);
                tabCrawler.setDir(dir);
                tabCrawler.setDom(null);
                start.setText("Riprendi");
                aiutoStart.setText("Riprende l'esplorazione.");

                printURIOnGUI(notifDominio, "", dom);

                notifDirectory.setText(tabCrawler.getDir().toString());
            }
        });

        //Crea una nuova cartella per il salvataggio dell'esplorazione corrente. Pu� farlo
        //l'utente grazie a un oggetto DirectoryChooser.
        //Inoltre abilita la TextField del dominio, il bottone cancellaDati e disabilita il bottone
        //per visualizzare i dati.
        nuovo.setOnAction(event -> {
            File file = dirChooser.showDialog(stage);
            if (file != null){
                Path dir = Paths.get(file.getPath());
                enableNode(dominio);
                enableNode(cancellaDati);
                disableNode(visualDati);
                tabCrawler.setDir(dir);
                notifDirectory.setText(tabCrawler.getDir().toString());
                start.setText("Start");
                if (tabCrawler.getDom() == null) {
                    disableNode(start);
                    disableNode(uriSeedPreStart);
                    notificaDavide.setText("Inserisci dominio.");
                    printURIOnGUI(notifDominio, "", null);
                } else {
                    if (tabCrawler.getSeeds().size() == 0) notificaDavide.setText("Aggiungi, ora o dopo, seeds e inizia crawling.");
                    else notificaDavide.setText("Inizia crawling.");
                }
            }
        });
    }

    /** Imposta un TabCrawler.
     * @param tabCrawler viene passato al campo {@link VBoxPreStart#tabCrawler} */
    @Override
    public void changeTabCrawler(TabCrawler tabCrawler) {
        this.tabCrawler = tabCrawler;
    }

    /** Setta l'evento associato al bottone {@link VBoxPreStart#start}
     * @param event L'evento che si verificher� se {@link VBoxPreStart#start} viene premuto */
    public void setActionToStart(EventHandler<ActionEvent> event){
        start.setOnAction(event::handle);
    }

    /** Setta l'evento associato al bottone {@link VBoxPreStart#visualDati}
     * @param event L'evento che si verificher� se {@link VBoxPreStart#visualDati} viene premuto */
    public void setActionToVisData(EventHandler<ActionEvent> event){
        visualDati.setOnAction(event);
    }

    /** Cancella i dati immessi dall'utente */
    public void cancellaDati(){ cancellaDati.fire();}
}
