package wsa.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Una factory di oggetti TableView<Page>*/
public class TableFactory {

    /** Crea una TableView<Page></Page> e la ritorna.
     * @param listone ObservableList di oggetti Page. Ogni Page corrisponde a una riga e
     * ogni colonna a una StringProperty delle Page. L'ObservableList viene associata a una TableView
     * con il metodo setItems() della classe TableView. Ogni volta che la lista viene aggiornata, lo ? anche la tabella
     * @param noExtraPuntanti se true le celle della colonna relativa ai link puntanti alla pagina non saranno cliccabili,
     * altrimenti potranno essere cliccate e verr? visualizzata una tableView contenente i link che puntano alla pagina.
     * La TableView {@link wsa.gui.vboxes.VBoxPostStart#table} viene creata con questo parametro a true.
     * Invece la TableView {@link wsa.gui.vboxes.VBoxDati#table} viene creata con questo parametro a false
     * @param stageEstratti Stage dove verr? visualizzata la tabella contenente i link estratti da una pagina
     * dopo aver cliccato su una cella contenente il numero di link estratti
     * @param  stagePuntanti Stage dove verr? visualizzata la tabella dei link che puntano a una pagina, dopo aver
     * cliccato su una cella contenente il numero di link puntanti (se possibile)
     * @return una TableView di oggetti Page */
    private static TableView<Page> createTable(ObservableList<Page> listone, boolean noExtraPuntanti,
                                               Stage stageEstratti, Stage stagePuntanti) {

        stageEstratti.setTitle("Link estratti");
        TableView<Page> table = new TableView<>();
        table.setEditable(true);
        table.getSelectionModel().setCellSelectionEnabled(true);

        TableColumn<Page,String> urlCol = createURLCOL(390);

        TableColumn<Page,String> dominioCol = createDominioCOL(70);

        TableColumn<Page,String> esitoCol = createESITOCOL(165);

        TableColumn<Page,String> linkInterniCol = createLINKINTERNICOL(listone, stageEstratti);

        TableColumn<Page,String> linkPuntantiCol = createLINKPUNTANTICOL(stagePuntanti, noExtraPuntanti);

        table.getColumns().setAll(urlCol, dominioCol, esitoCol, linkInterniCol, linkPuntantiCol);

        return table;
    }


