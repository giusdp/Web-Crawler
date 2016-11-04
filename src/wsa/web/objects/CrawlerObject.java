package wsa.web.objects;

import wsa.web.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;


/** Created by Giuseppe on 5/23/2015. */

public class CrawlerObject implements Crawler {

    /** Un web crawler che partendo da uno o più URI scarica le relative pagine e
     * poi fa lo stesso per i link contenuti nelle pagine scaricate. Però il crawler
     * può essere creato in modo tale che solamente le pagine di URI selezionati
     * sono usate per continuare il crawling. */

    private AtomicReference<Set<URI>> toLoad, loaded, errs;
    private Predicate<URI> pageLink;

    private boolean running, cancelled;
    private AtomicBoolean futureFound;

    private AsyncLoader asyncLoader;
    private ConcurrentLinkedQueue<Future<LoadResult>> futures;
    private ConcurrentLinkedQueue<CrawlerResult> crawlerResults;
    private ConcurrentLinkedQueue<URL> urlSubmitted;
    private final ExecutorService executor;



    /** COSTRUTTORE */
    public CrawlerObject(Collection<URI> loaded,
                         Collection<URI> toLoad,
                         Collection<URI> errs,
                         Predicate<URI> pageLink)
    {
        this.loaded = new AtomicReference<>();
        this.loaded.set(new HashSet<>());
        this.toLoad = new AtomicReference<>();
        this.toLoad.set(new HashSet<>());
        this.errs = new AtomicReference<>();
        this.errs.set(new HashSet<>());

        if (loaded != null) loaded.stream().forEach(this.loaded.get()::add);
        if (toLoad != null) toLoad.stream().forEach(this.toLoad.get()::add);
        if (errs != null) errs.stream().forEach(this.errs.get()::add);

        if (pageLink != null) this.pageLink = pageLink;
        else this.pageLink = uri -> true;

        running = false;
        cancelled = false;
        futures = new ConcurrentLinkedQueue<>();
        crawlerResults = new ConcurrentLinkedQueue<>();
        urlSubmitted = new ConcurrentLinkedQueue<>();
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        asyncLoader = null;
        futureFound = new AtomicBoolean(false);
    }



    /** Aggiunge un URI all'insieme degli URI da scaricare. Se però è presente
     * tra quelli già scaricati, quelli ancora da scaricare o quelli che sono
     * andati in errore, l'aggiunta non ha nessun effetto. Se invece è un nuovo
     * URI, è aggiunto all'insieme di quelli da scaricare.
     * @throws IllegalStateException se il Crawler è cancellato
     * @param uri  un URI che si vuole scaricare */

    @Override
    public void add(URI uri) {
        except();
        if (loaded.get().contains(uri)) return;
        if (errs.get().contains(uri)) return;
        if (toLoad.get().contains(uri)) return;
        toLoad.get().add(uri);
    }

    /** Inizia l'esecuzione del Crawler se non è già in esecuzione e ci sono URI
     * da scaricare, altrimenti l'invocazione è ignorata. Quando è in esecuzione
     * il metodo isRunning ritorna true.
     * throws IllegalStateException se il Crawler è cancellato */

