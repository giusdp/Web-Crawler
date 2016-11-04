package wsa.gui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import wsa.web.TabCrawler;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

/** Una classe per la creazione e la visualizzazione di due BarCharts
 * Essi mostrano l'andamento del numero dei link uscenti o entranti delle pagine visitate.
 * I link uscenti da una pagina sono i link estratti che puntano ad altre pagine. I link entranti in una pagina
 * sono i link che puntano ad essa.
 * Il BarChart dei link uscenti raggruppa il numero di link uscenti in gruppi di 5 e mostra
 * l'andamento della distribuzione che per ogni k = 0,1,2,... da la percentuale di pagine del dominio
 * che hanno un numero di link uscenti compreso tra 5*k e 5*k + 4.
 * Stessa cosa per il BarChart dei link entranti */
public class Grafici {

    private final Stage stage = new Stage();
    private final int intervallo = 5;
    private int totalePagineScaricate;
    private Map<Integer, Float> oldMappaEntranti, mappaEntranti, oldMappaUscenti, mappaUscenti;
    private TabCrawler tabCrawler;

    /** Costruttore.
     * Crea i Grafici e li aggiunge a {@link Grafici#stage}.
     * @param tabCrawler viene passato a {@link Grafici#tabCrawler} per accedere alla lista di Page
     * {@link wsa.web.objects.TabCrawlerObject#listone} aggiornata */
    public Grafici(TabCrawler tabCrawler){
        this.tabCrawler = tabCrawler;
        stage.setTitle("Grafici");
        prepareMaps();
        prepareBarChart();
    }

    /** Aggiorna i grafici con le attuali pagine scaricate */
    private void update(){
        prepareMaps();
        if (oldMappaEntranti.equals(mappaEntranti) && oldMappaUscenti.equals(mappaUscenti)) return;
        prepareBarChart();
    }

    /** Crea una mappa per costruire il grafico dei link uscenti e una per il grafico dei link entranti e le passa
     * a {@link Grafici#mappaUscenti} e {@link Grafici#mappaEntranti}. Le due mappe contengono come valore il numero
     * di pagine che hanno un numero di link entranti/uscenti appartenente all'intervallo specificato nella chiave (un
     * intero k, che specifica intervalli da 5k a 5k+4).
     * Esempio per la mappa dei link uscenti: se un elemento ha chiave 5 e valore 1 vuol dire che 5 pagine hanno fra 5 e 9
     * link uscenti.
     * Se {@link Grafici#mappaUscenti} e {@link Grafici#mappaEntranti} già contenevano una mappa, vengono sovrascritti,
     * e le vecchie mappe vengono messe in {@link Grafici#oldMappaUscenti} e {@link Grafici#oldMappaEntranti} */
    private void prepareMaps(){
        totalePagineScaricate = 0;
        oldMappaEntranti = mappaEntranti;
        oldMappaUscenti = mappaUscenti;
        mappaEntranti = new TreeMap<>();
        mappaUscenti = new TreeMap<>();
        tabCrawler.getObservableList().stream().forEach(page -> {
            totalePagineScaricate++;

            if (!mappaEntranti.containsKey(page.getUriPuntanti().size() / intervallo))
                mappaEntranti.put(page.getUriPuntanti().size() / intervallo, 1f);
            else
                mappaEntranti.replace(page.getUriPuntanti().size() / intervallo, mappaEntranti.get(page.getUriPuntanti().size() / intervallo) + 1);

            if (page.getUriEstratti().size() != 0) {

                int contatoreUscenti = 0;
                for (URI uri : page.getUriEstrattiCorretti())
                    if (!uri.equals(page.getURI())) contatoreUscenti++;

                if (mappaUscenti.containsKey(contatoreUscenti / intervallo)) {
                    mappaUscenti.replace(contatoreUscenti / intervallo, mappaUscenti.get(contatoreUscenti / intervallo) + 1);
                } else {
                    mappaUscenti.put(contatoreUscenti / intervallo, 1f);
                }
            }
        });
    }

    /** Crea i BarChart e setta {@link Grafici#stage} con la Scene che li contiene */
    private void prepareBarChart(){
        HBox hbox = new HBox(10, getBarChart(mappaEntranti, "Entranti", "navy"), getBarChart(mappaUscenti, "Uscenti", "teal"));
        Scene scene = new Scene(hbox);
        stage.setScene(scene);
    }

    /** Crea un BarChart.
     * @param mappa una Map del tipo di quelle descritte nel javadoc di {@link Grafici#prepareMaps()}
     * @param info le informazioni sul grafico che saranno visualizzate passandoci sopra con il mouse
     * @param color il colore delle barre del grafico */
    private BarChart<String, Number> getBarChart(Map<Integer, Float> mappa, String info, String color){
        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        final BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        final Tooltip tip = new Tooltip("Percentuale di pagine con link "+info.toLowerCase()+" compresi tra 5k e 5k+4");
        Tooltip.install(barChart, tip);

        barChart.setTitle(info);
        barChart.setStyle("-fx-bar-fill: -fx-achieved;");

        xAxis.setLabel("Numero di link " + info.toLowerCase());
        yAxis.setLabel("% sulle pagine visitate");

        XYChart.Series series = new XYChart.Series();
        series.setName(info);
        mappa.forEach((key, value) -> {
            final XYChart.Data<String, Number> data = new XYChart.Data(String.valueOf(((key + 1) * intervallo) - intervallo) +
                    " - " + String.valueOf((key + 1) * intervallo - 1), (value / totalePagineScaricate) * 100);
            data.nodeProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    if (data.getYValue().intValue() >= 0) {
                        newValue.setStyle("-fx-bar-fill: " + color + ";");
                    }
                }
            });

            series.getData().add(data);
        });

        barChart.setLegendVisible(false);

        barChart.getData().addAll(series);

        return barChart;
    }

    /** Visualizza i grafici e inizia ad aggiornarli */
    public void showGraphs(){
        stage.show();
        newThread().start();
    }

    /** Chiude la finestra dei grafici */
    public void closeGraphs(){
        stage.close();
    }

    /** Ritorna un thread che aggiorna i grafici ogni 2.5 secondi */
    private Thread newThread(){
        Thread thread = new Thread(() -> {
            while (stage.isShowing()) {
                if (tabCrawler.getSiteCrawler().isRunning()) {
                    try {
                        Thread.sleep(2500);
                        Platform.runLater(this::update);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        return thread;

    }

}
