package wsa.gui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import wsa.web.CrawlerResult;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** Classe che rappresenta pagine web.
 * Ogni oggetto Page ha degli attributi che rappresentano informazioni relative a una pagina web.
 * Alcuni attributi sono StringProperty in modo da poter essere visualizzati in una TableView. */
public class Page {

    private Exception eccezione;
    private List<URI> uriEstrattiCorretti;
    private List<String> uriEstrattiErrati;
    private ObservableList<URI> uriPuntanti;
    private List<String> uriEstratti;
    private URI uri;

    /** Costruttore di oggetti Page.
     * @param result da cui viene passato al campo {@link Page#uri} l'uri contenuto nel CrawlerResult,
     * che rappresenta l'URI della pagina e verrï¿½ usato per settare la String Property {@link Page#url}.
     * result.exc viene passato al campo {@link Page#eccezione}, che rappresenta l'eccezione generata
     * se la pagina non è stata scaricata con successo. Si usa anche per settare la StringProperty {@link Page#esito}.
     * result.links viene passato al campo {@link Page#uriEstrattiCorretti}, che rappresenta
     * una lista contenente gli URI interni alla pagina scaricati con successo. Insieme a {@link Page#uriEstrattiErrati}
     * viene usato per riempire la lista {@link Page#uriEstratti}, che contiene tutti gli URI appartenenti alla
     * pagina convertiti in stringhe.
     * result.errRawLinks viene passato al campo {@link Page#uriEstrattiErrati};
     * una lista di stringhe che rappresentano gli URI interni alla pagina che hanno generato errori.
     * @param uriPuntanti viene passato al campo {@link Page#uriPuntanti} che rappresenta la lista osservabile
     * di URI che puntano alla pagina.
     * @param isInterno serve per notare se l'uri del CrawlerResult passato è appartenente al dominio oppure no,
     * perciò di conseguenza viene settata la property internoEsternoDmino*/
    public Page(CrawlerResult result, ObservableList<URI> uriPuntanti, boolean isInterno){
        uriEstratti = new ArrayList<>();
        this.uri = result.uri;
        eccezione = result.exc;
        this.uriEstrattiCorretti = result.links;
        this.uriEstrattiErrati = result.errRawLinks;
        this.uriPuntanti = uriPuntanti;
        setUrl(uri.toString());
        String esito = (eccezione == null ? "Pagina scaricata." : "Errore: " + this.eccezione.getMessage());
        setEsito(esito);
        if (this.uriEstrattiCorretti == null) this.uriEstrattiCorretti = new ArrayList<>();
        if (this.uriEstrattiErrati == null) this.uriEstrattiErrati = new ArrayList<>();
        setNumeroLinkInterni(String.valueOf(this.uriEstrattiCorretti.size() + this.uriEstrattiErrati.size()));
        try {
            if (uriPuntanti != null) {
                setNumeroLinkPuntanti(String.valueOf(uriPuntanti.size()));
                numeroLinkPuntanti.bind(Bindings.size(uriPuntanti).asString());
            } else {
                setNumeroLinkPuntanti("1");
            }
        }catch (Exception e){
            System.out.println("Fottitene");
            e.printStackTrace();
        }

        this.uriEstrattiCorretti.stream().forEach(uri -> {
            try {
                URL stringURL = uri.toURL();
                uriEstratti.add(stringURL.toString());
            } catch (MalformedURLException ignored) {
            }
        });
        this.uriEstrattiErrati.stream().forEach(uriEstratti::add);

        setInternoEsternoDominio((isInterno ? "Interna" : "Esterna"));
    }

    /** Costruttore di oggetti Page che prende solo un parametro, e setta la StringProperty {@link Page#esito}
     * della pagina in modo che indichi che non è ancora stata scaricata.
     * Questo costruttore si usa per creare gli oggetti Page corrispondenti a pagine ancora non scaricate,
     * in modo che possano lo stesso essere visualizzate in una TableView.
     * @param url viene usato per settare la StringProperty {@link Page#url} */
    public Page(String url, boolean errato){
        setUrl(url);
        if (errato) setEsito("Impossibile ricavare URI assoluto");
        else setEsito("Pagina non scaricata.");
    }

    public Page(String url){
        setUrl(url);
    }


    /** @return l'URI della pagina */
    public URI getURI(){return this.uri;}

    /** @return l'eccezione sollevata se la pagina non è stata scaricata con successo,
     * o null se è stata scaricata con successo */
    public Exception getEccezione(){ return eccezione; }

    /** @return una lista con gli URI interni alla pagina scaricati con successo */
    public List<URI> getUriEstrattiCorretti(){ return uriEstrattiCorretti; }

    /** @return una lista di stringhe che rappresentano gli URI interni alla pagina che hanno generato errori */
    public List<String> getUriEstrattiErrati(){ return uriEstrattiErrati; }

    /** @return una lista con gli URI che puntano alla pagina */
    public List<URI> getUriPuntanti(){ return uriPuntanti; }

