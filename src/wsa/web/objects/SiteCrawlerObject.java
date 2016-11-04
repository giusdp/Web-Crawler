package wsa.web.objects;

import wsa.web.*;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;


/** Created by Giuseppe on 30/05/2015. */

/** Un crawler specializzato per siti web. Se il SiteCrawler è stato creato con una
 * directory per l'archiviazione, allora ogni 30 secondi durante l'esplorazione
 * l'archivio deve essere aggiornato in modo tale che se per qualsiasi motivo
 * l'esplorazione si interrompe, l'esplorazione può essere ripresa senza perdere
 * troppo lavoro. Inoltre l'archivio deve essere aggiornato in caso di sospensione
 * (metodo {@link SiteCrawler#suspend()}) e quando l'esplorazione termina normalmente. */
public class SiteCrawlerObject implements SiteCrawler {

    private URI dom;
    private Path dir;
    private boolean running, cancelled, canArchive;
    private Set<URI> loaded, toLoad, errs;
    private Crawler crawler;
    private Path pathToArchive;
    private final ExecutorService exec;
    private AtomicReference<List<CrawlerResult>> cResults;
    private int index;

    /** COSTRUTTORE */
    public SiteCrawlerObject(URI dom, Path dir) {
        this.dom = dom;
        this.dir = dir;

        running = false;
        cancelled = false;
        loaded = new HashSet<>();
        toLoad = new HashSet<>();
        errs = new HashSet<>();
        cResults = new AtomicReference<>(new ArrayList<>());
        exec = Executors.newFixedThreadPool(2, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(false);
            return thread;
        });

        canArchive = false;

        if (this.dir != null) {
            pathToArchive = Paths.get(dir.toString() + "/Crawler_Archive.ser");
            canArchive = true;
            if (dom == null) loadArchive();
        }

