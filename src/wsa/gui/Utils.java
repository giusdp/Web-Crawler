package wsa.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import wsa.gui.vboxes.VBoxDati;
import wsa.gui.vboxes.VBoxPostStart;
import wsa.web.SiteCrawler;

import java.net.URI;
import java.util.*;

/** Classe di metodi di utilità */
public class Utils {

    /** Ritorna la distanza fra due URI specificati.
     * @param a primo URI
     * @param b secondo URI
     * @param listone un ObservableList<Page> che consente di accedere agli oggetti {@link wsa.gui.Page}
     * che forniscono i link in una pagina.
     * @return la distanza fra l'URI a e l'URI b */
    public static int DistanzaURI(URI a, URI b, ObservableList<Page> listone) {
        int c  = 0;
        List<URI> lista;
        List<URI> listaFigli = new ArrayList<>();
        listaFigli.add(a);
        while(!listaFigli.isEmpty()){
            lista = listaFigli;
            listaFigli = new ArrayList<>();
            c++;
            for(URI uri : lista){
                Optional<Page> pagino = listone.stream().filter(pagina ->  pagina.getURI().equals(uri) ).findFirst();
                if (!pagino.isPresent()) continue;
                Page page = pagino.get();
                if ( page.getUriEstrattiCorretti() != null){
                    for(URI link : page.getUriEstrattiCorretti()){
                        if(link.equals(b)) return c;
                        else listaFigli.add(link);
                    }
                }
            }
        }
        return -1;
    }

    /** Ritorna la distanza massima fra tutte le coppie di URI appartenenti alla Collection uris. Viene
     * chiamato dal metodo setOnAction() del bottone {@link wsa.gui.vboxes.VBoxDistanze#findMaxDistance}
     * all'interno di {@link wsa.gui.vboxes.VBoxDistanze#setActions()}.
     * @param uris Collection di URI. Le coppie di URI vengono generate a partire da questa Collection.
     * @param listone un ObservableList<Page> da passare a {@link Utils#DistanzaURI(URI, URI, ObservableList)}
     * @return la distanza massima fra tutte le coppie di URI della Collection uris b */
    public static int DistanzaMaxURI (Collection<URI> uris, ObservableList<Page> listone) {
        int c;
        int max = -1;
        HashMap<URI,Set<URI>> map = new HashMap<>();
        for (URI uri1 : uris){
            Set<URI> set1 = new HashSet<>();
            if(!map.containsKey(uri1)){
                map.put(uri1, set1);
            }
            for (URI uri2 : uris){
                Set<URI> set2 = new HashSet<>();
                if(!map.containsKey(uri2)){
                    map.put(uri2, set2);
                }
                if(!map.get(uri1).contains(uri2) && !map.get(uri2).contains(uri1)){
                    set1.add(uri2);
                    map.replace(uri1, set1);
                    set2.add(uri1);
                    map.replace(uri2, set2);
                    c = DistanzaURI(uri1, uri2, listone);
                    if (max < c) {
                        max = c;
                    }
                }
            }
        }
        return max;
    }

    /** Ritorna il massimo fra il numero di link puntanti della pagina e l'intero passato. Viene usato dal
     * metodo {@link VBoxDati#calcolaStatistiche()} e dal metodo
     * {@link VBoxPostStart#createVBox()} per il calcolo del massimo numero di link che puntano a una pagina.
     * @param page un oggetto {@link wsa.gui.Page} da cui si estrarrà il suo numero di link puntanti.
     * @param maxLinkPuntanti un intero che rappresenta il massimo corrente di link puntanti a una pagina.
     * @return il massimo fra il numero di link puntanti alla pagina page e l'intero maxLinkPuntanti. b */
    public static synchronized int aggiornaNumeroMaxLinkPuntanti(Page page, int maxLinkPuntanti){
        int c = Integer.parseInt(page.getNumeroLinkPuntanti());
        if ( c > maxLinkPuntanti){
            maxLinkPuntanti = c;
        }
        return maxLinkPuntanti;
    }

    /** Ritorna il massimo fra il numero di link interni alla pagina e l'intero passato. Viene usato dal
     * metodo {@link VBoxDati#calcolaStatistiche()} e dal metodo
     * {@link wsa.gui.vboxes.VBoxPostStart#createVBox()} per il calcolo del massimo numero di link interni a una pagina.
     * @param page un oggetto {@link wsa.gui.Page} da cui si estrarr? il suo numero di link interni.
     * @param maxLinkInterni un intero che rappresenta il massimo corrente di link interni a una pagina.
     * @return il massimo fra il numero di link interni alla pagina page e l'intero maxLinkInterni. b */
    public static int aggiornaNumeroMaxLinkInterni(Page page, int maxLinkInterni){
        int c = Integer.parseInt(page.getNumeroLinkInterni());
        if ( c > maxLinkInterni){
            maxLinkInterni = c;
        }
        return maxLinkInterni;
    }

    /** Ritorna un ObservableList<URI> contenente tutti gli URI interni al dominio dom e appartenenti alla collezione col.
     * Il metodo viene chiamato dal metodo setOnAction() del bottone {@link wsa.gui.vboxes.VBoxPostStart#distanze}
     * all'interno di {@link wsa.gui.vboxes.VBoxPostStart#setActions()} e da {@link VBoxDati#calcolaStatistiche()}
     * che gli passano il dominio dell'esplorazione corrente e l'insieme degli URI fin ora scaricati.
     * @param dom un URI che rappresenta un dominio.
     * @param col collezione di URI.
     * @return un Set di URI appartenenti a col e interni a dom b */
    public static ObservableList<URI> calcolaInsiemeInterni(URI dom, Collection<URI> col){
        ObservableList<URI> interni = FXCollections.observableArrayList();
        col.stream().filter(uri -> SiteCrawler.checkSeed(dom, uri)).forEach(interni::add);
        return interni;
    }
}