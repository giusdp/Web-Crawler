package wsa.web;

import javafx.collections.ObservableList;
import wsa.gui.Page;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;


/** Un oggetto per la GUI JavaFX che utilizza un crawler specializzato {@link SiteCrawler}
 * per ricavarne oggetti utili alla creazione di dati e statistiche da mostrare per il sito aperto */
public interface TabCrawler {

    /** Ritorna una {@link ObservableList<Page>} possibilmente vuota, delle pagine scaricate
     * che la GUI userà per visualizzare i vari dati.
     * @return ObservableList utile a JavaFx */
    ObservableList<Page> getObservableList();

    /** Inizia l'esecuzione del crawler e si occupa di creare oggetti {@link Page}
     * che verranno poi passati alla visualizzazione in JavaFX.
     * Se il crawler utilizzato è già in funzione, l'invocazione ï¿½ ignorata.
     * {@code true} avvia l'esecuzione del SiteCrawler
     * {@code false} carica l'archivio senza eseguire il crawler
     * @param avvia avvia o carica il SiteCrawler */
    void start(boolean avvia);

    /** Crea il {@link SiteCrawler} usato dalla pagina della GUI del relativo sito.
     * Il SiteCrawler è creato utilizzando dominio e directory impostati dai metodi
     * {@link TabCrawler#setDom(URI)} e {@link TabCrawler#setDir(Path)}
     * @throws IOException se accade un errore nella creazione del SiteCrawler
     * @throws IllegalArgumentException se dominio e directory usate per la creazione del SiteCrawler
     * non sono corrette */
    void createSiteCrawler() throws IOException, IllegalArgumentException;

    /** Ritorna il {@link SiteCrawler} utilizzato, possibilmente null nel caso in cui
     * questo metodo viene invocato prima di averne creato uno con {@link TabCrawler#createSiteCrawler()}
     * @return SiteCrawler */
    SiteCrawler getSiteCrawler();

    /** Imposta il dominio che verrà utilizzato al momento della creazione del {@link SiteCrawler}
     * L'invocazione è ignorata se il SiteCrawler è già stato creato.
     * @param dom dominio utilizzato dal SiteCrawler */
    void setDom(URI dom);

    /** Metodo di utilità che ritorna il dominio utilizzato dal {@link SiteCrawler}
     * @return URI dominio del SiteCrawler */
    URI getDom();

    /** Imposta la directory che verrà utilizzata al momento della creazione del {@link SiteCrawler}
     * L'invocazione è ignorata se il SiteCrawler è già stato creato.
     * @param dir directory utilizzata dal SiteCrawler */
    void setDir(Path dir);

    /** Metodo di utilità che ritorna la directory utilizzata dal {@link SiteCrawler}
     * @return Path directory del SiteCrawler */
    Path getDir();

    /** Aggiunge un URI seed da scaricare al {@link SiteCrawler} se già è stato creato
     * altrimenti verrà aggiunto in seguito quando il SiteCrawler sarï¿½ disponibile.
     * @param seed da scaricare */
    void addSeed(URI seed);

    /** Ritorna l'insieme degli uri seed ancora non aggiunti al {@link SiteCrawler}
     * possibilmente vuoto
     * @return Insieme di URI */
    Set<URI> getSeeds();

    /** Cancella i seed aggiunti finora e non ancora passati al crawler {@link SiteCrawler} */
    void cancelSeeds();

    /** Cancella il {@link SiteCrawler} per sempre */
    void cancelCrawler();

}
