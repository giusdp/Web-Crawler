package wsa.web.objects;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import org.w3c.dom.Document;
import wsa.web.LoadResult;
import wsa.web.Loader;
import wsa.web.html.Parsed;
import wsa.web.html.ParsingTree;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Created by Giuseppe on 5/21/2015. */

/** Oggetto Loader concreto */
public class LoaderObject implements Loader {

    private AtomicReference<WebEngine> webEngine;  //web engine atomica per il download delle pagine
    private AtomicReference<Parsed> tree; // albero di parsing atomico per avere i dati delle pagine scaricate
    private AtomicBoolean isFinished; // booleano atomico per controllare se il download della pagina è completato


    /** COSTRUTTORE */
    public LoaderObject(){
        tree = new AtomicReference<>();
        webEngine = new AtomicReference<>();
        isFinished = new AtomicBoolean();
    }

    /** Inizializzazione della {@link WebEngine}, viene usato da {@link LoaderObject#load(URL)}
     *  Crea e imposta la WebEngine nel Thread JavaFX la prima volta che viene chiamato.
     *  controlla prima se la WebEngine è null, in questo caso ne crea una nuova (la prima volta), e imposta un listener sullo stato
     *  del Worker. Quando la WebEngine carica una pagina il listener controlla il Worker, se il download è andato a buon fine
     *  viene preso il {@link Document} per poi creare l'albero di parsing, infine viene fatta scaricare la pagina dell' URL passato.
     *  Nel caso in cui la WebEngine fosse già stata craeta (tutte le altre volte), viene semplicemente fatta scaricare
     *  la pagina dell'URL passato.
     *  @param url URL della pagina da scaricare. */
    private void useWEngine(URL url)
    {
        Platform.runLater(() -> {
            if (webEngine.get() == null) {
                webEngine.set(new WebEngine()); // creazione della WebEngine, avviene solo una volta

                webEngine.get().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue == Worker.State.SUCCEEDED || newValue == Worker.State.CANCELLED || newValue == Worker.State.FAILED) {
                        if (newValue == Worker.State.SUCCEEDED) {   // se la WebEngine ha scaricato
                            Document doc = webEngine.get().getDocument(); // prende il document dalla pagina appena scaricata e
                            if (doc != null)            // viene controllato se esiste, nel caso viene creato l'albero altrimenti ritorna
                                try {
                                    tree.set(new ParsingTree(doc));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            else return;
                        } else
                            tree.set(null); // se la webEngine non è riuscita a scaricare la pagina, l'albero è impostato come null

                        isFinished.set(true);  //in qualsiasi caso ha completato il lavoro e
                    }                          // può il main Thread puòò uscire dall'attesa nel metodo load
                });
                webEngine.get().load(url.toString());
                //Altrimenti carica normalmente, usando poi il listener già collegato all webEngine
            } else {
                //webEngine.get().load("");
                webEngine.get().load(url.toString());
            }
        });
    }

    /** Ritorna il risultato del tentativo di scaricare la pagina specificata.
     * Nel Thread JavaFX viene iniziato il download della pagina e si mette in attesa il Main Thread, una volta completato il download
     * si prende il risultato dell'albero di parsing (che può essere giusto, null oppure è stata lanciata un eccezione)
     * e si ritorna un {@link LoadResult} creato a seconda dei casi.
     * @param url  l'URL di una pagina web
     * @return il risultato del tentativo di scaricare la pagina */
    @Override
    public LoadResult load(URL url) {
        isFinished.set( false );
        try {
            // Thread JavaFX INIZIATO
            useWEngine(url);
            // Thread JavaFX RITORNATO

            while (!isFinished.get()) { Thread.sleep(50); }

            if (tree.get() != null)
                return new LoadResult(url, tree.get(), null);
            else
                return new LoadResult(url, null, new Exception("Creazione albero fallita!"));

        } catch (Exception ex) {
            ex = new Exception("Download fallito!");
            return new LoadResult(url, null, ex);
        }
    }

    /** Viene fatto un controllo sull' {@link URL} creando una connessione alla pagina con un limite massimo
     * di tempo consentito, se non ci sono problemi a connettersi ritorna null, altrimenti l'url crea problemi e si
     * ritorna l'eccezione generata. Con questo metodo si può controllare quale url è buono e quale no.
     * @param url  un URL
     * @return null se l'URL è scaricabile senza errori, altrimenti
     * l'eccezione */
    @Override
    public Exception check(URL url) {
        try {

            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept", "text/html;q=1.0,*;q=0");
            connection.setRequestProperty("Accept-Encoding", "identity;q=1.0,*;q=0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.connect();

            return null;

        } catch (Exception ex) {
            return ex;
        }
    }
}
