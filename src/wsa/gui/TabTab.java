package wsa.gui;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import wsa.gui.vboxes.VBoxDati;
import wsa.gui.vboxes.VBoxPostStart;
import wsa.gui.vboxes.VBoxPreStart;
import wsa.web.TabCrawler;
import wsa.web.WebFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/** Ogni oggetto di questa classe � una Tab della GUI */
public class TabTab extends Tab {

    private TabCrawler tabCrawler;
    private VBox megaVBox;
    private VBoxPreStart vBoxPreStart;
    private VBoxPostStart vBoxPostStart;
    private VBoxDati vBoxDati;

    /** Costruttore di TabTab. Ogni TabTab ha associato un TabCrawler (il campo {@link TabTab#tabCrawler}),
     * da cui prende i dati dell'esplorazione che dovr� visualizzare, e di cui verr� cancellato il SiteCrawler quando
     * la Tab viene chiusa (con il metodo {@link TabCrawler#cancelCrawler()}).
     * Il contenuto di ogni Tab � il VBox {@link TabTab#megaVBox}, che quindi � sempre visualizzato.
     * Al suo interno conterr� solo un altro Vbox, che pu� essere:
     * {@link TabTab#vBoxPreStart}, {@link TabTab#vBoxPostStart} o {@link TabTab#vBoxDati},
     * i quali contengono tutti i nodi da visualizzare in un dato momento.
     * @param text � il titolo della Tab, visualizzato sopra di essa. Viene impostato chiamando il
     * costruttore della classe Tab
     * @param stage lo Stage dove viene visualizzato il VBox {@link TabTab#vBoxPreStart},
     * il VBox aggiunto a {@link TabTab#megaVBox} appena si crea una nuova Tab. */
    public TabTab(Stage stage, String text) {
        super(text);
        tabCrawler = WebFactory.getTabCrawler();
        megaVBox = new VBox();

        vBoxPreStart = new VBoxPreStart(tabCrawler, stage);

        megaVBox.getChildren().add(vBoxPreStart.getvBox());
        VBox.setVgrow(megaVBox, Priority.ALWAYS);
        megaVBox.setAlignment(Pos.CENTER);

        super.setContent(megaVBox);

        super.setOnClosed(event -> {
            if (tabCrawler.getSiteCrawler() != null)
                if (!tabCrawler.getSiteCrawler().isCancelled())
                    tabCrawler.cancelCrawler();
        });

        setDefaultStartAction();
        setDefaultVisDATAction();
    }


    /** Setta l'evento associato al bottone {@link wsa.gui.vboxes.VBoxPreStart#start}, che
     *  inizia l'esplorazione del dominio e mette il {@link TabTab#vBoxPostStart}
     *  all'interno di {@link TabTab#megaVBox} al posto di {@link TabTab#vBoxPreStart} */
    private void setDefaultStartAction(){
        vBoxPreStart.setActionToStart(event -> {
            try {
                if (tabCrawler.getDir() != null && tabCrawler.getDom() != null) {
                    Path pathForDom = Paths.get(tabCrawler.getDir().toString() + "/Dominio.ser");
                    try (OutputStream output = Files.newOutputStream(pathForDom);
                         ObjectOutputStream obOut = new ObjectOutputStream(output)) {
                        obOut.writeObject(tabCrawler.getDom()); //salva il dominio su un file
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                tabCrawler.createSiteCrawler();

                if (vBoxPostStart == null) {         //se il vBoxPostStart non � gi� stato inizializzato, viene fatto.
                    vBoxPostStart = new VBoxPostStart(tabCrawler, vBoxPreStart.getDom(), attivaLink);
                    vBoxPostStart.setBackAction(consumer::accept); //setta l'evento associato al bottone indietro del vBoxPostStart
                } else {                            //altrimenti gli viene associato il TabCrawler dell'esplorazione corrente.
                    vBoxPostStart.changeTabCrawler(tabCrawler);
                    vBoxPostStart.printDom(vBoxPreStart.getDom()); //visualizza il dominio
                }
                megaVBox.getChildren().clear();
                megaVBox.getChildren().add(vBoxPostStart.getvBox());

                tabCrawler.start(true);
                vBoxPreStart.cancellaDati();
                tabCrawler.setDom(vBoxPreStart.getDom());

            } catch (Exception ex) {
                System.out.println("errore nello start");
                ex.printStackTrace();
            }
        });
    }

    /** Setta l'evento associato al bottone {@link wsa.gui.vboxes.VBoxPreStart#visualDati}, che crea
     *  un SiteCrawler usato da {@link TabTab#tabCrawler}, ma senza far
     *  partire l'esplorazione, setta il dominio e mette {@link TabTab#vBoxDati} all'interno di
     *  {@link TabTab#megaVBox} al posto del suo contenuto precedente. */
    private void setDefaultVisDATAction(){
        vBoxPreStart.setActionToVisData(event -> {
            try {
                tabCrawler.createSiteCrawler();
                tabCrawler.start(false);

                tabCrawler.setDom(vBoxPreStart.getDom());

                if (vBoxDati == null) {                  //se vBoxDati non � gi� stato inizializzato, viene fatto.
                    vBoxDati = new VBoxDati(tabCrawler);
                    vBoxDati.setIndietroAction(consumer::accept); //setta l'evento associato al bottone indietro nel vBoxDati
                } else {                                //altrimenti gli viene associato il TabCrawler dell'esplorazione corrente
                    vBoxDati.changeTabCrawler(tabCrawler);
                }

                megaVBox.getChildren().clear();
                megaVBox.getChildren().add(vBoxDati.getvBox());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /** Oggetto consumer che viene utilizzato per settare gli eventi
     * associati ai bottoni {@link wsa.gui.vboxes.VBoxPostStart#indietro} e {@link wsa.gui.vboxes.VBoxDati#indietro}.
     * Cancella il SiteCrawler associato al TabCrawler corrente, ma mantiene la directory dell'esplorazione
     * creando un nuovo TabCrawler con specificata solo la directory. Poi visualizza {@link TabTab#vBoxPreStart}
     * mettendolo in {@link TabTab#megaVBox} */
    private Consumer<ActionEvent> consumer = actionEvent -> {
            TabCrawler appoggio = WebFactory.getTabCrawler();
            appoggio.setDir(tabCrawler.getDir());
            tabCrawler.cancelCrawler();
            tabCrawler = appoggio;
            appoggio = null;
            vBoxPreStart.changeTabCrawler(tabCrawler);
            megaVBox.getChildren().clear();
            megaVBox.getChildren().add(vBoxPreStart.getvBox());
        };


    /** Viene passato al costruttore di {@link wsa.gui.vboxes.VBoxPostStart}
     * per inizializzare il campo {@link TabTab#vBoxPostStart}.
     * Indica se il bottone {@link wsa.gui.vboxes.VBoxPostStart#linkVersoSite}
     * � attivo oppure no. */
    private boolean attivaLink;

    /** @param attiva se {@link TabTab#vBoxPostStart} non � stato ancora inizializzato
     * viene passato al campo {@link TabTab#attivaLink}.
     * Altrimenti al metodo {@link wsa.gui.vboxes.VBoxPostStart#attivaLinks(boolean)}
     * che attiva o disattiva il bottone {@link wsa.gui.vboxes.VBoxPostStart#linkVersoSite} */
    public void attivaLinks(boolean attiva){
        if (vBoxPostStart == null) {
            attivaLink = attiva;
            return;
        }
        vBoxPostStart.attivaLinks(attiva);
    }

    /** @return il dominio dell'esplorazione */
    public URI getDom(){
        return tabCrawler.getDom();
    }
}