    /** Metodo che ritorna una Callback, la quale a sua volta
     * prende una TableColumn come parametro e ritorna una TableCell.
     * Il metodo è stato scritto per essere passato al metodo setCellFactory della classe TableColumn,
     * che prende una Callback come parametro, la quale determinerà il comportamento delle celle della TableColumn
     * su cui viene chiamato setCellFactory (dato che rimpiazzerà il valore della property cellFactory).
     * @param consumer un Consumer che prende un MouseEvent. Esso determinerà l'azione svolta al click su una cella.
     * @param disattiva se true disattiva le celle della colonna.
     * @param opacity setta l'opacità delle celle.
     * @return una Callback, che a sua volta ritorna una TableCell sulla cui base vengono create
     * tutte le celle della TableColumn. */
    private static Callback<TableColumn<Page, String>, TableCell<Page, String>> cellFactory(Consumer<MouseEvent> consumer, boolean disattiva, double opacity){

        return new Callback<TableColumn<Page, String>, TableCell<Page, String>>() {

            @Override
            public TableCell<Page, String> call(TableColumn p) {

                TableCell<Page, String> cell = new TableCell<Page, String>() {

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? null : getString());
                        setGraphic(null);
                    }

                    private String getString() {
                        return getItem() == null ? "" : getItem();
                    }
                };
                cell.setOpacity(opacity);
                cell.setDisable(disattiva);
                cell.addEventHandler(MouseEvent.MOUSE_CLICKED, consumer::accept);

                return cell;
            }
        };
    }


    /** ritorna una TableView di oggetti Page, chiamando
     * {@link TableFactory#createTable(ObservableList, boolean, Stage, Stage)}
     * con i parametri che gli vengono passati.
     * @return una TableView di oggetti Page */
    public static TableView<Page> getTable(boolean b, ObservableList<Page> list, Stage stage, Stage stagePuntanti) {
        if (b){
            return createTable(list, true, stage, stagePuntanti);
        } else{
            return createTable(list, false, stage, stagePuntanti);
        }
    }

    /** Crea una colonna d url, con la StringProperty "url" degli oggetti Page.
     * Setta la cellFactory in modo che se una cella viene cliccata
     * visualizza la pagina web a cui punta l'url (trasformato in stringa) al suo interno.
     * Lo stage per la visualizzazione della pagina è {@link wsa.gui.Main#browser}.
     * @param prefWidth la larghezza della colonna
     * @return una TableColumn di stringhe che rappresentano url */
    private static TableColumn<Page, String> createURLCOL(int prefWidth){

        TableColumn<Page,String> urlCol = new TableColumn<>("Url");

        urlCol.setCellFactory(cellFactory(mouseEvent -> {
            if (mouseEvent.getClickCount() == 1) {
                TableCell c = (TableCell) mouseEvent.getSource();
                if (c.getText() == null) return;
                Main.browser.show();
                Main.we.load(c.getText());
            }
        }, false, 1));
        urlCol.setCellValueFactory(new PropertyValueFactory("url"));
        if (prefWidth != 0) urlCol.setPrefWidth(prefWidth);

        return urlCol;
    }

    /** Crea una colonna le cui celle mostrano se la pagina è interna o esterna al dominio.
     * Le celle visualizzano la StringProperty "internoEsternoDominio" degli oggetti Page.
     * Setta la cellFactory in modo che le celle siano disabilitate (quindi non selezionabili)
     * e con un opacità un po' più bassa.
     * @param prefWidth la larghezza della colonna
     * @return una TableColumn di stringhe che rappresentano l'esito dello scaricamento di una pagina web */
    private static TableColumn<Page, String> createDominioCOL(int prefWidth){
        TableColumn<Page,String> dominioCol = new TableColumn<>("Dominio");
        dominioCol.setStyle( "-fx-alignment: CENTER;");
        dominioCol.setCellValueFactory(new PropertyValueFactory("internoEsternoDominio"));
        dominioCol.setCellFactory(cellFactory(mouseEvent -> {
        }, true, 0.6));
        dominioCol.setPrefWidth(prefWidth);
        return dominioCol;
    }

    /** Crea una colonna le cui celle mostrano l'esito dello scaricamento di una pagina web.
     * Le celle visualizzano la StringProperty "esito" degli oggetti Page.
     * Setta la cellFactory in modo che le celle siano disabilitate (quindi non selezionabili)
     * e con un opacità un po' più bassa.
     * @param prefWidth la larghezza della colonna
     * @return una TableColumn di stringhe che rappresentano l'esito dello scaricamento di una pagina web */
    private static TableColumn<Page, String> createESITOCOL(int prefWidth){
        TableColumn<Page,String> esitoCol = new TableColumn<>("Esito");
        esitoCol.setStyle( "-fx-alignment: CENTER;");
        esitoCol.setCellValueFactory(new PropertyValueFactory("esito"));
        esitoCol.setCellFactory(cellFactory(mouseEvent -> {}, true, 0.6));
        esitoCol.setPrefWidth(prefWidth);
        return esitoCol;
    }

    /** Crea una colonna le cui celle mostrano il numero di link estratti da una pagina web.
     * Le celle visualizzano la StringProperty "numeroLinkInterni" degli oggetti Page.
     * Setta la cellFactory in modo che se una cella viene cliccata
     * visualizza un altra TableView di oggetti Page che mostra gli url estratti dalla pagina, e
     * se essi sono stati scaricati con successo oppure no.
     * @param listone ObservableList di Page associata alla TableView che contiene la colonna. Viene utilizzata
     * per controllare se i link all'interno della pagina sono stati scaricati con successo, hanno incontrato errori,
     * o devono essere ancora scaricati
     * @param stageEstratti lo Stage utilizzato per visualizzare la TableView contenente i link estratti dalla pagina
     * @return una TableColumn le cui celle mostrano il numero di link estratti da una pagina web */
    private static TableColumn<Page, String> createLINKINTERNICOL(ObservableList<Page> listone, Stage stageEstratti ){
        TableColumn<Page,String> linkInterniCol = new TableColumn<>("Link estratti");
        linkInterniCol.setPrefWidth(75);
        linkInterniCol.setStyle( "-fx-alignment: CENTER;");

        linkInterniCol.setSortable(true);
        linkInterniCol.setComparator((o1, o2) -> {
            if(Integer.parseInt(o1)<Integer.parseInt(o2))return -1;
            if(Integer.parseInt(o1)>Integer.parseInt(o2))return 1;
            return 0;
        });

        ObservableList<Page> piccoloListoneEstratti = FXCollections.observableArrayList();

        TableView<Page> tabella = new TableView<>();
        tabella.setItems(piccoloListoneEstratti);

        TableColumn<Page,String> urlEstrattiCol = createURLCOL(350);

        TableColumn<Page,String> scaricataCol = createESITOCOL(150);

        tabella.getColumns().setAll(urlEstrattiCol, scaricataCol);

        Scene sceneTabella1 = new Scene(tabella, 500, 400);
        stageEstratti.setScene(sceneTabella1);

        linkInterniCol.setCellFactory(cellFactory(mouseEvent -> {
            if (mouseEvent.getClickCount() == 1) {
                TableCell c = (TableCell) mouseEvent.getSource();
                Page pagina = (Page) c.getTableRow().getItem();
                if (pagina == null) return;
                List<String> uriEstratti = pagina.getUriEstratti();
                if (pagina.getUriEstratti().size() == 0) return;
                piccoloListoneEstratti.clear();
                stageEstratti.show();
                boolean paginaAggiunta = false;
                for (String s : uriEstratti) {
                    for (Page page : listone) {
                        if (s.equals(page.getUrl())) {
                            piccoloListoneEstratti.add(page);
                            paginaAggiunta = true;
                        }
                    }
                    if (!paginaAggiunta) {
                        Page p;
                        if (pagina.getUriEstrattiErrati().contains(s)) p = new Page(s, true);
                        else p = new Page(s, false);
                        piccoloListoneEstratti.add(p);
                    }
                    paginaAggiunta = false;
                }
            }
        }, false, 1));
        linkInterniCol.setCellValueFactory(new PropertyValueFactory("numeroLinkInterni"));
        return linkInterniCol;
    }

    /** Crea una colonna le cui celle mostrano il numero di link che puntano a una pagina web.
     * Le celle visualizzano la StringProperty "numeroLinkPuntanti" degli oggetti Page.
     * @param noExtraPuntanti se true la cellFactory viene settata in modo che le celle siano
     * disabilitate (quindi non selezionabili) e con un opacità un po' più bassa.
     * Se false, la cellFactory viene settata in modo che se una cella viene cliccata
     * visualizza un altra TableView di oggetti Page che mostra gli url che puntano alla pagina.
     * @param stagePuntanti lo Stage utilizzato per visualizzare la TableView contenente i link che puntano pagina
     * @return una TableColumn le cui celle mostrano il numero di link che puntano a una pagina web */
    private static TableColumn<Page, String> createLINKPUNTANTICOL(Stage stagePuntanti, boolean noExtraPuntanti){
        TableColumn<Page, String> linkPuntantiCol = new TableColumn<>("Link puntanti");
        linkPuntantiCol.setStyle( "-fx-alignment: CENTER;");

        linkPuntantiCol.setSortable(true);
        linkPuntantiCol.setComparator((o1, o2) -> {
            if (Integer.parseInt(o1) < Integer.parseInt(o2)) return -1;
            if (Integer.parseInt(o1) > Integer.parseInt(o2)) return 1;
            return 0;
        });

        linkPuntantiCol.setCellValueFactory(new PropertyValueFactory("numeroLinkPuntanti"));
        linkPuntantiCol.setCellFactory(cellFactory(mouseEvent1 -> {
        }, true, 0.6));

        if (!noExtraPuntanti){
            TableView<Page> tabella2 = new TableView<>();
            ObservableList<Page> piccoloListonePuntanti = FXCollections.observableArrayList();
            tabella2.setItems(piccoloListonePuntanti);

            Scene scenaTabella2 = new Scene(tabella2, 500, 400);
            stagePuntanti.setScene(scenaTabella2);

            TableColumn<Page,String> urlPuntantiCol = createURLCOL(500);
            tabella2.getColumns().add(urlPuntantiCol);

            linkPuntantiCol.setCellFactory(cellFactory(mouseEvent -> {
                if (mouseEvent.getClickCount() == 1) {
                    TableCell c = (TableCell) mouseEvent.getSource();
                    Page pagina = (Page) c.getTableRow().getItem();
                    if (pagina == null) return;
                    if (pagina.getUriPuntanti().size() == 0) return;
                    piccoloListonePuntanti.clear();
                    stagePuntanti.show();
                    Set<URI> uriPuntanti = pagina.getUriPuntanti().stream().collect(Collectors.toSet());
                    uriPuntanti.stream().forEach(u -> {
                        Page page = new Page(u.toString());
                        piccoloListonePuntanti.add(page);
                    });
                }
            }, false, 1));
        }

        return linkPuntantiCol;
    }

}