    /** @return una lista di stringhe che rappresentano gli URI interni alla pagina */
    public List<String> getUriEstratti(){ return uriEstratti; }


    /** sei righe riguardanti la StringProperty url di una Page,
     *  che verrà visualizzata in una colonna della TableView
     *  ritornata da {@link wsa.gui.TableFactory#createTable(ObservableList, boolean, Stage, Stage)}.
     *  La seconda riga è il metodo {@link Page#setUrl(String)} che setta il valore di url.
     *  La terza è il metodo {@link Page#getUrl()} che ritorna il valore di url.
     *  Il metodo {@link Page#urlProperty()} inizializza url e la ritorna. */
    private StringProperty url;
    public void setUrl(String value) { urlProperty().set(value); }
    public String getUrl() { return urlProperty().get(); }
    public StringProperty urlProperty() {
        if (url == null) url = new SimpleStringProperty(this, "url");
        return url;
    }

    /** sei righe riguardanti la StringProperty esito di una Page,
     *  che verrà visualizzata in una colonna della TableView
     *  ritornata da {@link wsa.gui.TableFactory#createTable(ObservableList, boolean, Stage, Stage)}.
     *  La seconda riga è il metodo {@link Page#setEsito(String)} che setta il valore di esito.
     *  La terza è il metodo {@link Page#getEsito()} che ritorna il valore di esito.
     *  Il metodo {@link Page#esitoProperty()} inizializza esito e la ritorna. */
    private StringProperty esito;
    public void setEsito(String value) { esitoProperty().set(value); }
    public String getEsito() { return esitoProperty().get(); }
    public StringProperty esitoProperty() {
        if (esito == null) esito = new SimpleStringProperty(this, "Esito");
        return esito;
    }

    /** sei righe riguardanti la StringProperty numeroLinkInterni,
     *  che verrà visualizzata in una colonna della TableView ritornata da
     *  {@link wsa.gui.TableFactory#createTable(ObservableList, boolean, Stage, Stage)}.
     *  La seconda riga è il metodo {@link Page#setNumeroLinkInterni(String)},
     *  che setta il valore di numeroLinkInterni.
     *  La terza è il metodo getNumeroLinkInterni(),
     *  che ritorna il valore di numeroLinkInterni.
     *  Il metodo {@link Page#numeroLinkInterniProperty()} inizializza numeroLinkInterni e la ritorna. */
    private StringProperty numeroLinkInterni;
    public void setNumeroLinkInterni(String value){numeroLinkInterniProperty().set(value);}
    public String getNumeroLinkInterni(){return numeroLinkInterniProperty().get();}
    public StringProperty numeroLinkInterniProperty(){
        if (numeroLinkInterni == null) numeroLinkInterni = new SimpleStringProperty(this, "numeroLinkInterni");
        return numeroLinkInterni;
    }

    /** sei righe riguardanti la StringProperty numeroLinkPuntanti,
     *  che verrà visualizzata in una colonna della TableView ritornata da
     *  {@link wsa.gui.TableFactory#createTable(ObservableList, boolean, Stage, Stage)}.
     *  La seconda riga è il metodo {@link Page#setNumeroLinkPuntanti(String)},
     *  che setta il valore di numeroLinkPuntanti.
     *  La terza è il metodo {@link Page#getNumeroLinkPuntanti()},
     *  che ritorna il valore di numeroLinkPuntanti.
     *  Il metodo {@link Page#numeroLinkPuntantiProperty()} inizializza numeroLinkPuntanti e la ritorna. */
    private StringProperty numeroLinkPuntanti;
    public void setNumeroLinkPuntanti(String value){numeroLinkPuntantiProperty().set(value);}
    public String getNumeroLinkPuntanti(){return numeroLinkPuntantiProperty().get();}
    public StringProperty numeroLinkPuntantiProperty(){
        if (numeroLinkPuntanti == null) numeroLinkPuntanti = new SimpleStringProperty(this, "numeroLinkPuntanti");
        return numeroLinkPuntanti;
    }

    /** sei righe riguardanti la StringProperty internoEsternoDominio,
     *  che verrà visualizzata in una colonna della TableView ritornata da
     *  {@link wsa.gui.TableFactory#createTable(ObservableList, boolean, Stage, Stage)}.
     *  La seconda riga è il metodo {@link Page#setInternoEsternoDominio(String)},
     *  che setta il valore di internoEsternoDominio.
     *  La terza è il metodo {@link Page#getInternoEsternoDominio()},
     *  che ritorna il valore di internoEsternoDominio.
     *  Il metodo {@link Page#internoEsternoDominioProperty()} inizializza internoEsternoDominio e la ritorna. */
    private StringProperty internoEsternoDominio;
    public void setInternoEsternoDominio(String value){ internoEsternoDominioProperty().setValue(value); }
    public String getInternoEsternoDominio(){ return internoEsternoDominioProperty().get(); }
    public StringProperty internoEsternoDominioProperty(){
        if (internoEsternoDominio == null) internoEsternoDominio = new SimpleStringProperty(this, "internoEsternoDominio");
        return internoEsternoDominio;
    }
}
