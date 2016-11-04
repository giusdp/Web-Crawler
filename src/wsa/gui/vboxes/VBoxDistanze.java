package wsa.gui.vboxes;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import wsa.gui.Utils;
import wsa.web.LoadResult;
import wsa.web.Loader;
import wsa.web.TabCrawler;
import wsa.web.WebFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Classe che rappresenta la schermata che viene visualizzata se viene premuto il pulsante {@link VBoxDati#altreInfo}.
 * Permette di visualizzare informazioni sulle pagine scaricate.
 * Un oggetto di questa classe ha sempre associato il medesimo VBox {@link VBoxPostStart#vBox}, contenente
 * gli stessi nodi */
public class VBoxDistanze extends VBoxes {

    private Button findDistance, findMaxDistance, stop, cancellaURI, calcolaNImg, calcolaNodi;
    private Text numImg, numNodi, distanza2URI, distanzaMaxURI, uri1String, uri2String;
    private TextField chooseURI;
    private ListView<URI> listView;
    private URI uri1, uri2;
    private ObservableList<URI> interni;
    private ExecutorService esecutore;
    private Future futureDistanza, futureDistMax;
    private Loader loader;
    private List<LoadResult> pagineCalcolate;
    private int numeroNodi;
    private boolean maxDistUsed = false;

    public Scene scene;

    /** Costruttore.
     * @param tabCrawler un TabCrawler che viene passato al costruttore di {@link VBoxes} */
    public VBoxDistanze(TabCrawler tabCrawler) {
        super(tabCrawler);
        dom = tabCrawler.getDom();

        loader = WebFactory.getLoader();
        pagineCalcolate = new ArrayList<>();

        scene = new Scene(vBox, 800, 500);
    }

    /** Crea un ExecutorService che viene utilizzato per le operazioni che richiedono più tempo,
     * che si svolgono in threads apparte per mantenere l'interfaccia reattiva.
     * Inoltre crea tutti i Node di {@link VBoxDistanze#vBox} e ne imposta le azioni */
    public void preparaDistanze(ObservableList<URI> interni){
        this.interni = interni;
        esecutore = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        createVBox();
        setActions();
    }

    /** Ferma le operazioni del calcolo della distanza di due URI o il calcolo della distanza massima fra tutte
     * le coppie di URI. In più chiude {@link VBoxDistanze#esecutore} */
    public void stop(){
        stop.fire();
        esecutore.shutdown();
    }


    @Override
    protected void createVBox() {

        Tooltip aiutoFindDistance = new Tooltip("Calcola la distanza tra due URI interni");
        Tooltip aiutoFindMaxDistance = new Tooltip("Calcola la massima distanza tra tutte le coppie di URI interni.");
        Tooltip aiutoStop = new Tooltip("Annulla il calcolo della distanza.");
        Tooltip aiutoCancellaURI = new Tooltip("Cancella dati immessi.");
        Tooltip aiutoCalcolaNImg = new Tooltip("Calcola numero di immagini.");
        Tooltip aiutroCalcolaNodi = new Tooltip("Calcola numero di nodi.");

        calcolaNImg = new Button("Numero Immagini");
        Tooltip.install(calcolaNImg, aiutoCalcolaNImg);
        calcolaNodi = new Button("Numero Nodi");
        Tooltip.install(calcolaNodi, aiutroCalcolaNodi);
        findDistance = new Button("Distanza");
        Tooltip.install(findDistance, aiutoFindDistance);
        findMaxDistance = new Button("Distanza Massima");
        Tooltip.install(findMaxDistance, aiutoFindMaxDistance);
        stop = new Button("Stop");
        Tooltip.install(stop, aiutoStop);
        cancellaURI = new Button("Cancella");
        Tooltip.install(cancellaURI, aiutoCancellaURI);
        disableNode(calcolaNImg);
        disableNode(calcolaNodi);
        disableNode(findDistance);
        disableNode(stop);
        disableNode(cancellaURI);
        HBox hBoxDistanze = new HBox(10, calcolaNImg, calcolaNodi, findDistance, findMaxDistance, stop, cancellaURI);

        numImg = new Text("Numero immagini: ");
        numNodi = new Text("Numero nodi: ");
        distanza2URI = new Text("Distanza tra due uri: ");
        distanzaMaxURI = new Text("Massima distanza: ");
        HBox hBox = new HBox(20, numImg, numNodi, distanza2URI, distanzaMaxURI);

        chooseURI = new TextField();
        chooseURI.setPromptText("Inserisci un indirizzo o scegli dalla lista. (Invio per aggiungere)");
        chooseURI.setMinWidth(400);
        chooseURI.setMaxWidth(500);


        Text uri1Text = new Text("Primo URI: ");
        Text uri2Text = new Text("Secondo URI: ");
        uri1String = new Text();
        uri2String = new Text();
        VBox vBoxURInfo = new VBox(3, uri1Text, uri1String, uri2Text, uri2String);

        listView = new ListView<>(interni);

        vBoxURInfo.setAlignment(Pos.CENTER);
        hBoxDistanze.setAlignment(Pos.CENTER);
        hBox.setAlignment(Pos.CENTER);

        vBox.setSpacing(10);
        vBox.getChildren().clear();
        vBox.getChildren().addAll(vBoxURInfo, hBoxDistanze, hBox, chooseURI, listView);
    }


    @Override
    protected void setActions() {

        //Trova la distanza fra due coppie di URI dell'esplorazione e la visualizza.
        //Abilita il bottone per fermare l'operazione e si disabilita, in modo che non
        //possa essere cliccato mentre effettua il calcolo.
        //L'operazione è effettuata in un thread apparte per mantenere
        //l'interfaccia reattiva
        findDistance.setOnAction(event -> {
            futureDistanza = esecutore.submit(() -> {
                int d = Utils.DistanzaURI(uri1, uri2, tabCrawler.getObservableList());
                distanza2URI.setText("Distanza tra questi due uri: " + (d != -1 ? d : "non collegati"));
                if (futureDistMax == null || futureDistMax.isDone()) disableNode(stop);
                enableNode(findDistance);
            });
            enableNode(stop);
            disableNode(findDistance);
        });

        //Trova la distanza massima fra tutte le coppie di URI dell'esplorazione e la visualizza.
        //Abilita il pulsante per fermare l'operazione e si disabilita, in modo che non
        //possa essere cliccato mentre effettua il calcolo.
        //L'operazione è effettuata in un thread apparte per mantenere
        //l'interfaccia reattiva
        findMaxDistance.setOnAction(event -> {
            futureDistMax = esecutore.submit(() -> {
                int d = Utils.DistanzaMaxURI(interni, tabCrawler.getObservableList());
                distanzaMaxURI.setText("Distanza massima: " + (d != -1 ? d : "incalcolabile"));
                if (futureDistanza == null || futureDistanza.isDone()) disableNode(stop);
                if (uri1 != null && uri2 != null) enableNode(findDistance);
                maxDistUsed = true;
            });
            disableNode(findMaxDistance);
            enableNode(stop);
        });

        //Ferma le operazioni del calcolo della distanza di due URI o il calcolo della distanza massima fra tutte
        //le coppie di URI.
        stop.setOnAction(event -> {
            if (futureDistanza != null) futureDistanza.cancel(true);
            if (futureDistMax != null) futureDistMax.cancel(true);
            disableNode(stop);
            if (!maxDistUsed) enableNode(findMaxDistance);
        });

        //chiama setURIs(URI uri) sull'URI selezionato dalla lista.
        listView.setOnMouseClicked(event -> {
            URI uri = listView.getSelectionModel().getSelectedItem();
            if (uri == null) return;
            setURIs(uri);
        });

        //Cancella gli URI selezionati e cancella le informazioni ad essi relative.
        //Inoltre disabilita i bottoni per ricavare informazioni dagli URI immessi (dato che non ci sono più).
        cancellaURI.setOnAction(event -> {
            uri1 = null;
            uri2 = null;
            uri1String.setText("");
            uri2String.setText("");
            numImg.setText("Numero immagini: ");
            numNodi.setText("Numero nodi: ");
            distanza2URI.setText("Distanza tra due uri:");
            disableNode(calcolaNImg);
            disableNode(calcolaNodi);
            disableNode(findDistance);
            disableNode(cancellaURI);
        });

        //Setta un URI dalla TextField invece che dalla lista
        chooseURI.setOnAction(event -> {
            try {
                URI uri = new URI(chooseURI.getText());
                if (!tabCrawler.getSiteCrawler().getLoaded().contains(uri)) throw new IllegalArgumentException();
                setURIs(uri);
                chooseURI.clear();
            } catch (Exception e) {
                chooseURI.clear();
                chooseURI.setText("URI ERRATO!");
            }
        });

        //Calcola il numero di immagini all'interno della pagina puntata dall'URI immesso dall'utente.
        //Il calcolo avviene in un thread apparte, per mantenere l'interfaccia reattiva durante
        //l'operazione
        calcolaNImg.setOnAction(event ->
                esecutore.submit(() -> {
                    LoadResult loadResult = getLoadResult();
                    numImg.setText("Numero immagini: " + loadResult.parsed.getByTag("img").size());
                }));

        //Calcola il numero di nodi dell'albero di parsing della pagina puntata dall'URI immesso dall'utente.
        //Il calcolo avviene in un thread apparte, per mantenere l'interfaccia reattiva durante
        //l'operazione.
        calcolaNodi.setOnAction(event ->
                esecutore.submit(() -> {
                    getLoadResult().parsed.visit(node -> numeroNodi++);
                    numNodi.setText("Numero nodi: " + numeroNodi);
                }));
    }

    @Override
    public void changeTabCrawler(TabCrawler tabCrawler) {
    }

    /** @param uri Setta l'URI su cui verranno effettuate le operazioni, e abilita i
     * bottoni per queste ultime. */
    private void setURIs(URI uri){
        if (uri1 == null) {
            uri1 = uri;
            uri1String.setText(uri1.toString());
            enableNode(calcolaNImg);
            enableNode(calcolaNodi);
            enableNode(cancellaURI);
        } else if (uri2 == null) {
            uri2 = uri;
            uri2String.setText(uri2.toString());
            enableNode(findDistance);
            disableNode(calcolaNImg);
            disableNode(calcolaNodi);
        }
    }

    /** @return il LoadResult associato all'URI immesso dall'utente */
    private LoadResult getLoadResult(){
        try {
            for (LoadResult res : pagineCalcolate)
                if (res.url.equals(uri1.toURL())) return res;
            LoadResult loadResult = loader.load(uri1.toURL());
            pagineCalcolate.add(loadResult);
            return loadResult;
        }catch (Exception e){
            e.printStackTrace();
        } return new LoadResult(null, null, null);
    }
}
