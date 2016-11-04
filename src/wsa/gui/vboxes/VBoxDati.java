package wsa.gui.vboxes;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import wsa.gui.*;
import wsa.web.TabCrawler;

import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Classe che rappresenta la schermata che viene visualizzata se viene premuto
 * il pulsante {@link VBoxPreStart#visualDati}.
 * Permette di visualizzare informazioni relative a un esplorazione caricata.
 * Un oggetto di questa classe ha sempre associato il medesimo VBox {@link VBoxDati#vBox}, contenente
 * gli stessi nodi */
public class VBoxDati extends VBoxes {

    private TableView<Page> table;
    private Button altreInfo, mostraGrafici, indietro;
    private Text nMassimoLinkInterni, nMassimoLinkPuntanti, nURInterni, uriVisitati, nURIErrati;
    private ExecutorService esecutore;
    private ObservableList<URI> interni;
    private Stage tableInfoStage1, tableInfoStage2, stats;
    private int maxLinkInterni, maxLinkPuntanti;
    private Future future;
    private VBoxDistanze vBoxDistanze;
    private Grafici grafici;

    /** Costruttore. Crea tutti i Node di {@link VBoxDati#vBox}.
     * @param tabCrawler un TabCrawler che viene passato al costruttore di {@link VBoxes} */
    public VBoxDati(TabCrawler tabCrawler) {
        super(tabCrawler);
        dom = tabCrawler.getDom();
        esecutore = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        tableInfoStage1 = new Stage();
        tableInfoStage2 = new Stage();

        createVBox();
        vBoxDistanze = new VBoxDistanze(tabCrawler);
    }

    @Override
    protected void createVBox() {

        Tooltip aiutoAltreInfo = new Tooltip("Permette di ottenere altre informazioni \nsui dati dell'esplorazione \n(n. nodi, n. immagini, distanze).");
        Tooltip aiutoMostraGrafici = new Tooltip("Mostra istogrammi relativi ai link uscenti e a quelli entranti");
        Tooltip aiutoIndietro = new Tooltip("Torna alla schermata iniziale\n(potrai riprendere l'esplorazione,\ncaricare un nuovo archivio o inserire nuovi dati).");


        indietro = new Button("Indietro");
        Tooltip.install(indietro, aiutoIndietro);
        altreInfo = new Button("Altre Informazioni");
        Tooltip.install(altreInfo, aiutoAltreInfo);
        mostraGrafici = new Button("Grafici");
        Tooltip.install(mostraGrafici, aiutoMostraGrafici);
        table = TableFactory.getTable(false, tabCrawler.getObservableList(), tableInfoStage1, tableInfoStage2);
        tableInfoStage2.setTitle("Link Puntanti");
        table.setMinHeight(375);

        uriVisitati = new Text();
        nURIErrati = new Text();
        nURInterni = new Text();
        nMassimoLinkInterni = new Text();
        nMassimoLinkPuntanti = new Text();


        HBox hBox = new HBox(20, uriVisitati, nURInterni, nURIErrati);
        HBox hBox2 = new HBox(20, nMassimoLinkInterni, nMassimoLinkPuntanti);
        VBox vBoxInfo = new VBox(10, hBox, hBox2);

        HBox hBoxStatistiche = new HBox(10, mostraGrafici, altreInfo);

        hBox.setAlignment(Pos.CENTER);
        hBox2.setAlignment(Pos.CENTER);
        vBoxInfo.setAlignment(Pos.CENTER);
        hBoxStatistiche.setAlignment(Pos.CENTER);
        HBox hBoxone = new HBox(40, indietro, vBoxInfo, hBoxStatistiche);
        hBoxone.setAlignment(Pos.CENTER);

        Label nota = new Label("    Nota: le celle piu' scure della tabella sono cliccabili.");
        nota.setFont(Font.font("Calibri", FontWeight.BOLD, 12));
        HBox hBoxNota = new HBox(nota);
        hBoxNota.setAlignment(Pos.BOTTOM_LEFT);

        vBox.setSpacing(22);
        vBox.setFillWidth(true);
        vBox.getChildren().addAll(table, hBoxone, hBoxNota);


        calcolaStatistiche();
    }

    @Override
    public void changeTabCrawler(TabCrawler tabCrawler) {
        this.tabCrawler = tabCrawler;
        esecutore = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        dom = loadDomFromArchive(Paths.get(tabCrawler.getDir().toString() + "/Dominio.ser"));
        vBoxDistanze.changeTabCrawler(tabCrawler);
        calcolaStatistiche();
    }

    /** Setta l'evento associato al bottone {@link VBoxDati#indietro}
     *  Chiude tutte le finestre aperte dai pulsanti in {@link VBoxDati}
     *  @param event l'evento che si verifica al click sul bottone, dopo le operazioni
     *  iniziali specificate */
    public void setIndietroAction(EventHandler<ActionEvent> event){
        indietro.setOnAction(e -> {
            esecutore.shutdown();
            Main.browser.close();
            tableInfoStage1.close();
            tableInfoStage2.close();
            stats.close();
            if (grafici != null) grafici.closeGraphs();
            event.handle(e);
        });
    }

    /** Calcola tutte i dati relativi all'esplorazione caricata. Alcune operazioni sono svolte
     * in un thread a parte per dare il tempo a {@link VBoxDati#tabCrawler} di prendere tutti
     * i CrawlerResult e creare le Page corrispondenti, con cui saranno calcolate statistiche
     * quali il massimo numero di link interni e puntanti */
    private void calcolaStatistiche(){
        table.setItems(tabCrawler.getObservableList());
        uriVisitati.setText("Pagine visitate: " + tabCrawler.getSiteCrawler().getLoaded().size());
        nURIErrati.setText("URI con errori: " + tabCrawler.getSiteCrawler().getErrors().size());

        future = esecutore.submit(() -> {
            try {
                Thread.sleep(500);
                maxLinkInterni = 0;
                maxLinkPuntanti = 0;
                interni = Utils.calcolaInsiemeInterni(dom, tabCrawler.getSiteCrawler().getLoaded());

                vBoxDistanze.preparaDistanze(interni); //prepara il vboxDistanze

                tabCrawler.getObservableList().stream().forEach(page ->
                        maxLinkInterni = Utils.aggiornaNumeroMaxLinkInterni(page, maxLinkInterni));
                tabCrawler.getObservableList().stream().forEach(page ->
                        maxLinkPuntanti = Utils.aggiornaNumeroMaxLinkPuntanti(page, maxLinkPuntanti));
                nURInterni.setText("URI interni al dominio: " + interni.size());
                nMassimoLinkInterni.setText("Massimo numero link interni: " + maxLinkInterni);
                nMassimoLinkPuntanti.setText("Massimo numero link puntanti: " + maxLinkPuntanti);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Platform.runLater(() -> {
            while (!future.isDone()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            stats = new Stage();
            stats.setTitle("Statistiche Extra");
            stats.setScene(vBoxDistanze.scene);
            stats.setOnCloseRequest(event -> vBoxDistanze.stop());

            grafici = new Grafici(tabCrawler);

            setActions();
        });
    }

    /** Setta gli eventi associati al bottone per visualizzare statistiche sotto forma di grafici
     * e il bottone per visualizzare {@link wsa.gui.vboxes.VBoxDistanze#vBox} */
    @Override
    protected void setActions(){
        mostraGrafici.setOnAction(event -> grafici.showGraphs());
        altreInfo.setOnAction(event -> stats.show());
    }
}