        Predicate<URI> pageLink = uri -> SiteCrawler.checkSeed(dom, uri);
        crawler = WebFactory.getCrawler(loaded, toLoad, errs, pageLink);
    }

    /** Aggiunge un seed URI. Se però è presente tra quelli già scaricati,
     * quelli ancora da scaricare o quelli che sono andati in errore,
     * l'aggiunta non ha nessun effetto. Se invece è un nuovo URI, è aggiunto
     * all'insieme di quelli da scaricare.
     * @throws IllegalArgumentException se uri non appartiene al dominio di
     * questo SiteCrawler
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @param uri  un URI */
    @Override
    public void addSeed(URI uri) {
        except();
        if ( ! SiteCrawler.checkSeed(dom, uri))
            throw new IllegalArgumentException();
        crawler.add(uri);
    }

    /** Inizia l'esecuzione del SiteCrawler se non è già in esecuzione e ci sono
     * URI da scaricare, altrimenti l'invocazione è ignorata. Quando è in
     * esecuzione il metodo isRunning ritorna true.
     * @throws IllegalStateException se il SiteCrawler è cancellato */
    @Override
    public void start() {
        except();

        if (isRunning()) return;

        running = true;

        crawler.start();

        exec.submit(() -> {
            while (isRunning()) {
                if (dom != null) {
                    Optional<CrawlerResult> result = crawler.get();
                    if (result.isPresent()) {
                        CrawlerResult crawlerResult = result.get();
                        if (crawlerResult.uri != null)
                            cResults.get().add(crawlerResult);
                        else try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        exec.submit(() -> {
            while (isRunning() && canArchive) {
                try {
                    Thread.sleep(30000);
                    saveArchive();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    /** Sospende l'esecuzione del SiteCrawler. Se non è in esecuzione, ignora
     * l'invocazione. L'esecuzione può essere ripresa invocando start. Durante
     * la sospensione l'attività dovrebbe essere ridotta al minimo possibile
     * (eventuali thread dovrebbero essere terminati). Se è stata specificata
     * una directory per l'archiviazione, lo stato del crawling è archiviato.
     * @throws IllegalStateException se il SiteCrawler è cancellato */
    @Override
    public void suspend() {
        except();
        if (!isRunning()) return;
        running = false;
        if (dir != null) saveArchive();
        crawler.suspend();
    }

    /** Cancella il SiteCrawler per sempre. Dopo questa invocazione il
     * SiteCrawler non può più essere usato. Tutte le risorse sono
     * rilasciate. */
    @Override
    public void cancel() {
        if (isCancelled()) return;
        suspend();
        crawler.cancel();
        exec.shutdown();
        cancelled = true;

        toLoad.clear();
        loaded.clear();
        errs.clear();
        cResults.get().clear();

        crawler = null;
        dom = null;
        dir = null;
        pathToArchive = null;
        cResults.set(null);
        cResults = null;
        toLoad = null;
        loaded = null;
        errs = null;
    }

    /** Ritorna il risultato relativo al prossimo URI. Se il SiteCrawler non è
     * in esecuzione, ritorna un Optional vuoto. Non è bloccante, ritorna
     * immediatamente anche se il prossimo risultato non è ancora pronto.
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @return  il risultato relativo al prossimo URI scaricato */
    @Override
    public Optional<CrawlerResult> get() {
        except();
        if (!isRunning())
            return Optional.empty();
        Optional<CrawlerResult> optional = Optional.of(new CrawlerResult(null, false, null, null, null));
        if (index < cResults.get().size()){
            optional = Optional.of(cResults.get().get(index));
            index++;
        }
        return optional;
    }

    /** Ritorna il risultato del tentativo di scaricare la pagina che
     * corrisponde all'URI dato.
     * @param uri  un URI
     * @throws IllegalArgumentException se uri non è nell'insieme degli URI
     * scaricati né nell'insieme degli URI che hanno prodotto errori.
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @return il risultato del tentativo di scaricare la pagina */
    @Override
    public CrawlerResult get(URI uri) {
        except();
        if ( !getLoaded().contains(uri) && !getErrors().contains(uri) ) throw new IllegalArgumentException();
        CrawlerResult crawlerResult = new CrawlerResult(null, false, null, null, new Exception("Risultato non trovato"));
        for (CrawlerResult result : cResults.get())
            if (result.uri.equals(uri))
                crawlerResult = result;
        return crawlerResult;
    }

    /** Ritorna l'insieme di tutti gli URI scaricati, possibilmente vuoto.
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @return l'insieme di tutti gli URI scaricati (mai null) */
    @Override
    public Set<URI> getLoaded() {
        except();
        if (crawler.isRunning()) return crawler.getLoaded();
        return loaded;
    }

    /** Ritorna l'insieme, possibilmente vuoto, degli URI che devono essere
     * ancora scaricati. Quando l'esecuzione del crawler termina normalmente
     * l'insieme è vuoto.
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @return l'insieme degli URI ancora da scaricare (mai null) */
    @Override
    public Set<URI> getToLoad() {
        except();
        if (crawler.isRunning()) return crawler.getToLoad();
        return toLoad;
    }

    /** Ritorna l'insieme, possibilmente vuoto, degli URI che non è stato
     * possibile scaricare a causa di errori.
     * @throws IllegalStateException se il SiteCrawler è cancellato
     * @return l'insieme degli URI che hanno prodotto errori (mai null) */
    @Override
    public Set<URI> getErrors() {
        except();
        if (crawler.isRunning()) return crawler.getErrors();
        return errs;
    }

    /** Ritorna true se il SiteCrawler è in esecuzione.
     * @return true se il SiteCrawler è in esecuzione */
    @Override
    public boolean isRunning() {
        return running;
    }

    /** Ritorna true se il SiteCrawler è stato cancellato. In tal caso non può
     * più essere usato.
     * @return true se il SiteCrawler è stato cancellato */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /** Lancia l'eccezione quando il SiteCrawler è cancellato, metodo usato dai vari
     * metodi della classe.
     * @throws IllegalArgumentException */
    private void except(){
        if (isCancelled())
            throw new IllegalStateException();
    }

    /** Salva lo stato del SiteCrawler in un file Crawler_Archive.ser, cioé il dominio, gli insiemi toLoad, Loaded ed errs e la lista
     * di CrawlerResult results, solo se, per come è stato creato il SiteCrawler, può salvare. */
    private void saveArchive() {
        if (canArchive) {
            try (OutputStream output = Files.newOutputStream(pathToArchive);
                 ObjectOutputStream obOut = new ObjectOutputStream(output) ){
                toLoad = getToLoad();
                loaded = getLoaded();
                errs = getErrors();
                List<ResultToSave> resultToSaves = new ArrayList<>();
                cResults.get().stream().forEach(result -> resultToSaves.add(new ResultToSave(result)));
                obOut.writeObject(dom);
                obOut.writeObject(toLoad);
                obOut.writeObject(loaded);
                obOut.writeObject(errs);
                obOut.writeObject(resultToSaves);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** Carica gli oggetti salvati nel file Crawler_Archive.ser */
    private void loadArchive(){
        try ( InputStream input = Files.newInputStream(pathToArchive);
              ObjectInputStream obIn = new ObjectInputStream(input) ){
            dom = (URI) obIn.readObject();
            toLoad = (Set<URI>) obIn.readObject();
            loaded = (Set<URI>) obIn.readObject();
            errs = (Set<URI>) obIn.readObject();
            List<ResultToSave> resultsToSave = (ArrayList<ResultToSave>) obIn.readObject();
            resultsToSave.stream().forEach(result -> cResults.get().add(result.getCResult()));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