    @Override
    public void start() {
        except();

        if (isRunning()) return;

        running = true;
        if (asyncLoader == null) asyncLoader = WebFactory.getAsyncLoader();

        executor.submit(() -> {
            while (isRunning()) {
                if (!toLoad.get().isEmpty()) {
                    ConcurrentLinkedQueue<URI> urisToRemove = new ConcurrentLinkedQueue<>();

                    toLoad.get().stream().forEach(uriToLoad -> {
                        try {
                            URL url = uriToLoad.toURL();
                            if (!urlSubmitted.contains(url)) {
                                urlSubmitted.add(url);
                                futures.add(asyncLoader.submit(url));
                            }
                        } catch (MalformedURLException e) {
                            errs.get().add(uriToLoad);
                        }
                        urisToRemove.add(uriToLoad);
                    });
                    urisToRemove.stream().forEach(uri -> toLoad.get().remove(uri));

                } else {
                    if (!futures.isEmpty()) {

                        futureFound.set(false);

                        futures.stream().forEach(this::handleFutures);

                        if (!futureFound.get()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
    }

    private void handleFutures(Future<LoadResult> future){
        List<String> links;
        try {
            if (future.isDone()) {

                futureFound.set(true);

                futures.remove(future);

                LoadResult loadResult = future.get();

                if (loadResult == null )return;

                URI mainURI = loadResult.url.toURI();

                if (loadResult.exc != null) {
                    errs.get().add(mainURI);
                    crawlerResults.add(new CrawlerResult(mainURI, false, null, null, loadResult.exc));
                } else {

                    List<URI> urisForCralRes = new ArrayList<>();
                    List<String> errsForCralRes = new ArrayList<>();

                    if (pageLink.test(mainURI)) {
                        links = loadResult.parsed.getLinks();

                        links.stream().forEach(s -> {
                            try {
                                URI uri = new URI(s);
                                URI absURI = mainURI.resolve(uri);
                                add(absURI);
                                urisForCralRes.add(absURI);
                            } catch (URISyntaxException e) {
                                errsForCralRes.add(s);
                            }
                        });
                    }
                        if (!loaded.get().contains(mainURI)) {
                            crawlerResults.add(new CrawlerResult(mainURI,
                                    !urisForCralRes.isEmpty(),
                                    !urisForCralRes.isEmpty() ? urisForCralRes : null,
                                    !errsForCralRes.isEmpty() ? errsForCralRes : null,
                                    null));
                        }
                    loaded.get().add(mainURI);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Ritorna il risultato relativo al prossimo URI. Se il Crawler non è in
     * esecuzione, ritorna un Optional vuoto. Non è bloccante, ritorna
     * immediatamente anche se il prossimo risultato non è ancora pronto.
     * @throws IllegalStateException se il Crawler è cancellato
     * @return  il risultato relativo al prossimo URI scaricato */

    @Override
    public Optional<CrawlerResult> get() {
        except();
        if (!isRunning()) return Optional.empty();
        if (crawlerResults.isEmpty()) return Optional.of(new CrawlerResult(null, false, null, null, null));
        return Optional.of(crawlerResults.poll());
    }

    /** Sospende l'esecuzione del Crawler. Se non è in esecuzione, ignora
     * l'invocazione. L'esecuzione può essere ripresa invocando start. Durante
     * la sospensione l'attività del Crawler dovrebbe essere ridotta al minimo
     * possibile (eventuali thread dovrebbero essere terminati).
     * @throws IllegalStateException se il Crawler è cancellato */

    @Override
    public void suspend() {
        except();
        if ( ! isRunning())
            return;
        running = false;
    }

    /** Cancella il Crawler per sempre. Dopo questa invocazione il Crawler non
     * può più essere usato. Tutte le risorse devono essere rilasciate. */

    @Override
    public void cancel() {
        if (isCancelled()) return;
        running = false;
        cancelled = true;
        if (asyncLoader != null) asyncLoader.shutdown();
        executor.shutdown();
        futures.clear();
        urlSubmitted.clear();
        crawlerResults.clear();
        toLoad.get().clear();
        loaded.get().clear();
        errs.get().clear();
        toLoad = null;
        loaded = null;
        errs = null;
        pageLink = null;
        asyncLoader = null;
        futures = null;
        crawlerResults = null;
        urlSubmitted = null;
    }

    /** Ritorna l'insieme di tutti gli URI scaricati, possibilmente vuoto.
     * @throws IllegalStateException se il Crawler è cancellato
     * @return l'insieme di tutti gli URI scaricati (mai null) */

    @Override
    public Set<URI> getLoaded() {
        except();
        return loaded.get();
    }

    /** Ritorna l'insieme, possibilmente vuoto, degli URI che devono essere
     * ancora scaricati. Quando l'esecuzione del crawler termina normalmente
     * l'insieme è vuoto.
     * @throws IllegalStateException se il Crawler è cancellato
     * @return l'insieme degli URI ancora da scaricare (mai null) */

    @Override
    public Set<URI> getToLoad() {
        except();
        return toLoad.get();
    }

    /** Ritorna l'insieme, possibilmente vuoto, degli URI che non è stato
     * possibile scaricare a causa di errori.
     * @throws IllegalStateException se il crawler è cancellato
     * @return l'insieme degli URI che hanno prodotto errori (mai null) */

    @Override
    public Set<URI> getErrors() {
        except();
        return errs.get();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    private void except(){
        if (isCancelled())
            throw new IllegalStateException();
    }
}
