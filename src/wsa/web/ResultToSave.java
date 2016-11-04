package wsa.web;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

/** Created by Giuseppe on 26/08/2015. */
public class ResultToSave implements Serializable {

    private static final long serialVersionUID = 4120945L;

    /** L'URI della pagina o null. Se null, significa che la prossima pagina
     * non è ancora pronta. */
    private final URI uri;
    /** true se l'URI è di una pagina i cui link sono seguiti. Se false i campi
     * links e errRawLinks sono null. */
    private final boolean linkPage;
    /** La lista degli URI assoluti dei link della pagina o null */
    private final List<URI> links;
    /** La lista dei link che non è stato possibile trasformare in URI assoluti
     * o null */
    private final List<String> errRawLinks;
    /** Se è null, la pagina è stata scaricata altrimenti non è stato possibile
     * scaricarla e l'eccezione ne dà la causa */
    private final Exception exc;

    public ResultToSave(CrawlerResult crawlerResult) {
        uri = crawlerResult.uri;
        linkPage = crawlerResult.linkPage;
        links = crawlerResult.links;
        errRawLinks = crawlerResult.errRawLinks;
        exc = crawlerResult.exc;
    }

    public CrawlerResult getCResult(){
        return new CrawlerResult(uri, linkPage, links, errRawLinks, exc);
    }
}
