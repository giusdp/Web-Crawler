package wsa.web.objects;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import wsa.gui.Page;
import wsa.web.CrawlerResult;
import wsa.web.SiteCrawler;
import wsa.web.TabCrawler;
import wsa.web.WebFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Un oggetto per la GUI JavaFX che utilizza un crawler specializzato {@link SiteCrawler}
 * per ricavarne oggetti utili alla creazione di dati e statistiche da mostrare per il sito aperto */
public class TabCrawlerObject implements TabCrawler{

    private URI dom;
    private Path dir;
    private AtomicReference<SiteCrawler> siteCrawler;
    private AtomicReference<ObservableList<Page>> listone;
    private AtomicReference<Set<URI>> seeds;
    private AtomicReference<Map<URI, ObservableList<URI>>> uriPuntanti;
    private final ExecutorService exec;

    private List<CrawlerResult> crawlerResultList;

    /** COSTRUTTORE */
    public TabCrawlerObject() {
        dom = null;
        dir = null;

        crawlerResultList = new ArrayList<>();

        listone = new AtomicReference<>(FXCollections.observableArrayList());
        exec = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        siteCrawler = new AtomicReference<>(null);
        seeds = new AtomicReference<>(new HashSet<>());
        uriPuntanti = new AtomicReference<>(new HashMap<>());
    }

    /** Crea il {@link SiteCrawler} usato dalla pagina della GUI del relativo sito.
     * Il SiteCrawler è creato utilizzando dominio e directory impostati dai metodi
     * {@link TabCrawler#setDom(URI)} e {@link TabCrawler#setDir(Path)}
     * @throws IOException se accade un errore nella creazione del SiteCrawler
     * @throws IllegalArgumentException se dominio e directory usate per la creazione del SiteCrawler
     * non sono corrette */
    @Override
    public void createSiteCrawler() throws IOException, IllegalArgumentException {
        siteCrawler.set(WebFactory.getSiteCrawler(dom, dir));
        seeds.get().stream().forEach(uri -> {
            try {
                addSeed(uri);
            } catch (IllegalArgumentException e) {
                System.out.println("Questo: " + uri + " non va bene");
            }
        });
        seeds.set(new HashSet<>());
    }

