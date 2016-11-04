package wsa.gui.vboxes;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import wsa.gui.*;
import wsa.web.SiteCrawler;
import wsa.web.TabCrawler;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Classe che rappresenta la schermata che viene visualizzata durante l'esplorazione di un sito web,
 * quando viene premuto il pulsante start {@link wsa.gui.vboxes.VBoxPreStart#start}.
 * Un oggetto di questa classe ha sempre associato il medesimo VBox {@link VBoxPostStart#vBox}, contenente
 * gli stessi nodi */
public class VBoxPostStart extends VBoxes {

    private final Stage tableInfoStage = new Stage(), stageSceltaURI = new Stage(),
    stageDistanze = new Stage();
    private Button ripSosp, termina, indietro, distanze, linkVersoSite, graficiButton; // TASTI POST START
    private TextField uriSeedPostStart;
    private Text directoryArchiviazione, textDominio, statoEsplorazione, nLinkVersoSite;
    private Label nMassimoLinkInterni, nMassimoLinkPuntanti, nURInterni, uriVisitati, nURIErrati;
    private TableView<Page> table;
    private Tooltip aiutoRipSosp;
    private ListView<URI> listView;
    private VBoxDistanze vBoxDistanze;
    private Grafici grafici;
    private AtomicInteger numeroInterni, maxNumInt, maxNumPunt;
    private AtomicReference<ObservableList<URI>> uriInterniScaricati = new AtomicReference<>(FXCollections.observableArrayList());

    private ListChangeListener<Page> listener = c -> {
        if (c != null) {
            try {
                ObservableList<Page> pages = (ObservableList<Page>) c.getList();
                Page page = pages.get(pages.size() - 1);
                if (page != null) {
                    if (page.getEccezione() != null)
                        nURIErrati.setText("URI con errori: " + tabCrawler.getSiteCrawler().getErrors().size());
                    if (SiteCrawler.checkSeed(dom, page.getURI())) {
                        uriInterniScaricati.get().add(page.getURI());
                        numeroInterni.set(numeroInterni.get() + 1);
                        nURInterni.setText("URI interni al dominio: " + numeroInterni.get());
                        maxNumInt.set(Utils.aggiornaNumeroMaxLinkInterni(page, maxNumInt.get()));
                        nMassimoLinkInterni.setText("Massimo numero link interni: " + maxNumInt.get());
                        tabCrawler.getObservableList().stream().forEach(pagina -> maxNumPunt.set(Utils.aggiornaNumeroMaxLinkPuntanti(pagina, maxNumPunt.get())));
                        nMassimoLinkPuntanti.setText("Massimo numero link puntanti: " + maxNumPunt.get());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    /** Costruttore. Crea tutti i Node di {@link VBoxPostStart#vBox} e ne imposta le azioni.
     * @param tabCrawler un TabCrawler che viene passato al costruttore di {@link VBoxes}
     * @param domToVisualize il dominio dell'esplorazione corrente, che viene passato a {@link VBoxPostStart#dom}.
     * @param attivaLink se true, il bottone {@link VBoxPostStart#linkVersoSite} è attivato, altrimenti disattivato */
    public VBoxPostStart(TabCrawler tabCrawler, URI domToVisualize, boolean attivaLink) {
        super(tabCrawler);

        numeroInterni = new AtomicInteger(0);
        maxNumPunt = new AtomicInteger(0);
        maxNumInt = new AtomicInteger(0);

        directoryArchiviazione = new Text();
        textDominio = new Text("Dominio: ");
        statoEsplorazione = new Text("Stato esplorazione: in corso");

        uriSeedPostStart = new TextField();

        table = TableFactory.getTable(true, tabCrawler.getObservableList(), tableInfoStage, null);
        table.setMaxHeight(310.5);
        table.setItems(tabCrawler.getObservableList());

        printDom(domToVisualize);

        printDir(tabCrawler.getDir());

        createVBox();
        setActions();

        attivaLinks(attivaLink);
    }

    @Override
    protected void createVBox() {
        VBox informations = new VBox(directoryArchiviazione, textDominio, statoEsplorazione);
        informations.setSpacing(5);
        informations.setAlignment(Pos.BOTTOM_CENTER);

        uriVisitati = new Label("Pagine scaricate: ");
        nURIErrati = new Label("URI con errori: 0");
        nURInterni = new Label("URI interni al dominio: 0");
        nMassimoLinkInterni = new Label("Max numero link interni: 0");
        nMassimoLinkPuntanti = new Label("Max numero link puntanti: 0");

        uriVisitati.textProperty().bind(Bindings.concat("Pagine scaricate: ", Bindings.size(tabCrawler.getObservableList()).asString()));
        tabCrawler.getObservableList().addListener(listener);

        HBox hBoxExtraInfo = new HBox(10, uriVisitati, nURInterni, nURIErrati, nMassimoLinkInterni, nMassimoLinkPuntanti);
        hBoxExtraInfo.setAlignment(Pos.CENTER);

        aiutoRipSosp = new Tooltip("Mette in pausa l'esplorazione del sito web.");
        Tooltip aiutoTermina = new Tooltip("Termina quest'esplorazione." + "\n" +
                "Un' esplorazione terminata puo' essere ripresa" + "\n" + "solo caricandola dal suo archivio.");
        Tooltip aiutoIndietro = new Tooltip("Torna alla schermata iniziale.");
        Tooltip aiutoDistanze = new Tooltip("Permette di calcolare la distanza tra due URI interni o\nla massima distanza tra tutte le coppie di URI interni");
        Tooltip aiutoLinkVersoSite = new Tooltip("Calcola il numero di link verso un' altra esplorazione aperta");
        Tooltip aiutoGraficiButton = new Tooltip("Mostra istogrammi relativi ai link uscenti e a quelli entranti");

        ripSosp = new Button("Sospendi");
        Tooltip.install(ripSosp, aiutoRipSosp);
        termina = new Button("Termina");
        Tooltip.install(termina, aiutoTermina);
        indietro = new Button("Indietro");
        Tooltip.install(indietro, aiutoIndietro);
        distanze = new Button("Distanze");
        Tooltip.install(distanze, aiutoDistanze);
        disableNode(indietro);
        disableNode(distanze);

        HBox RSA = new HBox(8.0, ripSosp, termina);
        RSA.setAlignment(Pos.BOTTOM_CENTER);

        uriSeedPostStart.promptTextProperty().setValue("Seed (Invio per aggiungere)");
        uriSeedPostStart.setMinWidth(400);
        uriSeedPostStart.setMaxWidth(500);
        HBox hBoxTastiSinistra = new HBox(8.0, indietro, distanze);

        HBox hBox = new HBox(30, hBoxTastiSinistra, uriSeedPostStart, RSA);
        hBox.setAlignment(Pos.CENTER);

        graficiButton = new Button("Grafici");
        Tooltip.install(graficiButton, aiutoGraficiButton);
        linkVersoSite = new Button("Calcola links verso altra esplorazione");
        Tooltip.install(linkVersoSite, aiutoLinkVersoSite);
        disableNode(linkVersoSite);
        nLinkVersoSite = new Text("Numero links: (apri un'altra esplorazione per attivare)");
        HBox hBoxVersoSite = new HBox(5, graficiButton, linkVersoSite, nLinkVersoSite);
        hBoxVersoSite.setAlignment(Pos.CENTER);

        Label nota = new Label("    Nota: le celle piu' scure della tabella sono cliccabili.");
        nota.setFont(Font.font("Calibri", FontWeight.BOLD, 12));
        HBox hBoxNota = new HBox(nota);
        hBoxNota.setAlignment(Pos.BOTTOM_LEFT);

        vBox.setSpacing(10);
        vBox.getChildren().addAll(table, informations, hBox, hBoxExtraInfo, hBoxVersoSite, hBoxNota);

        listView = new ListView<>();
        Scene scene = new Scene(listView, 600, 300);
        stageSceltaURI.setTitle("Scelta dominio");
        stageSceltaURI.setScene(scene);
    }

    @Override
    protected void setActions() {
        //Se l'utente preme Invio, aggiunge l' URI seed immesso al tabCrawler.
        uriSeedPostStart.setOnAction(event1 -> {
            try {
                URI seed = new URI(uriSeedPostStart.getText());
                tabCrawler.addSeed(seed);
                uriSeedPostStart.clear();
            } catch (Exception e) {
                System.out.println("errore seed non passato");
            }
        });

        //Sospende o riprende l'esplorazione, e cambia lo stato dell'esplorazione visualizzato.
        ripSosp.setOnAction(event1 -> {
            if (tabCrawler.getSiteCrawler().isRunning()) {
                tabCrawler.getSiteCrawler().suspend();
                statoEsplorazione.setText("Stato esplorazione: sospesa");
                ripSosp.setText("Riprendi");
                aiutoRipSosp.setText("Riprende l'esplorazione del sito web.");
            } else {
                tabCrawler.start(true);
                statoEsplorazione.setText("Stato esplorazione: in corso");
                ripSosp.setText("Sospendi");
                aiutoRipSosp.setText("Mette in pausa l'esplorazione del sito web.");
            }
        });

        //Termina l'esplorazione, cancellando il SiteCrawler associato al tabCrawler; cambia lo stato
        //dell'esplorazione visualizzato e abilita i bottoni per tornare indietro e per calcolare
        //le distanze fra gli URI delle pagine scaricate. Inoltre disabilita il pulsante per riprendere o
        //sospendere l'esplorazione e lo stesso pulsante termina.
        termina.setOnAction(event -> {
            try {
                tabCrawler.getSiteCrawler().cancel();
                statoEsplorazione.setText("Stato esplorazione: terminata");
                enableNode(distanze);
                enableNode(indietro);
                disableNode(ripSosp);
                disableNode(termina);
            } catch (IllegalStateException e) {
                System.out.println("Crawling annullato");
            }
        });

        //Visualizza una finestra utile al calcolo delle distanze fra gli URI dell'esplorazione
        //e alla visualizzazione di altre informazioni sulle pagine.
        distanze.setOnAction(event -> {
            vBoxDistanze = new VBoxDistanze(tabCrawler);
            vBoxDistanze.preparaDistanze(uriInterniScaricati.get());
            stageDistanze.setScene(vBoxDistanze.scene);
            stageDistanze.show();
        });

        //Visualizza una finestra per il calcolo del numero di URI che puntano dal sito aperto da quest'esplorazione
        //a quello aperto da un altra esplorazione.
        linkVersoSite.setOnAction(event -> {
            ObservableList<URI> doms = Main.getDoms();
            doms.remove(tabCrawler.getDom());
            listView.setItems(doms);
            stageSceltaURI.show();
        });

        //Ogni elemento di questa lista (visualizzata dal bottone linkVersoSite) rappresenta
        //un altra esplorazione aperta. Se un elemento viene selezionato la finestra contenente la lista
        //si chiude e viene calcolato e visualizzato il numero di URI che puntano da quest'esplorazione
        //a quella selezionata.
        listView.setOnMouseClicked(event -> {
            URI secondDomain = listView.getSelectionModel().getSelectedItem();
            numeroLinkVersoSite = 0;
            stageSceltaURI.close();
            tabCrawler.getObservableList().stream().forEach(page -> calcolaNumVersoSite(secondDomain, page));
            nLinkVersoSite.setText("Numero links: " + numeroLinkVersoSite);
        });

        //Visualizza delle statistiche riguardanti l'esplorazione corrente sotto forma di grafici.
        graficiButton.setOnAction(event -> {
            grafici = new Grafici(tabCrawler);
            grafici.showGraphs();
        });
    }

    private int numeroLinkVersoSite;

    /** Se l'URI associato alla Page passata appartiene al dominio passato
     * incrementa il valore di {@link VBoxPostStart#numeroLinkVersoSite}, che
     * rappresenta il numero di link dell'esplorazione corrente che puntano a
     * secondDomain
     * @param page Page che contiene l'URI che si vuole controllare
     * @param seconDomain il dominio dell'esplorazione che si controlla se contiene l'URI di page o no */
    private void calcolaNumVersoSite(URI seconDomain, Page page){
        if (SiteCrawler.checkSeed(seconDomain, page.getURI())) numeroLinkVersoSite++;
    }

    /** Setta l'evento associato al bottone {@link VBoxPostStart#indietro}
     *  Chiude tutte le finestre aperte dai pulsanti in {@link VBoxPostStart}
     *  @param event l'evento che si verifica al click sul bottone, dopo le operazioni
     *  iniziali specificate */
    public void setBackAction(EventHandler<ActionEvent> event){
        indietro.setOnAction(e -> {
            ripSosp.setText("Sospendi"); //ma tutta sta roba non serve a una ciospa.
            Main.browser.close();
            tableInfoStage.close();
            stageSceltaURI.close();
            stageDistanze.close();
            if (grafici != null) grafici.closeGraphs();
            enableNode(ripSosp);
            enableNode(termina);
            event.handle(e);
            disableNode(distanze);
            disableNode(indietro);
        });
    }

    /** Imposta un TabCrawler e resetta tutte le informazioni relative all'esplorazione con il precedente
     * TabCrawler.
     * @param tabCrawler viene passato al campo {@link VBoxPreStart#tabCrawler} */
    @Override
    public void changeTabCrawler(TabCrawler tabCrawler) {

        this.tabCrawler.getObservableList().removeListener(listener);
        this.tabCrawler = tabCrawler;
        table.setItems(tabCrawler.getObservableList());
        numeroInterni.set(0);
        maxNumInt.set(0);
        maxNumPunt.set(0);
        uriInterniScaricati.set(FXCollections.observableArrayList());

        printDir(tabCrawler.getDir());

        uriVisitati.textProperty().unbind();
        uriVisitati.setText("Pagine scaricate: 0");
        nURIErrati.setText("URI con errori: 0");
        nURInterni.setText("URI interni al dominio: 0");
        nMassimoLinkInterni.setText("Max numero link interni: 0");
        nMassimoLinkPuntanti.setText("Max numero link puntanti: 0");
        statoEsplorazione.setText("Stato esplorazione: in corso");

        uriVisitati.textProperty().bind(Bindings.concat("Pagine scaricate: ", Bindings.size(tabCrawler.getObservableList()).asString()));
        this.tabCrawler.getObservableList().addListener(listener);

    }

    /** @param domToVisualize viene passato a {@link VBoxPostStart#dom}
     *  e dom viene passato a {@link wsa.gui.vboxes.VBoxes#printURIOnGUI(Text, String, URI)}
     *  che gli attaccherà in testa la stringa "Dominio: " e visualizzerà la stringa risultante
     *  settandola come testo di {@link VBoxPostStart#textDominio} */
    public void printDom(URI domToVisualize){
        dom = domToVisualize;
        if (tabCrawler.getDom() != null) dom = tabCrawler.getDom();
        printURIOnGUI(textDominio, "Dominio: ", dom);
    }

    /** Setta il testo di {@link VBoxPostStart#directoryArchiviazione}
     * per visualizzare la directory dov'è salvata l'esplorazione corrente,
     * se esiste */
    public void printDir(Path dir){
        if (dir == null) directoryArchiviazione.setText("Archiviazione: nessuna");
        else directoryArchiviazione.setText("Archiviazione: " + dir);
    }

    /** @param attiva se true, il bottone {@link VBoxPostStart#linkVersoSite}
     è attivato, altrimenti disattivato */
    public void attivaLinks(boolean attiva){
        if (attiva) {
            nLinkVersoSite.setText("Numero links: ");
            enableNode(linkVersoSite);
        }
        else {
            nLinkVersoSite.setText("Numero links: (apri un'altra esplorazione per attivare)");
            disableNode(linkVersoSite);
        }
    }

}
