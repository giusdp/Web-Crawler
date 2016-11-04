package wsa.gui.vboxes;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import wsa.web.TabCrawler;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/** Classe astratta implementata da tutte le classi nel package {@link wsa.gui.vboxes}
 * Ogni oggetto di questa classe ha come attributo almeno un Vbox e un TabCrawler, che utilizza
 * per prendere i dati da visualizzare nella GUI.
 * Ogni classe che la implementa è utilizzata per creare un solo VBox, tramite il metodo {@link VBoxes#createVBox()}
 * che può essere acceduto dal metodo {@link VBoxes#getvBox()}.
 * Ogni VBox corrisponde a una particolare visualizzazione nella GUI */
public abstract class VBoxes {

    protected TabCrawler tabCrawler;
    protected VBox vBox;
    protected URI dom;

    /** Costruttore che crea un Vbox e lo assegna al campo {@link VBoxes#vBox}
     * e associa un TabCrawler all'oggetto VBoxes creato.
     * @param tabCrawler viene assegnato al campo {@link VBoxes#tabCrawler} */
    public VBoxes(TabCrawler tabCrawler) {
        this.tabCrawler = tabCrawler;
        vBox = new VBox(30);
        VBox.setVgrow(vBox, Priority.ALWAYS);
        vBox.setAlignment(Pos.CENTER);
    }
    /** @return il VBox */
    public VBox getvBox(){ return vBox; }

    /** @return il campo che contiene il dominio dell'esplorazione */
    public URI getDom(){return dom;}

    /** Crea i Node del VBox {@link VBoxes#vBox} e ce li aggiunge */
    protected abstract void createVBox();

    /** Setta tutti gli eventi associati
     * agli oggetti Node dentro {@link VBoxes#vBox} */
    protected abstract void setActions();

    /** Cambia il TabCrawler associato all'oggetto VBoxes.
     * @param tabCrawler viene passato a {@link VBoxes#tabCrawler}*/
    public abstract void changeTabCrawler(TabCrawler tabCrawler);

    /** Prende un oggetto Text e ne setta il contenuto con la stringa head
     * in testa al dominio dell'esplorazione corrente dom.
     * @param text un oggetto Text da visualizzare nella GUI
     * @param head una stringa da attaccare in testa a dom. Es: "Dominio: "
     * @param dom il dominio dell'esplorazione corrente */
    protected void printURIOnGUI(Text text, String head, URI dom){
        if (dom != null) text.setText(head + dom.toString());
        else text.setText("Nessun dominio inserito.");
    }

    /** Disabilita l'oggetto Node passato e ne setta l'opacit� a 0.5.
     * @param node l'oggetto Node da disabilitare */
    protected void disableNode(Node node){
        if (node != null) {
            node.setDisable(true);
            node.setOpacity(0.5);
        }
    }

    /** Abilita l'oggetto Node passato e ne setta l'opacit� al
     * massimo valore.
     * @param node l'oggetto Node da abilitare */
    protected void enableNode(Node node){
        if (node != null){
            node.setDisable(false);
            node.setOpacity(1);
        }
    }

    /** Ritorna un oggetto URI caricato da un file.
     * Viene utilizzato per caricare il dominio di un esplorazione.
     * @param path il percorso del file dove ? salvato l'oggetto
     * @throws IllegalArgumentException se il file non viene trovato
     * @return l'oggetto URI caricato dal file al percorso path */
    protected URI loadDomFromArchive(Path path) throws IllegalArgumentException{
        try (InputStream input = Files.newInputStream(path);
             ObjectInputStream obin = new ObjectInputStream(input) ){
            return (URI) obin.readObject();
        } catch (Exception e){
            throw new IllegalArgumentException("Archivio non presente.");
        }
    }

}