    /** Inizia l'esecuzione del crawler e si occupa di creare oggetti {@link Page}
     * che verranno poi passati alla visualizzazione in JavaFX.
     * Se il crawler utilizzato è già in funzione, l'invocazione è gnorata.
     * {@code true} avvia l'esecuzione del SiteCrawler
     * {@code false} carica l'archivio senza eseguire il crawler
     * @param avvia avvia o carica il SiteCrawler */
    @Override
    public void start(boolean avvia){
        if (siteCrawler.get() == null || siteCrawler.get().isRunning()) return;

        if (avvia) {
            siteCrawler.get().start();
            // prende i crawler result
            exec.submit(() -> {
                while (siteCrawler.get().isRunning()) {
                    Optional<CrawlerResult> davideOptional = siteCrawler.get().get();
                    if (davideOptional.isPresent()) {
                        CrawlerResult result = davideOptional.get();
                        if (result.uri != null) {
                            calcolaPuntanti(result);
                            Platform.runLater(() -> listone.get().add(createPage(result)));
                        } else {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        } else {
            exec.submit(() -> {
                try {
                    getSiteCrawler().getLoaded().stream().forEach(uri -> crawlerResultList.add(getSiteCrawler().get(uri)));
                    getSiteCrawler().getErrors().stream().forEach(uri -> crawlerResultList.add(getSiteCrawler().get(uri)));
                    crawlerResultList.stream().filter(result -> result.uri != null).forEach(result -> {
                            calcolaPuntanti(result);
                            listone.get().add(createPage(result));
                        });
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
        }
    }

    /** Crea e ritorna un {@link Page} per visualizzare varie informazioni
     * ricavate dal {@link CrawlerResult} specificato.
     * @param result il risultato dello scaricamento di una pagina da cui ricavare Page
     * @return Page ricavato dal CrawlerResult */
    public Page createPage(CrawlerResult result) {
        return new Page(
                result,
                uriPuntanti.get().get(result.uri),
                SiteCrawler.checkSeed(dom, result.uri)
        );
    }


    /** Aggiunge un URI seed da scaricare al {@link SiteCrawler} se già è stato creato
     * altrimenti verrà aggiunto in seguito quando il SiteCrawler sarï¿½ disponibile.
     * @param seed da scaricare */
    @Override
    public void addSeed(URI seed) {
        if (siteCrawler.get() == null) seeds.get().add(seed);
        else siteCrawler.get().addSeed(seed);
    }

    /** Cancella i seed aggiunti finora e non ancora passati al crawler {@link SiteCrawler} */
    @Override
    public void cancelSeeds() {
        seeds.get().clear();
        seeds.set(new HashSet<>());
    }

    /** Ritorna il {@link SiteCrawler} utilizzato, possibilmente null nel caso in cui
     * questo metodo viene invocato prima di averne creato uno con {@link TabCrawler#createSiteCrawler()}
     * @return SiteCrawler */
    @Override
    public SiteCrawler getSiteCrawler() {
        return siteCrawler.get();
    }

    /** Ritorna una {@link ObservableList<Page>} possibilmente vuota, delle pagine scaricate
     * che la GUI userà per visualizzare i vari dati.
     * @return ObservableList utile a JavaFx */
    @Override
    public ObservableList<Page> getObservableList() {
        return listone.get();
    }

    /** Imposta il dominio che verrà utilizzato al momento della creazione del {@link SiteCrawler}
     * L'invocazione è ignorata se il SiteCrawler è già stato creato.
     * @param dom dominio utilizzato dal SiteCrawler */
    @Override
    public void setDom(URI dom) {
        this.dom = dom;
    }

    /** Metodo di utilità che ritorna il dominio utilizzato dal {@link SiteCrawler}
     * @return URI dominio del SiteCrawler */
    @Override
    public URI getDom() { return dom; }

    /** Imposta la directory che verrà utilizzata al momento della creazione del {@link SiteCrawler}
     * L'invocazione è ignorata se il SiteCrawler è già stato creato.
     * @param dir directory utilizzata dal SiteCrawler */
    @Override
    public void setDir(Path dir) {
        this.dir = dir;
    }

    /** Metodo di utilità che ritorna la directory utilizzata dal {@link SiteCrawler}
     * @return Path directory del SiteCrawler */
    @Override
    public Path getDir() { return dir; }

    /** Cancella il {@link SiteCrawler} per sempre */
    @Override
    public void cancelCrawler() {
        exec.shutdown();
        if (siteCrawler.get() != null && !siteCrawler.get().isCancelled()) siteCrawler.get().cancel();
        siteCrawler.set(null);
        siteCrawler = null;
    }

    /** Ritorna l'insieme degli uri seed ancora non aggiunti al {@link SiteCrawler}
     * possibilmente vuoto
     * @return Insieme di URI */
    @Override
    public Set<URI> getSeeds(){
        return seeds.get();
    }


    /** Calcola i link puntanti per ogni page nella ObservableList<Page>, cioè per ogni pagina scaricata
     * aggiorna il numero di link che puntano ad essa. Se la pagina non è stata ancora visitata allora
     * viene aggiunto l'uri alla mappa che porta il conto per ogni uri, del numero di volte puntato.
     * Vengono poi controllati ogni link per vedere a quale uri puntano, così da aumentare
     * il valore associato.
     * @param result risultato di una pagina scaricata usato per calcolare il numero di puntanti di ogni pagina*/
    private void calcolaPuntanti(CrawlerResult result){
        try {
            if (!uriPuntanti.get().containsKey(result.uri)) uriPuntanti.get().put(result.uri, FXCollections.observableArrayList());

            if (result.links != null) {
                result.links.stream().forEach(link -> {
                    ObservableList<URI> list = FXCollections.observableArrayList();
                    if (uriPuntanti.get().containsKey(link)) {
                        list = uriPuntanti.get().get(link);
                        list.add(result.uri);
                        uriPuntanti.get().replace(link, list);
                    } else {
                        list.add(result.uri);
                        uriPuntanti.get().put(link, list);
                    }
                });
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
