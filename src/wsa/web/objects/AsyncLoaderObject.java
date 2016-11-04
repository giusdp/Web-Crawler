package wsa.web.objects;

import wsa.web.AsyncLoader;
import wsa.web.LoadResult;
import wsa.web.Loader;
import wsa.web.WebFactory;

import java.net.URL;
import java.util.concurrent.*;

/** Created by Giuseppe on 5/22/2015. */

/** Un Loader asincrono concreto */
public class AsyncLoaderObject implements AsyncLoader {

    private LinkedBlockingQueue<Loader> queue;
    private final ExecutorService pool;

    /** COSTRUTTORE */
    public AsyncLoaderObject(){
        queue = new LinkedBlockingQueue<>(15);
        pool = Executors.newFixedThreadPool(15, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Sottomette il downloading della pagina dello specificato URL e ritorna
     * un Future per ottenere il risultato in modo asincrono.
     * @param url  un URL di una pagina web
     * @throws IllegalStateException se il loader è chiuso
     * @return Future per ottenere il risultato in modo asincrono */
    @Override
    public Future<LoadResult> submit(URL url) {
        if (isShutdown())
            throw new IllegalStateException();

    //Callable per il submit
        Callable<LoadResult> callable = () -> {

            Loader loader = null;

            //aspetta 20 millisecondi per prenderlo
            try {
                loader = queue.poll(20, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored){}
            //se non è riuscito a prenderlo allora ne creo uno nuovo
            if (loader == null) {
                loader = WebFactory.getLoader();
            }
            LoadResult loadResult = loader.load(url);
            queue.add(loader);

            return loadResult;
        };
    //Callable per il submit

        return pool.submit(callable);
    }

    /** Chiude il loader asincrono e rilascia tutte le risorse. Dopo di ciò non può più
     * essere usato.
     * Chiude il Thread JavaFX, l' {@link ExecutorService} pool
     * e cancella la referenza a {@link LinkedBlockingQueue} queue */
    @Override
    public void shutdown() {
        if (!isShutdown()) {
            pool.shutdown();
            queue.clear();
            queue = null;
        }
    }

    /** Ritorna true se è l' {@link ExecutorService} pool è stato chiuso.
     * @return true se è chiuso */
    @Override
    public boolean isShutdown() {
        return pool.isShutdown();
    }
}
