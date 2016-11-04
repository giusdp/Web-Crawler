package wsa.web;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Created by gius on 05/08/2015. */
public class testCrawl {

    public static void main(String[] args){

        try {

            URI dom = new URI("file:///Users/RIK/DIDATTICA/METODOLOGIEPROG2014_15/Esami/Homeworks" +
                    "/hw3_files/testSiteCrawler/pages/SiteDir/");
            URI seed = new URI("file:///Users/RIK/DIDATTICA/METODOLOGIEPROG2014_15/Esami/Homeworks/hw3_files/testSiteCrawler/pages/SiteDir/p0001.html");
            Path dir = Paths.get("C:\\Users\\gius\\Desktop\\Crawling 1");

            SiteCrawler siteCrawler = WebFactory.getSiteCrawler(dom, dir);

            siteCrawler.addSeed(seed);
            siteCrawler.start();
        } catch (Exception e){
            e.printStackTrace();
        }

        /*
        InputStream inputStream = Files.newInputStream(dir);
            ObjectInputStream obin = new ObjectInputStream(inputStream);
            Set<URI> loaded = (Set<URI>) obin.readObject();
            Set<URI> errs = (Set<URI>) obin.readObject();
            URI dominio = (URI) obin.readObject();
            List<CrawlerResult> map = (List<CrawlerResult>) obin.readObject();
            System.out.println("loaded size: " + loaded.size());
            System.out.println("errs size: " + errs.size());
            System.out.println("dominio: " + dominio);
            System.out.println("cralerResults size: " + map.size());
            obin.close();
            inputStream.close();
         */

    }
}
